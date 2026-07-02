package mesh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme for the designer.
 *
 * The values are chosen so that every text style Material 3 derives from the scheme
 * (`onBackground`, `onSurface`, `onSurfaceVariant`, ...) has comfortable contrast against the
 * near-black tool background. Accent colors drive sliders, switches, and buttons so the whole
 * UI reads as one theme instead of a mix of defaults and hardcoded colors.
 */
private val MeshDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9A7BFF),
    onPrimary = Color(0xFF1A1030),
    primaryContainer = Color(0xFF3B2A73),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF66D9C6),
    onSecondary = Color(0xFF00382F),
    background = Color(0xFF101014),
    onBackground = Color(0xFFE8E8EE),
    surface = Color(0xFF17171C),
    onSurface = Color(0xFFE8E8EE),
    surfaceVariant = Color(0xFF2A2A31),
    onSurfaceVariant = Color(0xFFC7C7D0),
    outline = Color(0xFF54545E),
)

/** Wraps [content] in the designer's dark [MaterialTheme]. */
@Composable
fun MeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MeshDarkColorScheme, content = content)
}
