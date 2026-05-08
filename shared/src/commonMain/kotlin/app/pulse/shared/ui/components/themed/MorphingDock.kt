package app.pulse.shared.ui.components.themed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.pulse.shared.ui.theme.Dimensions
import app.pulse.shared.ui.theme.LocalAppearance

@Composable
fun MorphingDock(
    progress: Float,
    isSubPage: Boolean,
    onSearchClick: () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val density = LocalDensity.current
    val spacing = 8.dp

    val lastNavState = LocalNavigationState.current
    val radioAction = LocalRadioAction.current
    val lastValidHomeProgress = remember { mutableStateOf(0f) }
    
    val animatable = remember { Animatable(if (isSubPage) 2f else 0f) }

    LaunchedEffect(isSubPage) {
        if (!isSubPage) {
            animatable.stop()
            animatable.snapTo(progress)
        } else {
            val startPoint = lastValidHomeProgress.value.coerceAtLeast(1f)
            if (animatable.value < startPoint) {
                animatable.snapTo(startPoint)
            }
            if (animatable.value < 2f) {
                animatable.animateTo(
                    targetValue = 2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    val p = if (!isSubPage) progress
            else animatable.value.coerceAtLeast(lastValidHomeProgress.value)

    val homeP = if (!isSubPage) p else p.coerceAtMost(1f)
    if (!isSubPage) {
        lastValidHomeProgress.value = homeP
    }

    val navMorphProgress    = (homeP / 0.90f).coerceIn(0f, 1f)
    val playerMorphProgress = ((homeP - 0.2f) / 0.7f).coerceIn(0f, 1f)
    val playerSlideProgress = ((homeP - 0.5f) / 0.4f).coerceIn(0f, 1f)

    val navHideFactor   = ((p - 1f) / 0.5f).coerceIn(0f, 1f)
    val radioShowFactor = ((p - 1.5f) / 0.5f).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colorPalette.background0.copy(alpha = 0.8f),
                        colorPalette.background0
                    )
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(Dimensions.items.collapsedPlayerHeight * 2 + spacing + 80.dp)
    ) {
        val fullWidth = maxWidth
        val baseSize  = Dimensions.items.collapsedPlayerHeight

        val morphFactor       = homeP.coerceIn(0f, 1f)
        val targetSize        = baseSize * (1f - 0.2f * morphFactor)
        val overscroll        = (homeP - 1f).coerceAtLeast(0f)
        val currentCircleSize = (targetSize - (baseSize * 0.4f * overscroll)).coerceAtLeast(baseSize * 0.6f)
        val commonDip         = with(density) { 16.dp.toPx() * overscroll }

        // ── 1. Search button (Stub) ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(currentCircleSize)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha        = 1f - navHideFactor
                    scaleX       = 1f - navHideFactor
                    scaleY       = 1f - navHideFactor
                    translationY = commonDip
                }
                .background(colorPalette.background1, CircleShape)
        )

        // ── 2. Navigation bar (Stub) ────────────────────────────────────────
        val currentNavState = lastNavState.value
        if (currentNavState != null && (1f - navHideFactor) > 0f) {
            val expandedNavWidth = fullWidth - baseSize - spacing
            val currentNavWidth  = (expandedNavWidth + (currentCircleSize - expandedNavWidth) * navMorphProgress)
                .coerceAtLeast(currentCircleSize)

            Box(
                modifier = Modifier
                    .height(currentCircleSize)
                    .width(currentNavWidth)
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        alpha        = 1f - navHideFactor
                        scaleX       = 1f - navHideFactor
                        scaleY       = 1f - navHideFactor
                        translationY = commonDip
                    }
                    .background(colorPalette.background1, RoundedCornerShape(50))
            )
        }

        // ── 3. Player & radio shared sizing ──────────────────────────────────
        val targetCompactWidth  = 240.dp
        val playerLandingWidth  = fullWidth - (currentCircleSize * 2) - (spacing * 2)
        val expandedPlayerWidth = fullWidth

        val currentPlayerWidth = if (p < 1f) {
            expandedPlayerWidth + (playerLandingWidth - expandedPlayerWidth) * playerMorphProgress
        } else {
            val entryProgress = (p - 1f) / 1f
            playerLandingWidth + (targetCompactWidth - playerLandingWidth) * entryProgress
        }

        // ── 4. Radio button (Stub) ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(currentCircleSize)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationX = (currentPlayerWidth / 2 + spacing / 2).toPx() * radioShowFactor
                    alpha        = radioShowFactor
                    scaleX       = radioShowFactor
                    scaleY       = radioShowFactor
                    val liftDistance = spacing.toPx() * (p - 1f).coerceIn(0f, 1f)
                    translationY = -liftDistance + commonDip
                }
                .background(colorPalette.accent, CircleShape)
        )

        // ── 5. Mini player (Stub) ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .height(currentCircleSize)
                .width(currentPlayerWidth)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val travelDistance = (baseSize + spacing).toPx()
                    val liftDistance   = spacing.toPx() * (p - 1f).coerceIn(0f, 1f)
                    translationY = -travelDistance * (1f - playerSlideProgress) - liftDistance + commonDip
                    translationX = -(currentCircleSize / 2 + spacing / 2).toPx() * radioShowFactor
                }
                .background(colorPalette.background2, RoundedCornerShape(50))
        )
    }
}

// Stubs for LocalComposition
val LocalRadioAction = staticCompositionLocalOf<(() -> Unit)?> { null }
val LocalRadioVisible = staticCompositionLocalOf<MutableState<Boolean>> { 
    mutableStateOf(false) 
}
