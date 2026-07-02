package mesh.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * The four bezier control-point directions attached to every vertex.
 *
 * Each direction carries a default offset (in normalized 0..1 space) that is applied when the
 * user first grabs the handle while the stored offset is still [Offset.Unspecified].
 */
enum class BezierDirection(val defaultOffset: Offset) {
    LEFT(Offset(-0.1f, 0f)),
    TOP(Offset(0f, -0.1f)),
    RIGHT(Offset(0.1f, 0f)),
    BOTTOM(Offset(0f, 0.1f)),
}

/**
 * Immutable model of an editable mesh gradient.
 *
 * A gradient with [rows] x [columns] cells has `(rows + 1) * (columns + 1)` vertices. Every list
 * below is indexed by `row * (columns + 1) + column`.
 *
 * Positions are normalized to the 0..1 range so the model is independent of the canvas size.
 * A bezier offset of [Offset.Unspecified] means "use the straight edge" (no curvature); such
 * offsets are omitted from the generated code.
 *
 * Ported from the Android reference sample; the Android-only `@SuppressLint` annotations were
 * dropped because they have no meaning on Compose Multiplatform.
 */
data class MeshData(
    val rows: Int,
    val columns: Int,
    val positions: List<Offset>,
    val colors: List<Color>,
    val leftBezierOffsets: List<Offset>,
    val rightBezierOffsets: List<Offset>,
    val topBezierOffsets: List<Offset>,
    val bottomBezierOffsets: List<Offset>,
) {
    /** Total number of vertices in the mesh. */
    val vertexCount: Int get() = (rows + 1) * (columns + 1)

    /** Returns the flat list index of the vertex at [row], [column]. */
    fun indexOf(row: Int, column: Int): Int = row * (columns + 1) + column

    /** Returns the stored bezier offset for [index] in the given [direction]. */
    fun bezierOffset(index: Int, direction: BezierDirection): Offset = when (direction) {
        BezierDirection.LEFT -> leftBezierOffsets[index]
        BezierDirection.TOP -> topBezierOffsets[index]
        BezierDirection.RIGHT -> rightBezierOffsets[index]
        BezierDirection.BOTTOM -> bottomBezierOffsets[index]
    }

    /** Returns a copy with the vertex position at [index] replaced. */
    fun withPosition(index: Int, position: Offset): MeshData =
        copy(positions = positions.replaceAt(index, position))

    /** Returns a copy with the vertex color at [index] replaced. */
    fun withColor(index: Int, color: Color): MeshData =
        copy(colors = colors.replaceAt(index, color))

    /** Returns a copy with the bezier offset at [index] in [direction] replaced. */
    fun withBezierOffset(index: Int, direction: BezierDirection, offset: Offset): MeshData =
        when (direction) {
            BezierDirection.LEFT -> copy(leftBezierOffsets = leftBezierOffsets.replaceAt(index, offset))
            BezierDirection.TOP -> copy(topBezierOffsets = topBezierOffsets.replaceAt(index, offset))
            BezierDirection.RIGHT -> copy(rightBezierOffsets = rightBezierOffsets.replaceAt(index, offset))
            BezierDirection.BOTTOM -> copy(bottomBezierOffsets = bottomBezierOffsets.replaceAt(index, offset))
        }
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

/**
 * Builds an evenly spaced linear grid of vertices with random colors, matching the reference
 * sample's `generateLinearMeshState`. Bezier offsets start out unspecified (straight edges).
 *
 * @param random source of randomness for the vertex colors; inject a seeded instance for tests.
 */
fun generateLinearMeshState(rows: Int, columns: Int, random: Random = Random.Default): MeshData {
    val count = (rows + 1) * (columns + 1)
    val positions = List(count) { index ->
        val row = index / (columns + 1)
        val col = index % (columns + 1)
        val x = if (columns > 0) col.toFloat() / columns else 0f
        val y = if (rows > 0) row.toFloat() / rows else 0f
        Offset(x, y)
    }
    val colors = List(count) {
        Color(
            red = random.nextInt(0, 256),
            green = random.nextInt(0, 256),
            blue = random.nextInt(0, 256),
        )
    }
    val unspecified = List(count) { Offset.Unspecified }
    return MeshData(
        rows = rows,
        columns = columns,
        positions = positions,
        colors = colors,
        leftBezierOffsets = unspecified,
        rightBezierOffsets = unspecified,
        topBezierOffsets = unspecified,
        bottomBezierOffsets = unspecified,
    )
}
