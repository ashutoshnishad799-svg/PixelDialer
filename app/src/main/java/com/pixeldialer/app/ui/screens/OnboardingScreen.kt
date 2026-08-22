package com.pixeldialer.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardStep(val title: String, val subtitle: String)

private val featureSteps = listOf(
    OnboardStep("All your calls,\nin one place", "Recents, contacts, and dialing — fast and organized, exactly how you'd expect."),
    OnboardStep("Stay ahead\nof spam", "Suspicious and unknown callers get flagged automatically, before they ever reach you."),
    OnboardStep("Make it\nyours", "Pick from gradient, solid, and dark themes — or let it follow your system automatically.")
)

/**
 * Layout design note: every page below occupies exactly ONE weighted slot
 * in the outer Column and never calls fillMaxSize() on its own — the
 * previous version had each page independently fillMaxSize()-ing itself
 * *inside* an already-weighted Box, which let a page's combined content
 * height exceed the space Compose had actually given it. The overflow
 * got compressed and re-laid-out on top of neighboring content, which is
 * what produced the "text and buttons duplicated near the status bar"
 * look in testing. Nothing here sizes itself larger than its assigned slot.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(0) } // 0 = welcome, 1..3 = features, 4 = personalize

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF12615A), Color(0xFF0E3B36))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars) // keeps every child clear of the status bar / nav bar — the missing piece that let content bleed under the clock in testing
        ) {
            // Fixed-height top bar — reserved whether or not Skip is
            // showing, so nothing below it ever shifts position between steps.
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 20.dp)) {
                if (step in 1..3) {
                    Text(
                        "Skip",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).clickable { step = 4 }
                    )
                }
            }

            // The one weighted region — every page renders inside this and
            // only this, sized to whatever's left after the fixed top bar
            // and fixed bottom controls.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    0 -> WelcomePage()
                    in 1..3 -> {
                        val f = featureSteps[step - 1]
                        FeaturePage(title = f.title, subtitle = f.subtitle) { phase ->
                            when (step) {
                                1 -> CallsIllustration(phase)
                                2 -> ShieldIllustration(phase)
                                else -> PaletteIllustration(phase)
                            }
                        }
                    }
                    else -> PersonalizePage(onDone = onFinished)
                }
            }

            // Fixed-height bottom controls, present only on steps 0-3 (step
            // 4 has its own in-page button) — reserved so the weighted
            // region above never has to guess at available space.
            if (step in 0..3) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    DotProgress(current = step, total = 4, modifier = Modifier.padding(bottom = 16.dp))
                    Button(
                        onClick = { step++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FE0D6))
                    ) {
                        Text(if (step == 0) "Get Started" else "Continue", color = Color(0xFF0B2E28), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ashu Phone's own icon, redrawn as a composable — glossy blue
        // circular badge with a phone-handset glyph, matching the app's
        // actual launcher icon rather than any borrowed mark.
        Box(
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(26.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF29C6F5), Color(0xFF1E7CF0), Color(0xFF4630E0)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(Color(0xFF6FE3FF), Color(0xFF2C86F5), Color(0xFF3550E8)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Welcome to", fontSize = 22.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        Text("Ashu Phone", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        Text(
            "A calling experience built around\nspeed, clarity, and your style.",
            fontSize = 15.sp, color = Color.White.copy(alpha = 0.65f), textAlign = TextAlign.Center, lineHeight = 21.sp
        )
    }
}

/**
 * The FeaturePage content — illustration on top, text below — is split
 * into two explicit weighted rows (0.62 / 0.38) that always sum to the
 * full available height, rather than letting the illustration take a
 * fixed dp size and hoping the text underneath fits in whatever's left.
 */
@Composable
private fun FeaturePage(title: String, subtitle: String, illustration: @Composable (Float) -> Unit) {
    val transition = rememberInfiniteTransition(label = "illustration-motion")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "motion-phase"
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        Box(modifier = Modifier.weight(0.62f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            illustration(phase)
        }
        Column(modifier = Modifier.weight(0.38f).fillMaxWidth(), verticalArrangement = Arrangement.Top) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 34.sp)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f), lineHeight = 20.sp)
        }
    }
}

/** Original illustration: phone card with orbiting call-log dots. */
@Composable
private fun CallsIllustration(phase: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF52E0C8).copy(alpha = 0.25f), Color.Transparent)), radius = size.minDimension / 2f, center = center)

        val cardW = size.width * 0.34f
        val cardH = size.height * 0.55f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(center.x - cardW / 2f, center.y - cardH / 2f),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(24f, 24f)
        )
        drawCircle(Color(0xFF12615A).copy(alpha = 0.5f), radius = 5f, center = Offset(center.x, center.y - cardH / 2f + 14f))

        repeat(3) { i ->
            val angle = (phase * 2 * Math.PI + i * (2 * Math.PI / 3)).toFloat()
            val orbitR = size.minDimension * 0.42f
            val dotCenter = Offset(center.x + orbitR * kotlin.math.cos(angle), center.y + orbitR * kotlin.math.sin(angle) * 0.6f)
            drawCircle(Color(0xFF7FE0D6), radius = 9f, center = dotCenter)
        }
    }
}

/** Original illustration: shield with a rotating scan-sweep arc. */
@Composable
private fun ShieldIllustration(phase: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFF6B5B).copy(alpha = 0.2f), Color.Transparent)), radius = size.minDimension / 2f, center = center)

        val w = size.width * 0.3f
        val h = size.height * 0.42f
        val shieldPath = Path().apply {
            moveTo(center.x, center.y - h / 2f)
            lineTo(center.x + w / 2f, center.y - h / 2f + h * 0.18f)
            lineTo(center.x + w / 2f, center.y + h * 0.1f)
            quadraticBezierTo(center.x + w / 2f, center.y + h / 2f, center.x, center.y + h / 2f + h * 0.15f)
            quadraticBezierTo(center.x - w / 2f, center.y + h / 2f, center.x - w / 2f, center.y + h * 0.1f)
            lineTo(center.x - w / 2f, center.y - h / 2f + h * 0.18f)
            close()
        }
        drawPath(shieldPath, color = Color.White.copy(alpha = 0.92f))

        val sweepAngle = phase * 360f
        drawArc(
            color = Color(0xFFFF6B5B).copy(alpha = 0.7f),
            startAngle = sweepAngle,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = Offset(center.x - 60f, center.y - 60f),
            size = Size(120f, 120f),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

/** Original illustration: three color swatches gently bobbing at phase-offset heights. */
@Composable
private fun PaletteIllustration(phase: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB18CFF).copy(alpha = 0.22f), Color.Transparent)), radius = size.minDimension / 2f, center = center)

        val swatches = listOf(Color(0xFF7FE0D6) to -60f, Color(0xFFFFC08C) to 0f, Color(0xFFB18CFF) to 60f)
        swatches.forEachIndexed { i, (color, xOffset) ->
            val bob = kotlin.math.sin((phase * 2 * Math.PI + i * 1.4).toFloat()) * 10f
            val pos = Offset(center.x + xOffset, center.y + bob)
            drawCircle(color = color, radius = 30f, center = pos)
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 30f, center = pos, style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun DotProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(total + 1) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 22.dp else 7.dp, 7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == current) Color(0xFF7FE0D6) else Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun PersonalizePage(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF7FE0D6), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(20.dp))
        Text("You're all set", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "You can change your theme anytime from Recents or Settings — right now, let's get calling.",
            fontSize = 14.5.sp, color = Color.White.copy(alpha = 0.65f), textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FE0D6))
        ) {
            Text("Start Using Ashu Phone", color = Color(0xFF0B2E28), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
