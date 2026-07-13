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
    fun keyframesSharePaletteViaRotationSoCycleLoopStaysOnTheme() {
        val animation = KeyColorAnimation.generate(keyColor)
        val first = animation.keyframes.first().colors.toSet()
        animation.keyframes.forEach { mesh ->
            assertEquals(first, mesh.colors.toSet())
        }
        // Rotation must actually move colors, otherwise nothing animates.
        assertTrue(animation.keyframes[0].colors != animation.keyframes[1].colors)
    }

    @Test
    fun paletteHuesStayNearTheKeyColor() {
        val (keyHue, _, _) = KeyColorAnimation.rgbToHsv(keyColor)
        val palette = KeyColorAnimation.harmoniousPalette(keyColor, 9)
        assertEquals(9, palette.size)
        palette.forEach { color ->
            val (hue, _, _) = KeyColorAnimation.rgbToHsv(color)
            val distance = minOf(abs(hue - keyHue), 360f - abs(hue - keyHue))
            assertTrue(distance <= 45f, "hue $hue drifted too far from key hue $keyHue")
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
