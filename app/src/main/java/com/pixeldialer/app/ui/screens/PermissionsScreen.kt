package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun PermissionsScreen(
    isDefaultDialer: Boolean,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onSetDefaultDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(palette.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Set up Ashu Phone",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Ashu Phone needs phone, contacts and call log access to work as your dialer.",
            fontSize = 14.sp, color = palette.textSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (!hasPermissions) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                Text("Grant permissions", fontWeight = FontWeight.SemiBold)
            }
        } else if (!isDefaultDialer) {
            Button(
                onClick = onSetDefaultDialer,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                Text("Set as default dialer", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text("All set — you're ready to go!", color = palette.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}
