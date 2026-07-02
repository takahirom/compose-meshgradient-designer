package mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mesh.codegen.KotlinCodeGenerator
import mesh.editor.ColorPickerDialog
import mesh.editor.ControlPanel
import mesh.editor.GradientCanvas
import mesh.editor.TimelinePanel
import mesh.editor.rememberAnimatedMeshGradientPainter
import mesh.editor.rememberMeshGradientPainter
import mesh.model.AnimationState
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.MeshInterpolator
import mesh.model.Presets
import mesh.model.SegmentEasing
import mesh.model.generateLinearMeshState
import mesh.model.resizedTo
import mesh.serialization.DesignerProject
import mesh.serialization.ProjectSerializer
import mesh.ui.CodePane
import mesh.ui.ImportDialog
import mesh.ui.MeshTheme
import mesh.ui.copyToClipboard
import mesh.ui.downloadTextFile
import mesh.ui.openUrl
import kotlin.math.floor

/** Where this project lives; linked from the header so users can star it. */
private const val GITHUB_URL = "https://github.com/takahirom/compose-meshgradient-designer"

/**
 * Root of the MeshGradient Designer tool.
 *
 * Left pane: an interactive gradient canvas, the keyframe timeline, and a control panel.
 * Right pane: the live-generated Kotlin code (static or animated) with a Copy button.
 *
 * On narrow viewports the two panes stack vertically instead of sitting side by side.
 */
