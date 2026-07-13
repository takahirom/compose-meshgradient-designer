package mesh.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyColorAnimationTest {

    private val keyColor = Color(0xFF4E54C8)

    @Test
    fun generateProducesPlayableAnimationOnTheGeneratorGrid() {
        val animation = KeyColorAnimation.generate(keyColor)
        assertTrue(animation.isAnimated)
        assertEquals(3, animation.keyframes.size)
        assertEquals(animation.keyframes.size, animation.easings.size)
        animation.keyframes.forEach { mesh ->
            assertEquals(KeyColorAnimation.ROWS, mesh.rows)
            assertEquals(KeyColorAnimation.COLUMNS, mesh.columns)
            assertEquals(mesh.vertexCount, mesh.colors.size)
        }
    }

    @Test
    fun firstKeyframeContainsTheKeyColorItself() {
        val animation = KeyColorAnimation.generate(keyColor)
        assertEquals(keyColor, animation.keyframes.first().colors.first())
    }

    @Test
    fun keyframesSharePaletteViaRotationSoTheLoopStaysOnTheme() {
        val animation = KeyColorAnimation.generate(keyColor)
        val first = animation.keyframes.first().colors.toSet()
        animation.keyframes.forEach { mesh ->
            assertEquals(first, mesh.colors.toSet())
        }
        // Rotation must actually move colors, otherwise nothing animates.
        assertTrue(animation.keyframes[0].colors != animation.keyframes[1].colors)
    }

    @Test
    fun paletteHuesStayWithinTheDynamismSpread() {
        listOf(0f, 0.5f, 1f).forEach { dynamism ->
            val bound = KeyColorAnimation.hueSpreadFor(dynamism) + 1f
            maxHueDistance(dynamism).let { distance ->
                assertTrue(
                    distance <= bound,
                    "dynamism $dynamism: hue drifted $distance°, allowed $bound°",
                )
            }
        }
    }

    @Test
    fun higherDynamismSpreadsHuesFurther() {
        assertTrue(maxHueDistance(1f) > maxHueDistance(0f))
    }

    @Test
    fun higherDynamismPlaysFasterAndJumpsFurtherPerKeyframe() {
        val calm = KeyColorAnimation.generate(keyColor, 0f)
        val vivid = KeyColorAnimation.generate(keyColor, 1f)
        assertTrue(calm.durationMillisPerSegment > vivid.durationMillisPerSegment)
        // Calm rotates one palette slot per keyframe, vivid three: the vivid second keyframe
        // must therefore differ from a one-slot rotation of its own first keyframe.
        val calmShift = calm.keyframes[0].colors.indexOf(calm.keyframes[1].colors.first())
        val vividShift = vivid.keyframes[0].colors.indexOf(vivid.keyframes[1].colors.first())
        assertTrue(vividShift > calmShift, "vivid shift $vividShift vs calm shift $calmShift")
    }

    private fun maxHueDistance(dynamism: Float): Float {
        val (keyHue, _, _) = KeyColorAnimation.rgbToHsv(keyColor)
        val palette = KeyColorAnimation.harmoniousPalette(keyColor, 9, dynamism)
        return palette.maxOf { color ->
            val (hue, _, _) = KeyColorAnimation.rgbToHsv(color)
            minOf(abs(hue - keyHue), 360f - abs(hue - keyHue))
        }
    }

    @Test
    fun grayKeyColorStillProducesUsableSaturation() {
        val palette = KeyColorAnimation.harmoniousPalette(Color(0xFF808080), 9)
        palette.drop(1).forEach { color ->
            val (_, saturation, value) = KeyColorAnimation.rgbToHsv(color)
            assertTrue(saturation >= 0.1f, "derived color should not be fully gray")
            assertTrue(value >= 0.2f, "derived color should stay visible")
        }
    }

    @Test
    fun rgbToHsvRoundTripsThroughComposeHsv() {
        val samples = listOf(
            Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF),
            Color(0xFF4E54C8), Color(0xFFED8F03), Color(0xFF123456),
        )
        samples.forEach { color ->
            val (h, s, v) = KeyColorAnimation.rgbToHsv(color)
            val roundTripped = Color.hsv(h, s, v)
            assertTrue(abs(roundTripped.red - color.red) < 0.01f)
            assertTrue(abs(roundTripped.green - color.green) < 0.01f)
            assertTrue(abs(roundTripped.blue - color.blue) < 0.01f)
        }
    }
}
