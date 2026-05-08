package app.pulse.shared.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import app.pulse.shared.ui.theme.LocalAppearance
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette

    HeaderIconButton(
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = true,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) = IconButton(
    icon = icon,
    color = color,
    onClick = onClick,
    enabled = enabled,
    indication = indication,
    modifier = modifier
        .padding(all = 4.dp)
        .size(18.dp)
)

@Composable
fun HeaderCircleIconButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colorPalette.background1)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (enabled) colorPalette.text else colorPalette.textDisabled),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette

    IconButton(
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = true,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) = Image(
    painter = painterResource(icon),
    contentDescription = null,
    colorFilter = ColorFilter.tint(color),
    modifier = Modifier
        .clickable(
            indication = indication,
            interactionSource = remember { MutableInteractionSource() },
            enabled = enabled,
            onClick = onClick
        )
        .then(modifier)
)
