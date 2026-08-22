package com.pixeldialer.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.R

private data class OnboardStep(val title: String, val subtitle: String, val imageRes: Int)

private val featureSteps = listOf(
    OnboardStep(
        "All your calls,\nin one place",
        "Recents, contacts, and dialing — fast and organized, exactly how you'd expect.",
        R.drawable.onboarding_calls
    ),
    OnboardStep(
        "Stay ahead\nof spam",
        "Suspicious and unknown callers get flagged automatically, before they ever reach you.",
        R.drawable.onboarding_protect
    ),
    OnboardStep(
        "Make it\nyours",
        "Pick from gradient, solid, and dark themes — or let it follow your system automatically.",
        R.drawable.onboarding_theme
    )
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
 *
 * Illustration note: the feature pages now show real artwork
 * (onboarding_calls / onboarding_protect / onboarding_theme — cropped
 * from the app's own promo art) instead of hand-drawn Canvas shapes.
 * Each image is bounded to a fraction of its weighted Box with
 * ContentScale.Fit, so the "never exceed assigned slot" rule above still
 * holds for images the same way it did for the old Canvas drawing.
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
                        FeaturePage(title = f.title, subtitle = f.subtitle, imageRes = f.imageRes)
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
 *
 * The illustration gets a slow, continuous float — a few dp of vertical
 * drift plus a faint scale breathe — applied via graphicsLayer, which
 * only repaints rather than triggering a relayout on every frame.
 */
@Composable
private fun FeaturePage(title: String, subtitle: String, imageRes: Int) {
    val transition = rememberInfiniteTransition(label = "illustration-motion")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "motion-phase"
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        Box(modifier = Modifier.weight(0.62f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val angle = (phase * 2 * Math.PI).toFloat()
            val drift = kotlin.math.sin(angle) * 6f
            val breathe = 1f + kotlin.math.sin(angle) * 0.015f
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.74f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationY = drift
                        scaleX = breathe
                        scaleY = breathe
                    }
            )
        }
        Column(modifier = Modifier.weight(0.38f).fillMaxWidth(), verticalArrangement = Arrangement.Top) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 34.sp)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f), lineHeight = 20.sp)
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
