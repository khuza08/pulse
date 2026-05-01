package app.pulse.android.ui.components.themed

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.pulse.android.ui.components.MorphingMiniPlayer
import app.pulse.android.ui.components.CompactMiniPlayer
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
    
    val isSubPage = navigationState == null
    val radioAction = LocalRadioAction.current
    val density = LocalDensity.current
    
    // --- 1. State for Sub-page Transition ---
    val animatable = remember { Animatable(progress) }
    
    // Keep the last navigation state so the bar can fade out instead of vanishing
    val lastNavState = remember { mutableStateOf(navigationState) }
    LaunchedEffect(navigationState) {
        if (navigationState != null) lastNavState.value = navigationState
    }

    // Synchronize animatable with scroll progress while on home page
    LaunchedEffect(progress, isSubPage) {
        if (!isSubPage) {
            animatable.snapTo(progress)
        }
    }

    LaunchedEffect(isSubPage) {
        if (isSubPage) {
            // Already snapped by the progress sync effect
            animatable.animateTo(
                targetValue = 2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessVeryLow // Lazier, smoother start
                )
            )
        }
    }

    // Capture the last valid progress from the home page to ensure a stable handoff.
    // This prevents flashes if the 'progress' prop resets to 0.0 during navigation.
    var lastValidHomeProgress by remember { mutableFloatStateOf(0f) }
    if (!isSubPage) {
        lastValidHomeProgress = progress
    }

    // Single source of truth for the entire timeline.
    val p = if (!isSubPage) progress else animatable.value.coerceAtLeast(lastValidHomeProgress)

    // --- 2. Phase 1 (Home Morph: 0.0 -> 1.0) ---
    // factors are allowed to exceed 1.0 for "wobble" overscroll on Home
    val homeP = if (!isSubPage) p else p.coerceAtMost(1f)
    
    // Widened stable landing zone (0.90) for maximum reliability
    val navMorphProgress = (homeP / 0.90f).coerceIn(0f, 1f)
    val playerMorphProgress = ((homeP - 0.2f) / 0.7f).coerceIn(0f, 1f)
    val playerSlideProgress = ((homeP - 0.5f) / 0.4f).coerceIn(0f, 1f)

    // --- 3. Phase 2 (Sub-page Entry: 1.0 -> 2.0) ---
    // 1.0 -> 1.5: Home UI (Nav, Search) fades out
    val navHideFactor = ((p - 1f) / 0.5f).coerceIn(0f, 1f)
    // 1.5 -> 2.0: Sub-page UI (Radio) fades in
    val radioShowFactor = ((p - 1.5f) / 0.5f).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(Dimensions.items.collapsedPlayerHeight * 2 + spacing + 80.dp) // Maximum headroom
    ) {
        val fullWidth = maxWidth
        val baseSize = Dimensions.items.collapsedPlayerHeight
        
        // --- Sizing Logic ---
        // 1. Structural Morph: Shrink to 80% of baseSize at p=1.0
        val morphFactor = homeP.coerceIn(0f, 1f)
        val targetSize = baseSize * (1f - 0.2f * morphFactor)
        
        // 2. Overscroll Wobble: Elastic squeeze when pulling past 1.0
        val overscroll = (homeP - 1f).coerceAtLeast(0f)
        val currentCircleSize = (targetSize - (baseSize * 0.4f * overscroll)).coerceAtLeast(baseSize * 0.6f)
        
        val commonDip = with(density) { (16.dp.toPx() * overscroll) } // Unified wobble dip
        
        Box(
            modifier = Modifier
                .size(currentCircleSize)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha = 1f - navHideFactor
                    scaleX = 1f - navHideFactor
                    scaleY = 1f - navHideFactor
                    translationY = commonDip // Unified wobble
                }
        ) {
            FloatingSearchButton(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Navigation Bar (Home UI) ---
        val currentNavState = lastNavState.value
        if (!isLandscape && currentNavState != null && (1f - navHideFactor) > 0f) {
            val expandedNavWidth = fullWidth - baseSize - spacing
            val currentNavWidth = (expandedNavWidth + (currentCircleSize - expandedNavWidth) * navMorphProgress)
                .coerceAtLeast(currentCircleSize)

            Box(
                modifier = Modifier
                    .height(currentCircleSize)
                    .width(currentNavWidth)
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        alpha = 1f - navHideFactor
                        scaleX = 1f - navHideFactor
                        scaleY = 1f - navHideFactor
                        translationY = commonDip // Unified wobble
                    }
            ) {
                MorphingNavigationBar(
                    progress = navMorphProgress,
                    tabs = currentNavState.tabs,
                    tabIndex = currentNavState.tabIndex,
                    onTabChange = currentNavState.onTabChange,
                    onSettingsClick = onSettingsClick,
                    hiddenTabs = currentNavState.hiddenTabs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // --- 3. Mini Player & Radio Shared Logic ---
        val targetCompactWidth = 240.dp
        val playerLandingWidth = fullWidth - (currentCircleSize * 2) - (spacing * 2)
        val expandedPlayerWidth = fullWidth
        
        val currentPlayerWidth = if (p < 1f) {
            expandedPlayerWidth + (playerLandingWidth - expandedPlayerWidth) * playerMorphProgress
        } else {
            val entryProgress = (p - 1f) / 1f
            playerLandingWidth + (targetCompactWidth - playerLandingWidth) * entryProgress
        }

        // --- 4. Radio Button (Sub-page UI) ---
        Box(
            modifier = Modifier
                .size(currentCircleSize)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    // Shift right to make room for player, ensuring the pair is centered
                    translationX = (currentPlayerWidth / 2 + spacing / 2).toPx() * radioShowFactor
                    
                    alpha = radioShowFactor
                    scaleX = radioShowFactor
                    scaleY = radioShowFactor
                    
                    val liftDistance = spacing.toPx() * (p - 1f).coerceIn(0f, 1f)
                    translationY = -liftDistance + commonDip // Align with player lift + unified wobble
                }
        ) {
            RadioCircleButton(
                modifier = Modifier.fillMaxSize(),
                onClick = { radioAction?.invoke() }
            )
        }

        // --- 5. Mini Player (Home -> Sub-page) ---
        Box(
            modifier = Modifier
                .height(currentCircleSize)
                .width(currentPlayerWidth)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    // Slide down from top row to bottom row
                    val travelDistance = (baseSize + spacing).toPx()
                    
                    // The 8dp lift from the old sub-page Row structure
                    val liftDistance = spacing.toPx() * (p - 1f).coerceIn(0f, 1f)
                    
                    translationY = -travelDistance * (1f - playerSlideProgress) - liftDistance + commonDip
                    
                    // Shift left slightly to make room for radio button
                    translationX = -(currentCircleSize / 2 + spacing / 2).toPx() * radioShowFactor
                }
        ) {
            if (p < 1f) {
                MorphingMiniPlayer(
                    progress = playerMorphProgress,
                    onClick = onPlayerClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CompactMiniPlayer(
                    onClick = onPlayerClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }



    }

}
