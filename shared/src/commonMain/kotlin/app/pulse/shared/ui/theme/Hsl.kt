package app.pulse.shared.ui.theme

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import kotlin.jvm.JvmInline

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class Hsl(@PublishedApi internal val raw: FloatArray) {
    object Saver : androidx.compose.runtime.saveable.Saver<Hsl, FloatArray> {
        override fun restore(value: FloatArray) = value.hsl
        override fun SaverScope.save(value: Hsl) = value.raw
    }

    init {
        require(raw.size == 3) { "Invalid Hsl value! Expected size: 3, actual size: ${raw.size}" }
    }

    inline val hue get() = raw[0]
    inline val saturation get() = raw[1]
    inline val lightness get() = raw[2]

    inline val color
        get() = Color.hsl(
            hue = hue,
            saturation = saturation,
            lightness = lightness
        )

    inline operator fun component1() = hue
    inline operator fun component2() = saturation
    inline operator fun component3() = lightness
}

val FloatArray.hsl get() = Hsl(raw = this)

val Color.hsl: Hsl
    get() {
        val r = red
        val g = green
        val b = blue

        val max = maxOf(r, maxOf(g, b))
        val min = minOf(r, minOf(g, b))
        val delta = max - min

        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (delta != 0f) {
            s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)

            h = when (max) {
                r -> (g - b) / delta + (if (g < b) 6f else 0f)
                g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }
            h /= 6f
        }

        return floatArrayOf(h * 360f, s, l).hsl
    }