@Composable
fun App() {
    MeshTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Start with the Aurora preset already playing so first-time visitors immediately
            // see an animated gradient instead of a static default.
            val initialPreset = remember { Presets.all.first { it.name == "Aurora" }.build() }

            // Shared editing state. rows/columns are shared across all keyframes.
            var keyframes by remember { mutableStateOf(initialPreset.keyframes) }
            var selectedIndex by remember { mutableIntStateOf(0) }
            var hasBicubicColor by remember { mutableStateOf(initialPreset.hasBicubicColor) }
            var showHandles by remember { mutableStateOf(true) }
            var loopMode by remember { mutableStateOf(initialPreset.loopMode) }
            // Per-keyframe outgoing easing, kept index-aligned with keyframes by every list op.
            var easings by remember {
                mutableStateOf(
                    List(initialPreset.keyframes.size) { index ->
                        initialPreset.easings.getOrElse(index) { SegmentEasing.Linear }
                    },
                )
            }
            var durationPerSegment by
                remember { mutableIntStateOf(initialPreset.durationMillisPerSegment) }
            var isPlaying by remember { mutableStateOf(initialPreset.keyframes.size >= 2) }
            var progress by remember { mutableFloatStateOf(0f) }
            var selectedVertex by remember { mutableStateOf<Int?>(null) }
            var showImport by remember { mutableStateOf(false) }

            val safeIndex = selectedIndex.coerceIn(0, keyframes.lastIndex)
            val selected = keyframes[safeIndex]
            val rows = selected.rows
            val columns = selected.columns

            fun updateSelected(mesh: MeshData) {
                keyframes = keyframes.toMutableList().also { it[safeIndex] = mesh }
            }

            // Applies a new shared grid to every keyframe (re-gridded, colors carried over).
            fun changeGrid(newRows: Int, newColumns: Int) {
                if (newRows == rows && newColumns == columns) return
                keyframes = keyframes.map { it.resizedTo(newRows, newColumns) }
                isPlaying = false
            }

            fun addKeyframe() {
                // Duplicate the current canvas state (and its easing) right after the selection.
                keyframes = keyframes.toMutableList().also { it.add(safeIndex + 1, selected) }
                easings = easings.toMutableList().also { it.add(safeIndex + 1, easings[safeIndex]) }
                selectedIndex = safeIndex + 1
            }

            fun deleteKeyframe(index: Int) {
                if (keyframes.size <= 1) return
                keyframes = keyframes.filterIndexed { i, _ -> i != index }
                easings = easings.filterIndexed { i, _ -> i != index }
                selectedIndex = selectedIndex.coerceAtMost(keyframes.lastIndex)
                isPlaying = false
            }

            fun move(from: Int, to: Int) {
                if (to !in keyframes.indices) return
                keyframes = keyframes.toMutableList().also {
                    val item = it.removeAt(from)
                    it.add(to, item)
                }
                easings = easings.toMutableList().also {
                    val item = it.removeAt(from)
                    it.add(to, item)
                }
                selectedIndex = to
            }

            // Serializes the whole editor state and both copies it to the clipboard and downloads
            // it as a file, so the user can save the project however they prefer.
            fun exportProject() {
                val project = DesignerProject(
                    keyframes = keyframes,
                    easings = easings,
                    loopMode = loopMode,
                    durationMillisPerSegment = durationPerSegment,
                    hasBicubicColor = hasBicubicColor,
                    selectedIndex = safeIndex,
                )
                val jsonText = ProjectSerializer.export(project)
                copyToClipboard(jsonText)
                downloadTextFile("mesh-gradient.json", jsonText)
            }

            // Replaces the entire editor state with an imported project and stops playback.
            fun applyProject(project: DesignerProject) {
                isPlaying = false
                progress = 0f
                selectedVertex = null
                keyframes = project.keyframes
                easings = project.easings
                loopMode = project.loopMode
                durationPerSegment = project.durationMillisPerSegment
                hasBicubicColor = project.hasBicubicColor
                selectedIndex = project.selectedIndex.coerceIn(0, project.keyframes.lastIndex)
                showImport = false
            }

            // Presets are complete animations: applying one replaces the whole editor state and
            // starts playback so the user immediately sees what they loaded.
            fun applyPreset(preset: AnimationState) {
                keyframes = preset.keyframes
                easings = List(preset.keyframes.size) { index ->
                    preset.easings.getOrElse(index) { SegmentEasing.Linear }
                }
                loopMode = preset.loopMode
                durationPerSegment = preset.durationMillisPerSegment
                hasBicubicColor = preset.hasBicubicColor
                selectedIndex = 0
                selectedVertex = null
                progress = 0f
                isPlaying = preset.keyframes.size >= 2
            }

            // Drives progress while playing. The full 0..1 sweep covers one loop cycle; PingPong
            // folds internally in MeshInterpolator.sample, Cycle appends the first keyframe.
            LaunchedEffect(isPlaying, durationPerSegment, loopMode, keyframes.size) {
                if (!isPlaying || keyframes.size < 2) return@LaunchedEffect
                val total = (durationPerSegment.toFloat() *
                    MeshInterpolator.segmentCountForLoop(keyframes.size, loopMode))
                    .coerceAtLeast(1f)
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val deltaMillis = (now - last) / 1_000_000f
                    last = now
                    val next = progress + deltaMillis / total
                    progress = next - floor(next)
                }
            }

            // While playing, a single painter samples the animation by reading `progress` inside
            // its draw block: each frame invalidates the draw phase only (no recomposition, no
            // painter re-allocation). This mirrors the generated code exactly, so the preview
            // doubles as a regression check for that pattern. While editing, the painter shows
            // the selected keyframe and is rebuilt on every edit as before.
            val playing = isPlaying && keyframes.size >= 2
            val editPainter = rememberMeshGradientPainter(selected, hasBicubicColor)
            val playbackPainter = rememberAnimatedMeshGradientPainter(
                keyframes = keyframes,
                easings = easings,
                loopMode = loopMode,
                hasBicubicColor = hasBicubicColor,
                progress = { progress },
            )
            val activePainter = if (playing) playbackPainter else editPainter

            val animation =
                AnimationState(keyframes, hasBicubicColor, durationPerSegment, loopMode, easings)
            // Regenerating the code string is proportional to the keyframe count; without remember
            // it would re-run on every recomposition (60x/s while the timeline is scrubbing).
            val code = remember(animation) { KotlinCodeGenerator.generate(animation) }

            val timeline: @Composable (Modifier) -> Unit = { m ->
                TimelinePanel(
                    keyframes = keyframes,
                    selectedIndex = safeIndex,
                    hasBicubicColor = hasBicubicColor,
                    isPlaying = isPlaying,
                    progress = progress,
                    durationPerSegment = durationPerSegment,
                    loopMode = loopMode,
                    easings = easings,
                    onSelect = { isPlaying = false; selectedIndex = it },
                    onAdd = ::addKeyframe,
                    onDelete = ::deleteKeyframe,
                    onMoveLeft = { move(safeIndex, safeIndex - 1) },
                    onMoveRight = { move(safeIndex, safeIndex + 1) },
                    onPlayToggle = { isPlaying = !isPlaying },
                    onProgress = { isPlaying = false; progress = it },
                    onDuration = { durationPerSegment = it },
                    onLoopMode = { loopMode = it },
                    onEasing = { chosen ->
                        easings = easings.toMutableList().also { it[safeIndex] = chosen }
                    },
                    modifier = m,
                )
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val stacked = maxWidth < 900.dp
                if (stacked) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        EditorPane(
                            activePainter, selected, hasBicubicColor,
                            showHandles, showHandles && !playing,
                            rows, columns,
                            onMeshChange = { updateSelected(it) },
                            onVertexTap = { selectedVertex = it },
                            onRows = { changeGrid(it, columns) },
                            onColumns = { changeGrid(rows, it) },
                            onBicubic = { hasBicubicColor = it },
                            onShowHandles = { showHandles = it },
                            onRandomize = { updateSelected(generateLinearMeshState(rows, columns)) },
                            onPreset = ::applyPreset,
                            onExport = ::exportProject,
                            onImport = { showImport = true },
                            timeline = timeline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // Bounded height: CodePane scrolls internally, which is disallowed under
                        // the infinite height constraints of this outer scrollable Column.
                        CodePane(code, Modifier.fillMaxWidth().height(480.dp).padding(top = 8.dp))
                    }
                } else {
                    Row(Modifier.fillMaxSize()) {
                        EditorPane(
                            activePainter, selected, hasBicubicColor,
                            showHandles, showHandles && !playing,
                            rows, columns,
                            onMeshChange = { updateSelected(it) },
                            onVertexTap = { selectedVertex = it },
                            onRows = { changeGrid(it, columns) },
                            onColumns = { changeGrid(rows, it) },
                            onBicubic = { hasBicubicColor = it },
                            onShowHandles = { showHandles = it },
                            onRandomize = { updateSelected(generateLinearMeshState(rows, columns)) },
                            onPreset = ::applyPreset,
                            onExport = ::exportProject,
                            onImport = { showImport = true },
                            timeline = timeline,
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        )
                        CodePane(code, Modifier.weight(1f).fillMaxSize())
                    }
                }
            }

            selectedVertex?.let { index ->
                if (index in selected.colors.indices) {
                    ColorPickerDialog(
                        currentColor = selected.colors[index],
                        onDismiss = { selectedVertex = null },
                        onColorPicked = { color ->
                            updateSelected(selected.withColor(index, color))
                            selectedVertex = null
                        },
                    )
                }
            }

            if (showImport) {
                ImportDialog(
                    onImport = ::applyProject,
                    onDismiss = { showImport = false },
                )
            }
        }
    }
}

