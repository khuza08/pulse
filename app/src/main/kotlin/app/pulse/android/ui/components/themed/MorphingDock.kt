package app.pulse.android.ui.components.themed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.pulse.android.ui.components.MorphingMiniPlayer
import app.pulse.core.ui.Dimensions

@Composable
fun MorphingDock(
    progress: Float,
    navigationState: NavigationState?,
    onPlayerClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = 8.dp
    
    // Smooth sub-progress calculation with Elastic Synchronized Landing
    // We synchronize all phases to reach 1.0 at progress 1.0.
    
    // Phase 1 (Starts 0.0): NavBar shrinks Pill -> Circle
    val phase1 = (progress / 1.0f).coerceAtLeast(0f)
    
    // Phase 2 (Starts 0.2): MiniPlayer shrinks Full -> Compact
    val phase2 = ((progress - 0.2f) / 0.8f).coerceAtLeast(0f)
    
    // Phase 3 (Starts 0.5): MiniPlayer moves Top -> Bottom row
    val phase3 = ((progress - 0.5f) / 0.5f).coerceAtLeast(0f)

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(Dimensions.items.collapsedPlayerHeight * 2 + spacing) // Room for two rows
    ) {
        val fullWidth = maxWidth
        val baseSize = Dimensions.items.collapsedPlayerHeight
        
        // Gradually reduce height by 20% as progress increases
        val currentCircleSize = (baseSize * (1f - 0.2f * progress)).coerceAtLeast(baseSize * 0.75f)
        
        // --- 1. Search Button (Stays at Bottom-Right) ---
        Box(
            modifier = Modifier
                .size(currentCircleSize)
                .align(Alignment.BottomEnd)
        ) {
            FloatingSearchButton(onClick = onSearchClick)
        }

        // --- 2. Navigation Bar (Stays at Bottom-Left, Morphs Width) ---
        if (!isLandscape && navigationState != null) {
            // Calculate the width: shrinks from (fullWidth - baseSize - spacing) to (currentCircleSize)
            // Note: We use baseSize for the expanded state width to maintain consistent spacing
            val expandedNavWidth = fullWidth - baseSize - spacing
            val currentNavWidth = (expandedNavWidth + (currentCircleSize - expandedNavWidth) * phase1)
                .coerceAtLeast(currentCircleSize)

            Box(
                modifier = Modifier
                    .height(currentCircleSize)
                    .width(currentNavWidth)
                    .align(Alignment.BottomStart)
            ) {
                MorphingNavigationBar(
                    progress = phase1.coerceIn(0f, 1f),
                    tabs = navigationState.tabs,
                    tabIndex = navigationState.tabIndex,
                    onTabChange = navigationState.onTabChange,
                    onSettingsClick = onSettingsClick,
                    hiddenTabs = navigationState.hiddenTabs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // --- 3. Mini Player (Slides from Top to Middle Gap) ---
        // Landing zone is between the NavCircle and SearchButton
        // As currentCircleSize shrinks, the landing width grows
        val playerLandingWidth = fullWidth - (currentCircleSize * 2) - (spacing * 2)
        
        // Expanded width is the full available width
        val expandedPlayerWidth = fullWidth
        val currentPlayerWidth = (expandedPlayerWidth + (playerLandingWidth - expandedPlayerWidth) * phase2)
            .coerceAtLeast(playerLandingWidth * 0.9f)
        
        Box(
            modifier = Modifier
                .height(currentCircleSize)
                .width(currentPlayerWidth)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    // Slide down from top row to bottom row
                    // We use baseSize here for travel distance to ensure it lands correctly
                    val travelDistance = (baseSize + spacing).toPx()
                    translationY = -travelDistance * (1f - phase3)
                    
                    // Fade out the player slightly if not active
                    alpha = if (navigationState == null && isLandscape) 0f else 1f
                }
        ) {
            MorphingMiniPlayer(
                progress = phase2.coerceIn(0f, 1f),
                onClick = onPlayerClick,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Handle Landscape / Null state search fallback
        if (isLandscape || navigationState == null) {
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd)) {
                FloatingSearchButton(onClick = onSearchClick)
            }
        }
    }
}
