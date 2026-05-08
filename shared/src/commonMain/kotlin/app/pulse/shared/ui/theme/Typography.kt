package app.pulse.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Immutable
data class Typography(
    internal val style: TextStyle,
    internal val fontFamily: BuiltInFontFamily
) {
    val xxs by lazy { style.copy(fontSize = 12.sp) }
    val xs by lazy { style.copy(fontSize = 14.sp) }
    val s by lazy { style.copy(fontSize = 16.sp) }
    val m by lazy { style.copy(fontSize = 18.sp) }
    val l by lazy { style.copy(fontSize = 20.sp) }
    val xxl by lazy { style.copy(fontSize = 32.sp) }

    fun copy(color: Color) = Typography(
        style = style.copy(color = color),
        fontFamily = fontFamily
    )
}

// TextStyle Utils
fun TextStyle.weight(weight: FontWeight) = copy(fontWeight = weight)
fun TextStyle.align(align: TextAlign) = copy(textAlign = align)
fun TextStyle.color(color: Color) = copy(color = color)

inline val TextStyle.medium get() = weight(FontWeight.Medium)
inline val TextStyle.semiBold get() = weight(FontWeight.SemiBold)
inline val TextStyle.bold get() = weight(FontWeight.Bold)
inline val TextStyle.center get() = align(TextAlign.Center)

inline val TextStyle.primary: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = color(LocalAppearance.current.colorPalette.onAccent)

inline val TextStyle.secondary: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = color(LocalAppearance.current.colorPalette.textSecondary)

inline val TextStyle.disabled: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = color(LocalAppearance.current.colorPalette.textDisabled)

enum class BuiltInFontFamily {
    Poppins,
    Roboto,
    Montserrat,
    Nunito,
    Rubik,
    System
}

fun typographyOf(
    color: Color,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean
): Typography {
    val textStyle = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Implement multiplatform font loading
        fontWeight = FontWeight.Normal,
        color = color
    )

    return Typography(
        style = textStyle,
        fontFamily = fontFamily
    )
}
