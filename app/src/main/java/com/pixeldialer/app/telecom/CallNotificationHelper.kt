package com.pixeldialer.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.R

/**
 * Shows the incoming/ongoing-call notification with a full-screen intent
 * and a custom glassy layout with direct Speaker/Mute/End buttons for
 * ongoing calls, and Answer/Decline for incoming ones.
 *
 * Why a full-screen intent at all: simply calling startActivity(InCallActivity)
 * from PixelInCallService.onCallAdded() works when the app is already in the
 * foreground, but silently does nothing when the screen is off or the app
 * is backgrounded — Android 10+ blocks activity starts from a background
 * service. A full-screen-intent notification is the system-sanctioned way
 * to reliably wake the screen and launch the call UI, which is why
 * WhatsApp/Truecaller/every real dialer uses this same pattern.
 *
 * Speed note: notification channels are created once in PixelDialerApp.onCreate()
 * rather than being checked/created on every single incoming call — that
 * per-call getNotificationChannel() + createNotificationChannel() round trip
 * was unnecessary work sitting directly in the "call just started ringing"
 * hot path.
 */
object CallNotificationHelper {

    const val CHANNEL_ID = "incoming_call_channel"
    private const val NOTIFICATION_ID = 7001

    private var lastCallerName: String = ""
    private var lastIsIncoming: Boolean = false

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
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
    }

    fun showIncomingCallNotification(context: Context, callerName: String, callerNumber: String) {
        try {
            lastCallerName = callerName
            lastIsIncoming = true

            val views = buildGlassLayout(context, callerName, statusText = "Incoming call", incoming = true)

            val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_notification)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views)
                .setCustomBigContentView(views)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            notify(context, notification)
        } catch (e: Exception) {
            // A notification failing to render must never crash the app —
            // the InCallActivity direct-launch path (also attempted by the
            // caller) is the fallback if this fails for any reason.
            android.util.Log.e("CallNotificationHelper", "Failed to show incoming call notification", e)
        }
    }

    fun showOngoingCallNotification(context: Context, callerName: String) {
        try {
            lastCallerName = callerName
            lastIsIncoming = false

            val views = buildGlassLayout(context, callerName, statusText = "Ongoing call", incoming = false)

            val contentIntent = Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, PixelDialerApp.CHANNEL_ONGOING_CALL)
                .setSmallIcon(R.drawable.ic_call_notification)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views)
                .setCustomBigContentView(views)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            notify(context, notification)
        } catch (e: Exception) {
            android.util.Log.e("CallNotificationHelper", "Failed to show ongoing call notification", e)
        }
    }

    /** Re-renders the ongoing-call notification so Speaker/Mute button highlight state stays in sync after a toggle. */
    fun refreshOngoing(context: Context) {
        if (lastIsIncoming || lastCallerName.isBlank()) return
        showOngoingCallNotification(context, lastCallerName)
    }

    fun clear(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
        lastCallerName = ""
    }

    private fun buildGlassLayout(
        context: Context,
        callerName: String,
        statusText: String,
        incoming: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_call_glass)
        views.setTextViewText(R.id.notif_name, callerName)
        views.setTextViewText(R.id.notif_status, statusText)

        if (incoming) {
            views.setViewVisibility(R.id.notif_actions_row, android.view.View.GONE)
            views.setViewVisibility(R.id.notif_incoming_actions_row, android.view.View.VISIBLE)
            views.setOnClickPendingIntent(R.id.notif_btn_answer, CallActionReceiver.answerIntent(context))
            views.setOnClickPendingIntent(R.id.notif_btn_decline, CallActionReceiver.declineIntent(context))
        } else {
            views.setViewVisibility(R.id.notif_actions_row, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.notif_incoming_actions_row, android.view.View.GONE)
            views.setOnClickPendingIntent(R.id.notif_btn_speaker, CallActionReceiver.toggleSpeakerIntent(context))
            views.setOnClickPendingIntent(R.id.notif_btn_mute, CallActionReceiver.toggleMuteIntent(context))
            views.setOnClickPendingIntent(R.id.notif_btn_end, CallActionReceiver.endIntent(context))

            val speakerOn = AudioRouteController(context).currentRoute() == AudioRoute.SPEAKER
            val muted = CallAudioQuickActions.isMuted(context)
            views.setInt(
                R.id.notif_btn_speaker, "setBackgroundResource",
                if (speakerOn) R.drawable.bg_notification_pill_active else R.drawable.bg_notification_pill_neutral
            )
            views.setInt(
                R.id.notif_btn_mute, "setBackgroundResource",
                if (muted) R.drawable.bg_notification_pill_active else R.drawable.bg_notification_pill_neutral
            )
        }
        return views
    }

    private fun notify(context: Context, notification: Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
