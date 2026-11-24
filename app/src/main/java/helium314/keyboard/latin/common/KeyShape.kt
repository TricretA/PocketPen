package helium314.keyboard.latin.common

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.Drawable

object KeyShape {
    const val PREF_CUSTOM = "pp_key_custom"
    const val PREF_RADIUS = "pp_key_radius"
    const val PREF_BORDER = "pp_key_border"
    const val PREF_BORDER_COLOR = "pp_key_border_color"
    const val PREF_LIP = "pp_key_lip"

    const val DEFAULT_RADIUS = 8
    const val DEFAULT_BORDER = 0f
    const val DEFAULT_LIP = 0f

    class Spec(val radius: Float, val border: Float, val borderColor: Int, val lip: Float)

    fun readOrDefault(prefs: SharedPreferences) = Spec(
        prefs.getInt(PREF_RADIUS, DEFAULT_RADIUS).toFloat(),
        prefs.getFloat(PREF_BORDER, DEFAULT_BORDER),
        prefs.getInt(PREF_BORDER_COLOR, 0),
        prefs.getFloat(PREF_LIP, DEFAULT_LIP)
    )

    fun read(prefs: SharedPreferences): Spec? = if (prefs.getBoolean(PREF_CUSTOM, false)) readOrDefault(prefs) else null
}

class KeyShapeDrawable(
    private val spec: KeyShape.Spec,
    private val density: Float,
    private val autoBorderColor: Int,
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var tint: ColorStateList? = null
    private var color = Color.WHITE
    private var empty = false
    private var pressed = false

    override fun isStateful() = true

    override fun setTintList(tint: ColorStateList?) {
        this.tint = tint
        onStateChange(state)
        invalidateSelf()
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {}

    override fun onStateChange(stateSet: IntArray): Boolean {
        val list = tint
        val newColor = list?.getColorForState(stateSet, list.defaultColor) ?: Color.WHITE
        val newEmpty = stateSet.contains(android.R.attr.state_empty)
        val newPressed = stateSet.contains(android.R.attr.state_pressed)
        val changed = newColor != color || newEmpty != empty || newPressed != pressed
        color = newColor
        empty = newEmpty
        pressed = newPressed
        return changed
    }

    override fun draw(canvas: Canvas) {
        if (empty && !pressed) return
        val bounds = bounds
        if (bounds.isEmpty) return
        val lip = spec.lip * density
        val border = spec.border * density
        val half = border / 2f
        val radius = spec.radius * density
        rect.set(bounds.left + half, bounds.top + half, bounds.right - half, bounds.bottom - half - lip)
        if (rect.height() <= 0f) return
        val r = radius.coerceAtMost(minOf(rect.width(), rect.height()) / 2f)
        if (lip > 0f) {
            lipPaint.color = darken(color)
            rect.offset(0f, lip)
            canvas.drawRoundRect(rect, r, r, lipPaint)
            rect.offset(0f, -lip)
        }
        fillPaint.color = color
        canvas.drawRoundRect(rect, r, r, fillPaint)
        if (border > 0f) {
            strokePaint.strokeWidth = border
            strokePaint.color = if (spec.borderColor != 0) spec.borderColor else autoBorderColor
            canvas.drawRoundRect(rect, r, r, strokePaint)
        }
    }

    private fun darken(c: Int): Int {
        val factor = 0.72f
        return Color.argb(Color.alpha(c), (Color.red(c) * factor).toInt(), (Color.green(c) * factor).toInt(), (Color.blue(c) * factor).toInt())
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        lipPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        lipPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
