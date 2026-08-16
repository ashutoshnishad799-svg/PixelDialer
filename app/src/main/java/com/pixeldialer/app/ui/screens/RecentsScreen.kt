package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.RecentCall
import com.pixeldialer.app.data.db.CallDirection
import com.pixeldialer.app.ui.components.Avatar
import com.pixeldialer.app.ui.components.BothWayIcon
import com.pixeldialer.app.ui.components.DirectionIcon
import com.pixeldialer.app.ui.components.ThemePickerButton
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import java.text.SimpleDateFormat
import java.util.*

private val recentFilters = listOf("All", "Missed", "Contacts", "Identified", "Spam")

@Composable
fun RecentsScreen(
    recents: List<RecentCall>,
    onOpenThemePicker: () -> Unit,
    onCall: (RecentCall) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var filter by remember { mutableStateOf("All") }

    val filtered = remember(recents, filter) {
        when (filter) {
            "Missed" -> recents.filter { it.direction == CallDirection.MISSED }
            else -> recents
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recents",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
            ThemePickerButton(onClick = onOpenThemePicker)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.searchBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Search Recent", color = palette.textSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(recentFilters) { tab ->
                val isSelected = filter == tab
                Surface(
                    onClick = { filter = tab },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) palette.cardBackground else Color.Transparent,
                    contentColor = if (isSelected) palette.accent else palette.textPrimary
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Today",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.cardBackground)
                ) {
                    filtered.forEachIndexed { index, call ->
                        RecentRow(call = call, onCall = { onCall(call) })
                        if (index != filtered.lastIndex) {
                            HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecentRow(call: RecentCall, onCall: () -> Unit) {
    val palette = LocalDialerPalette.current
    val isMissed = call.direction == CallDirection.MISSED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = call.displayName, photoUri = call.photoUri, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = call.displayName,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMissed) palette.danger else palette.textPrimary,
                    maxLines = 1
                )
                if (call.callCount > 1) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "(${call.callCount})",
                        fontSize = 14.5.sp,
                        color = if (isMissed) palette.danger else palette.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (call.callCount > 1 && call.direction != CallDirection.MISSED) {
                    BothWayIcon()
                } else {
                    DirectionIcon(direction = call.direction)
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Mobile • ${formatTime(call.timestampMillis)}",
                    fontSize = 13.5.sp,
                    color = if (isMissed) palette.danger.copy(alpha = 0.85f) else palette.textSecondary
                )
            }
        }
        IconButton(
            onClick = onCall,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.accentSoft)
        ) {
            Icon(Icons.Filled.Phone, contentDescription = "Call", tint = palette.accent, modifier = Modifier.size(17.dp))
        }
    }
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
