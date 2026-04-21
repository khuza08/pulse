package com.elza.pulse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue,
    secondary = Azure,
    tertiary = Gray,
    background = DeepBlack,
    surface = Charcoal,
    onPrimary = DeepBlack,
    onSecondary = DeepBlack,
    onTertiary = LightGray,
    onBackground = LightGray,
    onSurface = LightGray,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    secondary = Azure,
    tertiary = Gray,
    background = Color(0xFFF2F2F7), // iOS Light background
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DeepBlack,
    onBackground = DeepBlack,
    onSurface = DeepBlack,
)

@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}