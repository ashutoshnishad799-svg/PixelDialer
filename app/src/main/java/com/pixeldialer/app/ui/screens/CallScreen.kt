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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.telecom.AudioRoute
import com.pixeldialer.app.telecom.RecordingMode
import com.pixeldialer.app.ui.theme.DialerPalette
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class CallUiState { CONNECTING, RINGING, ACTIVE, ON_HOLD, ENDED }

@Composable
fun CallScreen(
    callerName: String,
    callerNumber: String,
    isSavedContact: Boolean = false,
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
    val palette = LocalDialerPalette.current

    var state by remember { mutableStateOf(CallUiState.CONNECTING) }
    var seconds by remember { mutableStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(64.dp))

            // Scalloped/flower-shaped avatar background — matches the
            // reference call screen. When the number isn't a saved
            // contact, this shows a plain person glyph rather than any
            // digit from the number, since a leading digit read as a
            // "name initial" was confusing/wrong for unsaved numbers.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ScallopedAvatar(
                    name = callerName,
                    isSavedContact = isSavedContact,
                    onHold = onHold,
                    palette = palette
                )
            }

            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                RecordingWaveformBar(seconds = seconds, mode = recordingMode)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        state == CallUiState.CONNECTING -> "calling…"
                        onHold -> "on hold"
                        else -> formatDuration(seconds)
                    },
                    fontSize = 15.sp,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isSavedContact) callerName else callerNumber,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = showKeypad,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                InCallKeypad(palette = palette, onDigit = { })
            }

            AnimatedVisibility(
                visible = showMore,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                MoreActionsSheet(
                    palette = palette,
                    canMerge = canMerge,
                    recordingAvailable = recordingAvailable,
                    isRecording = isRecording,
                    availableAudioRoutes = availableAudioRoutes,
                    currentAudioRoute = currentAudioRoute,
                    onToggleRecording = onToggleRecording,
                    onSelectAudioRoute = onSelectAudioRoute,
                    onMerge = onMerge,
                    onDismiss = { showMore = false }
                )
            }

            // 4-button row (Keypad / Mute / Speaker / More) — matches the
            // reference screenshot's control row instead of the previous
            // 3x3 grid.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallControlButton(Icons.Filled.Dialpad, "Keypad", active = showKeypad, palette = palette) {
                    showKeypad = !showKeypad
                    if (showKeypad) showMore = false
                }
                CallControlButton(Icons.Filled.MicOff, "Mute", active = muted, palette = palette) { muted = !muted }
                CallControlButton(
                    icon = iconForRoute(currentAudioRoute),
                    label = if (currentAudioRoute == AudioRoute.SPEAKER) "Speaker" else "Audio",
                    active = currentAudioRoute == AudioRoute.SPEAKER,
                    palette = palette
                ) {
                    val next = if (currentAudioRoute == AudioRoute.SPEAKER) AudioRoute.EARPIECE else AudioRoute.SPEAKER
                    onSelectAudioRoute(next)
                }
                CallControlButton(Icons.Filled.MoreHoriz, "More", active = showMore, palette = palette) {
                    showMore = !showMore
                    if (showMore) showKeypad = false
                }
            }

            Spacer(Modifier.height(8.dp))

            // Small pill-shaped end-call button, matching the reference —
            // a compact horizontal capsule rather than the previous large
            // circular button.
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .width(96.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFE53E3E))
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp).rotate(135f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScallopedAvatar(
    name: String,
    isSavedContact: Boolean,
    onHold: Boolean,
    palette: DialerPalette
) {
    Box(
        modifier = Modifier
            .size(180.dp)
            .scale(if (onHold) 0.95f else 1f)
            .drawScallopBackground(palette.avatarBackground),
        contentAlignment = Alignment.Center
    ) {
        if (isSavedContact) {
            Text(
                text = name.take(1).uppercase(),
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(76.dp)
            )
        }
    }
}

/** Draws a scalloped (flower/cloud-edge) circular background behind the avatar, matching the reference call screen. */
private fun Modifier.drawScallopBackground(color: Color): Modifier = this.then(
    Modifier.drawWithCache {
        val path = buildScallopPath(size.width, size.height, petals = 16)
        onDrawBehind {
            drawPath(path, color = color)
        }
    }
)

private fun buildScallopPath(width: Float, height: Float, petals: Int): Path {
    val path = Path()
    val cx = width / 2f
    val cy = height / 2f
    val outerR = width / 2f
    val step = (2 * Math.PI / petals).toFloat()

    for (i in 0 until petals) {
        val angle = i * step
        val midAngle = angle + step / 2f
        val outerX = cx + outerR * cos(angle)
        val outerY = cy + outerR * sin(angle)
        val bulgeX = cx + (outerR * 1.04f) * cos(midAngle)
        val bulgeY = cy + (outerR * 1.04f) * sin(midAngle)
        val nextX = cx + outerR * cos(angle + step)
        val nextY = cy + outerR * sin(angle + step)

        if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)
        path.quadraticBezierTo(bulgeX, bulgeY, nextX, nextY)
    }
    path.close()
    return path
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
            .background(Color.Black.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(0xFFE53E3E), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(text = formatDuration(seconds), color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun InCallKeypad(palette: DialerPalette, onDigit: (Char) -> Unit) {
    val rows = listOf("123", "456", "789", "*0#")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.cardBackground)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    IconButton(onClick = { onDigit(digit) }, modifier = Modifier.size(46.dp)) {
                        Text(digit.toString(), color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreActionsSheet(
    palette: DialerPalette,
    canMerge: Boolean,
    recordingAvailable: Boolean,
    isRecording: Boolean,
    availableAudioRoutes: List<AudioRoute>,
    currentAudioRoute: AudioRoute,
    onToggleRecording: () -> Unit,
    onSelectAudioRoute: (AudioRoute) -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.cardBackground)
            .padding(vertical = 8.dp)
    ) {
        MoreActionRow(Icons.Filled.NoteAlt, "Note", palette) { onDismiss() }
        MoreActionRow(
            icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
            label = if (isRecording) "Stop recording" else "Record call",
            palette = palette,
            enabled = recordingAvailable,
            onClick = { onToggleRecording(); onDismiss() }
        )
        if (canMerge) {
            MoreActionRow(Icons.Filled.CallMerge, "Merge calls", palette) { onMerge(); onDismiss() }
        } else {
            MoreActionRow(Icons.Filled.PersonAdd, "Add call", palette) { onDismiss() }
        }
        MoreActionRow(Icons.Filled.Videocam, "Video call", palette, enabled = false) { }
        if (availableAudioRoutes.size > 2) {
            availableAudioRoutes.forEach { route ->
                MoreActionRow(
                    icon = iconForRoute(route),
                    label = routeLabel(route),
                    palette = palette,
                    trailing = if (route == currentAudioRoute) "✓" else null,
                    onClick = { onSelectAudioRoute(route); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun MoreActionRow(
    icon: ImageVector,
    label: String,
    palette: DialerPalette,
    enabled: Boolean = true,
    trailing: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = interactionSource,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, fontSize = 15.sp, color = palette.accent, fontWeight = FontWeight.Bold)
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
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    palette: DialerPalette,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) palette.accent else palette.cardBackground)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Color.White else palette.textPrimary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
