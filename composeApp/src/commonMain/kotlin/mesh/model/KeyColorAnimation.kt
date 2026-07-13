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
     * How far the palette hues may fan away from the key hue, from calm (dynamism 0, tight
     * analogous colors) to vivid (dynamism 1, spanning toward the complementary side).
     */
    fun hueSpreadFor(dynamism: Float): Float = 15f + 65f * dynamism.coerceIn(0f, 1f)

    /**
     * Builds an [AnimationState] themed around [keyColor]: a [ROWS] x [COLUMNS] mesh whose
     * vertices cycle through an analogous palette over three keyframes.
     *
     * [dynamism] (0..1) controls how dramatic the result is. Calm (0) keeps hues tight, contrast
     * low, colors traveling one palette slot per keyframe, and playback slow; vivid (1) widens
     * the hue fan, deepens the light/dark ladder, jumps colors further per keyframe, and plays
     * faster.
     */
    fun generate(keyColor: Color, dynamism: Float = 0.5f): AnimationState {
        val d = dynamism.coerceIn(0f, 1f)
        val vertexCount = (ROWS + 1) * (COLUMNS + 1)
        val palette = harmoniousPalette(keyColor, vertexCount, d)
        // Each vertex travels `step` palette slots per keyframe: adjacent slots are the most
        // similar colors, so a bigger step reads as a bigger visual change. PingPong keeps every
        // transition the same size regardless of step; Cycle would need `3 * step` to divide the
        // palette evenly or the wrap segment would jump further than the others.
        val step = 1 + (d * 2f).toInt().coerceAtMost(2)
        val keyframes = List(3) { keyframe ->
            meshWithColors(palette.rotated(keyframe * step))
        }
        return AnimationState(
            keyframes = keyframes,
            durationMillisPerSegment = (5000f - 3500f * d).toInt(),
            loopMode = LoopMode.PingPong,
            easings = List(keyframes.size) { SegmentEasing.EaseInOut },
        )
    }

    /**
     * Derives [count] harmonious colors from [keyColor]: analogous hues fanned across
     * ±[hueSpreadFor] degrees combined with a saturation/value ladder, so the palette has both
     * hue variety and light/dark contrast. The key color itself is always the first entry.
     */
    fun harmoniousPalette(keyColor: Color, count: Int, dynamism: Float = 0.5f): List<Color> {
        val (h, s, v) = rgbToHsv(keyColor)
        val d = dynamism.coerceIn(0f, 1f)
        val maxSpread = hueSpreadFor(d)
        val valueAmplitude = 0.08f + 0.27f * d
        // Near-gray key colors have a meaningless hue; lean on the value ladder instead and
        // give them a little saturation so the gradient doesn't look muddy.
        val baseSaturation = if (s < 0.15f) 0.25f else s
        return List(count) { index ->
            if (index == 0) return@List keyColor
            // Deterministic fan: alternate sides of the key hue, widening as the index grows.
            val side = if (index % 2 == 0) 1f else -1f
            val spread =
                maxSpread * ((index + 1) / 2).toFloat() / (count / 2).coerceAtLeast(1).toFloat()
            val hue = (h + side * spread + 360f) % 360f
            // Ladder the value between darker and lighter than the key color.
            val valueShift = ((index % 3) - 1) * valueAmplitude
            val value = (v + valueShift).coerceIn(0.25f, 1f)
            val saturation = (baseSaturation + if (index % 2 == 0) -0.12f * d else 0.08f * d)
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
