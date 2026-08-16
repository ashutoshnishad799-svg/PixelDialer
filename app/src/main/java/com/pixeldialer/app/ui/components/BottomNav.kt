package com.pixeldialer.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.ui.theme.LocalDialerPalette

enum class DialerTab(val label: String) {
    CONTACTS("Contacts"),
    RECENT("Recent"),
    DIALER("Dialer"),
    PROTECT("Protect"),
    MORE("More")
}

@Composable
fun DialerBottomNav(
    selected: DialerTab,
    onSelect: (DialerTab) -> Unit
) {
    val palette = LocalDialerPalette.current

    NavigationBar(
        containerColor = palette.navBackground,
        contentColor = palette.textSecondary,
        tonalElevation = 0.dp.value.let { androidx.compose.ui.unit.dp }
    ) {
        DialerTab.values().forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = iconFor(tab, isSelected),
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    unselectedIconColor = palette.textSecondary,
                    unselectedTextColor = palette.textSecondary,
                    indicatorColor = palette.accentSoft
                )
            )
        }
    }
}

private fun iconFor(tab: DialerTab, selected: Boolean) = when (tab) {
    DialerTab.CONTACTS -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    DialerTab.RECENT -> if (selected) Icons.Filled.Phone else Icons.Outlined.Phone
    DialerTab.DIALER -> if (selected) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
    DialerTab.PROTECT -> if (selected) Icons.Filled.Shield else Icons.Outlined.Shield
    DialerTab.MORE -> if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
}
