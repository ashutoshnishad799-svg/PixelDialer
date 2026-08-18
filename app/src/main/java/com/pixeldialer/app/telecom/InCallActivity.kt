package com.pixeldialer.app.telecom

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.data.AppSettings
import com.pixeldialer.app.data.db.CallDirection
import com.pixeldialer.app.ui.screens.CallScreen
import com.pixeldialer.app.ui.screens.IncomingCallScreen
import com.pixeldialer.app.ui.theme.PixelDialerTheme
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenAndWakeFlags()

        val app = application as PixelDialerApp

        setContent {
            val themeId by app.themePreference.themeIdFlow.collectAsState(initial = "gradient")
            val settings by app.appSettingsRepository.settingsFlow.collectAsState(initial = AppSettings())

            var call by remember { mutableStateOf(PixelInCallService.currentCall) }
            var hasSecondCall by remember { mutableStateOf(PixelInCallService.hasMultipleCalls) }
            var isRecording by remember { mutableStateOf(false) }
            var recordingMode by remember { mutableStateOf<RecordingMode?>(null) }

            val audioRouteController = remember { AudioRouteController(this@InCallActivity) }
            var availableRoutes by remember { mutableStateOf(audioRouteController.availableRoutes()) }
            var currentRoute by remember { mutableStateOf(audioRouteController.currentRoute()) }

            DisposableEffect(Unit) {
                val listener: () -> Unit = {
                    call = PixelInCallService.currentCall
                    hasSecondCall = PixelInCallService.hasMultipleCalls
                    // Bluetooth/headset can connect or disconnect mid-call —
                    // re-check available routes on every call-state change
                    // rather than only once at screen open.
                    availableRoutes = audioRouteController.availableRoutes()
                    currentRoute = audioRouteController.currentRoute()
                }
                PixelInCallService.addCallListener(listener)
                onDispose { PixelInCallService.removeCallListener(listener) }
            }

            // No active call left — the InCallService already tore this down; close the screen.
            LaunchedEffect(call) {
                if (call == null) {
                    if (isRecording) {
                        app.callRecorder.stop()
                        isRecording = false
                    }
                    finish()
                }
            }

            fun startRecording(callerLabel: String) {
                val mode = app.callRecorder.start(callerLabel)
                recordingMode = mode
                isRecording = mode != RecordingMode.FAILED
            }

            fun stopRecording() {
                app.callRecorder.stop()
                isRecording = false
                recordingMode = null
            }

            PixelDialerTheme(themeId = themeId) {
                val current = call
                if (current != null) {
                    val number = current.details?.handle?.schemeSpecificPart ?: "Unknown"
                    val displayName = current.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: number

                    when (current.state) {
                        Call.STATE_RINGING -> {
                            IncomingCallScreen(
                                callerName = displayName,
                                callerNumber = number,
                                onAccept = {
                                    current.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                                    logCallAsync(app, number, displayName, CallDirection.INCOMING)
                                    if (settings.autoRecordAll && settings.callRecordingEnabled) {
                                        startRecording(displayName)
                                    }
                                },
                                onDecline = {
                                    current.reject(false, null)
                                    logCallAsync(app, number, displayName, CallDirection.MISSED)
                                    finish()
                                }
                            )
                        }
                        else -> {
                            CallScreen(
                                callerName = displayName,
                                callerNumber = number,
                                canMerge = hasSecondCall,
                                recordingAvailable = settings.callRecordingEnabled,
                                isRecording = isRecording,
                                recordingMode = recordingMode,
                                availableAudioRoutes = availableRoutes,
                                currentAudioRoute = currentRoute,
                                onToggleRecording = {
                                    if (isRecording) stopRecording() else startRecording(displayName)
                                },
                                onSelectAudioRoute = { route ->
                                    audioRouteController.selectRoute(route)
                                    currentRoute = route
                                },
                                onMerge = {
                                    val primary = PixelInCallService.currentCall
                                    val secondary = PixelInCallService.secondaryCall
                                    if (primary != null && secondary != null) primary.conference(secondary)
                                },
                                onSwap = {
                                    val primary = PixelInCallService.currentCall
                                    val secondary = PixelInCallService.secondaryCall
                                    primary?.hold()
                                    secondary?.unhold()
                                },
                                onEndCall = {
                                    if (isRecording) stopRecording()
                                    current.disconnect()
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Modern replacement for the deprecated manifest attributes
     * (showOnLockScreen/showWhenLocked/turnScreenOn) — these runtime flags
     * are what actually get this screen to show up over the lock screen
     * and wake the device reliably on current Android versions.
     */
    private fun setupLockScreenAndWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun logCallAsync(
        app: PixelDialerApp,
        number: String,
        name: String,
        direction: CallDirection
    ) {
        lifecycleScope.launch {
            app.callLogRepository.logCall(number = number, name = name, direction = direction)
        }
    }
}
