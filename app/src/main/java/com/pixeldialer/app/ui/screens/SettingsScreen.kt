package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.AppSettings
import com.pixeldialer.app.ui.theme.DialerPalette
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onToggleCallRecording: (Boolean) -> Unit,
    onToggleAutoRecordAll: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
            SectionLabel("Appearance", palette)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.cardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAppearance() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Theme", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                        Text("Choose a color palette", fontSize = 12.sp, color = palette.textSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Call recording", palette)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.cardBackground)
            ) {
                SettingsToggleRow(
                    icon = Icons.Filled.Mic,
                    title = "Enable call recording",
                    subtitle = "Record button appears during calls",
                    checked = settings.callRecordingEnabled,
                    palette = palette,
                    onToggle = onToggleCallRecording
                )
                HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                SettingsToggleRow(
                    icon = Icons.Filled.RecordVoiceOver,
                    title = "Auto-record all calls",
                    subtitle = "Starts recording the moment a call connects",
                    checked = settings.autoRecordAll,
                    enabled = settings.callRecordingEnabled,
                    palette = palette,
                    onToggle = onToggleAutoRecordAll
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Recording quality depends on your device — see the recording pill during a call for which mode is active. Recordings save locally to this app only.",
                fontSize = 12.sp, color = palette.textSecondary, modifier = Modifier.padding(horizontal = 4.dp)
            )
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
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    palette: DialerPalette,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (enabled) palette.accent else palette.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.5f)
            )
            Text(subtitle, fontSize = 12.sp, color = palette.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
    }
}
