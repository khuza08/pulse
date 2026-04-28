package app.pulse.android.ui.components.themed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.pulse.android.R
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PillNavigationBar(
    tabs: ImmutableList<Tab>,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    hiddenTabs: ImmutableList<String>,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    
    // Filter visible tabs: show if not hidden OR if it's currently selected
    val visibleTabsWithIndices = tabs.mapIndexed { index, tab -> index to tab }
        .filter { (index, tab) -> tab.key !in hiddenTabs || index == tabIndex }

    Box(
        modifier = modifier
            .height(Dimensions.items.collapsedPlayerHeight)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(colorPalette.background1, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            visibleTabsWithIndices.forEach { (index, tab) ->
                PillNavigationItem(
                    tab = tab,
                    isSelected = index == tabIndex,
                    onClick = { onTabChange(index) }
                )
            }
        }
    }
}

@Composable
private fun PillNavigationItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) colorPalette.background2 else colorPalette.background1,
        label = "backgroundColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colorPalette.accent else colorPalette.textSecondary,
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .size(Dimensions.items.collapsedPlayerHeight - 8.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(tab.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FloatingSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = modifier
            .size(Dimensions.items.collapsedPlayerHeight)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(colorPalette.background1, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier.size(24.dp)
        )
    }
}
