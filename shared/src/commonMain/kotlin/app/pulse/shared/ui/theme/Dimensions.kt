package app.pulse.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object Dimensions {
    object Thumbnails {
        val album = 108.dp
        val artist = 92.dp
        val song = 54.dp
        val playlist = album

        val player = Player

        object Player {
            // TODO: Use a KMP-safe screen size provider instead of LocalConfiguration
            val song @Composable get() = 300.dp 
        }
    }

    val thumbnails = Thumbnails

    object Items {
        val moodHeight = 64.dp
        val headerHeight = 80.dp
        val collapsedPlayerHeight = 64.dp

        val verticalPadding = 8.dp
        val horizontalPadding = 8.dp
        val alternativePadding = 12.dp

        val gap = 4.dp
    }

    val items = Items

    object NavigationRail {
        val width = 60.dp
        val widthLandscape = 120.dp
        val iconOffset = 6.dp
    }

    val navigationRail = NavigationRail
}
