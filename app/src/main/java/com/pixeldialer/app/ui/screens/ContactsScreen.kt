package com.pixeldialer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.ui.components.AddContactDialog
import com.pixeldialer.app.ui.components.Avatar
import com.pixeldialer.app.ui.components.NewContactInput
import com.pixeldialer.app.ui.theme.LocalDialerPalette
import kotlin.math.roundToInt

/** One person, with all of their phone numbers grouped together — this is what actually gets rendered as one card. */
private data class GroupedContact(
    val contactId: String,
    val displayName: String,
    val photoUri: String?,
    val isFavorite: Boolean,
    val numbers: List<Contact>
)

private fun groupByPerson(contacts: List<Contact>): List<GroupedContact> =
    contacts
        .groupBy { it.contactId }
        .map { (contactId, entries) ->
            GroupedContact(
                contactId = contactId,
                displayName = entries.first().displayName,
                photoUri = entries.firstOrNull { it.photoUri != null }?.photoUri,
                isFavorite = entries.any { it.isFavorite },
                numbers = entries
            )
        }
        .sortedBy { it.displayName.lowercase() }

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onMessage: (Contact) -> Unit = {},
    onSaveNewContact: (NewContactInput) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Matches on name OR on the digits of the number — the actual data bug
    // that made "some people's numbers don't show up" was in what got
    // loaded from the system (see ContactsRepository), not this filter.
    // With every number now loaded as its own row, this correctly finds a
    // person by any of their saved numbers, not just their first one.
    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else {
            val digitsQuery = query.filter { it.isDigit() }
            contacts.filter { c ->
                c.displayName.contains(query, ignoreCase = true) ||
                    (digitsQuery.isNotEmpty() && c.phoneNumber.filter { it.isDigit() }.contains(digitsQuery))
            }
        }
    }

    val grouped = remember(filtered) { groupByPerson(filtered) }
    val favorites = remember(grouped) { grouped.filter { it.isFavorite } }
    val alphaGroups = remember(grouped) {
        grouped.groupBy { it.displayName.trim().firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contacts", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(32.dp).clip(CircleShape).background(palette.cardBackground)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add contact", tint = palette.accent, modifier = Modifier.size(18.dp))
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
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f)
            ) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedVisibility(
            visible = grouped.isEmpty() && query.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No contacts match \"$query\"", color = palette.textSecondary, fontSize = 14.sp)
            }
        }

        if (!(grouped.isEmpty() && query.isNotEmpty())) {
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
                            items(favorites, key = { "fav-${it.contactId}" }) { c ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Box(modifier = Modifier.clickable { c.numbers.firstOrNull()?.let(onCall) }) {
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

                alphaGroups.forEach { (letter, list) ->
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
                            list.forEachIndexed { index, person ->
                                PersonRow(
                                    person = person,
                                    onCall = onCall,
                                    onMessage = onMessage
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

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onSave = { input ->
                onSaveNewContact(input)
                showAddDialog = false
            }
        )
    }
}

/**
 * A person's row. If they have exactly one number, tapping/swiping the row
 * acts directly on it (same behavior as before). If they have multiple
 * numbers, tapping expands an inline list so the user picks which one to
 * call — calling the wrong saved number for someone with two lines was the
 * alternative, which is worse than one extra tap.
 */
@Composable
private fun PersonRow(
    person: GroupedContact,
    onCall: (Contact) -> Unit,
    onMessage: (Contact) -> Unit
) {
    val palette = LocalDialerPalette.current
    var expanded by remember { mutableStateOf(false) }

    if (person.numbers.size == 1) {
        SwipeableContactRow(
            displayName = person.displayName,
            photoUri = person.photoUri,
            isFavorite = person.isFavorite,
            onCall = { onCall(person.numbers.first()) },
            onMessage = { onMessage(person.numbers.first()) }
        )
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = person.displayName, photoUri = person.photoUri, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = person.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("${person.numbers.size} numbers", fontSize = 12.sp, color = palette.textSecondary)
                if (person.isFavorite) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = palette.accent, modifier = Modifier.size(14.dp))
                }
                // Rotating chevron makes it visually clear this row is
                // expandable — without any indicator, "2 numbers" as plain
                // grey text didn't read as a tap target, so the second/
                // third number effectively looked missing rather than
                // just collapsed.
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse numbers" else "Expand numbers",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 4.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(start = 52.dp, bottom = 4.dp)) {
                    person.numbers.forEach { number ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCall(number) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(number.phoneNumber, fontSize = 14.sp, color = palette.textPrimary)
                                Text(number.numberLabel, fontSize = 11.sp, color = palette.textSecondary)
                            }
                            IconButton(onClick = { onCall(number) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Phone, contentDescription = "Call ${number.numberLabel}", tint = palette.callGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
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
    displayName: String,
    photoUri: String?,
    isFavorite: Boolean,
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
            Avatar(name = displayName, photoUri = photoUri, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isFavorite) {
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
