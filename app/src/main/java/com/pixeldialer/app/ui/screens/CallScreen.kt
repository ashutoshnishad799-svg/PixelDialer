package com.pixeldialer.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class CallUiState { CONNECTING, RINGING, ACTIVE, ON_HOLD, ENDED }

@Composable
fun CallScreen(
    callerName: String,
    callerNumber: String,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(CallUiState.CONNECTING) }
    var seconds by remember { mutableStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1800)
        state = CallUiState.ACTIVE
    }

    LaunchedEffect(state) {
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
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
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
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(callerName, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        state == CallUiState.CONNECTING -> "calling…"
                        onHold -> "on hold"
                        else -> formatDuration(seconds)
                    },
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CallActionButton(Icons.Filled.MicOff, "mute", active = muted) { muted = !muted }
                    CallActionButton(Icons.Filled.Dialpad, "keypad", active = false) { }
                    CallActionButton(Icons.Filled.VolumeUp, "speaker", active = speaker) { speaker = !speaker }
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CallActionButton(Icons.Filled.PersonAdd, "add call", active = false) { }
                    CallActionButton(Icons.Filled.Videocam, "video", active = false) { }
                    CallActionButton(Icons.Filled.PauseCircle, "hold", active = onHold) { onHold = !onHold }
                }

                Spacer(Modifier.height(30.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = "End call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp).rotate(135f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color.White.copy(alpha = 0.16f))
        ) {
            Icon(icon, contentDescription = label, tint = if (active) Color.Black else Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
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
