package com.pixeldialer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixeldialer.app.data.CallLogRepository
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.data.ContactsRepository
import com.pixeldialer.app.data.RecentCall
import com.pixeldialer.app.data.SystemCallLogRepository
import com.pixeldialer.app.data.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val callLogRepository: CallLogRepository,
    private val systemCallLogRepository: SystemCallLogRepository,
    private val contactsRepository: ContactsRepository,
    private val themePreference: ThemePreference
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
}
