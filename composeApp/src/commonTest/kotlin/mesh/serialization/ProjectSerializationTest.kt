package mesh.serialization

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import mesh.model.BezierDirection
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.SegmentEasing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProjectSerializationTest {

    @Test
    fun fullRoundTripPreservesEveryField() {
        val project = sampleProject()
        val restored = ProjectSerializer.import(ProjectSerializer.export(project))
        assertEquals(project, restored)
    }

    @Test
    fun unspecifiedBezierSurvivesAsUnspecified() {
        val project = sampleProject()
        val json = ProjectSerializer.export(project)
        val restored = ProjectSerializer.import(json)
        val mesh = restored.keyframes[0]
        // Vertex 0 keeps its straight (unspecified) left edge; it must NOT resolve to a default.
        assertEquals(Offset.Unspecified, mesh.leftBezierOffsets[0])
        assertTrue(mesh.leftBezierOffsets[0] != BezierDirection.LEFT.defaultOffset)
        // Vertex 0's specified right edge is preserved exactly.
        assertEquals(Offset(0.15f, 0.02f), mesh.rightBezierOffsets[0])
    }

    @Test
    fun perKeyframeEasingsArePreserved() {
        val project = sampleProject()
        val restored = ProjectSerializer.import(ProjectSerializer.export(project))
        assertEquals(
            listOf(SegmentEasing.EaseIn, SegmentEasing.EaseOut),
            restored.easings,
        )
    }

    @Test
    fun exportedJsonContainsVersionAndNullBezier() {
        val json = ProjectSerializer.export(sampleProject())
        assertTrue(json.contains("\"version\": 1"))
        // The unspecified left edge of vertex 0 must be stored as null, not a resolved default.
        assertTrue(json.contains("\"left\": null"))
    }

    @Test
    fun unknownVersionIsRejected() {
        val json = ProjectSerializer.export(sampleProject()).replace("\"version\": 1", "\"version\": 99")
        val ex = assertFailsWith<ProjectImportException> { ProjectSerializer.import(json) }
        assertTrue(ex.message!!.contains("version"))
    }

    @Test
    fun malformedJsonIsRejectedWithMessage() {
        val ex = assertFailsWith<ProjectImportException> { ProjectSerializer.import("{ not valid json") }
        assertTrue(ex.message!!.contains("Malformed JSON"))
    }

    @Test
    fun inconsistentVertexCountIsRejected() {
        // A 1x1 grid needs 4 vertices; this keyframe only lists 2, so import must reject it.
        val json = """
            {
              "version": 1,
              "rows": 1,
              "columns": 1,
              "hasBicubicColor": true,
              "durationMillisPerSegment": 2000,
              "loopMode": "PingPong",
              "selectedIndex": 0,
              "keyframes": [
                {
                  "easing": "Linear",
                  "vertices": [
                    { "x": 0.0, "y": 0.0, "color": "FFFF0000" },
                    { "x": 1.0, "y": 0.0, "color": "FF00FF00" }
                  ]
                }
              ]
            }
        """.trimIndent()
        val ex = assertFailsWith<ProjectImportException> { ProjectSerializer.import(json) }
        assertTrue(ex.message!!.contains("vertices"))
    }

    private fun sampleProject(): DesignerProject {
        // Two 1x1 keyframes. Keyframe 0 has one unspecified and one specified bezier edge.
        val positions = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f))
        val colors0 = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFFFFFF00))
        val colors1 = listOf(Color(0xFF112233), Color(0xFF445566), Color(0xFF778899), Color(0xFFAABBCC))
        val unspecified = List(4) { Offset.Unspecified }
        val right0 = unspecified.toMutableList().also { it[0] = Offset(0.15f, 0.02f) }
        val kf0 = MeshData(
            rows = 1,
            columns = 1,
            positions = positions,
            colors = colors0,
            leftBezierOffsets = unspecified,
            rightBezierOffsets = right0,
            topBezierOffsets = unspecified,
            bottomBezierOffsets = unspecified,
        )
        val kf1 = kf0.copy(colors = colors1)
        return DesignerProject(
            keyframes = listOf(kf0, kf1),
            easings = listOf(SegmentEasing.EaseIn, SegmentEasing.EaseOut),
            loopMode = LoopMode.Cycle,
            durationMillisPerSegment = 1750,
            hasBicubicColor = false,
            selectedIndex = 1,
        )
    }
}
