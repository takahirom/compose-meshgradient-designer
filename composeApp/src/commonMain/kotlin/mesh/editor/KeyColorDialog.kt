package mesh.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The "generate animation from a key color" dialog: pick the key color (RGB sliders or hex) and
 * how dramatic the generated animation should be, then hit Generate. Everything the generator
 * needs lives in this one dialog so the flow is self-explanatory.
 *
 * @param initialColor the color to seed the editor with (usually the current first vertex color).
 * @param dynamism current calm-to-vivid setting (0..1); hoisted so it survives re-opening.
 * @param onDynamismChange invoked while the dynamism slider moves.
 * @param onGenerate invoked with the chosen key color when the user confirms.
 * @param onDismiss invoked when the dialog is dismissed without generating.
 */
@Composable
fun KeyColorGeneratorDialog(
    initialColor: Color,
    dynamism: Float,
    onDynamismChange: (Float) -> Unit,
    onGenerate: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var keyColor by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate from key color") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "A matching palette and animation are generated from this one color.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                ColorEditor(initialColor) { keyColor = it }
                Spacer(Modifier.height(12.dp))
                Text("Dynamism")
                Slider(
                    value = dynamism,
                    onValueChange = onDynamismChange,
                    valueRange = 0f..1f,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Calm",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Vivid",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGenerate(keyColor) }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
