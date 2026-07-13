package mesh.editor

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HexColorTest {

    @Test
    fun parsesSixDigitHexWithAndWithoutHash() {
        assertEquals(Color(0xFF4E54C8), parseHexColor("#4E54C8"))
        assertEquals(Color(0xFF4E54C8), parseHexColor("4E54C8"))
        assertEquals(Color(0xFF4E54C8), parseHexColor("#4e54c8"))
        assertEquals(Color(0xFF4E54C8), parseHexColor("  #4E54C8  "))
    }

    @Test
    fun parsesThreeDigitShorthand() {
        assertEquals(Color(0xFFFFAA33), parseHexColor("#FA3"))
        assertEquals(Color(0xFF000000), parseHexColor("000"))
        assertEquals(Color(0xFFFFFFFF), parseHexColor("#fff"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#"))
        assertNull(parseHexColor("#4E54"))
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor("#4E54C8FF"))
        assertNull(parseHexColor("rgb(1,2,3)"))
    }

    @Test
    fun formatsAsUppercaseHexAndRoundTrips() {
        assertEquals("#4E54C8", Color(0xFF4E54C8).toHexString())
        assertEquals("#000000", Color.Black.toHexString())
        assertEquals("#FFFFFF", Color.White.toHexString())
        val color = Color(0xFFED8F03)
        assertEquals(color, parseHexColor(color.toHexString()))
    }
}
