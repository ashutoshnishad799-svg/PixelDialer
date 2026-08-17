package com.pixeldialer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.ui.components.Avatar
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import kotlin.math.roundToInt

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onMessage: (Contact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else contacts.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.phoneNumber.filter { ch -> ch.isDigit() }.contains(query.filter { ch -> ch.isDigit() })
        }
    }

    val favorites = remember(filtered) { filtered.filter { it.isFavorite } }
    val grouped = remember(filtered) {
        filtered.groupBy { it.displayName.trim().firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contacts", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            IconButton(
                onClick = { },
                modifier = Modifier.size(38.dp).clip(CircleShape).background(palette.cardBackground)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add contact", tint = palette.accent)
            }
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
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search Contacts", color = palette.textSecondary, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = palette.textPrimary, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty() && query.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No contacts match \"$query\"", color = palette.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            "Favourites", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = palette.textSecondary, modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(favorites, key = { "fav-${it.id}" }) { c ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Box(modifier = Modifier.clickable { onCall(c) }) {
                                        Avatar(name = c.displayName, photoUri = c.photoUri, size = 56.dp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = c.displayName.split(" ").first(),
                                        fontSize = 12.sp,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                grouped.forEach { (letter, list) ->
                    item(key = "header-$letter") {
                        Text(
                            text = letter.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    item(key = "group-$letter") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.cardBackground)
                        ) {
                            list.forEachIndexed { index, c ->
                                SwipeableContactRow(
                                    contact = c,
                                    onCall = { onCall(c) },
                                    onMessage = { onMessage(c) }
                                )
                                if (index != list.lastIndex) {
                                    HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * A contact row that reveals a Call action on right-swipe and a Message
 * action on left-swipe — same interaction pattern as Contacts/Messages
 * apps on both major platforms. Snaps back if not dragged past threshold.
 */
@Composable
private fun SwipeableContactRow(
    contact: Contact,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    val palette = LocalDialerPalette.current
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { 88.dp.toPx() }
    var offsetPx by remember { mutableStateOf(0f) }
    val animatedOffsetPx by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetPx,
        animationSpec = tween(180),
        label = "swipe-offset"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.matchParentSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetPx > 0) Arrangement.Start else Arrangement.End
        ) {
            if (offsetPx > 4f) {
                ActionPill(icon = Icons.Filled.Phone, color = palette.callGreen, label = "Call")
            } else if (offsetPx < -4f) {
                ActionPill(icon = Icons.Filled.Message, color = palette.accent, label = "Message")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.cardBackground)
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(-maxOffsetPx, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (offsetPx > maxOffsetPx * 0.55f) {
                            onCall()
                        } else if (offsetPx < -maxOffsetPx * 0.55f) {
                            onMessage()
                        }
                        offsetPx = 0f
                    }
                )
                .clickable { onCall() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(name = contact.displayName, photoUri = contact.photoUri, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = contact.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (contact.isFavorite) {
                Icon(
                    Icons.Filled.Star, contentDescription = "Favorite",
                    tint = palette.accent, modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
