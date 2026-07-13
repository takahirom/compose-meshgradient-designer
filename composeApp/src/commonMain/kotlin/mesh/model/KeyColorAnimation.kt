package mesh.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Generates a complete, immediately playable animation from a single key color.
 *
 * Picking every vertex color by hand is tedious, so this derives a harmonious palette from the
 * key color (analogous hues with a lightness ladder, in HSV space) and animates it by rotating
 * the palette across the mesh vertices between keyframes. Rotation keeps every transition inside
 * the palette, so the animation never drifts away from the key color's mood.
 */
object KeyColorAnimation {

    /** Grid used for generated animations: 2 rows gives a richer field than the 1x1 presets. */
    const val ROWS = 2
    const val COLUMNS = 2

    /**
     * Builds an [AnimationState] themed around [keyColor]: a [ROWS] x [COLUMNS] mesh whose
     * vertices cycle through an analogous palette over three keyframes.
     */
    fun generate(keyColor: Color): AnimationState {
        val vertexCount = (ROWS + 1) * (COLUMNS + 1)
        val palette = harmoniousPalette(keyColor, vertexCount)
        // Rotating by ~1/3 of the palette per keyframe moves every vertex to a related color;
        // three keyframes bring each vertex back to its start for a seamless Cycle loop.
        val step = vertexCount / 3
        val keyframes = List(3) { keyframe ->
            meshWithColors(palette.rotated(keyframe * step))
        }
        return AnimationState(
            keyframes = keyframes,
            durationMillisPerSegment = 3000,
            loopMode = LoopMode.Cycle,
            easings = List(keyframes.size) { SegmentEasing.EaseInOut },
        )
    }

    /**
     * Derives [count] harmonious colors from [keyColor]: analogous hues fanned across ±40°
     * combined with a saturation/value ladder, so the palette has both hue variety and
     * light/dark contrast. The key color itself is always the first entry.
     */
    fun harmoniousPalette(keyColor: Color, count: Int): List<Color> {
        val (h, s, v) = rgbToHsv(keyColor)
        // Near-gray key colors have a meaningless hue; lean on the value ladder instead and
        // give them a little saturation so the gradient doesn't look muddy.
        val baseSaturation = if (s < 0.15f) 0.25f else s
        return List(count) { index ->
            if (index == 0) return@List keyColor
            // Deterministic fan: alternate sides of the key hue, widening as the index grows.
            val side = if (index % 2 == 0) 1f else -1f
            val spread = 40f * ((index + 1) / 2).toFloat() / (count / 2).coerceAtLeast(1).toFloat()
            val hue = (h + side * spread + 360f) % 360f
            // Ladder the value between darker and lighter than the key color.
            val valueShift = ((index % 3) - 1) * 0.22f
            val value = (v + valueShift).coerceIn(0.25f, 1f)
            val saturation = (baseSaturation + if (index % 2 == 0) -0.12f else 0.08f)
                .coerceIn(0.15f, 1f)
            Color.hsv(hue, saturation, value)
        }
    }

    /** Converts [color] to HSV; hue is 0..360 (0 for grays), saturation and value are 0..1. */
    fun rgbToHsv(color: Color): Triple<Float, Float, Float> {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta % 6f + 6f) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return Triple(abs(hue % 360f), saturation, max)
    }

    /** Builds an evenly spaced [ROWS] x [COLUMNS] mesh with the given vertex [colors]. */
    private fun meshWithColors(colors: List<Color>): MeshData =
        generateLinearMeshState(ROWS, COLUMNS).copy(colors = colors)

    private fun <T> List<T>.rotated(by: Int): List<T> {
        if (isEmpty()) return this
        val shift = ((by % size) + size) % size
        return drop(shift) + take(shift)
    }
}