@Composable
private fun EditorPane(
    painter: Painter,
    mesh: MeshData,
    hasBicubicColor: Boolean,
    showHandles: Boolean,
    handlesVisible: Boolean,
    rows: Int,
    columns: Int,
    onMeshChange: (MeshData) -> Unit,
    onVertexTap: (Int) -> Unit,
    onRows: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onBicubic: (Boolean) -> Unit,
    onShowHandles: (Boolean) -> Unit,
    onRandomize: () -> Unit,
    onPreset: (AnimationState) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    timeline: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "MeshGradient Designer",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { openUrl(GITHUB_URL) }) { Text("★ Star on GitHub") }
                OutlinedButton(onClick = onImport) { Text("Import") }
                OutlinedButton(onClick = onExport) { Text("Export") }
            }
        }
        // Keep the canvas square so normalized 0..1 coordinates map to visible drag distances.
        GradientCanvas(
            painter = painter,
            mesh = mesh,
            showHandles = handlesVisible,
            onMeshChange = onMeshChange,
            onVertexTap = onVertexTap,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 12.dp),
        )
        timeline(
            Modifier.wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface),
        )
        ControlPanel(
            rows = rows,
            columns = columns,
            hasBicubicColor = hasBicubicColor,
            showHandles = showHandles,
            colors = mesh.colors,
            onRowsChange = onRows,
            onColumnsChange = onColumns,
            onBicubicChange = onBicubic,
            onShowHandlesChange = onShowHandles,
            onColorClick = onVertexTap,
            onRandomize = onRandomize,
            onApplyPreset = onPreset,
            modifier = Modifier.wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}
