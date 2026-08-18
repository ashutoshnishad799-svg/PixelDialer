package com.pixeldialer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.pixeldialer.app.data.AppSettingsRepository
import com.pixeldialer.app.data.AuthRepository
import com.pixeldialer.app.data.CallLogRepository
import com.pixeldialer.app.data.CloudBackupRepository
import com.pixeldialer.app.data.ContactsRepository
import com.pixeldialer.app.data.SystemCallLogRepository
import com.pixeldialer.app.data.ThemePreference
import com.pixeldialer.app.data.db.PixelDialerDatabase
import com.pixeldialer.app.telecom.CallNotificationHelper
import com.pixeldialer.app.telecom.CallRecorder

class PixelDialerApp : Application() {

    lateinit var database: PixelDialerDatabase
        private set
    lateinit var callLogRepository: CallLogRepository
        private set
    lateinit var systemCallLogRepository: SystemCallLogRepository
        private set
    lateinit var contactsRepository: ContactsRepository
        private set
    lateinit var themePreference: ThemePreference
        private set
    lateinit var appSettingsRepository: AppSettingsRepository
        private set
    lateinit var callRecorder: CallRecorder
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var cloudBackupRepository: CloudBackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = PixelDialerDatabase.getInstance(this)
        callLogRepository = CallLogRepository(database.callLogDao())
        systemCallLogRepository = SystemCallLogRepository(this)
        contactsRepository = ContactsRepository(this)
        themePreference = ThemePreference(this)
        appSettingsRepository = AppSettingsRepository(this)
        callRecorder = CallRecorder(this)
        authRepository = AuthRepository(this)
        cloudBackupRepository = CloudBackupRepository()
        createNotificationChannels()
        // Pre-create the incoming-call channel too, so it isn't a first-call cost.
        CallNotificationHelper.createChannels(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ONGOING_CALL,
                    "Ongoing call",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Shows the active call notification" }
            )
        }
    }

    companion object {
        const val CHANNEL_ONGOING_CALL = "ongoing_call_channel"
    }
}
