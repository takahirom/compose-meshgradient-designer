package mesh.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import mesh.codegen.KotlinCodeGenerator
import mesh.model.AnimationState
import mesh.model.LoopMode
import mesh.model.MeshData
import mesh.model.SegmentEasing
import java.io.File

/**
 * Runs [KotlinCodeGenerator] on a fixed set of representative designer states and writes each
 * result as a compilable `.kt` file into the directory given by `args[0]`.
 *
 * These files are the input to the `:codegenVerify` module, which compiles them against the real
 * Compose API. The states are hand-built (not randomized) so the verification is reproducible and
 * covers the meaningful branches:
 * - a static single-keyframe mesh with both specified and unspecified bezier control points,
 * - an animated PingPong sequence with all-linear easings,
 * - an animated Cycle sequence with mixed per-segment easings (exercises the `segmentEasings` table).
 *
 * Each snippet is written into its own package so the top-level `MeshGradient`/`MeshFrame`
 * declarations from different samples do not collide.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Usage: GenerateSamples <outputDir>" }
    val outDir = File(args[0])
    outDir.mkdirs()
    // Remove stale generated files so a renamed/removed sample cannot linger and pass verification.
    outDir.listFiles()?.filter { it.extension == "kt" }?.forEach { it.delete() }

    writeSample(outDir, "StaticSample", "mesh.generated.staticsample", staticState())
    writeSample(outDir, "PingPongLinearSample", "mesh.generated.pingponglinear", pingPongLinearState())
    writeSample(outDir, "CycleMixedEasingSample", "mesh.generated.cyclemixedeasing", cycleMixedEasingState())

    println("Wrote generated samples to ${outDir.absolutePath}")
}

private fun writeSample(dir: File, fileName: String, packageName: String, state: AnimationState) {
    val code = KotlinCodeGenerator.generate(state)
    File(dir, "$fileName.kt").writeText("package $packageName\n\n$code\n")
}

/** A 2x1 mesh with fixed colors and one specified bezier control point (the rest are straight). */
private fun staticState(): AnimationState {
    val count = (2 + 1) * (1 + 1)
    val positions = listOf(
        Offset(0f, 0f), Offset(1f, 0f),
        Offset(0f, 0.5f), Offset(1f, 0.5f),
        Offset(0f, 1f), Offset(1f, 1f),
    )
    val colors = listOf(
        Color(0xFFFF0000), Color(0xFF00FF00),
        Color(0xFF0000FF), Color(0xFFFFFF00),
        Color(0xFF00FFFF), Color(0xFFFF00FF),
    )
    val unspecified = List(count) { Offset.Unspecified }
    val right = unspecified.toMutableList().also { it[0] = Offset(0.15f, 0.02f) }
    val mesh = MeshData(
        rows = 2,
        columns = 1,
        positions = positions,
        colors = colors,
        leftBezierOffsets = unspecified,
        rightBezierOffsets = right,
        topBezierOffsets = unspecified,
        bottomBezierOffsets = unspecified,
    )
    return AnimationState(keyframes = listOf(mesh), hasBicubicColor = true)
}

private fun pingPongLinearState(): AnimationState = AnimationState(
    keyframes = listOf(
        corner1x1(0xFFFFB75E, 0xFFED8F03, 0xFFF107A3, 0xFF7B2FF7),
        corner1x1(0xFF00C9A7, 0xFF92FE9D, 0xFF00D2FF, 0xFF4E54C8),
        corner1x1(0xFF2E3192, 0xFF1BFFFF, 0xFF2C5364, 0xFF0F2027),
    ),
    hasBicubicColor = true,
    durationMillisPerSegment = 1500,
    loopMode = LoopMode.PingPong,
    easings = List(3) { SegmentEasing.Linear },
)

private fun cycleMixedEasingState(): AnimationState = AnimationState(
    keyframes = listOf(
        corner1x1(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00),
        corner1x1(0xFF00FFFF, 0xFFFF00FF, 0xFFFFFFFF, 0xFF000000),
        corner1x1(0xFF123456, 0xFF654321, 0xFFABCDEF, 0xFFFEDCBA),
    ),
    hasBicubicColor = false,
    durationMillisPerSegment = 2000,
    loopMode = LoopMode.Cycle,
    easings = listOf(SegmentEasing.EaseIn, SegmentEasing.EaseOut, SegmentEasing.EaseInOut),
)

/** A 1x1 mesh from four corner colors (clockwise from top-left), straight edges. */
private fun corner1x1(topLeft: Long, topRight: Long, bottomLeft: Long, bottomRight: Long): MeshData {
    val positions = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0f, 1f), Offset(1f, 1f))
    val colors = listOf(Color(topLeft), Color(topRight), Color(bottomLeft), Color(bottomRight))
    val unspecified = List(4) { Offset.Unspecified }
    return MeshData(
        rows = 1,
        columns = 1,
        positions = positions,
        colors = colors,
        leftBezierOffsets = unspecified,
        rightBezierOffsets = unspecified,
        topBezierOffsets = unspecified,
        bottomBezierOffsets = unspecified,
    )
}
