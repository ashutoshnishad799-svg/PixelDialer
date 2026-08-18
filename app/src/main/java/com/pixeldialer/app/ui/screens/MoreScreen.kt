package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.ui.theme.LocalDialerPalette

private val menuItems = listOf(
    "Account", "Appearance", "Settings", "Blocked numbers", "Call recording", "Voicemail", "Set as default dialer", "Help & feedback", "Privacy Policy"
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
            menuItems.forEachIndexed { index, item ->
                Text(
                    text = item,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                if (index != menuItems.lastIndex) {
                    HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                }
            }
        }
    }
}
