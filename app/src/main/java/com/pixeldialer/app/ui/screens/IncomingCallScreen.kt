package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hue = ((callerName.firstOrNull()?.code ?: 65) * 37) % 360

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.hsl(hue.toFloat(), 0.3f, 0.16f), Color(0xFF0A0A0D))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Incoming call", fontSize = 15.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color.hsl(hue.toFloat(), 0.45f, 0.38f), Color.hsl((hue + 40).toFloat(), 0.4f, 0.18f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(callerName.take(1).uppercase(), fontSize = 42.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
                Text(callerName, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(callerNumber, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(Color(0xFFFF3B30))
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Filled.Message, contentDescription = "Message", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Message", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(Color(0xFF34C759))
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Accept", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
