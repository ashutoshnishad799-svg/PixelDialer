package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.db.BlockedNumberEntity
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun BlockedNumbersScreen(
    blockedNumbers: List<BlockedNumberEntity>,
    onBack: () -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (BlockedNumberEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var newNumber by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Blocked Numbers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.searchBackground)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (newNumber.isEmpty()) {
                        Text("Enter number to block", color = palette.textSecondary, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        singleLine = true,
                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            }

            if (newNumber.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { onBlock(newNumber.trim()); newNumber = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.danger),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Block this number")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (blockedNumbers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(palette.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No blocked numbers", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Numbers you block won't be able to call or text you", fontSize = 12.5.sp, color = palette.textSecondary)
                }
            } else {
                Text(
                    "${blockedNumbers.size} blocked", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = palette.textSecondary, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.cardBackground)
                ) {
                    blockedNumbers.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.phoneNumber, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                                Text(entry.reason, fontSize = 12.sp, color = palette.textSecondary)
                            }
                            IconButton(onClick = { onUnblock(entry) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Unblock", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index != blockedNumbers.lastIndex) {
                            HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
