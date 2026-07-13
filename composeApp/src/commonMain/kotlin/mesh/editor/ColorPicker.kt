package mesh.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The RGB color editing controls: a live preview, one slider per channel, and a hex text field.
 * Owns its own slider/text state (seeded from [initialColor]) and reports every effective color
 * change through [onColorChange]. Shared by the vertex color picker and the key-color generator.
 */
@Composable
fun ColorEditor(initialColor: Color, onColorChange: (Color) -> Unit) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    // The hex field owns its raw text so partial input ("#4E") survives while typing; sliders
    // rewrite it only when they move, and valid hex input rewrites the sliders.
    var hexText by remember { mutableStateOf(initialColor.toHexString()) }
    val hexValid = parseHexColor(hexText) != null

    fun setColor(color: Color) {
        red = color.red
        green = color.green
        blue = color.blue
        onColorChange(color)
    }

    fun onSlider(channel: (Color) -> Color) {
        val next = channel(Color(red, green, blue))
        setColor(next)
        hexText = next.toHexString()
    }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(48.dp).background(Color(red, green, blue)))
        Slider(
            value = red,
            valueRange = 0f..1f,
            onValueChange = { value -> onSlider { it.copy(red = value) } },
            colors = SliderDefaults.colors(thumbColor = Color.Red),
        )
        Slider(
            value = green,
            valueRange = 0f..1f,
            onValueChange = { value -> onSlider { it.copy(green = value) } },
            colors = SliderDefaults.colors(thumbColor = Color.Green),
        )
        Slider(
            value = blue,
            valueRange = 0f..1f,
            onValueChange = { value -> onSlider { it.copy(blue = value) } },
            colors = SliderDefaults.colors(thumbColor = Color.Blue),
        )
        OutlinedTextField(
            value = hexText,
            onValueChange = { text ->
                hexText = text
                parseHexColor(text)?.let(::setColor)
            },
            label = { Text("Hex (RGB)") },
            isError = !hexValid,
            supportingText = if (hexValid) null else {
                { Text("Use #RRGGBB or #RGB") }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A simple RGB color picker dialog. Ported from the reference sample and migrated to Material 3.
 *
 * @param currentColor the color to seed the sliders with.
 * @param onColorPicked invoked with the chosen color when the user confirms.
 * @param onDismiss invoked when the dialog is dismissed without a choice.
 */
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorPicked: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(currentColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vertex color") },
        text = { ColorEditor(currentColor) { chosen = it } },
        confirmButton = {
            TextButton(onClick = { onColorPicked(chosen) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
