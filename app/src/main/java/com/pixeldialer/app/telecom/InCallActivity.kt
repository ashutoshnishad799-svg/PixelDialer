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
            var callState by remember { mutableStateOf(call?.state) }
            var hasSecondCall by remember { mutableStateOf(PixelInCallService.hasMultipleCalls) }
            var isRecording by remember { mutableStateOf(false) }
            var recordingMode by remember { mutableStateOf<RecordingMode?>(null) }

            val audioRouteController = remember { AudioRouteController(this@InCallActivity) }
            var availableRoutes by remember { mutableStateOf(audioRouteController.availableRoutes()) }
            var currentRoute by remember { mutableStateOf(audioRouteController.currentRoute()) }

            DisposableEffect(Unit) {
                val listener: () -> Unit = {
                    call = PixelInCallService.currentCall
                    callState = call?.state
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

            // Safety net alongside the listener above: Call.Callback events
            // are normally reliable, but if one is ever missed (e.g. a
            // rapid ringing→active transition landing between recompositions),
            // this catches the displayed state drifting from the real one
            // within a second rather than leaving the screen stuck showing
            // "ringing" for an already-answered call.
            LaunchedEffect(call) {
                while (call != null) {
                    kotlinx.coroutines.delay(500)
                    val liveState = call?.state
                    if (liveState != null && liveState != callState) {
                        callState = liveState
                    }
                }
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
                    val rawCallerDisplayName = current.details?.callerDisplayName?.takeIf { it.isNotBlank() }
                    val displayName = rawCallerDisplayName ?: number
                    // A non-blank callerDisplayName means Telecom matched this
                    // number to a saved contact — that's what drives whether
                    // the call screen shows a name+initial or a plain number
                    // with a generic person icon (no digit read as an "initial").
                    val isSavedContact = rawCallerDisplayName != null

                    when (callState) {
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
                                    // No manual missed-call log here — PixelInCallService
                                    // already logs missed calls at STATE_DISCONNECTED
                                    // regardless of whether this screen was ever open
                                    // (needed for the screen-on notification-only path,
                                    // where the activity may not exist at all). Logging
                                    // it again here would create a duplicate Recents entry.
                                    finish()
                                }
                            )
                        }
                        else -> {
                            CallScreen(
                                callerName = displayName,
                                callerNumber = number,
                                isSavedContact = isSavedContact,
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
