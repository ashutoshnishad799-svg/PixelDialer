package com.pixeldialer.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.R

/**
 * Shows the incoming/ongoing-call notification with a full-screen intent.
 *
 * Why this exists: simply calling startActivity(InCallActivity) from
 * PixelInCallService.onCallAdded() works when the app is already in the
 * foreground, but silently does nothing when the screen is off or the app
 * is backgrounded — Android 10+ blocks activity starts from a background
 * service. A full-screen-intent notification is the system-sanctioned way
 * to reliably wake the screen and launch the call UI, which is why
 * WhatsApp/Truecaller/every real dialer uses this same pattern.
 */
object CallNotificationHelper {

    private const val CHANNEL_ID = "incoming_call_channel"
    private const val NOTIFICATION_ID = 7001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows the full-screen incoming call UI"
            setSound(null, null) // ringtone is handled by the Telecom framework itself
            enableVibration(false) // ditto — avoid double vibration
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun showIncomingCallNotification(context: Context, callerName: String, callerNumber: String) {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = CallActionReceiver.answerIntent(context)
        val declineIntent = CallActionReceiver.declineIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(callerName)
            .setContentText(callerNumber)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(NotificationCompat.Action(0, "Decline", declineIntent))
            .addAction(NotificationCompat.Action(0, "Answer", answerIntent))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun showOngoingCallNotification(context: Context, callerName: String) {
        ensureChannel(context)

        val contentIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PixelDialerApp.CHANNEL_ONGOING_CALL)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle("Ongoing call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun clear(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }
}
