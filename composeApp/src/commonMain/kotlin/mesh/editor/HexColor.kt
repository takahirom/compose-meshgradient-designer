package mesh.editor

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Parses a hex color string into a [Color], or returns null when the text is not (yet) a valid
 * color. Accepts `RRGGBB` and `RGB` (CSS shorthand), each with an optional leading `#`, in any
 * case. Alpha is not supported because mesh vertex colors are always opaque.
 */
fun parseHexColor(text: String): Color? {
    val hex = text.trim().removePrefix("#")
    if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return when (hex.length) {
        3 -> {
            val (r, g, b) = hex.map { it.toString().repeat(2).toInt(16) }
            Color(r, g, b)
        }
        6 -> {
            val value = hex.toLong(16).toInt()
            Color(
                red = (value shr 16) and 0xFF,
                green = (value shr 8) and 0xFF,
                blue = value and 0xFF,
            )
        }
        else -> null
    }
}

/** Formats [color] as an uppercase `#RRGGBB` string (alpha dropped). */
fun Color.toHexString(): String {
    fun channel(value: Float): String =
        (value * 255f).roundToInt().coerceIn(0, 255)
            .toString(16).uppercase().padStart(2, '0')
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}
