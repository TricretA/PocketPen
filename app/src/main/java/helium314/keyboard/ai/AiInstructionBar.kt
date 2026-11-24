package helium314.keyboard.ai

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.CycleInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

class AiInstructionBar(context: Context, onBack: () -> Unit, onSend: () -> Unit) : LinearLayout(context) {
    private val colors = Settings.getValues().mColors
    private val density = resources.displayMetrics.density
    private val background = ColorUtils.setAlphaComponent(colors.get(ColorType.STRIP_BACKGROUND), 255)
    private val textColor = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 255)
    private val hintColor = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 140)
    private val accent = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 255)
    private val idle = ColorUtils.blendARGB(background, textColor, 0.16f)
    private val field = ColorUtils.blendARGB(background, textColor, 0.10f)
    private val input = TextView(context)
    private val handler = Handler(Looper.getMainLooper())
    private var value = ""
    private var cursorOn = true

    private val blink = object : Runnable {
        override fun run() {
            cursorOn = !cursorOn
            render()
            handler.postDelayed(this, 500L)
        }
    }

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

        input.setTextColor(textColor)
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        input.gravity = Gravity.CENTER_VERTICAL
        input.maxLines = 1
        input.ellipsize = TextUtils.TruncateAt.START
        input.background = GradientDrawable().apply {
            setColor(field)
            cornerRadius = 20 * density
        }
        input.setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
        addView(input, LayoutParams(0, size, 1f))

        val send = FrameLayout(context)
        send.background = circle(accent)
        val sendIcon = ImageView(context)
        sendIcon.scaleType = ImageView.ScaleType.CENTER
        sendIcon.setImageDrawable(context.getDrawable(R.drawable.ic_pp_check)?.mutate()?.apply { setTint(Color.WHITE) })
        send.addView(sendIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        send.setOnClickListener { onSend() }
        val sendParams = LayoutParams(size, size)
        sendParams.setMargins(margin, 0, margin * 2, 0)
        addView(send, sendParams)
        render()
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun render() {
        if (value.isEmpty()) {
            input.setTextColor(hintColor)
            input.text = context.getString(R.string.ai_tell_hint)
        } else {
            input.setTextColor(textColor)
            input.text = if (cursorOn) "$value|" else "$value "
        }
    }

    fun setValue(text: String) {
        value = text
        cursorOn = true
        render()
    }

    fun shake() {
        ObjectAnimator.ofFloat(input, TRANSLATION_X, 0f, 10f * density).apply {
            duration = 250L
            interpolator = CycleInterpolator(3f)
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.postDelayed(blink, 500L)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(blink)
        super.onDetachedFromWindow()
    }
}

object AiInstructionMode {
    private var bar: AiInstructionBar? = null
    private var session: AiBridge.Session? = null
    private val buffer = StringBuilder()

    @JvmStatic
    fun isActive(): Boolean = bar != null

    @JvmStatic
    fun start(ime: helium314.keyboard.latin.LatinIME, newSession: AiBridge.Session): Boolean {
        val strip = helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().suggestionStrip ?: return false
        cancel()
        buffer.setLength(0)
        session = newSession
        val newBar = AiInstructionBar(ime, { cancel() }, { submit(ime) })
        strip.addView(
            newBar,
            android.widget.RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        bar = newBar
        return true
    }

    @JvmStatic
    fun onEvent(ime: helium314.keyboard.latin.LatinIME, event: helium314.keyboard.event.Event) {
        if (bar == null) return
        val code = event.codePoint
        if (event.metaState == 0 && code != helium314.keyboard.event.Event.NOT_A_CODE_POINT
            && event.keyCode == helium314.keyboard.event.Event.NOT_A_KEY_CODE
        ) {
            when {
                code == helium314.keyboard.latin.common.Constants.CODE_ENTER -> submit(ime)
                code >= ' '.code && !event.isDead -> append(String(Character.toChars(code)))
            }
            return
        }
        when (event.keyCode) {
            helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.DELETE -> deleteLast()
            helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.DELETE_WORD -> deleteWord()
            helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.MULTIPLE_CODE_POINTS -> event.text?.let { append(it.toString()) }
        }
    }

    @JvmStatic
    fun onText(text: String) {
        if (bar != null) append(text)
    }

    @JvmStatic
    fun cancel() {
        bar?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        bar = null
        session = null
        buffer.setLength(0)
    }

    private fun append(text: String) {
        buffer.append(text)
        bar?.setValue(buffer.toString())
    }

    private fun deleteLast() {
        if (buffer.isEmpty()) return
        val end = buffer.length
        val start = buffer.offsetByCodePoints(end, -1)
        buffer.delete(start, end)
        bar?.setValue(buffer.toString())
    }

    private fun deleteWord() {
        var end = buffer.length
        while (end > 0 && buffer[end - 1].isWhitespace()) end--
        while (end > 0 && !buffer[end - 1].isWhitespace()) end--
        buffer.setLength(end)
        bar?.setValue(buffer.toString())
    }

    private fun submit(ime: helium314.keyboard.latin.LatinIME) {
        val instruction = buffer.toString().trim()
        val current = session
        if (instruction.isEmpty() || current == null) {
            bar?.shake()
            return
        }
        cancel()
        AiBridge.run(ime, current, instruction)
    }
}
