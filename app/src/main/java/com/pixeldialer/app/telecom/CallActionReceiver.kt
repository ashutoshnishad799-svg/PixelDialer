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
        when (intent.action) {
            ACTION_ANSWER -> {
                PixelInCallService.currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            }
            ACTION_DECLINE -> {
                PixelInCallService.currentCall?.reject(false, null)
                CallNotificationHelper.clear(context)
            }
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.pixeldialer.app.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.pixeldialer.app.ACTION_DECLINE_CALL"

        fun answerIntent(context: Context): PendingIntent {
            val intent = Intent(context, CallActionReceiver::class.java).apply { action = ACTION_ANSWER }
            return PendingIntent.getBroadcast(
                context, 10, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun declineIntent(context: Context): PendingIntent {
            val intent = Intent(context, CallActionReceiver::class.java).apply { action = ACTION_DECLINE }
            return PendingIntent.getBroadcast(
                context, 11, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
