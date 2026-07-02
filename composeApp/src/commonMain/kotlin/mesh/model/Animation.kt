package mesh.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.floor

/**
 * How a keyframe animation loops.
 *
 * - [PingPong] plays A -> B -> C -> B -> A -> ... (equivalent to `RepeatMode.Reverse`). The seam is
 *   structurally continuous because the sequence reverses at each end, so no extra keyframe is
 *   needed.
 * - [Cycle] plays A -> B -> C -> A -> ... (equivalent to `RepeatMode.Restart`). To keep the wrap
 *   seamless the first keyframe is implicitly appended to the end before sampling.
 */
enum class LoopMode { PingPong, Cycle }

/**
 * Easing applied to the local fraction of each keyframe transition (per segment, not across the
 * whole sweep), both in the live preview and in the generated code.
 *
 * Implemented as pure cubic polynomials (the CSS ease-in/out-cubic family) rather than
 * `CubicBezierEasing` so the exact same function can be emitted into the generated snippet and
 * asserted in unit tests.
 */
enum class SegmentEasing(val label: String) {
    Linear("Linear"),
    EaseIn("Ease in"),
    EaseOut("Ease out"),
    EaseInOut("Ease in-out");

    /** Maps a linear fraction in 0..1 to the eased fraction (0 -> 0, 1 -> 1). */
    fun transform(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return when (this) {
            Linear -> x
            EaseIn -> x * x * x
            EaseOut -> {
                val inv = 1f - x
                1f - inv * inv * inv
            }
            EaseInOut ->
                if (x < 0.5f) {
                    4f * x * x * x
                } else {
                    val inv = -2f * x + 2f
                    1f - inv * inv * inv / 2f
                }
        }
    }
}

/**
 * A keyframe animation: an ordered list of [keyframes] that are interpolated over time.
 *
 * Two or more keyframes are required to play. [rows]/[columns] are shared across every keyframe
 * (enforced by the editor); [durationMillisPerSegment] is the time spent transitioning between two
 * adjacent keyframes.
 */
data class AnimationState(
    val keyframes: List<MeshData>,
    val hasBicubicColor: Boolean = true,
    val durationMillisPerSegment: Int = 2000,
    val loopMode: LoopMode = LoopMode.PingPong,
    /**
     * Per-keyframe outgoing easing: `easings[i]` shapes the transition from keyframe `i` to the
     * next one (for [LoopMode.Cycle] the last entry shapes the wrap back to the first keyframe;
     * for [LoopMode.PingPong] the last entry is unused). Missing entries default to
     * [SegmentEasing.Linear].
     */
    val easings: List<SegmentEasing> = emptyList(),
) {
    /** True when there are enough keyframes to interpolate. */
    val isAnimated: Boolean get() = keyframes.size >= 2
}

/**
 * Pure interpolation logic shared by the live preview and reflected in the generated code.
 *
 * The critical detail is that [Offset.Unspecified] cannot be linearly interpolated (its components
 * are `NaN`), so every bezier offset is resolved to its directional default before mixing.
 */
object MeshInterpolator {

    /**
     * Resolves a stored bezier [offset] for [direction]. [Offset.Unspecified] (meaning "straight
     * edge") is replaced by the direction's default offset so the value can be interpolated.
     */
    fun resolveBezier(offset: Offset, direction: BezierDirection): Offset =
        if (offset == Offset.Unspecified) direction.defaultOffset else offset

    /**
     * Linearly interpolates two keyframes at fraction [t] (0 = [a], 1 = [b]).
     *
     * Positions and all four bezier offset lists are mixed as [Offset]s (unspecified offsets
     * resolved to their defaults first), and colors are mixed with Compose's [Color] `lerp`.
     * Both keyframes must share the same grid ([a]'s rows/columns win).
     */
    fun lerp(a: MeshData, b: MeshData, t: Float): MeshData {
        val positions = a.positions.mapIndexed { i, p -> lerp(p, b.positions[i], t) }
        val colors = a.colors.mapIndexed { i, c -> lerp(c, b.colors[i], t) }
        return a.copy(
            positions = positions,
            colors = colors,
            leftBezierOffsets = lerpBezier(a.leftBezierOffsets, b.leftBezierOffsets, BezierDirection.LEFT, t),
            topBezierOffsets = lerpBezier(a.topBezierOffsets, b.topBezierOffsets, BezierDirection.TOP, t),
            rightBezierOffsets = lerpBezier(a.rightBezierOffsets, b.rightBezierOffsets, BezierDirection.RIGHT, t),
            bottomBezierOffsets = lerpBezier(a.bottomBezierOffsets, b.bottomBezierOffsets, BezierDirection.BOTTOM, t),
        )
    }

    private fun lerpBezier(
        a: List<Offset>,
        b: List<Offset>,
        direction: BezierDirection,
        t: Float,
    ): List<Offset> = a.mapIndexed { i, offset ->
        lerp(resolveBezier(offset, direction), resolveBezier(b[i], direction), t)
    }

    /**
     * Maps a forward progress in 0..1 across [segmentCount] equal segments.
     *
     * Returns the segment index (0..[segmentCount]-1) and the local fraction within that segment.
     * `progress == 1` clamps to the last segment at local `t == 1` rather than overflowing.
     */
    fun segmentAt(progress: Float, segmentCount: Int): Pair<Int, Float> {
        if (segmentCount <= 0) return 0 to 0f
        val clamped = progress.coerceIn(0f, 1f)
        val scaled = clamped * segmentCount
        var index = floor(scaled).toInt()
        if (index >= segmentCount) index = segmentCount - 1
        return index to (scaled - index)
    }

