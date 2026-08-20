package com.pixeldialer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldialer.app.ui.theme.DialerPalette
import com.pixeldialer.app.ui.theme.LocalDialerPalette

enum class DialerTab(val label: String) {
    CONTACTS("Contacts"),
    RECENT("Recent"),
    DIALER("Dialer"),
    PROTECT("Protect"),
    MORE("More")
}

/**
 * Custom rounded pill-shaped bottom nav — replaces the stock flat
 * NavigationBar with rounded corners top-and-bottom (a floating capsule
 * rather than a bar flush with the screen edges) and a spring-animated
 * selection state per tab (icon bounce, indicator fade, color crossfade).
 */
@Composable
fun DialerBottomNav(
    selected: DialerTab,
    onSelect: (DialerTab) -> Unit
) {
    val palette = LocalDialerPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(palette.navBackground)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialerTab.values().forEach { tab ->
            NavTabItem(
                tab = tab,
                isSelected = tab == selected,
                palette = palette,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: DialerTab,
    isSelected: Boolean,
    palette: DialerPalette,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "icon-scale"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) palette.accentSoft else palette.accentSoft.copy(alpha = 0f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "indicator-color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) palette.accent else palette.textSecondary,
        label = "content-color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(indicatorColor)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = iconFor(tab, isSelected),
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp).scale(iconScale)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor
        )
    }
}

private fun iconFor(tab: DialerTab, selected: Boolean): ImageVector = when (tab) {
    DialerTab.CONTACTS -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    DialerTab.RECENT -> if (selected) Icons.Filled.Phone else Icons.Outlined.Phone
    DialerTab.DIALER -> if (selected) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
    DialerTab.PROTECT -> if (selected) Icons.Filled.Shield else Icons.Outlined.Shield
    DialerTab.MORE -> if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
}
