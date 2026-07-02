@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package mesh.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mesh.model.AnimationState
import mesh.model.Presets
import kotlin.math.roundToInt

/**
 * The controls that drive the gradient: rows/columns sliders, the bicubic-color toggle, the
 * show-handles toggle, per-vertex color swatches, a randomize button, and named presets.
 */
@Composable
fun ControlPanel(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    showHandles: Boolean,
    colors: List<Color>,
    onRowsChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onBicubicChange: (Boolean) -> Unit,
    onShowHandlesChange: (Boolean) -> Unit,
    onColorClick: (Int) -> Unit,
    onRandomize: () -> Unit,
    onApplyPreset: (AnimationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(8.dp)) {
        Text("Rows: $rows")
        Slider(
            value = rows.toFloat(),
            onValueChange = { onRowsChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
        )
        Text("Columns: $columns")
        Slider(
            value = columns.toFloat(),
            onValueChange = { onColumnsChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
        )

        ToggleRow("Bicubic color interpolation", hasBicubicColor, onBicubicChange)
        ToggleRow("Show point controls", showHandles, onShowHandlesChange)

        Spacer(Modifier.height(8.dp))
        Text("Vertex colors", fontWeight = FontWeight.SemiBold)
        Text(
            "Click a swatch (or tap a point on the canvas) to change its color. " +
                "Swatches are ordered row by row, top-left first.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(4.dp))
        // One row of swatches per mesh row, mirroring the canvas layout.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0..rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (column in 0..columns) {
                        val index = row * (columns + 1) + column
                        val color = colors.getOrNull(index) ?: continue
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable { onColorClick(index) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRandomize, modifier = Modifier.fillMaxWidth()) {
            Text("Randomize colors")
        }

        Spacer(Modifier.height(8.dp))
        Text("Presets", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.all.forEach { preset ->
                OutlinedButton(onClick = { onApplyPreset(preset.build()) }) {
                    Text(preset.name)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
