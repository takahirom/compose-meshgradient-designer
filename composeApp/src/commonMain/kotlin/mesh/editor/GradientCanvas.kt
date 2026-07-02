package mesh.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.MeshGradientScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import mesh.model.BezierDirection
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.MeshInterpolator
import mesh.model.SegmentEasing
import kotlin.math.roundToInt

/** Diameter of the draggable handles. */
private val HandleSize = 16.dp

/**
 * While editing, the gradient occupies this fraction of the canvas, leaving a margin around it
 * where handles dragged outside the 0..1 mesh area remain visible and grabbable.
 */
private const val GradientFraction = 0.8f

/** Feeds every vertex of [mesh] into this [MeshGradientScope]. */
private fun MeshGradientScope.applyMesh(mesh: MeshData) {
    for (row in 0..mesh.rows) {
        for (column in 0..mesh.columns) {
            val index = mesh.indexOf(row, column)
            setVertex(
                row,
                column,
                position = mesh.positions[index],
                color = mesh.colors[index],
                leftControlPoint = mesh.leftBezierOffsets[index],
                topControlPoint = mesh.topBezierOffsets[index],
                rightControlPoint = mesh.rightBezierOffsets[index],
                bottomControlPoint = mesh.bottomBezierOffsets[index],
            )
        }
    }
}

/**
 * Builds a [MeshGradientPainter] for [mesh], re-created whenever [mesh] or [hasBicubicColor]
 * changes. Bezier control points left as [Offset.Unspecified] fall back to straight edges.
 */
@Composable
fun rememberMeshGradientPainter(mesh: MeshData, hasBicubicColor: Boolean): Painter =
    remember(mesh, hasBicubicColor) {
        MeshGradientPainter(
            rows = mesh.rows,
            columns = mesh.columns,
            hasBicubicColor = hasBicubicColor,
        ) {
            applyMesh(mesh)
        }
    }

/**
 * Builds a single [MeshGradientPainter] that samples the keyframe animation itself.
 *
 * [MeshGradientPainter] executes its block on every draw, so reading [progress] (snapshot state)
 * inside the block makes each animation frame invalidate the draw phase only — no recomposition
 * and no painter re-allocation. This is intentionally the exact pattern the generated code uses,
 * so the live preview doubles as a regression check for it.
 */
@Composable
fun rememberAnimatedMeshGradientPainter(
    keyframes: List<MeshData>,
    easings: List<SegmentEasing>,
    loopMode: LoopMode,
    hasBicubicColor: Boolean,
    progress: () -> Float,
): Painter = remember(keyframes, easings, loopMode, hasBicubicColor) {
    val first = keyframes.first()
    MeshGradientPainter(
        rows = first.rows,
        columns = first.columns,
        hasBicubicColor = hasBicubicColor,
    ) {
        applyMesh(MeshInterpolator.sample(keyframes, loopMode, progress(), easings))
    }
}

/**
 * The interactive gradient preview: renders [painter] and, when [showHandles] is true, overlays
 * the draggable vertex and bezier handles (positioned from [mesh]) ported from the Android
 * reference sample. During playback the caller passes an animated painter while [mesh] stays the
 * selected keyframe.
 *
 * @param onMeshChange invoked with an updated [MeshData] whenever the user drags a handle.
 * @param onVertexTap invoked with a vertex index when the user taps a vertex (to recolor it).
 */
