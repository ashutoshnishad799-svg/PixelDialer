package com.pixeldialer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.telecom.AudioRoute
import com.pixeldialer.app.telecom.RecordingMode
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class CallUiState { CONNECTING, RINGING, ACTIVE, ON_HOLD, ENDED }

@Composable
fun CallScreen(
    callerName: String,
    callerNumber: String,
    canMerge: Boolean = false,
    recordingAvailable: Boolean = false,
    isRecording: Boolean = false,
    recordingMode: RecordingMode? = null,
    availableAudioRoutes: List<AudioRoute> = listOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER),
    currentAudioRoute: AudioRoute = AudioRoute.EARPIECE,
    onToggleRecording: () -> Unit = {},
    onSelectAudioRoute: (AudioRoute) -> Unit = {},
    onMerge: () -> Unit = {},
    onSwap: () -> Unit = {},
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(CallUiState.CONNECTING) }
    var seconds by remember { mutableStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    var showAudioPicker by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1800)
        state = CallUiState.ACTIVE
    }

    LaunchedEffect(state, onHold) {
        while (state == CallUiState.ACTIVE && !onHold) {
            delay(1000)
            seconds++
        }
    }

    val hue = remember { ((callerName.firstOrNull()?.code ?: 65) * 37) % 360 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(hueColor(hue, 0.35f, 0.22f), Color(0xFF0A0A0D)),
                    radius = 900f
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 64.dp).fillMaxWidth().padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(if (onHold) 0.95f else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(hueColor(hue, 0.45f, 0.38f), hueColor(hue + 40, 0.4f, 0.18f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callerName.take(1).uppercase(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(callerName, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        state == CallUiState.CONNECTING -> "calling…"
                        onHold -> "on hold"
                        else -> formatDuration(seconds)
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                RecordingWaveformBar(seconds = seconds, mode = recordingMode)
            }

            AnimatedVisibility(
                visible = showKeypad,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                InCallKeypad(onDigit = { })
            }

            AnimatedVisibility(
                visible = showAudioPicker,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                AudioRoutePicker(
                    routes = availableAudioRoutes,
                    current = currentAudioRoute,
                    onSelect = { route ->
                        onSelectAudioRoute(route)
                        showAudioPicker = false
                    }
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp)) {
                GridRow {
                    GridButton(Icons.Filled.Videocam, "Video call", active = false) { }
                    GridButton(
                        icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        label = "Recording",
                        active = isRecording,
                        enabled = recordingAvailable,
                        highlightColor = Color(0xFFFF3B30),
                        onClick = { if (recordingAvailable) onToggleRecording() }
                    )
                    GridButton(Icons.Filled.Description, "Note", active = showNote) { showNote = !showNote }
                }
                Spacer(Modifier.height(16.dp))
                GridRow {
                    GridButton(Icons.Filled.MicOff, "Mute", active = muted) { muted = !muted }
                    GridButton(Icons.Filled.Pause, "Hold", active = onHold) { onHold = !onHold }
                    if (canMerge) {
                        GridButton(Icons.Filled.CallMerge, "Merge", active = false, onClick = onMerge)
                    } else {
                        GridButton(Icons.Filled.PersonAdd, "Add call", active = false) { }
                    }
                }
                Spacer(Modifier.height(16.dp))
                GridRow {
                    GridButton(
                        icon = iconForRoute(currentAudioRoute),
                        label = routeLabel(currentAudioRoute),
                        active = showAudioPicker,
                        onClick = {
                            if (availableAudioRoutes.size > 2) {
                                showAudioPicker = !showAudioPicker
                            } else {
                                val next = if (currentAudioRoute == AudioRoute.SPEAKER) AudioRoute.EARPIECE else AudioRoute.SPEAKER
                                onSelectAudioRoute(next)
                            }
                        }
                    )
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier.size(62.dp).clip(CircleShape).background(Color(0xFFFF3B30))
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = "End call",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp).rotate(135f)
                            )
                        }
                    }
                    GridButton(Icons.Filled.Dialpad, "Dialpad", active = showKeypad) { showKeypad = !showKeypad }
                }
            }
        }
    }
}

@Composable
private fun GridRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, content = content)
}

@Composable
private fun RecordingWaveformBar(seconds: Int, mode: RecordingMode?) {
    val bars = remember { List(28) { Random.nextFloat() * 0.7f + 0.15f } }
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "waveform-phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF3B30), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(text = formatDuration(seconds), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f).height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEachIndexed { i, base ->
                val animatedHeight = (base + 0.25f * kotlin.math.sin(phase * 6.28f + i * 0.5f)).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight(animatedHeight)
                        .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun InCallKeypad(onDigit: (Char) -> Unit) {
    val rows = listOf("123", "456", "789", "*0#")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    IconButton(onClick = { onDigit(digit) }, modifier = Modifier.size(46.dp)) {
                        Text(digit.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRoutePicker(
    routes: List<AudioRoute>,
    current: AudioRoute,
    onSelect: (AudioRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 8.dp)
    ) {
        routes.forEach { route ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(iconForRoute(route), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text(routeLabel(route), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (route == current) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun iconForRoute(route: AudioRoute): ImageVector = when (route) {
    AudioRoute.EARPIECE -> Icons.Filled.PhoneInTalk
    AudioRoute.SPEAKER -> Icons.Filled.VolumeUp
    AudioRoute.BLUETOOTH -> Icons.Filled.BluetoothAudio
    AudioRoute.WIRED_HEADSET -> Icons.Filled.Headset
}

private fun routeLabel(route: AudioRoute): String = when (route) {
    AudioRoute.EARPIECE -> "Phone"
    AudioRoute.SPEAKER -> "Speaker"
    AudioRoute.BLUETOOTH -> "Bluetooth"
    AudioRoute.WIRED_HEADSET -> "Headset"
}

@Composable
private fun GridButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    highlightColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active && highlightColor != Color.White -> highlightColor.copy(alpha = 0.22f)
                        active -> Color.White
                        !enabled -> Color.White.copy(alpha = 0.06f)
                        else -> Color.White.copy(alpha = 0.14f)
                    }
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = when {
                    active && highlightColor != Color.White -> highlightColor
                    active -> Color.Black
                    !enabled -> Color.White.copy(alpha = 0.3f)
                    else -> Color.White
                }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 10.5.sp,
            color = if (enabled) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.3f),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

private fun hueColor(hue: Int, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360) + 360) % 360
    return Color.hsl(h.toFloat(), saturation, lightness)
}
