package com.pixeldialer.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.PersonAdd
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
    onAddContact: (String) -> Unit = {},
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

    // Once the typed number is long enough to plausibly be a real number
    // and doesn't match anyone saved, offer to save it — same threshold
    // logic stock dialers use (too short and every partial dial would
    // flash the prompt pointlessly).
    val showAddContactHint = number.length >= 5 && matchedContact == null

    fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // Everything sits inside one bottom-weighted Column instead of the keypad
    // being centered in leftover space — that centering was exactly what
    // created the large empty gap between the number display and the keys
    // seen in the "spacing looks off" feedback. Fixed small top padding,
    // then keys immediately below, then the call button hugging the bottom.
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(top = 10.dp, bottom = 4.dp),
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
                        modifier = Modifier.padding(top = 6.dp)
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

            androidx.compose.animation.AnimatedVisibility(
                visible = showAddContactHint,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }) + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentSoft)
                        .clickable { onAddContact(number) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Add to Contacts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = true).heightIn(max = 24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            keys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp),
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
                    .background(palette.callGreen)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
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
    // StiffnessMedium settles noticeably faster than the previous
    // StiffnessLow — fewer animated frames per key-press, which matters
    // when up to 12 of these can be live at once right as a tab-switch
    // slide/fade is also animating in.
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
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
            .size(70.dp)
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
            Text(key.digit, fontSize = 28.sp, fontWeight = FontWeight.Normal, color = palette.textPrimary)
            if (key.letters.isNotEmpty()) {
                Text(
                    key.letters, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = palette.textSecondary, letterSpacing = 1.2.sp
                )
            }
        }
    }
}
