package com.pixeldialer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pixeldialer.app.PixelDialerApp

class ViewModelFactory(private val app: PixelDialerApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                app.callLogRepository,
                app.systemCallLogRepository,
                app.contactsRepository,
                app.themePreference,
                app.appSettingsRepository,
                app.authRepository,
                app.cloudBackupRepository,
                app.database.blockedNumberDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