@Composable
fun GradientCanvas(
    painter: Painter,
    mesh: MeshData,
    showHandles: Boolean,
    onMeshChange: (MeshData) -> Unit,
    onVertexTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // While editing, the gradient is inset so vertices dragged outside the 0..1 mesh area
        // (a legitimate design) keep their handles visible in the surrounding margin. During
        // playback the preview uses the full canvas.
        val fraction = if (showHandles) GradientFraction else 1f
        Box(
            Modifier
                .fillMaxSize(fraction)
                .align(Alignment.Center)
                .paint(painter)
        )
        if (!showHandles) return@BoxWithConstraints
        val fullWidth = constraints.maxWidth.toFloat()
        val fullHeight = constraints.maxHeight.toFloat()
        // Normalized 0..1 mesh coordinates map to the inset gradient area, not the full canvas.
        val width = fullWidth * fraction
        val height = fullHeight * fraction
        val marginX = (fullWidth - width) / 2f
        val marginY = (fullHeight - height) / 2f
        val meshState = rememberUpdatedState(mesh)
        val onChangeState = rememberUpdatedState(onMeshChange)
        val onTapState = rememberUpdatedState(onVertexTap)

        mesh.positions.forEachIndexed { index, point ->
            VertexHandle(
                centerX = marginX + point.x * width,
                centerY = marginY + point.y * height,
                boundsWidth = fullWidth,
                boundsHeight = fullHeight,
                index = index,
                onDrag = { dragAmount ->
                    val current = meshState.value
                    val newPosition = current.positions[index] +
                        Offset(dragAmount.x / width, dragAmount.y / height)
                    onChangeState.value(current.withPosition(index, newPosition))
                },
                onTap = { onTapState.value(index) },
            )
            BezierDirection.entries.forEach { direction ->
                val stored = mesh.bezierOffset(index, direction)
                val effective =
                    if (stored != Offset.Unspecified) stored else direction.defaultOffset
                val controlPoint = point + effective
                BezierHandle(
                    centerX = marginX + controlPoint.x * width,
                    centerY = marginY + controlPoint.y * height,
                    boundsWidth = fullWidth,
                    boundsHeight = fullHeight,
                    direction = direction,
                    onDrag = { dragAmount ->
                        val current = meshState.value
                        val stored = current.bezierOffset(index, direction)
                        val base = if (stored == Offset.Unspecified) direction.defaultOffset else stored
                        val next = base + Offset(dragAmount.x / width, dragAmount.y / height)
                        onChangeState.value(current.withBezierOffset(index, direction, next))
                    },
                )
            }
        }
    }
}

@Composable
private fun VertexHandle(
    centerX: Float,
    centerY: Float,
    boundsWidth: Float,
    boundsHeight: Float,
    index: Int,
    onDrag: (Offset) -> Unit,
    onTap: () -> Unit,
) {
    val onDragState = rememberUpdatedState(onDrag)
    val onTapState = rememberUpdatedState(onTap)
    Box(
        Modifier
            .offset { handleTopLeft(centerX, centerY, boundsWidth, boundsHeight) }
            .size(HandleSize)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color.Black, CircleShape)
            .pointerInput(index) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragState.value(dragAmount)
                }
            }
            .pointerInput(index) {
                detectTapGestures(onTap = { onTapState.value() })
            }
    )
}

@Composable
private fun BezierHandle(
    centerX: Float,
    centerY: Float,
    boundsWidth: Float,
    boundsHeight: Float,
    direction: BezierDirection,
    onDrag: (Offset) -> Unit,
) {
    val onDragState = rememberUpdatedState(onDrag)
    val color = when (direction) {
        BezierDirection.LEFT -> Color.Red
        BezierDirection.TOP -> Color.Green
        BezierDirection.RIGHT -> Color.Blue
        BezierDirection.BOTTOM -> Color.Yellow
    }
    Box(
        Modifier
            .offset { handleTopLeft(centerX, centerY, boundsWidth, boundsHeight) }
            .size(HandleSize)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Black, CircleShape)
            .pointerInput(direction) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragState.value(dragAmount)
                }
            }
    )
}

/**
 * Centers a [HandleSize] handle on the pixel coordinate ([x], [y]), clamped so the handle stays
 * fully inside the canvas ([width] x [height]). Edge and outward-pointing bezier handles would
 * otherwise be clipped and invisible/ungrabbable. Runs inside a Density scope.
 */
private fun androidx.compose.ui.unit.Density.handleTopLeft(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): IntOffset {
    val sizePx = HandleSize.toPx()
    val half = sizePx / 2f
    val left = (x - half).coerceIn(0f, (width - sizePx).coerceAtLeast(0f))
    val top = (y - half).coerceIn(0f, (height - sizePx).coerceAtLeast(0f))
    return IntOffset(left.roundToInt(), top.roundToInt())
}
