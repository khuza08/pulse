package app.pulse.shared.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val yellow: Color = Color(0xfffff176),
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDefault: Boolean,
    val isDark: Boolean
) {
    companion object {
        val Saver: Saver<ColorPalette, List<Any>> = Saver(
            save = { palette ->
                listOf(
                    palette.background0.toArgb(),
                    palette.background1.toArgb(),
                    palette.background2.toArgb(),
                    palette.accent.toArgb(),
                    palette.onAccent.toArgb(),
                    palette.red.toArgb(),
                    palette.blue.toArgb(),
                    palette.yellow.toArgb(),
                    palette.text.toArgb(),
                    palette.textSecondary.toArgb(),
                    palette.textDisabled.toArgb(),
                    palette.isDefault,
                    palette.isDark
                )
            },
            restore = { list ->
                ColorPalette(
                    background0 = Color(list[0] as Int),
                    background1 = Color(list[1] as Int),
                    background2 = Color(list[2] as Int),
                    accent = Color(list[3] as Int),
                    onAccent = Color(list[4] as Int),
                    red = Color(list[5] as Int),
                    blue = Color(list[6] as Int),
                    yellow = Color(list[7] as Int),
                    text = Color(list[8] as Int),
                    textSecondary = Color(list[9] as Int),
                    textDisabled = Color(list[10] as Int),
                    isDefault = list[11] as Boolean,
                    isDark = list[12] as Boolean
                )
            }
        )
    }
}

private val defaultAccentColor = Color(0xffffffff).hsl

val defaultLightPalette = ColorPalette(
    background0 = Color(0xffffffff),
    background1 = Color(0xfff5f5f5),
    background2 = Color(0xffe0e0e0),
    text = Color(0xff000000),
    textSecondary = Color(0xff616161),
    textDisabled = Color(0xffbdbdbd),
    accent = Color(0xff000000),
    onAccent = Color.White,
    isDefault = true,
    isDark = false
)

val defaultDarkPalette = ColorPalette(
    background0 = Color(0xff000000),
    background1 = Color(0xff121212),
    background2 = Color(0xff1e1e1e),
    text = Color(0xffffffff),
    textSecondary = Color(0xffbdbdbd),
    textDisabled = Color(0xff757575),
    accent = Color(0xffffffff),
    onAccent = Color.Black,
    isDefault = true,
    isDark = true
)

val pinkLightPalette = ColorPalette(
    background0 = Color(0xffffe5ec),
    background1 = Color(0xffffd1dc),
    background2 = Color(0xffffb7c5),
    accent = Color(0xffff8fab),
    onAccent = Color.White,
    text = Color(0xff4d001a),
    textSecondary = Color(0xff7a4d5a),
    textDisabled = Color(0xffbdbdbd),
    isDefault = false,
    isDark = false
)

private fun lightColorPalette(hue: Float, saturation: Float) = ColorPalette(
    background0 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.925f
    ),
    background1 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.3f),
        lightness = 0.90f
    ),
    background2 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.4f),
        lightness = 0.85f
    ),
    text = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.02f),
        lightness = 0.12f
    ),
    textSecondary = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.40f
    ),
    textDisabled = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.2f),
        lightness = 0.65f
    ),
    accent = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    isDefault = false,
    isDark = false
)

private fun darkColorPalette(
    hue: Float,
    saturation: Float,
    darkness: Darkness
) = ColorPalette(
    background0 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.10f
    ) else Color.Black,
    background1 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.3f),
        lightness = 0.15f
    ) else Color.Black,
    background2 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.4f),
        lightness = 0.2f
    ) else Color.Black,
    text = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.02f),
        lightness = 0.88f
    ),
    textSecondary = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.65f
    ),
    textDisabled = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.2f),
        lightness = 0.40f
    ),
    accent = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(if (darkness == Darkness.AMOLED) 0.4f else 0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    isDefault = false,
    isDark = true
)

fun colorPaletteOf(
    source: ColorSource,
    darkness: Darkness,
    isDark: Boolean,
    materialAccentColor: Color?,
    // sampleBitmap: Bitmap? // Removed for now
): ColorPalette {
    if (source == ColorSource.Pink) {
        return if (isDark) defaultDarkPalette else pinkLightPalette
    }

    val accentColor = when (source) {
        ColorSource.MaterialYou -> materialAccentColor?.hsl ?: defaultAccentColor
        ColorSource.Pink -> pinkLightPalette.accent.hsl
        else -> defaultAccentColor // TODO: Implement Dynamic/Dynamic support via expect/actual
    }

    return (if (isDark) darkColorPalette(accentColor.hue, accentColor.saturation, darkness) else lightColorPalette(accentColor.hue, accentColor.saturation))
        .copy(isDefault = accentColor == defaultAccentColor)
}

inline val ColorPalette.isPureBlack get() = background0 == Color.Black
inline val ColorPalette.collapsedPlayerProgressBar
    get() = if (isPureBlack) defaultDarkPalette.background0 else background2
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent
inline val ColorPalette.surface get() = if (isPureBlack) Color(0xff272727) else background2

inline val ColorPalette.overlay get() = Color.Black.copy(alpha = 0.75f)
inline val ColorPalette.onOverlay get() = defaultDarkPalette.text
inline val ColorPalette.onOverlayShimmer get() = defaultDarkPalette.shimmer
