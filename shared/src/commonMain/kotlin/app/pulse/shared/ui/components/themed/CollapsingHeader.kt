package app.pulse.shared.ui.components.themed

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import app.pulse.shared.ui.theme.LocalAppearance
import app.pulse.shared.ui.theme.medium

@Composable
fun CollapsingHeader(
    title: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val scrollPixels = scrollState.value.toFloat()
    CollapsingHeaderInternal(
        title = title,
        scrollPixels = scrollPixels,
        modifier = modifier,
        headerActions = headerActions,
        content = content
    )
}

@Composable
fun CollapsingHeader(
    title: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val scrollPixels = if (lazyListState.firstVisibleItemIndex > 0) {
        Float.MAX_VALUE
    } else {
        lazyListState.firstVisibleItemScrollOffset.toFloat()
    }
    
    CollapsingHeaderInternal(
        title = title,
        scrollPixels = scrollPixels,
        modifier = modifier,
        headerActions = headerActions,
        content = content
    )
}

@Composable
fun CollapsingHeader(
    title: String,
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val scrollPixels = if (lazyGridState.firstVisibleItemIndex > 0) {
        Float.MAX_VALUE
    } else {
        lazyGridState.firstVisibleItemScrollOffset.toFloat()
    }
    
    CollapsingHeaderInternal(
        title = title,
        scrollPixels = scrollPixels,
        modifier = modifier,
        headerActions = headerActions,
        content = content
    )
}


@Composable
private fun CollapsingHeaderInternal(
    title: String,
    scrollPixels: Float,
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val typography = appearance.typography
    val density = LocalDensity.current

    val expandedHeight = 120.dp
    val collapsedHeight = 72.dp
    val collapseThresholdPx = with(density) { expandedHeight.toPx() }

    val collapseProgress = (scrollPixels / collapseThresholdPx).coerceIn(0f, 1f)

    val currentHeight = lerp(expandedHeight, collapsedHeight, collapseProgress)
    
    // Lerp text style
    val expandedStyle = typography.xxl.medium.copy(fontSize = 38.sp)
    val collapsedStyle = typography.l.medium.copy(fontSize = 28.sp)
    val currentFontSize = lerp(expandedStyle.fontSize, collapsedStyle.fontSize, collapseProgress)
    val currentStyle = expandedStyle.copy(fontSize = currentFontSize)

    Box(modifier = modifier.fillMaxSize()) {
        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }

        // Header Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeight + 76.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorPalette.background0.copy(alpha = collapseProgress),
                            colorPalette.background0.copy(alpha = collapseProgress * 0.9f),
                            colorPalette.background0.copy(alpha = collapseProgress * 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Header Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(currentHeight)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                BasicText(
                    text = title,
                    style = currentStyle,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    content = headerActions
                )
            }
        }
    }
}
