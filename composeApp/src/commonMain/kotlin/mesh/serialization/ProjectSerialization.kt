package mesh.serialization

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.SegmentEasing
import kotlin.math.roundToInt

/**
 * The full designer state captured for Export/Import: every keyframe (positions, colors, and the
 * four bezier offset lists), the per-keyframe easings, and the playback settings, plus which
 * keyframe was selected.
 */
data class DesignerProject(
    val keyframes: List<MeshData>,
    val easings: List<SegmentEasing>,
    val loopMode: LoopMode,
    val durationMillisPerSegment: Int,
    val hasBicubicColor: Boolean,
    val selectedIndex: Int,
)

/** Thrown by [ProjectSerializer.import] with a human-readable message when input is not usable. */
class ProjectImportException(message: String) : Exception(message)

/**
 * Lossless JSON (de)serialization of a [DesignerProject].
 *
 * Design decisions that matter for round-trip fidelity:
 * - A bezier offset of [Offset.Unspecified] ("straight edge") is stored as `null` and restored to
 *   [Offset.Unspecified]. It is never resolved to a directional default, which would silently turn
 *   a straight edge into a curved one.
 * - Colors are stored as 8-digit `AARRGGBB` hex strings; positions and offsets keep their raw
 *   floats. Enums are stored by name.
 * - A top-level `version` field guards forward compatibility; [import] rejects unknown versions.
 */
object ProjectSerializer {

    const val CURRENT_VERSION: Int = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Serializes [project] to a pretty-printed JSON string. */
    fun export(project: DesignerProject): String = json.encodeToString(project.toDto())

    /**
     * Parses [text] back into a [DesignerProject].
     *
     * @throws ProjectImportException if the JSON is malformed, the version is unsupported, or the
     * data is internally inconsistent (empty keyframes, vertex count not matching the grid).
     */
    fun import(text: String): DesignerProject {
        val dto = try {
            json.decodeFromString<ProjectDto>(text)
        } catch (e: SerializationException) {
            throw ProjectImportException("Malformed JSON: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw ProjectImportException("Malformed JSON: ${e.message}")
        }
        if (dto.version != CURRENT_VERSION) {
            throw ProjectImportException(
                "Unsupported project version ${dto.version} (this build supports $CURRENT_VERSION).",
            )
        }
        return dto.toProject()
    }

    // --- DTO layer -------------------------------------------------------------------------------

    @Serializable
    private data class ProjectDto(
        val version: Int = CURRENT_VERSION,
        val rows: Int,
        val columns: Int,
        val hasBicubicColor: Boolean,
        val durationMillisPerSegment: Int,
        val loopMode: String,
        val selectedIndex: Int,
        val keyframes: List<KeyframeDto>,
    )

    @Serializable
    private data class KeyframeDto(
        val easing: String,
        val vertices: List<VertexDto>,
    )

    @Serializable
    private data class VertexDto(
        val x: Float,
        val y: Float,
        val color: String,
        // null means Offset.Unspecified (a straight edge); it must survive the round trip as null.
        val left: OffsetDto? = null,
        val top: OffsetDto? = null,
        val right: OffsetDto? = null,
        val bottom: OffsetDto? = null,
    )

    @Serializable
    private data class OffsetDto(val x: Float, val y: Float)

    private fun DesignerProject.toDto(): ProjectDto {
        val grid = keyframes.first()
        return ProjectDto(
            version = CURRENT_VERSION,
            rows = grid.rows,
            columns = grid.columns,
            hasBicubicColor = hasBicubicColor,
            durationMillisPerSegment = durationMillisPerSegment,
            loopMode = loopMode.name,
            selectedIndex = selectedIndex,
            keyframes = keyframes.mapIndexed { i, mesh ->
                KeyframeDto(
                    easing = easings.getOrElse(i) { SegmentEasing.Linear }.name,
                    vertices = List(mesh.vertexCount) { index -> mesh.vertexDto(index) },
                )
            },
        )
    }

    private fun MeshData.vertexDto(index: Int): VertexDto = VertexDto(
        x = positions[index].x,
        y = positions[index].y,
        color = colors[index].toHex(),
        left = leftBezierOffsets[index].toDto(),
        top = topBezierOffsets[index].toDto(),
        right = rightBezierOffsets[index].toDto(),
        bottom = bottomBezierOffsets[index].toDto(),
    )

    private fun ProjectDto.toProject(): DesignerProject {
        if (keyframes.isEmpty()) throw ProjectImportException("Project has no keyframes.")
        val expected = (rows + 1) * (columns + 1)
        val loop = enumByName<LoopMode>(loopMode, LoopMode.entries) { "loop mode" }
        val meshes = keyframes.mapIndexed { i, kf ->
            if (kf.vertices.size != expected) {
                throw ProjectImportException(
                    "Keyframe ${i + 1} has ${kf.vertices.size} vertices but the ${rows}x$columns " +
                        "grid needs $expected.",
                )
            }
            kf.toMesh(rows, columns)
        }
        val easings = keyframes.map { enumByName<SegmentEasing>(it.easing, SegmentEasing.entries) { "easing" } }
        return DesignerProject(
            keyframes = meshes,
            easings = easings,
            loopMode = loop,
            durationMillisPerSegment = durationMillisPerSegment,
            hasBicubicColor = hasBicubicColor,
            selectedIndex = selectedIndex.coerceIn(0, meshes.lastIndex),
        )
    }

    private fun KeyframeDto.toMesh(rows: Int, columns: Int): MeshData = MeshData(
        rows = rows,
        columns = columns,
        positions = vertices.map { Offset(it.x, it.y) },
        colors = vertices.map { it.color.toColor() },
        leftBezierOffsets = vertices.map { it.left.toOffset() },
        topBezierOffsets = vertices.map { it.top.toOffset() },
        rightBezierOffsets = vertices.map { it.right.toOffset() },
        bottomBezierOffsets = vertices.map { it.bottom.toOffset() },
    )

    // --- Value conversions -----------------------------------------------------------------------

    /** [Offset.Unspecified] serializes as null; any other offset keeps its raw floats. */
    private fun Offset.toDto(): OffsetDto? = if (this == Offset.Unspecified) null else OffsetDto(x, y)

    /** null restores to [Offset.Unspecified]; never to a directional default. */
    private fun OffsetDto?.toOffset(): Offset = if (this == null) Offset.Unspecified else Offset(x, y)

    private fun Color.toHex(): String {
        val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val r = (red * 255f).roundToInt().coerceIn(0, 255)
        val g = (green * 255f).roundToInt().coerceIn(0, 255)
        val b = (blue * 255f).roundToInt().coerceIn(0, 255)
        val argb = (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
        return argb.toString(16).uppercase().padStart(8, '0')
    }

    private fun String.toColor(): Color {
        val value = toLongOrNull(16)
            ?: throw ProjectImportException("Invalid color \"$this\" (expected AARRGGBB hex).")
        return Color(value)
    }

    private inline fun <reified T : Enum<T>> enumByName(
        name: String,
        values: List<T>,
        label: () -> String,
    ): T = values.firstOrNull { it.name == name }
        ?: throw ProjectImportException("Unknown ${label()} \"$name\".")
}
