package com.pixeldialer.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

private data class Sparkle(
    val id: Int,
    val startOffsetX: Float, // -1..1, horizontal jitter around the puck's centerline
    val sizeFraction: Float, // relative particle size, randomized per-sparkle so the trail doesn't look uniform
    val speedFraction: Float // relative rise speed, randomized so particles don't move in lockstep
)

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hue = ((callerName.firstOrNull()?.code ?: 65) * 37) % 360

    // Drag state for the swipe puck: positive = dragged up (toward accept),
    // negative = dragged down (toward decline). Threshold is a fraction of
    // the available travel distance, computed once layout size is known.
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var travelRangePx by remember { mutableStateOf(1f) }
    val progress = (dragOffsetPx / travelRangePx).coerceIn(-1f, 1f)
    val thresholdFraction = 0.55f

    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f), // slightly stiffer spring = snappier feel
        label = "drag-offset"
    )

    val blurRadius by animateFloatAsState(
        targetValue = if (abs(progress) > 0.05f) 28f else 18f,
        animationSpec = tween(220),
        label = "blur-radius"
    )

    val puckColor by animateColorAsState(
        targetValue = when {
            progress > thresholdFraction -> Color(0xFF34C759)
            progress < -thresholdFraction -> Color(0xFFFF3B30)
            else -> Color.White
        },
        animationSpec = tween(160),
        label = "puck-color"
    )

    // Sparkle particle pool for the "glassy stars rising" trail: as the
    // puck moves upward (progress > 0), new particles spawn near the puck
    // and drift up + fade + shrink over their lifetime. Age is derived
    // from each particle's position in the list rather than owning
    // per-particle animation state, so ~40 live particles stay cheap.
    val sparkles = remember { mutableStateListOf<Sparkle>() }
    var nextSparkleId by remember { mutableStateOf(0) }

    // Emit a new sparkle roughly every ~45ms while actively swiping upward
    // past a small dead-zone.
    LaunchedEffect(progress > 0.08f) {
        if (progress > 0.08f) {
            while (true) {
                sparkles.add(
                    Sparkle(
                        id = nextSparkleId++,
                        startOffsetX = Random.nextFloat() * 2f - 1f,
                        sizeFraction = 0.5f + Random.nextFloat() * 0.5f,
                        speedFraction = 0.7f + Random.nextFloat() * 0.6f
                    )
                )
                if (sparkles.size > 40) sparkles.removeAt(0)
                kotlinx.coroutines.delay(45)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.hsl(hue.toFloat(), 0.3f, 0.16f), Color(0xFF0A0A0D))
                )
            )
    ) {
        // Blurred decorative glow layer behind everything — the "glassy"
        // depth cue requested. InCallActivity is its own opaque window (no
        // real background to capture), so this is a soft radial glow run
        // through a blur rather than a true backdrop blur.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = blurRadius.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(340.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.hsl(hue.toFloat(), 0.5f, 0.3f).copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Incoming call", fontSize = 15.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))

                PulsingAvatar(callerName = callerName, hue = hue)

                Spacer(Modifier.height(20.dp))
                Text(callerName, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(callerNumber, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f))
            }

            // Quick actions row stays as a tap-based fallback alongside the
            // swipe gesture below — not everyone will discover/want the swipe.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAction(Icons.Filled.CallEnd, "Decline", Color(0xFFFF3B30), onDecline)
                QuickAction(Icons.Filled.Message, "Message", Color.White.copy(alpha = 0.16f)) { }
                QuickAction(Icons.Filled.Phone, "Accept", Color(0xFF34C759), onAccept)
            }

            // Swipe-to-answer track: drag the center puck up to accept,
            // down to decline. Crossing the threshold triggers the action;
            // releasing before threshold just springs back to center.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 56.dp, vertical = 12.dp)
                    .onSizeChanged { size ->
                        travelRangePx = (size.height / 2f - 60f).coerceAtLeast(40f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = (0.3f + (progress.coerceAtLeast(0f) * 0.7f))),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(22.dp)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = (0.3f + (-progress.coerceAtMost(0f) * 0.7f))),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(22.dp)
                    )
                }

                // Sparkle trail — a Canvas layer behind the puck, sized to
                // the whole track so particles can rise the full travel
                // distance. Each particle's position/fade is derived from
                // its list index (newest last), avoiding per-particle
                // Animatable state.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackHeightPx = size.height
                    val centerX = size.width / 2f

                    sparkles.forEachIndexed { index, sparkle ->
                        val ageMs = (sparkles.size - index) * 45f
                        val lifetimeMs = 900f / sparkle.speedFraction
                        val lifeFraction = (ageMs / lifetimeMs).coerceIn(0f, 1f)
                        if (lifeFraction >= 1f) return@forEachIndexed

                        val riseDistance = trackHeightPx * 0.55f * sparkle.speedFraction
                        val y = (trackHeightPx / 2f) - (riseDistance * lifeFraction)
                        val x = centerX + sparkle.startOffsetX * 18.dp.toPx()
                        val alpha = (1f - lifeFraction) * 0.85f
                        val radius = (2.5.dp.toPx() + 2.dp.toPx() * sparkle.sizeFraction) * (1f - lifeFraction * 0.4f)

                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(x, y)
                        )
                        // Faint cross-glint on the larger particles for a
                        // "glassy sparkle" look rather than plain dots.
                        if (sparkle.sizeFraction > 0.75f) {
                            drawLine(
                                color = Color.White.copy(alpha = alpha * 0.6f),
                                start = Offset(x - radius * 1.6f, y),
                                end = Offset(x + radius * 1.6f, y),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White.copy(alpha = alpha * 0.6f),
                                start = Offset(x, y - radius * 1.6f),
                                end = Offset(x, y + radius * 1.6f),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (abs(progress) > thresholdFraction) 1.12f else 1f)
                        .offset { IntOffset(0, -animatedDragOffset.toInt()) }
                        .clip(CircleShape)
                        .background(puckColor)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                // Dragging finger up = negative delta in screen
                                // coords; flip sign so "up" reads as positive progress.
                                dragOffsetPx = (dragOffsetPx - delta).coerceIn(-travelRangePx, travelRangePx)
                            },
                            onDragStopped = {
                                when {
                                    progress > thresholdFraction -> onAccept()
                                    progress < -thresholdFraction -> onDecline()
                                }
                                dragOffsetPx = 0f
                                sparkles.clear()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = "Swipe to answer or decline",
                        tint = if (puckColor == Color.White) Color.Black else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                "Swipe up to answer, down to decline",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PulsingAvatar(callerName: String, hue: Int) {
    val transition = rememberInfiniteTransition(label = "avatar-pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse-scale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "ring-alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Expanding ring pulse behind the avatar — reads as "actively ringing".
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(1f + (1f - ringAlpha) * 0.35f)
                .clip(CircleShape)
                .background(Color.hsl(hue.toFloat(), 0.5f, 0.5f).copy(alpha = ringAlpha * 0.4f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color.hsl(hue.toFloat(), 0.45f, 0.38f), Color.hsl((hue + 40).toFloat(), 0.4f, 0.18f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(callerName.take(1).uppercase(), fontSize = 42.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, bgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(60.dp).clip(CircleShape).background(bgColor)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}
