package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.db.BlockedNumberEntity
import com.pixeldialer.app.ui.theme.DialerPalette
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun ProtectScreen(
    blockedNumbers: List<BlockedNumberEntity>,
    onOpenBlockedNumbers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var spamProtectionOn by remember { mutableStateOf(true) }
    var silenceUnknown by remember { mutableStateOf(false) }
    var flagInternational by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Protect", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.cardBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(palette.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (spamProtectionOn) "You're protected" else "Protection is off",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text("${blockedNumbers.size} numbers blocked", fontSize = 13.sp, color = palette.textSecondary)
            }

            Spacer(Modifier.height(14.dp))

            SectionLabel("Protection settings", palette)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.cardBackground)
            ) {
                ProtectToggleRow(Icons.Filled.Shield, "Spam detection", "Flag suspicious callers automatically", spamProtectionOn, palette) { spamProtectionOn = it }
                HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                ProtectToggleRow(Icons.Filled.VolumeOff, "Silence unknown callers", "Numbers not in your contacts won't ring", silenceUnknown, palette) { silenceUnknown = it }
                HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                ProtectToggleRow(Icons.Filled.PhoneDisabled, "Flag international numbers", "Warn on calls from unfamiliar country codes", flagInternational, palette) { flagInternational = it }
            }

            Spacer(Modifier.height(14.dp))

            SectionLabel("Blocked numbers", palette)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.cardBackground)
                    .clickable { onOpenBlockedNumbers() }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Block, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage blocked numbers", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                        Text(
                            if (blockedNumbers.isEmpty()) "No numbers blocked yet" else "${blockedNumbers.size} blocked",
                            fontSize = 12.sp, color = palette.textSecondary
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, palette: DialerPalette) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun ProtectToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: DialerPalette,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
            Text(subtitle, fontSize = 11.5.sp, color = palette.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = palette.accent))
    }
}
