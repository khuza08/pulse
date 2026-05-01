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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.pulse.android.R
import app.pulse.core.ui.Dimensions
import app.pulse.core.ui.LocalAppearance
import androidx.compose.foundation.text.BasicText
import app.pulse.android.utils.semiBold
import androidx.compose.ui.unit.sp
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
                        onClick = { onTabChange(originalIndex) },
                        labelAlpha = 1f // Always show labels in the static pill bar
                    )
                }

                item {
                    SettingsNavigationItem(onClick = onSettingsClick, labelAlpha = 1f)
                }
            }
        }
    }
}

@Composable
internal fun PillNavigationItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    labelAlpha: Float = 0f
) {
    val (colorPalette, typography) = LocalAppearance.current
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) colorPalette.background2 else colorPalette.background1,
        label = "backgroundColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colorPalette.accent else colorPalette.textSecondary,
        label = "iconColor"
    )

    Column(
    modifier = Modifier
        .width(72.dp)
        .height(Dimensions.items.collapsedPlayerHeight - 8.dp)
        .clip(CircleShape)
        .background(backgroundColor)
        .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Image(
        painter = painterResource(tab.icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(iconColor),
        modifier = Modifier.size(22.dp)
    )

    if (labelAlpha > 0.05f) {
        Spacer(modifier = Modifier.height(2.dp * labelAlpha))
        BasicText(
            text = tab.title(),
            style = typography.xs.semiBold.copy(
                color = iconColor,
                fontSize = 9.sp
            ),           
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
            maxLines = 1
        )
    }
}
}

@Composable
internal fun SettingsNavigationItem(
    onClick: () -> Unit,
    enabled: Boolean = true,
    labelAlpha: Float = 0f
) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(Dimensions.items.collapsedPlayerHeight - 8.dp)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.settings),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
            modifier = Modifier.size(22.dp)
        )
        if (labelAlpha > 0.05f) {
            Spacer(modifier = Modifier.height(2.dp * labelAlpha))
            BasicText(
                text = "Settings",
                style = typography.xs.semiBold.copy(
                    color = colorPalette.textSecondary,
                    fontSize = 9.sp
                ),
                modifier = Modifier.graphicsLayer { alpha = labelAlpha },
                maxLines = 1
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphingNavigationBar(
    progress: Float,
    tabs: kotlinx.collections.immutable.ImmutableList<Tab>,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    hiddenTabs: kotlinx.collections.immutable.ImmutableList<String>,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val dockScrolled = LocalDockScrolled.current
    val lazyListState = rememberLazyListState()
    
    // Filter visible tabs (same logic as PillNavigationBar)
    val visibleTabsWithIndices = remember(tabs, hiddenTabs, tabIndex) {
        tabs.mapIndexed { index, tab -> index to tab }
            .filter { (index, tab) -> tab.key !in hiddenTabs || index == tabIndex }
    }

    Box(
        modifier = modifier
            .height(Dimensions.items.collapsedPlayerHeight)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(colorPalette.background1, CircleShape)
            .clip(CircleShape)
            .clickable(
                enabled = progress > 0.8f,
                onClick = { dockScrolled.value = false }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Widen the crossfade range for more fluidity (0.5 to 1.0)
        val crossfadeProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val lazyRowAlpha = (1f - crossfadeProgress).coerceIn(0f, 1f)
        val singleIconAlpha = crossfadeProgress.coerceIn(0f, 1f)

        // We render both but control alpha via graphicsLayer. 
        // This avoids "snapping" when the 'if' condition changes.
        
        // 1. Full LazyRow (Rendered first, at the bottom of the stack)
        @Suppress("DEPRECATION")
        val overscrollConfig = LocalOverscrollConfiguration provides null
        CompositionLocalProvider(overscrollConfig) {
            Box(modifier = Modifier.graphicsLayer { alpha = lazyRowAlpha }) {
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    itemsIndexed(visibleTabsWithIndices) { _, (originalIndex, tab) ->
                        val isSelected = originalIndex == tabIndex
                        val itemAlpha = if (isSelected) 1f else (1f - progress).coerceIn(0f, 1f)
                        Box(modifier = Modifier.graphicsLayer { alpha = itemAlpha }) {
                            PillNavigationItem(
                                tab = tab,
                                isSelected = isSelected,
                                onClick = { onTabChange(originalIndex) },
                                enabled = progress < 0.2f,
                                labelAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) }) {
                            SettingsNavigationItem(
                                onClick = onSettingsClick,
                                enabled = progress < 0.2f,
                                labelAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Single centered selected item (Rendered LAST so it's on top of the LazyRow)
        val selectedTab = tabs.getOrNull(tabIndex)
        if (selectedTab != null) {
            Box(modifier = Modifier.graphicsLayer { 
                alpha = singleIconAlpha 
                val scale = (0.8f + (singleIconAlpha * 0.2f)).coerceIn(0f, 1.2f)
                scaleX = scale
                scaleY = scale
            }) {
                PillNavigationItem(
                    tab = selectedTab,
                    isSelected = true,
                    onClick = { dockScrolled.value = false },
                    enabled = progress > 0.8f
                )
            }
        }
    }
}

@Composable
fun CollapsedNavigationCircle(
    tabs: kotlinx.collections.immutable.ImmutableList<Tab>,
    tabIndex: Int,
    modifier: Modifier = Modifier
) {
    val dockScrolled = LocalDockScrolled.current
    val currentTab = tabs.getOrNull(tabIndex) ?: return

    Box(
        modifier = modifier
            .size(app.pulse.core.ui.Dimensions.items.collapsedPlayerHeight)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(LocalAppearance.current.colorPalette.background1, CircleShape)
            .clip(CircleShape)
            .clickable { dockScrolled.value = false },
        contentAlignment = Alignment.Center
    ) {
        PillNavigationItem(
            tab = currentTab,
            isSelected = true,
            onClick = { dockScrolled.value = false }
        )
    }
}

@Composable
fun RadioCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(colorPalette.background1, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(R.drawable.radio),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier.size(24.dp)
        )
    }
}

