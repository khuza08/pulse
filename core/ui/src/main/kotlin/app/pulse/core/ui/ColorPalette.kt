package app.pulse.core.ui

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

typealias ParcelableColor = @WriteWith<ColorParceler> Color
typealias ParcelableDp = @WriteWith<DpParceler> Dp

@Parcelize
@Immutable
data class ColorPalette(
    val background0: ParcelableColor,
    val background1: ParcelableColor,
    val background2: ParcelableColor,
    val accent: ParcelableColor,
    val onAccent: ParcelableColor,
    val red: ParcelableColor = Color(0xffbf4040),
    val blue: ParcelableColor = Color(0xff4472cf),
    val yellow: ParcelableColor = Color(0xfffff176),
    val text: ParcelableColor,
    val textSecondary: ParcelableColor,
    val textDisabled: ParcelableColor,
    val isDefault: Boolean,
    val isDark: Boolean
) : Parcelable

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

private fun lightColorPalette(accent: Hsl) = lightColorPalette(
    hue = accent.hue,
    saturation = accent.saturation
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

private fun darkColorPalette(accent: Hsl, darkness: Darkness) = darkColorPalette(
    hue = accent.hue,
    saturation = accent.saturation,
    darkness = darkness
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

fun accentColorOf(
    source: ColorSource,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
) = when (source) {
    ColorSource.Default -> defaultAccentColor
    ColorSource.Dynamic -> sampleBitmap?.let { dynamicAccentColorOf(it, isDark) }
        ?: defaultAccentColor

    ColorSource.MaterialYou -> materialAccentColor?.hsl ?: defaultAccentColor
    ColorSource.Pink -> pinkLightPalette.accent.hsl
}

fun dynamicAccentColorOf(
    bitmap: Bitmap,
    isDark: Boolean
): Hsl? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(8)
        .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 36f..100f }) else null)
        .generate()

    val hsl = if (isDark) {
        palette.dominantSwatch ?: Palette
            .from(bitmap)
            .maximumColorCount(8)
            .generate()
            .dominantSwatch
    } else {
        palette.dominantSwatch
    }?.hsl ?: return null

    val arr = if (hsl[1] < 0.08)
        palette.swatches
            .map(Palette.Swatch::getHsl)
            .sortedByDescending(FloatArray::component2)
            .find { it[1] != 0f }
            ?: hsl
    else hsl

    return arr.hsl
}

fun ColorPalette.amoled() = if (isDark) {
    val (hue, saturation) = accent.hsl

    copy(
        background0 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.10f
        ),
        background1 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.15f
        ),
        background2 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.2f
        )
    )
} else this

fun colorPaletteOf(
    source: ColorSource,
    darkness: Darkness,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
): ColorPalette {
    if (source == ColorSource.Pink) {
        return if (isDark) defaultDarkPalette else pinkLightPalette
    }

    val accentColor = accentColorOf(
        source = source,
        isDark = isDark,
        materialAccentColor = materialAccentColor,
        sampleBitmap = sampleBitmap
    )

    return (if (isDark) darkColorPalette(accentColor, darkness) else lightColorPalette(accentColor))
        .copy(isDefault = accentColor == defaultAccentColor)
}

inline val ColorPalette.isPureBlack get() = background0 == Color.Black
inline val ColorPalette.collapsedPlayerProgressBar
    get() = if (isPureBlack) defaultDarkPalette.background0 else background2
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent
inline val ColorPalette.surface get() = if (isPureBlack) Color(0xff272727) else background2

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.overlay get() = Color.Black.copy(alpha = 0.75f)

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlay get() = defaultDarkPalette.text

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlayShimmer get() = defaultDarkPalette.shimmer

object ColorParceler : Parceler<Color> {
    override fun Color.write(parcel: Parcel, flags: Int) = parcel.writeLong(value.toLong())
    override fun create(parcel: Parcel) = Color(parcel.readLong())
}

object DpParceler : Parceler<Dp> {
    override fun Dp.write(parcel: Parcel, flags: Int) = parcel.writeFloat(value)
    override fun create(parcel: Parcel) = parcel.readFloat().dp
}
