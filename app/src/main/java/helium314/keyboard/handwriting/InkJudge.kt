package helium314.keyboard.handwriting

import kotlin.math.abs
import kotlin.math.max

data class InkGeometry(
    val strokeCount: Int,
    val width: Float,
    val height: Float,
    val areaHeight: Float,
    val closedLoop: Boolean,
)

object InkJudge {
    private const val MANY_STROKES = 3
    private const val LONG_TEXT = 3
    private const val BIG_SHAPE_FRACTION = 0.6f
    private const val CONFIDENT_GAP = 0.35f

    fun isText(text: String, shapeConfident: Boolean, geometry: InkGeometry): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (geometry.strokeCount >= MANY_STROKES || trimmed.length >= LONG_TEXT) return true
        if (shapeConfident) return false
        val bigLoop = geometry.closedLoop && geometry.strokeCount == 1 && geometry.height >= geometry.areaHeight * BIG_SHAPE_FRACTION
        return !bigLoop
    }

    fun isConfident(scores: List<Float?>): Boolean {
        val values = scores.filterNotNull()
        if (values.size < 2) return false
        val gap = abs(values[0] - values[1])
        val scale = max(max(abs(values[0]), abs(values[1])), 1e-6f)
        return gap / scale >= CONFIDENT_GAP
    }
}
