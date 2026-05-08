package app.pulse.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShapeCorners: Dp
) {
    val thumbnailShape = RoundedCornerShape(thumbnailShapeCorners)
}

val LocalAppearance = staticCompositionLocalOf<Appearance> { error("No appearance provided") }

@Composable
fun appearance(
    source: ColorSource,
    mode: ColorMode,
    darkness: Darkness,
    materialAccentColor: Color?,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean,
    thumbnailRoundness: Dp,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme()
): Appearance {
    val isDark = remember(mode, isSystemInDarkTheme) {
        mode == ColorMode.Dark || (mode == ColorMode.System && isSystemInDarkTheme)
    }

    val colorPalette = rememberSaveable(
        source,
        darkness,
        isDark,
        materialAccentColor,
        saver = ColorPalette.Saver
    ) {
        colorPaletteOf(
            source = source,
            darkness = darkness,
            isDark = isDark,
            materialAccentColor = materialAccentColor
        )
    }

    return remember(
        colorPalette,
        fontFamily,
        applyFontPadding,
        thumbnailRoundness,
        isDark
    ) {
        Appearance(
            colorPalette = colorPalette,
            typography = typographyOf(
                color = colorPalette.text,
                fontFamily = fontFamily,
                applyFontPadding = applyFontPadding
            ),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }
}
