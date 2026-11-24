package helium314.keyboard.voice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

class VoiceBar(context: Context, onBack: () -> Unit, onMic: () -> Unit) : LinearLayout(context) {
    private val colors = Settings.getValues().mColors
    private val density = resources.displayMetrics.density
    private val background = ColorUtils.setAlphaComponent(colors.get(ColorType.STRIP_BACKGROUND), 255)
    private val textColor = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 255)
    private val accent = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 255)
    private val idle = ColorUtils.blendARGB(background, textColor, 0.16f)
    private val status = TextView(context)
    private val mic = FrameLayout(context)
    private val micIcon = ImageView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(background)
        isClickable = true
        val size = (40 * density).toInt()
        val margin = (6 * density).toInt()

        val back = FrameLayout(context)
        back.background = circle(idle)
        val backIcon = ImageView(context)
        backIcon.scaleType = ImageView.ScaleType.CENTER
        backIcon.setImageDrawable(context.getDrawable(R.drawable.ic_pp_back)?.mutate()?.apply { setTint(textColor) })
        back.addView(backIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        back.setOnClickListener { onBack() }
        val backParams = LayoutParams(size, size)
        backParams.setMargins(margin * 2, 0, margin, 0)
        addView(back, backParams)

        status.setTextColor(textColor)
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        status.gravity = Gravity.CENTER
        status.maxLines = 1
        status.ellipsize = TextUtils.TruncateAt.START
        addView(status, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        mic.background = circle(idle)
        micIcon.scaleType = ImageView.ScaleType.CENTER
        micIcon.setImageDrawable(context.getDrawable(R.drawable.sym_keyboard_voice_lxx)?.mutate()?.apply { setTint(textColor) })
        mic.addView(micIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        mic.setOnClickListener { onMic() }
        val micParams = LayoutParams(size, size)
        micParams.setMargins(margin, 0, margin * 2, 0)
        addView(mic, micParams)
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    fun setStatus(text: String) {
        status.text = text
    }

    fun setListening(listening: Boolean) {
        mic.background = circle(if (listening) accent else idle)
        micIcon.drawable?.setTint(if (listening) Color.WHITE else textColor)
        if (!listening) setLevel(0f)
    }

    fun setLevel(level: Float) {
        val scale = 1f + 0.3f * level.coerceIn(0f, 1f)
        mic.scaleX = scale
        mic.scaleY = scale
    }
}
