package com.pixeldialer.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "pixel_dialer_settings")

data class AppSettings(
    val callRecordingEnabled: Boolean = false,
    val autoRecordAll: Boolean = false,
    val cloudBackupEnabled: Boolean = false
)

class AppSettingsRepository(private val context: Context) {

    private val keyCallRecording = booleanPreferencesKey("call_recording_enabled")
    private val keyAutoRecordAll = booleanPreferencesKey("auto_record_all")
    private val keyCloudBackup = booleanPreferencesKey("cloud_backup_enabled")

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            callRecordingEnabled = prefs[keyCallRecording] ?: false,
            autoRecordAll = prefs[keyAutoRecordAll] ?: false,
            cloudBackupEnabled = prefs[keyCloudBackup] ?: false
        )
    }

    suspend fun setCallRecordingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyCallRecording] = enabled }
    }

    suspend fun setAutoRecordAll(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyAutoRecordAll] = enabled }
    }

    suspend fun setCloudBackupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyCloudBackup] = enabled }
    }
}
