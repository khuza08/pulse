package app.pulse.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.pulse.core.ui.LocalAppearance

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val (colorPalette) = LocalAppearance.current

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
    @DrawableRes icon: Int,
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
fun HeaderPillIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = modifier
            .height(42.dp)
            .widthIn(min = 42.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(colorPalette.background2)
            .border(0.5.dp, colorPalette.textSecondary.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (enabled) colorPalette.text else colorPalette.textDisabled),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun HeaderCircleIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (colorPalette) = LocalAppearance.current
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
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val (colorPalette) = LocalAppearance.current

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
    @DrawableRes icon: Int,
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
