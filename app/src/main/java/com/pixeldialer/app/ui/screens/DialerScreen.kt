package com.pixeldialer.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.telecom.DtmfPlayer
import com.pixeldialer.app.ui.theme.LocalDialerPalette

private data class KeyDef(val digit: String, val letters: String)

private val keys = listOf(
    KeyDef("1", ""), KeyDef("2", "ABC"), KeyDef("3", "DEF"),
    KeyDef("4", "GHI"), KeyDef("5", "JKL"), KeyDef("6", "MNO"),
    KeyDef("7", "PQRS"), KeyDef("8", "TUV"), KeyDef("9", "WXYZ"),
    KeyDef("*", ""), KeyDef("0", "+"), KeyDef("#", "")
)

@Composable
fun DialerScreen(
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

    fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = if (number.length > 10) 30.sp else 38.sp,
                fontWeight = FontWeight.Light,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            keys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { if (number.isNotEmpty()) onCall(number) },
                enabled = number.isNotEmpty(),
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (number.isNotEmpty()) palette.callGreen else palette.cardBackground)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = "Call",
                    tint = if (number.isNotEmpty()) Color.White else palette.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            if (number.isNotEmpty()) {
                IconButton(
                    onClick = { number = number.dropLast(1) },
                    modifier = Modifier.align(Alignment.CenterEnd).size(44.dp)
                ) {
                    Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = palette.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun DialerKey(
    key: KeyDef,
    palette: com.pixeldialer.app.ui.theme.DialerPalette,
    onPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(palette.cardBackground)
            .border(1.dp, palette.cardBorder, CircleShape)
            .clickableNoRipple(onPress),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(key.digit, fontSize = 30.sp, fontWeight = FontWeight.Normal, color = palette.textPrimary)
            if (key.letters.isNotEmpty()) {
                Text(
                    key.letters, fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
                    color = palette.textSecondary, letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        indication = null,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        onClick = onClick
    )
)
