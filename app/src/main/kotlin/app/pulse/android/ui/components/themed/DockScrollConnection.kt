package app.pulse.android.ui.components.themed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * A [NestedScrollConnection] that toggles the dock's scroll state based on scroll direction.
 * Downward scroll collapses the dock, upward scroll expands it.
 */
@Composable
fun rememberDockScrollConnection(
    isScrolled: MutableState<Boolean>,
    threshold: Float = 10f
): NestedScrollConnection {
    return remember(isScrolled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -threshold) {
                    isScrolled.value = true
                } else if (delta > threshold) {
                    isScrolled.value = false
                }
                return Offset.Zero
            }
        }
    }
}
