package com.pixeldialer.app.telecom

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

enum class AudioRoute { EARPIECE, SPEAKER, BLUETOOTH, WIRED_HEADSET }

/**
 * Figures out which audio outputs are actually available right now and
 * routes calls accordingly.
 *
 * The behavior the user asked for: if only earpiece+speaker exist, a
 * single button just toggles between them (no picker needed — there's
 * nothing to pick from). Once a third route shows up (Bluetooth
 * headset/earbuds connected, or a wired headset plugged in), the button
 * opens a picker instead, since now there's an actual choice to make.
 */
class AudioRouteController(private val context: Context) {

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** All routes currently physically available, in a stable display order. */
    fun availableRoutes(): List<AudioRoute> {
        val routes = mutableListOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER)

        if (isWiredHeadsetConnected()) {
            routes.add(AudioRoute.WIRED_HEADSET)
        }
        if (isBluetoothAudioConnected()) {
            routes.add(AudioRoute.BLUETOOTH)
        }
        return routes
    }

    fun currentRoute(): AudioRoute = when {
        isBluetoothScoOn() -> AudioRoute.BLUETOOTH
        audioManager.isSpeakerphoneOn -> AudioRoute.SPEAKER
        isWiredHeadsetConnected() && !audioManager.isSpeakerphoneOn -> AudioRoute.WIRED_HEADSET
        else -> AudioRoute.EARPIECE
    }

    fun selectRoute(route: AudioRoute) {
        when (route) {
            AudioRoute.SPEAKER -> {
                stopBluetoothScoIfNeeded()
                audioManager.isSpeakerphoneOn = true
            }
            AudioRoute.EARPIECE -> {
                stopBluetoothScoIfNeeded()
                audioManager.isSpeakerphoneOn = false
            }
            AudioRoute.WIRED_HEADSET -> {
                stopBluetoothScoIfNeeded()
                audioManager.isSpeakerphoneOn = false
            }
            AudioRoute.BLUETOOTH -> {
                audioManager.isSpeakerphoneOn = false
                startBluetoothSco()
            }
        }
    }

    private fun isWiredHeadsetConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            return devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
        }
        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn
    }

    private fun isBluetoothAudioConnected(): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!adapter.isEnabled) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }
            } else {
                @Suppress("DEPRECATION")
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
            }
        } catch (e: SecurityException) {
            // Missing BLUETOOTH_CONNECT at runtime — treat as "not available" rather than crash.
            false
        }
    }

    private fun isBluetoothScoOn(): Boolean = audioManager.isBluetoothScoOn

    private fun startBluetoothSco() {
        try {
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        } catch (e: Exception) {
            // Some OEM audio stacks throw here if no SCO device is actually
            // ready yet — non-fatal, the call simply stays on its current route.
        }
    }

    private fun stopBluetoothScoIfNeeded() {
        try {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
        } catch (e: Exception) {
        }
    }
}
