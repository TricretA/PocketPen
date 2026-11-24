package helium314.keyboard.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import java.lang.ref.WeakReference

object AiBridge {
    class Session(
        val sourceText: String,
        val hasSelection: Boolean,
        val beforeLength: Int,
        val afterLength: Int,
    )

    private const val RESUME_DELAY_MS = 350L
    private const val MAX_ATTEMPTS = 20
    private val handler = Handler(Looper.getMainLooper())
    private var imeRef: WeakReference<LatinIME>? = null
    private var pending: Session? = null

    fun requestInstruction(ime: LatinIME, session: Session) {
        imeRef = WeakReference(ime)
        pending = session
        val intent = Intent(ime, AiPromptActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ime.startActivity(intent)
    }

    fun deliver(instruction: String) {
        val session = pending ?: return
        val service = imeRef?.get() ?: return
        pending = null
        resume(service, session, instruction, 0)
    }

    fun cancel() {
        pending = null
    }

    private fun resume(service: LatinIME, session: Session, instruction: String, attempt: Int) {
        handler.postDelayed({
            val token = KeyboardSwitcher.getInstance().mainKeyboardView?.windowToken
            if (token != null && service.isInputViewShown) {
                service.showOptionsDialog(AiPanel(service).createResult(token, session, instruction))
            } else if (attempt < MAX_ATTEMPTS) {
                resume(service, session, instruction, attempt + 1)
            }
        }, RESUME_DELAY_MS)
    }
}

class AiPromptActivity : Activity() {
    private var delivered = false
    private lateinit var input: EditText

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val cardColor = if (night) Color.rgb(29, 32, 41) else Color.WHITE
        val textColor = if (night) Color.rgb(247, 244, 237) else Color.rgb(18, 20, 27)
        val mutedColor = if (night) Color.rgb(154, 160, 174) else Color.rgb(59, 64, 76)
        val fieldColor = if (night) Color.rgb(43, 47, 59) else Color.rgb(227, 223, 213)
        val signal = Color.rgb(240, 74, 12)
        val semibold: Typeface? = ResourcesCompat.getFont(this, R.font.poppins_semibold)
        val medium: Typeface? = ResourcesCompat.getFont(this, R.font.poppins_medium)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.argb(150, 0, 0, 0))
        root.setOnClickListener { finish() }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(cardColor, 28)
            setPadding(dp(22), dp(22), dp(22), dp(18))
            isClickable = true
        }
        card.addView(TextView(this).apply {
            text = getString(R.string.ai_prompt_title)
            textSize = 18f
            typeface = semibold
            setTextColor(textColor)
            gravity = Gravity.CENTER
        })
        input = EditText(this).apply {
            hint = getString(R.string.ai_prompt_hint)
            setHintTextColor(mutedColor)
            setTextColor(textColor)
            typeface = medium
            textSize = 16f
            background = rounded(fieldColor, 18)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 3
            setHorizontallyScrolling(false)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, _, _ ->
                send()
                true
            }
        }
        val inputParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        inputParams.topMargin = dp(16)
        card.addView(input, inputParams)

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(label: String, filled: Boolean, onClick: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            typeface = semibold
            textSize = 15f
            setTextColor(if (filled) Color.rgb(18, 20, 27) else textColor)
            background = rounded(if (filled) signal else fieldColor, 24)
            setOnClickListener { onClick() }
        }
        val left = LinearLayout.LayoutParams(0, dp(52), 1f)
        left.rightMargin = dp(6)
        val right = LinearLayout.LayoutParams(0, dp(52), 1f)
        right.leftMargin = dp(6)
        buttons.addView(button(getString(R.string.ai_cancel), false) { finish() }, left)
        buttons.addView(button(getString(R.string.ai_prompt_go), true) { send() }, right)
        val buttonParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        buttonParams.topMargin = dp(16)
        card.addView(buttons, buttonParams)

        val cardParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cardParams.gravity = Gravity.TOP
        cardParams.topMargin = dp(96)
        cardParams.leftMargin = dp(18)
        cardParams.rightMargin = dp(18)
        root.addView(card, cardParams)
        setContentView(root)
        input.requestFocus()
    }

    private fun send() {
        val instruction = input.text.toString().trim()
        if (instruction.isEmpty()) return
        delivered = true
        AiBridge.deliver(instruction)
        finish()
    }

    override fun onDestroy() {
        if (!delivered) AiBridge.cancel()
        super.onDestroy()
    }
}
