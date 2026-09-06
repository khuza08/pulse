package app.pulse.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import app.pulse.android.utils.medium
import app.pulse.android.utils.secondary
import app.pulse.android.utils.semiBold
import app.pulse.core.ui.LocalAppearance

private val DefaultMenuShape: Shape = RoundedCornerShape(16.dp)

private val MenuGap: Dp = 12.dp
private val MenuGapX: Dp = 10.dp

@Composable
fun NewMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = DefaultMenuShape,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (!transitionState.targetState && !transitionState.currentState) return

    val colorPalette = LocalAppearance.current.colorPalette

    Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.TopEnd,
        offset = with(LocalDensity.current) { IntOffset(MenuGapX.roundToPx(), MenuGap.roundToPx()) }
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    transformOrigin = TransformOrigin(1f, 0f)
                ),
            exit = fadeOut(tween(120)) +
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(150),
                    transformOrigin = TransformOrigin(1f, 0f)
                )
        ) {
            Column(
                modifier = modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(min = 250.dp, max = 340.dp)
                    .clip(shape)
                    .background(colorPalette.background1)
            ) {
                content()
            }
        }
    }
}

@Composable
fun NewMenuEntry(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    secondaryText: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true
) {
    val (colorPalette, typography) = LocalAppearance.current
    val rowColor = if (checked) colorPalette.accent else colorPalette.text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        icon?.let {
            Image(
                painter = painterResource(it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(rowColor),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = text,
                style = typography.s.semiBold.copy(
                    color = if (enabled) rowColor else rowColor.copy(alpha = 0.4f)
                )
            )

            secondaryText?.let {
                BasicText(
                    text = it,
                    style = typography.xxs.medium.secondary.copy(
                        color = colorPalette.textSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.size(2.dp))
    }
}

@Composable
fun NewMenuDivider(
    modifier: Modifier = Modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(colorPalette.text.copy(alpha = 0.12f))
    )
}
