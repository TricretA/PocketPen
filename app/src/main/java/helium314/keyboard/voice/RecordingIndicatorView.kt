package helium314.keyboard.voice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View

class RecordingIndicatorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val startTime = SystemClock.uptimeMillis()
    private var level = 0f
    private var smoothed = 0f
    private var running = false

    fun setLevel(value: Float) {
        level = value.coerceIn(0f, 1f)
    }

    fun setRunning(value: Boolean) {
        running = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2f
        smoothed += (level - smoothed) * 0.25f
        if (running) {
            val phase = ((SystemClock.uptimeMillis() - startTime) % RIPPLE_MS) / RIPPLE_MS.toFloat()
            for (i in 0..1) {
                val progress = (phase + i * 0.5f) % 1f
                paint.color = Color.argb(((1f - progress) * 90).toInt(), 229, 57, 53)
                canvas.drawCircle(cx, cy, maxRadius * (0.45f + 0.55f * progress), paint)
            }
        }
        paint.color = if (running) Color.rgb(229, 57, 53) else Color.GRAY
        canvas.drawCircle(cx, cy, maxRadius * (0.35f + 0.15f * smoothed), paint)
        if (running) postInvalidateOnAnimation()
    }

    companion object {
        private const val RIPPLE_MS = 1500L
    }
}