    /**
     * Samples [keyframes] played forward across `keyframes.size - 1` segments. A single keyframe
     * (or empty list guarded by the caller) returns as-is.
     */
    fun sampleForward(
        keyframes: List<MeshData>,
        progress: Float,
        easing: SegmentEasing = SegmentEasing.Linear,
    ): MeshData {
        if (keyframes.size == 1) return keyframes[0]
        return sampleForward(keyframes, progress, List(keyframes.size - 1) { easing })
    }

    /**
     * As [sampleForward], but with a per-segment easing list: `easings[i]` shapes the transition
     * out of keyframe `i`. Missing entries fall back to [SegmentEasing.Linear].
     */
    fun sampleForward(
        keyframes: List<MeshData>,
        progress: Float,
        easings: List<SegmentEasing>,
    ): MeshData {
        if (keyframes.size == 1) return keyframes[0]
        val (index, localT) = segmentAt(progress, keyframes.size - 1)
        val easing = easings.getOrElse(index) { SegmentEasing.Linear }
        return lerp(keyframes[index], keyframes[index + 1], easing.transform(localT))
    }

    /**
     * Returns the effective keyframe list for [loopMode]: unchanged for [LoopMode.PingPong], and
     * with the first keyframe appended for [LoopMode.Cycle] so the wrap is seamless.
     */
    fun effectiveKeyframes(keyframes: List<MeshData>, loopMode: LoopMode): List<MeshData> =
        when (loopMode) {
            LoopMode.PingPong -> keyframes
            LoopMode.Cycle -> if (keyframes.size >= 2) keyframes + keyframes.first() else keyframes
        }

    /**
     * Samples the full user-facing timeline at [progress] in 0..1.
     *
     * - [LoopMode.PingPong] folds progress so 0 -> A, 0.5 -> last keyframe, 1 -> A again, giving the
     *   A -> B -> C -> B -> A reflection within a single 0..1 sweep.
     * - [LoopMode.Cycle] plays straight through the first-appended list, so 1 wraps back to A.
     */
    fun sample(
        keyframes: List<MeshData>,
        loopMode: LoopMode,
        progress: Float,
        easing: SegmentEasing = SegmentEasing.Linear,
    ): MeshData = sample(keyframes, loopMode, progress, List(keyframes.size) { easing })

    /**
     * As [sample], but with per-keyframe outgoing easings (see [AnimationState.easings]).
     */
    fun sample(
        keyframes: List<MeshData>,
        loopMode: LoopMode,
        progress: Float,
        easings: List<SegmentEasing>,
    ): MeshData {
        if (keyframes.size < 2) return keyframes.first()
        val clamped = progress.coerceIn(0f, 1f)
        return when (loopMode) {
            LoopMode.PingPong -> {
                val folded = if (clamped <= 0.5f) clamped * 2f else (1f - clamped) * 2f
                sampleForward(keyframes, folded, easings)
            }
            LoopMode.Cycle ->
                sampleForward(effectiveKeyframes(keyframes, loopMode), clamped, easings)
        }
    }

    /**
     * The segment index and pre-easing local fraction currently playing at [progress], using the
     * same folding as [sample]. The index refers to the keyframe the segment departs from, so it
     * also selects the segment's easing. Drives the easing curve display.
     */
    fun segmentPosition(keyframeCount: Int, loopMode: LoopMode, progress: Float): Pair<Int, Float> {
        if (keyframeCount < 2) return 0 to 0f
        val clamped = progress.coerceIn(0f, 1f)
        val forward = when (loopMode) {
            LoopMode.PingPong -> if (clamped <= 0.5f) clamped * 2f else (1f - clamped) * 2f
            LoopMode.Cycle -> clamped
        }
        val segments = when (loopMode) {
            LoopMode.PingPong -> keyframeCount - 1
            LoopMode.Cycle -> keyframeCount
        }
        return segmentAt(forward, segments)
    }

    /**
     * The pre-easing local fraction (0..1) of the segment currently playing at [progress]. Drives
     * the position marker on the easing curve display.
     */
    fun localFraction(keyframeCount: Int, loopMode: LoopMode, progress: Float): Float =
        segmentPosition(keyframeCount, loopMode, progress).second

    /**
     * Number of transition segments in one full [progress] sweep, used to scale the total play
     * duration. PingPong covers `(n - 1)` segments each way (2 * (n - 1) round trip); Cycle covers
     * `n` segments including the wrap back to the first keyframe.
     */
    fun segmentCountForLoop(keyframeCount: Int, loopMode: LoopMode): Int = when (loopMode) {
        LoopMode.PingPong -> (keyframeCount - 1) * 2
        LoopMode.Cycle -> keyframeCount
    }
}

/**
 * Regenerates this mesh onto a new [rows] x [columns] grid, carrying over as many vertex colors as
 * the smaller grid allows (by flat index) and resetting bezier offsets to straight edges. Used when
 * the shared grid changes so every keyframe is re-gridded consistently, matching Phase 1's
 * `generateLinearMeshState`-based approach.
 */
fun MeshData.resizedTo(rows: Int, columns: Int): MeshData {
    if (rows == this.rows && columns == this.columns) return this
    val regenerated = generateLinearMeshState(rows, columns)
    val carried = regenerated.colors.mapIndexed { index, generated ->
        colors.getOrElse(index) { generated }
    }
    return regenerated.copy(colors = carried)
}
