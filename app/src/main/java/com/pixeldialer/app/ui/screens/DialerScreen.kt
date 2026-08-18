package com.pixeldialer.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.telecom.DtmfPlayer
import com.pixeldialer.app.ui.components.Avatar
import com.pixeldialer.app.ui.theme.LocalDialerPalette

private data class KeyDef(val digit: String, val letters: String)

private val keys = listOf(
    KeyDef("1", ""), KeyDef("2", "ABC"), KeyDef("3", "DEF"),
    KeyDef("4", "GHI"), KeyDef("5", "JKL"), KeyDef("6", "MNO"),
    KeyDef("7", "PQRS"), KeyDef("8", "TUV"), KeyDef("9", "WXYZ"),
    KeyDef("*", ""), KeyDef("0", "+"), KeyDef("#", "")
)

/** Normalizes a phone number for matching: strips spaces, dashes, parens. Keeps a leading +. */
private fun normalizeForMatch(raw: String): String {
    val hasPlus = raw.trimStart().startsWith("+")
    val digitsOnly = raw.filter { it.isDigit() }
    return if (hasPlus) "+$digitsOnly" else digitsOnly
}

@Composable
fun DialerScreen(
    contacts: List<Contact>,
    onCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }

    val dtmfPlayer = remember { DtmfPlayer() }
    DisposableEffect(Unit) {
        onDispose { dtmfPlayer.release() }
    }

    // Live contact match — updates as the user types, like a stock dialer.
    val matchedContact = remember(number, contacts) {
        if (number.length < 3) return@remember null
        val target = normalizeForMatch(number)
        contacts.firstOrNull { c ->
            val candidate = normalizeForMatch(c.phoneNumber)
            candidate.isNotEmpty() && (candidate == target || candidate.endsWith(target) || target.endsWith(candidate))
        }
    }

    fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number,
                fontSize = if (number.length > 10) 28.sp else 34.sp,
                fontWeight = FontWeight.Light,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = matchedContact != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                matchedContact?.let { contact ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Avatar(name = contact.displayName, photoUri = contact.photoUri, size = 22.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = contact.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            keys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        DialerKey(
                            key = key,
                            palette = palette,
                            onPress = {
                                number += key.digit
                                dtmfPlayer.play(key.digit.first())
                                vibrate()
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val callInteractionSource = remember { MutableInteractionSource() }
            val isCallPressed by callInteractionSource.collectIsPressedAsState()
            val callScale by animateFloatSpring(if (isCallPressed) 0.9f else 1f)

            IconButton(
                onClick = { if (number.isNotEmpty()) onCall(number) },
                enabled = number.isNotEmpty(),
                interactionSource = callInteractionSource,
                modifier = Modifier
                    .size(62.dp)
                    .scale(callScale)
                    .clip(CircleShape)
                    .background(if (number.isNotEmpty()) palette.callGreen else palette.cardBackground)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = "Call",
                    tint = if (number.isNotEmpty()) Color.White else palette.textSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            AnimatedVisibility(
                visible = number.isNotEmpty(),
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                IconButton(
                    onClick = { number = number.dropLast(1) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun animateFloatSpring(target: Float) = androidx.compose.animation.core.animateFloatAsState(
    targetValue = target,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "press-scale"
)

@Composable
private fun DialerKey(
    key: KeyDef,
    palette: com.pixeldialer.app.ui.theme.DialerPalette,
    onPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatSpring(if (isPressed) 0.88f else 1f)

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(palette.cardBackground)
            .border(1.dp, palette.cardBorder, CircleShape)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(key.digit, fontSize = 26.sp, fontWeight = FontWeight.Normal, color = palette.textPrimary)
            if (key.letters.isNotEmpty()) {
                Text(
                    key.letters, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
                    color = palette.textSecondary, letterSpacing = 1.2.sp
                )
            }
        }
    }
}
