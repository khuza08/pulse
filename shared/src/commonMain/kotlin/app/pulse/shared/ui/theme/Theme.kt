package app.pulse.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PulseTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appearance = appearance(
        source = ColorSource.Default,
        mode = if (isDark) ColorMode.Dark else ColorMode.Light,
        darkness = Darkness.Normal,
        materialAccentColor = null,
        fontFamily = BuiltInFontFamily.System,
        applyFontPadding = false,
        thumbnailRoundness = 8.dp
    )

    CompositionLocalProvider(
        LocalAppearance provides appearance,
        content = content
    )
}
