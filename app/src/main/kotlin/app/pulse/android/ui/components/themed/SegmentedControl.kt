package app.pulse.android.ui.components.themed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.pulse.android.utils.center
import app.pulse.android.utils.medium
import app.pulse.core.ui.LocalAppearance
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SegmentedControl(
    segments: List<String>,
    selectedSegment: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(colorPalette.background1)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / segments.size
        val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
        val maxOffsetPx = tabWidthPx * (segments.size - 1)
        
        val offsetX = remember { Animatable(0f) }

        LaunchedEffect(selectedSegment, tabWidthPx) {
            val target = selectedSegment * tabWidthPx
            if (offsetX.targetValue != target) {
                offsetX.animateTo(target, tween(300))
            }
        }

        // Sliding Background Pill
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(colorPalette.background2)
        )

        // Foreground Text & Drag Handler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(tabWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val targetSegment = (offsetX.value / tabWidthPx).roundToInt().coerceIn(0, segments.size - 1)
                            onSegmentSelected(targetSegment)
                            coroutineScope.launch {
                                offsetX.animateTo(targetSegment * tabWidthPx, tween(300))
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx)
                            offsetX.snapTo(newOffset)
                        }
                    }
                }
        ) {
            segments.forEachIndexed { index, segment ->
                val isSelected = index == selectedSegment
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSegmentSelected(index) }
                        )
                ) {
                    BasicText(
                        text = segment,
                        style = typography.xs.medium.center.copy(
                            color = if (isSelected) colorPalette.text else colorPalette.textSecondary
                        )
                    )
                }
            }
        }
    }
}
