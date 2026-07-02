package mesh.codegen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import mesh.model.AnimationState
import mesh.model.LoopMode
import mesh.model.MeshData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationCodeGenTest {

    @Test
    fun singleKeyframeEmitsStaticCode() {
        val state = AnimationState(keyframes = listOf(mesh1x1()), hasBicubicColor = true)
        val code = KotlinCodeGenerator.generate(state)
        // Static output has no infinite transition and no keyframe list.
        assertFalse(code.contains("rememberInfiniteTransition"))
        assertFalse(code.contains("keyframes"))
        assertTrue(code.contains("MeshGradientPainter(rows = 1, columns = 1, hasBicubicColor = true)"))
    }

    @Test
    fun twoKeyframesEmitAnimatedCode() {
        val state = AnimationState(keyframes = listOf(mesh1x1(), mesh1x1()))
        val code = KotlinCodeGenerator.generate(state)
        assertTrue(code.contains("rememberInfiniteTransition"))
        assertTrue(code.contains("private val keyframes: List<MeshFrame>"))
        assertTrue(code.contains("fun lerpFrame("))
        assertTrue(code.contains("fun sampleFrame("))
        assertTrue(code.contains("remember(frame)"))
    }

    @Test
    fun pingPongUsesReverseRepeatMode() {
        val state = AnimationState(
            keyframes = listOf(mesh1x1(), mesh1x1(), mesh1x1()),
            loopMode = LoopMode.PingPong,
        )
        val code = KotlinCodeGenerator.generate(state)
        assertTrue(code.contains("repeatMode = RepeatMode.Reverse"))
        assertFalse(code.contains("RepeatMode.Restart"))
        // PingPong emits the keyframes as-is: 3 frames -> 3 data entries.
        assertEquals(3, Regex("positions = listOf\\(").findAll(code).count())
    }

    @Test
    fun cycleUsesRestartRepeatModeAndAppendsFirstKeyframe() {
        val state = AnimationState(
            keyframes = listOf(mesh1x1(), mesh1x1(), mesh1x1()),
            loopMode = LoopMode.Cycle,
        )
        val code = KotlinCodeGenerator.generate(state)
        assertTrue(code.contains("repeatMode = RepeatMode.Restart"))
        assertFalse(code.contains("RepeatMode.Reverse"))
        // Cycle appends the first keyframe: 3 frames -> 4 data entries.
        assertEquals(4, Regex("positions = listOf\\(").findAll(code).count())
    }

    @Test
    fun totalDurationScalesWithSegmentCount() {
        // PingPong over 3 keyframes emits (3 - 1) = 2 segments (Reverse handles the return).
        val pingPong = AnimationState(
            keyframes = listOf(mesh1x1(), mesh1x1(), mesh1x1()),
            durationMillisPerSegment = 1000,
            loopMode = LoopMode.PingPong,
        )
        assertTrue(KotlinCodeGenerator.generate(pingPong).contains("tween(durationMillis = 2000"))
        // Cycle over 3 keyframes emits 3 segments (wrap included).
        val cycle = pingPong.copy(loopMode = LoopMode.Cycle)
        assertTrue(KotlinCodeGenerator.generate(cycle).contains("tween(durationMillis = 3000"))
    }

    @Test
    fun animatedCodeResolvesUnspecifiedBeziersToDefaults() {
        val state = AnimationState(keyframes = listOf(mesh1x1(), mesh1x1()))
        val code = KotlinCodeGenerator.generate(state)
        // No Offset.Unspecified must leak into the emitted keyframe data (it cannot be lerped).
        assertFalse(code.contains("Unspecified"))
        // LEFT default offset appears in the resolved left list.
        assertTrue(code.contains("left = listOf(Offset(-0.1f, 0f)"))
    }

    @Test
    fun animatedCodeIncludesAnimationImports() {
        val state = AnimationState(keyframes = listOf(mesh1x1(), mesh1x1()))
        val code = KotlinCodeGenerator.generate(state)
        assertTrue(code.contains("import androidx.compose.animation.core.rememberInfiniteTransition"))
        assertTrue(code.contains("import androidx.compose.ui.graphics.lerp"))
        assertTrue(code.contains("import androidx.compose.ui.geometry.lerp"))
    }

    private fun mesh1x1(): MeshData = MeshData(
        rows = 1,
        columns = 1,
        positions = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f)),
        colors = List(4) { Color(0xFFFF0000) },
        leftBezierOffsets = List(4) { Offset.Unspecified },
        rightBezierOffsets = List(4) { Offset.Unspecified },
        topBezierOffsets = List(4) { Offset.Unspecified },
        bottomBezierOffsets = List(4) { Offset.Unspecified },
    )
}
