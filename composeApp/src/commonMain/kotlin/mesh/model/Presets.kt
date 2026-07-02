package mesh.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * A named starting point the user can load from the control panel. A preset is a complete
 * animation (keyframes, per-segment easings, loop mode, timing), so applying one replaces the
 * whole editor state and is immediately playable.
 */
class Preset(val name: String, val build: () -> AnimationState)

/**
 * A small library of hand-picked animated gradients. Each is a single-cell (1x1) mesh whose
 * corner colors (and, for some presets, bezier edges) move across keyframes.
 */
object Presets {
    val all: List<Preset> = listOf(
        // Warm colors rotating around the quad, swinging back and forth.
        Preset("Sunset") {
            AnimationState(
                keyframes = listOf(
                    corners(
                        topLeft = Color(0xFFFFB75E),
                        topRight = Color(0xFFED8F03),
                        bottomRight = Color(0xFF7B2FF7),
                        bottomLeft = Color(0xFFF107A3),
                    ),
                    corners(
                        topLeft = Color(0xFFF107A3),
                        topRight = Color(0xFFFFB75E),
                        bottomRight = Color(0xFFED8F03),
                        bottomLeft = Color(0xFF7B2FF7),
                    ),
                ),
                durationMillisPerSegment = 3000,
                loopMode = LoopMode.PingPong,
                easings = listOf(SegmentEasing.EaseInOut, SegmentEasing.EaseInOut),
            )
        },
        // Cool colors cycling one-way, with the top edge waving via bezier offsets.
        Preset("Aurora") {
            val base = corners(
                topLeft = Color(0xFF00C9A7),
                topRight = Color(0xFF92FE9D),
                bottomRight = Color(0xFF4E54C8),
                bottomLeft = Color(0xFF00D2FF),
            )
            AnimationState(
                keyframes = listOf(
                    base,
                    corners(
                        topLeft = Color(0xFF92FE9D),
                        topRight = Color(0xFF00C9A7),
                        bottomRight = Color(0xFF00D2FF),
                        bottomLeft = Color(0xFF4E54C8),
                    ).copy(
                        topBezierOffsets = listOf(
                            Offset(0.15f, 0.12f), Offset(-0.15f, 0.12f),
                            Offset.Unspecified, Offset.Unspecified,
                        ),
                    ),
                    corners(
                        topLeft = Color(0xFF4E54C8),
                        topRight = Color(0xFF00D2FF),
                        bottomRight = Color(0xFF92FE9D),
                        bottomLeft = Color(0xFF00C9A7),
                    ),
                ),
                durationMillisPerSegment = 2500,
                loopMode = LoopMode.Cycle,
                easings = listOf(
                    SegmentEasing.EaseInOut, SegmentEasing.EaseIn, SegmentEasing.EaseOut,
                ),
            )
        },
        // Deep blues slowly swelling, with the bottom edge swaying via bezier offsets.
        Preset("Ocean") {
            AnimationState(
                keyframes = listOf(
                    corners(
                        topLeft = Color(0xFF2E3192),
                        topRight = Color(0xFF1BFFFF),
                        bottomRight = Color(0xFF0F2027),
                        bottomLeft = Color(0xFF2C5364),
                    ),
                    corners(
                        topLeft = Color(0xFF0F2027),
                        topRight = Color(0xFF2C5364),
                        bottomRight = Color(0xFF2E3192),
                        bottomLeft = Color(0xFF1BFFFF),
                    ).copy(
                        bottomBezierOffsets = listOf(
                            Offset.Unspecified, Offset.Unspecified,
                            Offset(0.12f, -0.15f), Offset(-0.12f, -0.15f),
                        ),
                    ),
                ),
                durationMillisPerSegment = 4000,
                loopMode = LoopMode.PingPong,
                easings = listOf(SegmentEasing.EaseInOut, SegmentEasing.EaseInOut),
            )
        },
    )

    /** Builds a 1x1 mesh from the four corner colors with straight (unspecified) bezier edges. */
    private fun corners(
        topLeft: Color,
        topRight: Color,
        bottomRight: Color,
        bottomLeft: Color,
    ): MeshData {
        val positions = listOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(0f, 1f), Offset(1f, 1f),
        )
        val colors = listOf(topLeft, topRight, bottomLeft, bottomRight)
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
}
