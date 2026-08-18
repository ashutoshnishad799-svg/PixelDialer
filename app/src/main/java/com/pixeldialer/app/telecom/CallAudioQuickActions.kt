package com.pixeldialer.app.telecom

import android.content.Context
import android.media.AudioManager

/**
 * Wraps AudioManager's mic-mute state for the notification's Mute button.
 *
 * Speaker routing intentionally does NOT live here — AudioRouteController
 * is the single source of truth for speaker/earpiece/Bluetooth routing so
 * the notification's speaker button and the in-call screen's audio-route
 * picker never disagree about the current route.
 */
object CallAudioQuickActions {

    fun isMuted(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMicrophoneMute
    }

    fun toggleMute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
    }
}
