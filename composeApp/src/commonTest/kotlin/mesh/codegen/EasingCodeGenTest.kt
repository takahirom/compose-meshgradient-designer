package mesh.codegen

import mesh.model.AnimationState
import mesh.model.LoopMode
import mesh.model.SegmentEasing
import mesh.model.generateLinearMeshState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EasingCodeGenTest {

    private fun state(
        easings: List<SegmentEasing>,
        keyframeCount: Int = 2,
        loopMode: LoopMode = LoopMode.PingPong,
    ) = AnimationState(
        keyframes = List(keyframeCount) { generateLinearMeshState(1, 1) },
        loopMode = loopMode,
        easings = easings,
    )

    @Test
    fun allLinearEmitsNoEasingTable() {
        val code = KotlinCodeGenerator.generate(state(listOf(SegmentEasing.Linear, SegmentEasing.Linear)))
        assertFalse(code.contains("segmentEasings"))
        assertTrue(code.contains("lerpFrame(frames[index], frames[index + 1], scaled - index)"))
    }

    @Test
    fun emptyEasingsDefaultsToLinear() {
        val code = KotlinCodeGenerator.generate(state(emptyList()))
        assertFalse(code.contains("segmentEasings"))
    }

    @Test
    fun easedCodeEmitsPerSegmentTable() {
        val code = KotlinCodeGenerator.generate(
            state(listOf(SegmentEasing.EaseInOut, SegmentEasing.Linear), keyframeCount = 3),
        )
        assertTrue(code.contains("private val segmentEasings: List<(Float) -> Float> = listOf("))
        assertTrue(code.contains("// Ease in-out"))
        assertTrue(code.contains("lerpFrame(frames[index], frames[index + 1], segmentEasings[index](scaled - index))"))
    }

    @Test
    fun cycleEmitsOneEasingPerKeyframeIncludingWrap() {
        // 2 keyframes in Cycle = 2 segments (including the wrap): both easings must appear.
        val code = KotlinCodeGenerator.generate(
            state(
                listOf(SegmentEasing.EaseIn, SegmentEasing.EaseOut),
                loopMode = LoopMode.Cycle,
            ),
        )
        assertTrue(code.contains("{ t -> t * t * t }, // Ease in"))
        assertTrue(code.contains("// Ease out"))
    }

    @Test
    fun pingPongIgnoresLastKeyframeEasing() {
        // 2 keyframes in PingPong = 1 forward segment; the last keyframe's easing is unused.
        val code = KotlinCodeGenerator.generate(
            state(listOf(SegmentEasing.Linear, SegmentEasing.EaseIn)),
        )
        assertFalse(code.contains("segmentEasings"))
    }
}
