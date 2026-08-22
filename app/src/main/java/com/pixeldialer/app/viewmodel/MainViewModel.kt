package com.pixeldialer.app.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixeldialer.app.data.AppSettingsRepository
import com.pixeldialer.app.data.AuthRepository
import com.pixeldialer.app.data.BackupResult
import com.pixeldialer.app.data.CallLogRepository
import com.pixeldialer.app.data.CloudBackupRepository
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.data.ContactsRepository
import com.pixeldialer.app.data.RecentCall
import com.pixeldialer.app.data.SignInResult
import com.pixeldialer.app.data.SignedInUser
import com.pixeldialer.app.data.SystemCallLogRepository
import com.pixeldialer.app.data.ThemePreference
import com.pixeldialer.app.data.db.BlockedNumberEntity
import com.pixeldialer.app.data.db.CallLogEntity
import com.pixeldialer.app.ui.screens.BackupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val callLogRepository: CallLogRepository,
    private val systemCallLogRepository: SystemCallLogRepository,
    private val contactsRepository: ContactsRepository,
    private val themePreference: ThemePreference,
    private val appSettingsRepository: AppSettingsRepository,
    private val authRepository: AuthRepository,
    private val cloudBackupRepository: CloudBackupRepository,
    private val blockedNumberDao: com.pixeldialer.app.data.db.BlockedNumberDao
) : ViewModel() {

    val themeId: StateFlow<String> = themePreference.themeIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gradient")

    val recents: StateFlow<List<RecentCall>> = callLogRepository.observeGroupedRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missedOnly: StateFlow<List<RecentCall>> = callLogRepository.observeMissed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    val currentUser: StateFlow<SignedInUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings = appSettingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.pixeldialer.app.data.AppSettings())

    private val _backupState = MutableStateFlow(BackupState.IDLE)
    val backupState: StateFlow<BackupState> = _backupState

    private val _lastBackedUpAtMillis = MutableStateFlow(0L)
    val lastBackedUpAtMillis: StateFlow<Long> = _lastBackedUpAtMillis

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = blockedNumberDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun blockNumber(number: String) {
        if (number.isBlank()) return
        viewModelScope.launch { blockedNumberDao.block(BlockedNumberEntity(phoneNumber = number)) }
    }

    fun unblockNumber(entry: BlockedNumberEntity) {
        viewModelScope.launch { blockedNumberDao.unblock(entry) }
    }

    fun setTheme(id: String) {
        viewModelScope.launch {
            themePreference.setTheme(id)
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = contactsRepository.loadAllContacts()
        }
    }

    fun saveNewContact(input: com.pixeldialer.app.ui.components.NewContactInput, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = contactsRepository.insertContact(
                firstName = input.firstName,
                lastName = input.lastName,
                phoneNumber = input.phoneNumber,
                phoneLabel = input.phoneLabel,
                email = input.email
            )
            if (success) loadContacts()
            onDone(success)
        }
    }

    /** Pulls existing device call history into Recents. Call once permissions are granted. */
    fun syncCallHistory() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                callLogRepository.syncFromSystem(systemCallLogRepository)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearCallHistory() {
        viewModelScope.launch { callLogRepository.clearHistory() }
    }

    fun setCallRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setCallRecordingEnabled(enabled) }
    }

    fun setAutoRecordAll(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setAutoRecordAll(enabled) }
    }

    // ── Auth ──────────────────────────────────────────────────────────

    /** Null when Firebase/Google Sign-In isn't configured for this build — caller should show a message instead of launching. */
    fun signInIntent(): Intent? = authRepository.signInIntent()

    fun handleSignInResult(data: Intent?, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.handleSignInResult(data)) {
                is SignInResult.Success -> onDone(true, null)
                is SignInResult.Failure -> onDone(false, result.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            appSettingsRepository.setCloudBackupEnabled(false)
        }
    }

    fun deleteAccount(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid
            if (uid != null) {
                cloudBackupRepository.deleteUserData(uid)
            }
            val result = authRepository.deleteAccount()
            appSettingsRepository.setCloudBackupEnabled(false)
            onDone(result.isSuccess)
        }
    }

    // ── Cloud backup ──────────────────────────────────────────────────

    fun setCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setCloudBackupEnabled(enabled)
            if (enabled) backupNow()
        }
    }

    fun backupNow() {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            _backupState.value = BackupState.IN_PROGRESS
            val callLog: List<CallLogEntity> = callLogRepository.rawEntriesForBackup()
            val blocked: List<BlockedNumberEntity> = blockedNumbers.value
            val result = cloudBackupRepository.backup(uid, callLog, blocked, themeId.value)
            when (result) {
                is BackupResult.Success -> {
                    _backupState.value = BackupState.SUCCESS
                    _lastBackedUpAtMillis.value = System.currentTimeMillis()
                }
                is BackupResult.Failure -> _backupState.value = BackupState.FAILED
            }
        }
    }

    fun restoreFromCloud(onDone: (Boolean) -> Unit) {
        val uid = currentUser.value?.uid ?: run { onDone(false); return }
        viewModelScope.launch {
            val snapshot = cloudBackupRepository.restore(uid)
            if (snapshot != null) {
                callLogRepository.replaceAllFromBackup(snapshot.callLog)
                themePreference.setTheme(snapshot.themeId)
                _lastBackedUpAtMillis.value = snapshot.lastBackedUpAtMillis
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
}
