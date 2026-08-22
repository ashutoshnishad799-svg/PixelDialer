package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.ui.theme.LocalDialerPalette

private data class MenuEntry(val label: String, val icon: ImageVector)

private val menuItems = listOf(
    MenuEntry("Account", Icons.Filled.AccountCircle),
    MenuEntry("Appearance", Icons.Filled.Palette),
    MenuEntry("Settings", Icons.Filled.Settings),
    MenuEntry("Blocked numbers", Icons.Filled.Block),
    MenuEntry("Voicemail", Icons.Filled.Voicemail),
    MenuEntry("Set as default dialer", Icons.Filled.PhonelinkSetup),
    MenuEntry("Help & feedback", Icons.Filled.Help),
    MenuEntry("Privacy Policy", Icons.Filled.PrivacyTip)
)

@Composable
fun MoreScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "More", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
        ) {
            menuItems.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(entry.label) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(entry.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = entry.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                }
                if (index != menuItems.lastIndex) {
                    HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                }
            }
        }
    }
}
