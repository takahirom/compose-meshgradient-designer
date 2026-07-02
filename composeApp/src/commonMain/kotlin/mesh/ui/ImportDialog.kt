package mesh.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import mesh.serialization.DesignerProject
import mesh.serialization.ProjectImportException
import mesh.serialization.ProjectSerializer

/**
 * A dialog with a text area for pasting exported project JSON. Validates on confirm: a successful
 * parse calls [onImport] with the restored [DesignerProject]; a failure shows a human-readable
 * error and keeps the dialog open so the user can fix the input.
 */
@Composable
fun ImportDialog(onImport: (DesignerProject) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import project") },
        text = {
            Column {
                Text(
                    "Paste project JSON exported from MeshGradient Designer. This replaces the " +
                        "current keyframes and playback settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("{ \"version\": 1, ... }") },
                    isError = error != null,
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    onImport(ProjectSerializer.import(text))
                } catch (e: ProjectImportException) {
                    error = e.message ?: "Import failed."
                }
            }) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
