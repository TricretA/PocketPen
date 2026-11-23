package helium314.keyboard.handwriting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class HandwritingView(context: Context) : View(context) {
    interface Listener {
        fun onStrokeStarted()
        fun onStrokeFinished()
    }

    private class Stroke(val path: Path, val points: MutableList<Ink.Point>)

    var listener: Listener? = null
    var palmRejection = true
    var hint = ""

    private val density = context.resources.displayMetrics.density
    private val strokes = mutableListOf<Stroke>()
    private var current: Stroke? = null
    private var lastX = 0f
    private var lastY = 0f
    private var lastStylusTime = -PALM_WINDOW_MS
    private var inkColor = INK_COLOR

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = STROKE_DP * density
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GUIDE_COLOR
        strokeWidth = density
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GUIDE_COLOR
        textAlign = Paint.Align.CENTER
        textSize = HINT_SP * density
    }

    val strokeCount get() = strokes.size

    fun hasInk() = strokes.isNotEmpty()

    fun clear() {
        strokes.clear()
        current = null
        inkColor = INK_COLOR
        invalidate()
    }

    fun undoLast() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }

    fun markRejected(rejected: Boolean) {
        inkColor = if (rejected) REJECTED_COLOR else INK_COLOR
        invalidate()
    }

    fun buildInk(): Ink {
        val builder = Ink.builder()
        strokes.forEach { stroke ->
            val strokeBuilder = Ink.Stroke.builder()
            stroke.points.forEach { strokeBuilder.addPoint(it) }
            builder.addStroke(strokeBuilder.build())
        }
        return builder.build()
    }

    fun geometry(): InkGeometry {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        strokes.forEach { stroke ->
            stroke.points.forEach {
                minX = min(minX, it.x)
                minY = min(minY, it.y)
                maxX = max(maxX, it.x)
                maxY = max(maxY, it.y)
            }
        }
        val width = if (maxX >= minX) maxX - minX else 0f
        val height = if (maxY >= minY) maxY - minY else 0f
        var closed = false
        if (strokes.size == 1) {
            val points = strokes[0].points
            if (points.size > 2) {
                val gap = hypot(points.first().x - points.last().x, points.first().y - points.last().y)
                closed = gap <= max(width, height) * CLOSED_FRACTION
            }
        }
        return InkGeometry(strokes.size, width, height, this.height.toFloat(), closed)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val isStylus = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isStylus) lastStylusTime = event.eventTime
                else if (palmRejection && event.eventTime - lastStylusTime < PALM_WINDOW_MS) return true
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onStrokeStarted()
                startStroke(event.x, event.y, event.eventTime)
            }
            MotionEvent.ACTION_MOVE -> {
                if (current == null) return true
                if (isStylus) lastStylusTime = event.eventTime
                for (i in 0 until event.historySize) {
                    extendStroke(event.getHistoricalX(i), event.getHistoricalY(i), event.getHistoricalEventTime(i))
                }
                extendStroke(event.x, event.y, event.eventTime)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (current == null) return true
                extendStroke(event.x, event.y, event.eventTime)
                finishStroke()
            }
        }
        return true
    }

    private fun startStroke(x: Float, y: Float, time: Long) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = Stroke(path, mutableListOf(Ink.Point.create(x, y, time)))
        strokes.add(stroke)
        current = stroke
        lastX = x
        lastY = y
        inkColor = INK_COLOR
        invalidate()
    }

    private fun extendStroke(x: Float, y: Float, time: Long) {
        val stroke = current ?: return
        stroke.path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
        stroke.points.add(Ink.Point.create(x, y, time))
        lastX = x
        lastY = y
        invalidate()
    }

    private fun finishStroke() {
        current = null
        listener?.onStrokeFinished()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val baseline = height * BASELINE_FRACTION
        canvas.drawLine(density * 12f, baseline, width - density * 12f, baseline, guidePaint)
        if (strokes.isEmpty() && hint.isNotEmpty()) {
            canvas.drawText(hint, width / 2f, height / 2f, hintPaint)
        }
        inkPaint.color = inkColor
        strokes.forEach { canvas.drawPath(it.path, inkPaint) }
    }

    companion object {
        private const val STROKE_DP = 3.5f
        private const val HINT_SP = 18f
        private const val PALM_WINDOW_MS = 1500L
        private const val BASELINE_FRACTION = 0.7f
        private const val CLOSED_FRACTION = 0.25f
        private val INK_COLOR = Color.rgb(32, 33, 36)
        private val REJECTED_COLOR = Color.rgb(176, 176, 176)
        private val GUIDE_COLOR = Color.rgb(224, 224, 224)
    }
}
