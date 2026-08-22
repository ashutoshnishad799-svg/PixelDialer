package com.pixeldialer.app.telecom

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles the Answer/Decline buttons on the incoming-call notification
 * without needing to open an Activity first — matches how every stock
 * dialer's notification actions behave.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                ACTION_ANSWER -> {
                    PixelInCallService.currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                }
                ACTION_DECLINE -> {
                    PixelInCallService.currentCall?.reject(false, null)
                    CallNotificationHelper.clear(context)
                }
                ACTION_END -> {
                    PixelInCallService.currentCall?.disconnect()
                    CallNotificationHelper.clear(context)
                }
                ACTION_TOGGLE_SPEAKER -> {
                    val controller = AudioRouteController(context)
                    val current = controller.currentRoute()
                    val next = if (current == AudioRoute.SPEAKER) AudioRoute.EARPIECE else AudioRoute.SPEAKER
                    controller.selectRoute(next)
                    CallNotificationHelper.refreshOngoing(context)
                }
                ACTION_TOGGLE_MUTE -> {
                    CallAudioQuickActions.toggleMute(context)
                    CallNotificationHelper.refreshOngoing(context)
                }
            }
        } catch (e: Exception) {
            // A notification-button tap must never crash the app or the
            // system's notification host process.
            android.util.Log.e("CallActionReceiver", "Action failed: ${intent.action}", e)
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.pixeldialer.app.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.pixeldialer.app.ACTION_DECLINE_CALL"
        const val ACTION_END = "com.pixeldialer.app.ACTION_END_CALL"
        const val ACTION_TOGGLE_SPEAKER = "com.pixeldialer.app.ACTION_TOGGLE_SPEAKER"
        const val ACTION_TOGGLE_MUTE = "com.pixeldialer.app.ACTION_TOGGLE_MUTE"

        fun answerIntent(context: Context): PendingIntent = broadcastFor(context, ACTION_ANSWER, 10)
        fun declineIntent(context: Context): PendingIntent = broadcastFor(context, ACTION_DECLINE, 11)
        fun endIntent(context: Context): PendingIntent = broadcastFor(context, ACTION_END, 12)
        fun toggleSpeakerIntent(context: Context): PendingIntent = broadcastFor(context, ACTION_TOGGLE_SPEAKER, 13)
        fun toggleMuteIntent(context: Context): PendingIntent = broadcastFor(context, ACTION_TOGGLE_MUTE, 14)

        private fun broadcastFor(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, CallActionReceiver::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
