package mesh.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentEasingTest {

    @Test
    fun allEasingsHitBothBoundaries() {
        SegmentEasing.entries.forEach { easing ->
            assertEquals(0f, easing.transform(0f), 1e-6f, "${easing.label} at 0")
            assertEquals(1f, easing.transform(1f), 1e-6f, "${easing.label} at 1")
        }
    }

    @Test
    fun cubicMidpoints() {
        assertEquals(0.5f, SegmentEasing.Linear.transform(0.5f), 1e-6f)
        assertEquals(0.125f, SegmentEasing.EaseIn.transform(0.5f), 1e-6f)
        assertEquals(0.875f, SegmentEasing.EaseOut.transform(0.5f), 1e-6f)
        assertEquals(0.5f, SegmentEasing.EaseInOut.transform(0.5f), 1e-6f)
    }

    @Test
    fun outOfRangeInputIsClamped() {
        assertEquals(0f, SegmentEasing.EaseIn.transform(-1f), 1e-6f)
        assertEquals(1f, SegmentEasing.EaseIn.transform(2f), 1e-6f)
    }

    @Test
    fun sampleAppliesEasingToSegmentFraction() {
        val a = generateLinearMeshState(1, 1)
        val b = a.copy(positions = a.positions.map { it.copy(x = it.x + 0.4f) })
        // PingPong progress 0.25 is halfway through the forward segment; EaseIn maps 0.5 -> 0.125.
        val eased = MeshInterpolator.sample(
            listOf(a, b), LoopMode.PingPong, 0.25f, SegmentEasing.EaseIn,
        )
        val expected = MeshInterpolator.lerp(a, b, 0.125f)
        assertEquals(expected.positions, eased.positions)
    }

    @Test
    fun localFractionMatchesSampleFolding() {
        // 3 keyframes, PingPong: progress 0.125 is halfway through the first forward segment.
        // (A segment boundary itself reports localT = 0 of the next segment.)
        assertEquals(0.5f, MeshInterpolator.localFraction(3, LoopMode.PingPong, 0.125f), 1e-5f)
        // Cycle with 2 keyframes has 2 segments (wrap included): 0.25 is halfway through the first.
        assertEquals(0.5f, MeshInterpolator.localFraction(2, LoopMode.Cycle, 0.25f), 1e-5f)
    }
}
