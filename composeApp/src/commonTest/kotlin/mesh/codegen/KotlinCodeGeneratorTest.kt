package mesh.codegen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import mesh.model.MeshData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinCodeGeneratorTest {

    @Test
    fun formatFloat_wholeNumbers() {
        assertEquals("0f", KotlinCodeGenerator.formatFloat(0f))
        assertEquals("1f", KotlinCodeGenerator.formatFloat(1f))
        assertEquals("-1f", KotlinCodeGenerator.formatFloat(-1f))
    }

    @Test
    fun formatFloat_trimsTrailingZerosAndRoundsToFourDecimals() {
        assertEquals("0.5f", KotlinCodeGenerator.formatFloat(0.5f))
        assertEquals("-0.1f", KotlinCodeGenerator.formatFloat(-0.1f))
        // 1/3 rounds to 4 decimals.
        assertEquals("0.3333f", KotlinCodeGenerator.formatFloat(1f / 3f))
        // 5th decimal rounds away.
        assertEquals("0.1235f", KotlinCodeGenerator.formatFloat(0.12345f))
    }

    @Test
    fun formatFloat_nonFiniteFallsBackToZero() {
        assertEquals("0f", KotlinCodeGenerator.formatFloat(Float.NaN))
        assertEquals("0f", KotlinCodeGenerator.formatFloat(Float.POSITIVE_INFINITY))
    }

    @Test
    fun formatColor_producesAARRGGBB() {
        assertEquals("Color(0xFFFF0000)", KotlinCodeGenerator.formatColor(Color(0xFFFF0000)))
        assertEquals("Color(0xFF000000)", KotlinCodeGenerator.formatColor(Color(0xFF000000)))
        assertEquals("Color(0x80112233)", KotlinCodeGenerator.formatColor(Color(0x80112233)))
    }

    @Test
    fun formatOffset_usesFSuffix() {
        assertEquals("Offset(0f, 1f)", KotlinCodeGenerator.formatOffset(Offset(0f, 1f)))
        assertEquals("Offset(0.5f, -0.1f)", KotlinCodeGenerator.formatOffset(Offset(0.5f, -0.1f)))
    }

    @Test
    fun vertexCall_omitsUnspecifiedBeziers() {
        val mesh = mesh1x1(
            left = List(4) { Offset.Unspecified },
            top = List(4) { Offset.Unspecified },
            right = List(4) { Offset.Unspecified },
            bottom = List(4) { Offset.Unspecified },
        )
        val call = KotlinCodeGenerator.vertexCall(mesh, 0, 0)
        assertEquals(
            "setVertex(0, 0, position = Offset(0f, 0f), color = Color(0xFFFF0000))",
            call,
        )
        assertFalse(call.contains("ControlPoint"))
    }

    @Test
    fun vertexCall_includesSpecifiedBeziersInLTRBOrder() {
        val left = MutableList(4) { Offset.Unspecified }.also { it[0] = Offset(-0.1f, 0f) }
        val right = MutableList(4) { Offset.Unspecified }.also { it[0] = Offset(0.2f, 0f) }
        val mesh = mesh1x1(
            left = left,
            top = List(4) { Offset.Unspecified },
            right = right,
            bottom = List(4) { Offset.Unspecified },
        )
        val call = KotlinCodeGenerator.vertexCall(mesh, 0, 0)
        assertEquals(
            "setVertex(0, 0, position = Offset(0f, 0f), color = Color(0xFFFF0000), " +
                "leftControlPoint = Offset(-0.1f, 0f), rightControlPoint = Offset(0.2f, 0f))",
            call,
        )
    }

    @Test
    fun generate_emitsHeaderImportsAndPainter() {
        val mesh = mesh1x1(
            left = List(4) { Offset.Unspecified },
            top = List(4) { Offset.Unspecified },
            right = List(4) { Offset.Unspecified },
            bottom = List(4) { Offset.Unspecified },
        )
        val code = KotlinCodeGenerator.generate(mesh, hasBicubicColor = true)
        assertTrue(code.contains("import androidx.compose.ui.graphics.MeshGradientPainter"))
        assertTrue(code.contains("MeshGradientPainter(rows = 1, columns = 1, hasBicubicColor = true)"))
        assertTrue(code.contains("Box(modifier.fillMaxSize().paint(painter))"))
        // Four vertices for a 1x1 mesh.
        assertEquals(4, Regex("setVertex\\(").findAll(code).count())
    }

    @Test
    fun generate_withoutImportsSkipsHeader() {
        val mesh = mesh1x1(
            left = List(4) { Offset.Unspecified },
            top = List(4) { Offset.Unspecified },
            right = List(4) { Offset.Unspecified },
            bottom = List(4) { Offset.Unspecified },
        )
        val code = KotlinCodeGenerator.generate(mesh, hasBicubicColor = false, includeImports = false)
        assertFalse(code.contains("import "))
        assertTrue(code.contains("hasBicubicColor = false"))
    }

    private fun mesh1x1(
        left: List<Offset>,
        top: List<Offset>,
        right: List<Offset>,
        bottom: List<Offset>,
    ): MeshData = MeshData(
        rows = 1,
        columns = 1,
        positions = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f)),
        colors = List(4) { Color(0xFFFF0000) },
        leftBezierOffsets = left,
        rightBezierOffsets = right,
        topBezierOffsets = top,
        bottomBezierOffsets = bottom,
    )
}
