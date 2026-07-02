package mesh.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.MeshInterpolator
import mesh.model.SegmentEasing
import kotlin.math.roundToInt

private val ThumbSize = 56.dp

/**
 * The keyframe timeline: a horizontal strip of mesh-gradient thumbnails plus playback controls.
 *
 * Thumbnails can be selected (the selected keyframe is what the canvas edits), reordered with the
 * left/right buttons, duplicated (add) and deleted. Playback offers play/pause, a per-segment
 * duration slider, a loop-mode toggle, and a progress scrubber.
 */
@Composable
fun TimelinePanel(
    keyframes: List<MeshData>,
    selectedIndex: Int,
    hasBicubicColor: Boolean,
    isPlaying: Boolean,
    progress: Float,
    durationPerSegment: Int,
    loopMode: LoopMode,
    easings: List<SegmentEasing>,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onPlayToggle: () -> Unit,
    onProgress: (Float) -> Unit,
    onDuration: (Int) -> Unit,
    onLoopMode: (LoopMode) -> Unit,
    onEasing: (SegmentEasing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(8.dp)) {
        Text("Keyframes", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            keyframes.forEachIndexed { index, keyframe ->
                KeyframeThumbnail(
                    mesh = keyframe,
                    hasBicubicColor = hasBicubicColor,
                    index = index,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Keyframe editing controls act on the selected keyframe.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAdd) { Text("+ Add") }
            OutlinedButton(
                onClick = { onDelete(selectedIndex) },
                enabled = keyframes.size > 1,
            ) { Text("Delete") }
            OutlinedButton(onClick = onMoveLeft, enabled = selectedIndex > 0) { Text("<") }
            OutlinedButton(
                onClick = onMoveRight,
                enabled = selectedIndex < keyframes.lastIndex,
            ) { Text(">") }
        }

        Spacer(Modifier.height(12.dp))

        // Playback controls are only meaningful with two or more keyframes.
        val canPlay = keyframes.size >= 2
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayPauseButton(isPlaying = isPlaying, enabled = canPlay, onClick = onPlayToggle)
            Column {
                Text(
                    if (isPlaying) "Playing" else "Preview animation",
                    fontWeight = FontWeight.SemiBold,
                )
                if (!canPlay) {
                    Text(
                        "Add a second keyframe to play",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            LoopModeToggle(loopMode = loopMode, onLoopMode = onLoopMode)
        }

        Spacer(Modifier.height(12.dp))
        // Easing is per segment: the chips edit the transition out of the selected keyframe,
        // while the curve shows the segment currently playing (falling back to the selection).
        val (playingSegment, playingLocalT) =
            MeshInterpolator.segmentPosition(keyframes.size, loopMode, progress)
        val editedSegment = selectedIndex.coerceAtMost(keyframes.size - 1)
        val displayedSegment = if (isPlaying) playingSegment else editedSegment
        val displayedEasing = easings.getOrElse(displayedSegment) { SegmentEasing.Linear }
        val selectedEasing = easings.getOrElse(editedSegment) { SegmentEasing.Linear }
        Text(
            "Easing (keyframe ${editedSegment + 1} → next)",
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentEasing.entries.forEach { candidate ->
                EasingChip(candidate.label, candidate == selectedEasing) { onEasing(candidate) }
            }
        }
        Spacer(Modifier.height(8.dp))
        EasingCurve(
            easing = displayedEasing,
            marker = if (canPlay) playingLocalT else null,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        )

        Spacer(Modifier.height(8.dp))
        Text("Duration per segment: ${durationPerSegment}ms")
        Slider(
            value = durationPerSegment.toFloat(),
            onValueChange = { onDuration(it.roundToInt()) },
            valueRange = 250f..5000f,
        )

        Text("Progress")
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onProgress,
            valueRange = 0f..1f,
            enabled = canPlay,
        )
    }
}

@Composable
private fun KeyframeThumbnail(
    mesh: MeshData,
    hasBicubicColor: Boolean,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val painter = rememberMeshGradientPainter(mesh, hasBicubicColor)
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(ThumbSize)
            .clip(RoundedCornerShape(6.dp))
            .paint(painter)
            .border(if (selected) 3.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            "${index + 1}",
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

/**
 * A large, filled circular play/pause button so playback stands out from the option buttons.
 * The icon is drawn with Canvas to avoid depending on the material icons artifact.
 */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val background =
        if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant
    val iconColor =
        if (enabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            if (isPlaying) {
                val barWidth = size.width * 0.3f
                drawRect(iconColor, size = Size(barWidth, size.height))
                drawRect(
                    iconColor,
                    topLeft = Offset(size.width - barWidth, 0f),
                    size = Size(barWidth, size.height),
                )
            } else {
                val triangle = Path().apply {
                    moveTo(size.width * 0.2f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width * 0.2f, size.height)
                    close()
                }
                drawPath(triangle, iconColor)
            }
        }
    }
}

/** Draws the easing curve (x: linear segment time, y: eased fraction) with an optional marker. */
@Composable
private fun EasingCurve(easing: SegmentEasing, marker: Float?, modifier: Modifier = Modifier) {
    val curveColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val markerColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(gridColor, style = Stroke(1f))
        // Diagonal = linear reference.
        drawLine(gridColor, Offset(0f, h), Offset(w, 0f))
        val path = Path()
        val steps = 64
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = t * w
            val y = h - easing.transform(t) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, curveColor, style = Stroke(3f))
        if (marker != null) {
            val x = marker * w
            val y = h - easing.transform(marker) * h
            drawCircle(markerColor, radius = 5.dp.toPx() / 2f, center = Offset(x, y))
        }
    }
}

@Composable
private fun EasingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun LoopModeToggle(loopMode: LoopMode, onLoopMode: (LoopMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LoopModeChip("PingPong", loopMode == LoopMode.PingPong) { onLoopMode(LoopMode.PingPong) }
        LoopModeChip("Cycle", loopMode == LoopMode.Cycle) { onLoopMode(LoopMode.Cycle) }
    }
}

@Composable
private fun LoopModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
