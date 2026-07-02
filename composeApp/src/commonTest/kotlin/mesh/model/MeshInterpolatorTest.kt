package mesh.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeshInterpolatorTest {

    // --- lerp: boundaries ---

    @Test
    fun lerp_atZeroReturnsFirstKeyframeValues() {
        val a = mesh1x1(color0 = Color(0xFF102030))
        val b = mesh1x1(color0 = Color(0xFF405060), pos0 = Offset(0.2f, 0.3f))
        val result = MeshInterpolator.lerp(a, b, 0f)
        assertEquals(a.positions[0], result.positions[0])
        assertEquals(a.colors[0], result.colors[0])
    }

    @Test
    fun lerp_atOneReturnsSecondKeyframeValues() {
        val a = mesh1x1(color0 = Color(0xFF102030))
        val b = mesh1x1(color0 = Color(0xFF405060), pos0 = Offset(0.2f, 0.3f))
        val result = MeshInterpolator.lerp(a, b, 1f)
        assertEquals(b.positions[0], result.positions[0])
        assertEquals(b.colors[0], result.colors[0])
    }

    @Test
    fun lerp_midpointInterpolatesPositionsLinearly() {
        val a = mesh1x1(pos0 = Offset(0f, 0f))
        val b = mesh1x1(pos0 = Offset(0.4f, 0.8f))
        val result = MeshInterpolator.lerp(a, b, 0.5f)
        assertEquals(0.2f, result.positions[0].x, 1e-5f)
        assertEquals(0.4f, result.positions[0].y, 1e-5f)
    }

    // --- lerp: Offset.Unspecified resolution ---

    @Test
    fun lerp_resolvesUnspecifiedBezierToDirectionalDefault() {
        // Only side a is Unspecified; it must resolve to LEFT default (-0.1, 0) before mixing.
        val a = mesh1x1(left0 = Offset.Unspecified)
        val b = mesh1x1(left0 = Offset(-0.3f, 0f))
        val result = MeshInterpolator.lerp(a, b, 0.5f)
        // Midpoint of (-0.1, 0) and (-0.3, 0).
        assertEquals(-0.2f, result.leftBezierOffsets[0].x, 1e-5f)
        assertEquals(0f, result.leftBezierOffsets[0].y, 1e-5f)
        assertTrue(result.leftBezierOffsets[0].isSpecified())
    }

    @Test
    fun lerp_bothUnspecifiedResolvesToDefaultOnBothSides() {
        val a = mesh1x1(top0 = Offset.Unspecified)
        val b = mesh1x1(top0 = Offset.Unspecified)
        val result = MeshInterpolator.lerp(a, b, 0.5f)
        // TOP default is (0, -0.1); interpolating a default with itself keeps the default.
        assertEquals(0f, result.topBezierOffsets[0].x, 1e-5f)
        assertEquals(-0.1f, result.topBezierOffsets[0].y, 1e-5f)
    }

    // --- segmentAt ---

    @Test
    fun segmentAt_boundariesAndCrossing() {
        assertEquals(0 to 0f, MeshInterpolator.segmentAt(0f, 2))
        // Exactly on the segment boundary lands on the next segment at local t = 0.
        val (idx, t) = MeshInterpolator.segmentAt(0.5f, 2)
        assertEquals(1, idx)
        assertEquals(0f, t, 1e-5f)
        // progress == 1 clamps to the last segment at local t = 1.
        val (lastIdx, lastT) = MeshInterpolator.segmentAt(1f, 2)
        assertEquals(1, lastIdx)
        assertEquals(1f, lastT, 1e-5f)
    }

    @Test
    fun sampleForward_crossesSegments() {
        val a = mesh1x1(pos0 = Offset(0f, 0f))
        val b = mesh1x1(pos0 = Offset(1f, 0f))
        val c = mesh1x1(pos0 = Offset(1f, 1f))
        val frames = listOf(a, b, c)
        // 0.25 -> first segment, half way A..B -> x = 0.5, y = 0.
        val q = MeshInterpolator.sampleForward(frames, 0.25f)
        assertEquals(0.5f, q.positions[0].x, 1e-5f)
        assertEquals(0f, q.positions[0].y, 1e-5f)
        // 0.75 -> second segment, half way B..C -> x = 1, y = 0.5.
        val r = MeshInterpolator.sampleForward(frames, 0.75f)
        assertEquals(1f, r.positions[0].x, 1e-5f)
        assertEquals(0.5f, r.positions[0].y, 1e-5f)
    }

    // --- sample: PingPong reflection & Cycle wrap ---

    @Test
    fun sample_pingPongReflectsAroundLastKeyframe() {
        val a = mesh1x1(pos0 = Offset(0f, 0f))
        val b = mesh1x1(pos0 = Offset(1f, 0f))
        val c = mesh1x1(pos0 = Offset(1f, 1f))
        val frames = listOf(a, b, c)
        // progress 0 -> A, 0.5 -> C (fold peak), 1 -> A again.
        assertEquals(a.positions[0], MeshInterpolator.sample(frames, LoopMode.PingPong, 0f).positions[0])
        assertEquals(c.positions[0], MeshInterpolator.sample(frames, LoopMode.PingPong, 0.5f).positions[0])
        assertEquals(a.positions[0], MeshInterpolator.sample(frames, LoopMode.PingPong, 1f).positions[0])
        // 0.25 and 0.75 are reflections of each other -> both equal B.
        val forward = MeshInterpolator.sample(frames, LoopMode.PingPong, 0.25f).positions[0]
        val backward = MeshInterpolator.sample(frames, LoopMode.PingPong, 0.75f).positions[0]
        assertEquals(b.positions[0].x, forward.x, 1e-5f)
        assertEquals(forward.x, backward.x, 1e-5f)
        assertEquals(forward.y, backward.y, 1e-5f)
    }

    @Test
    fun sample_cycleWrapsBackToFirstKeyframe() {
        val a = mesh1x1(pos0 = Offset(0f, 0f))
        val b = mesh1x1(pos0 = Offset(1f, 0f))
        val c = mesh1x1(pos0 = Offset(1f, 1f))
        val frames = listOf(a, b, c)
        // Cycle appends A: effective [A, B, C, A], 3 segments. progress 1 -> back to A.
        assertEquals(a.positions[0], MeshInterpolator.sample(frames, LoopMode.Cycle, 1f).positions[0])
        // Two thirds through -> start of the wrap segment C..A -> C.
        val atTwoThirds = MeshInterpolator.sample(frames, LoopMode.Cycle, 2f / 3f).positions[0]
        assertEquals(c.positions[0].x, atTwoThirds.x, 1e-4f)
        assertEquals(c.positions[0].y, atTwoThirds.y, 1e-4f)
    }

    @Test
    fun segmentCountForLoop_matchesLoopSemantics() {
        assertEquals(4, MeshInterpolator.segmentCountForLoop(3, LoopMode.PingPong))
        assertEquals(3, MeshInterpolator.segmentCountForLoop(3, LoopMode.Cycle))
    }

    @Test
    fun resizedTo_carriesColorsAndResetsBeziers() {
        val original = mesh1x1(color0 = Color(0xFF112233))
        val resized = original.resizedTo(rows = 2, columns = 2)
        assertEquals(2, resized.rows)
        assertEquals(2, resized.columns)
        assertEquals(9, resized.vertexCount)
        // First color is carried over.
        assertEquals(Color(0xFF112233), resized.colors[0])
        // Bezier offsets are reset to straight edges.
        assertEquals(Offset.Unspecified, resized.leftBezierOffsets[0])
    }

    private fun Offset.isSpecified(): Boolean = !x.isNaN() && !y.isNaN()

    private fun mesh1x1(
        pos0: Offset = Offset(0f, 0f),
        color0: Color = Color(0xFFFF0000),
        left0: Offset = Offset.Unspecified,
        top0: Offset = Offset.Unspecified,
    ): MeshData = MeshData(
        rows = 1,
        columns = 1,
        positions = listOf(pos0, Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f)),
        colors = listOf(color0, Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFFFFFFFF)),
        leftBezierOffsets = listOf(left0, Offset.Unspecified, Offset.Unspecified, Offset.Unspecified),
        rightBezierOffsets = List(4) { Offset.Unspecified },
        topBezierOffsets = listOf(top0, Offset.Unspecified, Offset.Unspecified, Offset.Unspecified),
        bottomBezierOffsets = List(4) { Offset.Unspecified },
    )
}
