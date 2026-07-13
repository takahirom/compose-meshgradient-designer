package mesh.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A simple RGB color picker dialog. Ported from the reference sample and migrated to Material 3.
 *
 * @param currentColor the color to seed the sliders with.
 * @param onColorPicked invoked with the chosen color when the user confirms.
 * @param onDismiss invoked when the dialog is dismissed without a choice.
 * @param title the dialog title; also reused by the key-color generator flow.
 */
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorPicked: (Color) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Vertex color",
) {
    var red by remember { mutableFloatStateOf(currentColor.red) }
    var green by remember { mutableFloatStateOf(currentColor.green) }
    var blue by remember { mutableFloatStateOf(currentColor.blue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(48.dp).background(Color(red, green, blue)))
                Slider(
                    value = red,
                    valueRange = 0f..1f,
                    onValueChange = { red = it },
                    colors = SliderDefaults.colors(thumbColor = Color.Red),
                )
                Slider(
                    value = green,
                    valueRange = 0f..1f,
                    onValueChange = { green = it },
                    colors = SliderDefaults.colors(thumbColor = Color.Green),
                )
                Slider(
                    value = blue,
                    valueRange = 0f..1f,
                    onValueChange = { blue = it },
                    colors = SliderDefaults.colors(thumbColor = Color.Blue),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorPicked(Color(red, green, blue)) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
