package com.pixeldialer.app.telecom

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.pixeldialer.app.PixelDialerApp
import com.pixeldialer.app.data.db.CallDirection
import com.pixeldialer.app.ui.screens.CallScreen
import com.pixeldialer.app.ui.screens.IncomingCallScreen
import com.pixeldialer.app.ui.theme.PixelDialerTheme
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PixelDialerApp

        setContent {
            val themeId by app.themePreference.themeIdFlow.collectAsState(initial = "gradient")
            var call by remember { mutableStateOf(PixelInCallService.currentCall) }

            DisposableEffect(Unit) {
                val listener: (Call?) -> Unit = { updated -> call = updated }
                PixelInCallService.addCallListener(listener)
                onDispose { PixelInCallService.removeCallListener(listener) }
            }

            // No active call left — the InCallService already tore this down; close the screen.
            LaunchedEffect(call) {
                if (call == null) finish()
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
                                onEndCall = {
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
