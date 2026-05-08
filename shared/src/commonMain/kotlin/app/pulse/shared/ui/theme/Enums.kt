package app.pulse.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThumbnailRoundness(val dp: Dp) {
    None(0.dp),
    Light(2.dp),
    Medium(8.dp),
    Heavy(12.dp),
    Heavier(16.dp),
    Heaviest(18.dp);

    val shape get() = RoundedCornerShape(dp)
}

enum class ColorSource {
    Default,
    Dynamic,
    MaterialYou,
    Pink
}

enum class ColorMode {
    System,
    Light,
    Dark
}

enum class Darkness {
    Normal,
    AMOLED,
    PureBlack
}
