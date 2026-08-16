package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.db.BlockedNumberEntity
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun ProtectScreen(
    blockedNumbers: List<BlockedNumberEntity>,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var spamProtectionOn by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Protect", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Spam protection", fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 15.sp)
                Text(
                    "Flag suspicious callers automatically",
                    fontSize = 12.5.sp, color = palette.textSecondary
                )
            }
            Switch(
                checked = spamProtectionOn,
                onCheckedChange = { spamProtectionOn = it },
                colors = SwitchDefaults.colors(checkedTrackColor = palette.accent)
            )
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(palette.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (spamProtectionOn) "Spam protection is on" else "Spam protection is off",
                fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (blockedNumbers.isEmpty())
                    "Unknown and suspicious callers are flagged automatically before they reach you."
                else
                    "${blockedNumbers.size} number(s) currently blocked.",
                fontSize = 14.sp, color = palette.textSecondary, textAlign = TextAlign.Center
            )
        }
    }
}
