package app.pulse.android.ui.components.themed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.pulse.android.R
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PillNavigationBar(
    tabs: ImmutableList<Tab>,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    hiddenTabs: ImmutableList<String>,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val lazyListState = rememberLazyListState()
    
    // Filter visible tabs: show if not hidden OR if it's currently selected
    val visibleTabsWithIndices = tabs.mapIndexed { index, tab -> index to tab }
        .filter { (index, tab) -> tab.key !in hiddenTabs || index == tabIndex }

    // Custom arrangement to center items if they fit, otherwise align to start
    val centerOrStartArrangement = remember {
        object : Arrangement.Horizontal {
            override val spacing = 4.dp
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) {
                val spacingPx = spacing.roundToPx()
                val totalItemSize = sizes.sum() + (spacingPx * (sizes.size - 1).coerceAtLeast(0))
                
                var current = if (totalItemSize < totalSize) {
                    (totalSize - totalItemSize) / 2 // Center if items fit
                } else {
                    0 // Start if items overflow
                }
                
                sizes.forEachIndexed { i, size ->
                    outPositions[i] = current
                    current += size + spacingPx
                }
            }
        }
    }

    // Auto-scroll to current tab and center it
    LaunchedEffect(tabIndex) {
        val targetIndex = visibleTabsWithIndices.indexOfFirst { it.first == tabIndex }
        if (targetIndex != -1) {
            val layoutInfo = lazyListState.layoutInfo
            val containerWidth = layoutInfo.viewportSize.width
            
            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            
            // Calculate offset to center the item
            val offset = if (visibleItem != null) {
                (containerWidth - visibleItem.size) / 2
            } else {
                containerWidth / 2
            }
            
            lazyListState.animateScrollToItem(targetIndex, -offset)
        }
    }

    Box(
        modifier = modifier
            .height(Dimensions.items.collapsedPlayerHeight)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(colorPalette.background1, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        @Suppress("DEPRECATION")
        val overscrollConfig = LocalOverscrollConfiguration provides null

        CompositionLocalProvider(overscrollConfig) {
            LazyRow(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = centerOrStartArrangement,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(visibleTabsWithIndices) { _, (originalIndex, tab) ->
                    PillNavigationItem(
                        tab = tab,
                        isSelected = originalIndex == tabIndex,
                        onClick = { onTabChange(originalIndex) }
                    )
                }

                item {
                    SettingsNavigationItem(onClick = onSettingsClick)
                }
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
private fun SettingsNavigationItem(
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = Modifier
            .size(Dimensions.items.collapsedPlayerHeight - 8.dp)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.settings),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
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
