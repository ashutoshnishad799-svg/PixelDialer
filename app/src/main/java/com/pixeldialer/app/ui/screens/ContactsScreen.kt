package com.pixeldialer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.data.Contact
import com.pixeldialer.app.ui.components.Avatar
import com.pixeldialer.app.ui.theme.LocalDialerPalette

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDialerPalette.current
    val favorites = contacts.filter { it.isFavorite }
    val grouped = contacts.groupBy { it.displayName.trim().firstOrNull()?.uppercaseChar() ?: '#' }
        .toSortedMap()

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
            Text("Search Contacts", color = palette.textSecondary, fontSize = 15.sp)
        }

        Spacer(Modifier.height(14.dp))

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
                        items(favorites) { c ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(64.dp)
                            ) {
                                Box(
                                    modifier = Modifier.clickable { onCall(c) }
                                ) {
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
                item {
                    Text(
                        text = letter.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.cardBackground)
                    ) {
                        list.forEachIndexed { index, c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCall(c) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(name = c.displayName, photoUri = c.photoUri, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = c.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (c.isFavorite) {
                                    Icon(
                                        Icons.Filled.Star, contentDescription = "Favorite",
                                        tint = palette.accent, modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
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
