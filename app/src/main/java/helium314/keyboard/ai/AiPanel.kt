package helium314.keyboard.ai

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.ExtractedTextRequest
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

private const val MAX_FIELD_CHARS = 6000
private const val CARD_MAX_DP = 400

fun createAiPanelDialog(latinIme: LatinIME, windowToken: IBinder): Dialog? = AiPanel(latinIme).create(windowToken)

internal class AiPanel(private val ime: LatinIME) {
    private val context: Context = getPlatformDialogThemeContext(ime)
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var pending: Future<*>? = null
    private var generation = 0
    private var session = AiBridge.Session("", false, 0, 0)
    private var result: String? = null
    private var replaced = false
    private lateinit var dialog: Dialog
    private lateinit var header: LinearLayout
    private lateinit var content: LinearLayout

    private val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val cardColor = if (night) Color.rgb(29, 32, 41) else Color.WHITE
    private val textColor = if (night) Color.rgb(247, 244, 237) else Color.rgb(18, 20, 27)
    private val mutedColor = if (night) Color.rgb(154, 160, 174) else Color.rgb(59, 64, 76)
    private val chipColor = if (night) Color.rgb(43, 47, 59) else Color.rgb(227, 223, 213)
    private val signal = Color.rgb(240, 74, 12)
    private val ink = Color.rgb(18, 20, 27)
    private val semibold: Typeface? = ResourcesCompat.getFont(context, R.font.poppins_semibold)
    private val medium: Typeface? = ResourcesCompat.getFont(context, R.font.poppins_medium)
    private val regular: Typeface? = ResourcesCompat.getFont(context, R.font.poppins_regular)

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun ripple(base: android.graphics.drawable.Drawable) =
        RippleDrawable(ColorStateList.valueOf(Color.argb(40, 0, 0, 0)), base, null)

    private fun text(value: String, size: Float, color: Int, face: Typeface?, center: Boolean = true) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = face
        if (center) gravity = Gravity.CENTER
    }

    private fun icon(res: Int, tint: Int, sizeDp: Int) = ImageView(context).apply {
        setImageResource(res)
        setColorFilter(tint)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    private fun logo(provider: AiProvider, sizeDp: Int, ring: Boolean = false): View {
        val holder = FrameLayout(context)
        val background = circle(provider.brandColor)
        if (ring) background.setStroke(dp(3), signal)
        holder.background = background
        val mark = ImageView(context)
        mark.setImageResource(provider.logo)
        mark.setColorFilter(Color.WHITE)
        val markSize = (sizeDp * 0.56f).toInt()
        val params = FrameLayout.LayoutParams(dp(markSize), dp(markSize))
        params.gravity = Gravity.CENTER
        holder.addView(mark, params)
        holder.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return holder
    }

    private fun chip(label: String, iconRes: Int?, filled: Boolean, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(dp(14), 0, dp(14), 0)
        row.background = ripple(rounded(if (filled) signal else chipColor, 22))
        row.isClickable = true
        row.isFocusable = true
        val tint = if (filled) ink else textColor
        if (iconRes != null) {
            val glyph = icon(iconRes, tint, 18)
            (glyph.layoutParams as LinearLayout.LayoutParams).rightMargin = dp(8)
            row.addView(glyph)
        }
        row.addView(text(label, 14f, tint, medium).apply { maxLines = 1 })
        row.setOnClickListener { onClick() }
        return row
    }

    private fun cell(view: View, weight: Float = 1f, height: Int = 46, start: Int = 4, end: Int = 4): View {
        val params = LinearLayout.LayoutParams(0, dp(height), weight)
        params.leftMargin = dp(start)
        params.rightMargin = dp(end)
        view.layoutParams = params
        return view
    }

    fun create(windowToken: IBinder): Dialog? {
        val inputType = ime.currentInputEditorInfo?.inputType ?: 0
        if (InputTypeUtils.isAnyPasswordInputType(inputType)) {
            Toast.makeText(ime, R.string.ai_error_password, Toast.LENGTH_SHORT).show()
            return null
        }
        session = captureSession()
        buildDialog(windowToken)
        if (session.sourceText.isBlank()) showEmpty() else showActions()
        return dialog
    }

    fun createResult(windowToken: IBinder, resumed: AiBridge.Session, instruction: String): Dialog {
        session = resumed
        buildDialog(windowToken)
        runAction(AiAction.CUSTOM, instruction)
        return dialog
    }

    private fun captureSession(): AiBridge.Session {
        val connection = ime.currentInputConnection ?: return AiBridge.Session("", false, 0, 0)
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) return AiBridge.Session(selected.toString(), true, 0, 0)
        val before = connection.getTextBeforeCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        return AiBridge.Session(before + after, false, before.length, after.length)
    }

    private fun buildDialog(windowToken: IBinder) {
        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.background = rounded(cardColor, 28)
        card.setPadding(dp(18), dp(16), dp(18), dp(20))

        header = LinearLayout(context)
        header.orientation = LinearLayout.VERTICAL
        header.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        content = LinearLayout(context)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL
        val contentParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        contentParams.topMargin = dp(14)
        card.addView(content, contentParams)

        refreshHeader()

        val scroll = ScrollView(context)
        scroll.isFillViewport = false
        scroll.addView(card)

        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(scroll)
        dialog.setOnDismissListener {
            generation++
            pending?.cancel(true)
            executor.shutdownNow()
        }
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.token = windowToken
        layoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        layoutParams?.dimAmount = 0.45f
        val screenWidth = context.resources.displayMetrics.widthPixels
        layoutParams?.width = minOf((screenWidth * 0.92f).toInt(), dp(CARD_MAX_DP))
        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams?.gravity = Gravity.CENTER
        window?.attributes = layoutParams
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun refreshHeader() {
        header.removeAllViews()
        val provider = AiSettings.provider(ime)
        val top = LinearLayout(context)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(View(context), LinearLayout.LayoutParams(dp(40), dp(40)))
        val center = LinearLayout(context)
        center.orientation = LinearLayout.VERTICAL
        center.gravity = Gravity.CENTER_HORIZONTAL
        center.addView(logo(provider, 52))
        center.addView(text(provider.label, 16f, textColor, semibold).apply {
            setPadding(0, dp(6), 0, 0)
        })
        center.addView(text(AiSettings.model(ime, provider), 11f, mutedColor, regular).apply { maxLines = 1 })
        top.addView(center, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val close = FrameLayout(context)
        close.background = ripple(circle(chipColor))
        close.isClickable = true
        close.isFocusable = true
        close.contentDescription = ime.getString(R.string.ai_close)
        val glyph = icon(R.drawable.ic_pp_close, textColor, 18)
        val glyphParams = FrameLayout.LayoutParams(dp(18), dp(18))
        glyphParams.gravity = Gravity.CENTER
        close.addView(glyph, glyphParams)
        close.setOnClickListener { dialog.dismiss() }
        top.addView(close, LinearLayout.LayoutParams(dp(40), dp(40)))
        header.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val ready = AiProvider.entries.filter { AiKeyStore.hasKey(ime, it) }
        if (ready.size > 1) {
            val switcher = LinearLayout(context)
            switcher.orientation = LinearLayout.HORIZONTAL
            switcher.gravity = Gravity.CENTER
            ready.forEach { other ->
                val button = logo(other, 34, ring = other == provider)
                (button.layoutParams as LinearLayout.LayoutParams).apply {
                    leftMargin = dp(5)
                    rightMargin = dp(5)
                }
                button.isClickable = true
                button.contentDescription = other.label
                button.setOnClickListener {
                    AiSettings.setProvider(ime, other)
                    refreshHeader()
                }
                switcher.addView(button)
            }
            val switcherParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            switcherParams.topMargin = dp(12)
            header.addView(switcher, switcherParams)
        }
    }

    private fun sourceLabel(): String =
        ime.getString(if (session.hasSelection) R.string.ai_source_selection else R.string.ai_source_field, session.sourceText.length)

    private fun showActions() {
        content.removeAllViews()
        content.addView(text(sourceLabel(), 12f, mutedColor, regular).apply {
            background = rounded(chipColor, 14)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        })
        val tell = chip(ime.getString(R.string.ai_action_custom), R.drawable.ic_ai, true) { askInstruction() }
        val tellParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        tellParams.topMargin = dp(14)
        tellParams.bottomMargin = dp(8)
        content.addView(tell, tellParams)
        val actions = AiAction.entries.filter { it != AiAction.ASK && it != AiAction.CUSTOM }
        actions.chunked(2).forEach { pair ->
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            pair.forEach { action ->
                row.addView(cell(chip(ime.getString(action.labelRes), null, false) { runAction(action, null) }))
            }
            if (pair.size == 1) row.addView(cell(View(context)))
            val rowParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rowParams.topMargin = dp(8)
            content.addView(row, rowParams)
        }
    }

    private fun showEmpty() {
        content.removeAllViews()
        val badge = FrameLayout(context)
        badge.background = circle(chipColor)
        val glyph = icon(R.drawable.ic_ai, signal, 34)
        val glyphParams = FrameLayout.LayoutParams(dp(34), dp(34))
        glyphParams.gravity = Gravity.CENTER
        badge.addView(glyph, glyphParams)
        content.addView(badge, LinearLayout.LayoutParams(dp(76), dp(76)))
        content.addView(text(ime.getString(R.string.ai_empty_title), 18f, textColor, semibold).apply {
            setPadding(0, dp(14), 0, dp(4))
        })
        content.addView(text(ime.getString(R.string.ai_empty_hint), 13f, mutedColor, regular))
        val ask = chip(ime.getString(R.string.ai_action_custom), R.drawable.ic_ai, true) { askInstruction() }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        params.topMargin = dp(20)
        content.addView(ask, params)
    }

    private fun askInstruction() {
        dialog.dismiss()
        AiBridge.requestInstruction(ime, session)
    }

    private fun showWorking() {
        content.removeAllViews()
        val spinner = ProgressBar(context)
        spinner.isIndeterminate = true
        spinner.indeterminateTintList = ColorStateList.valueOf(signal)
        content.addView(spinner, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(text(ime.getString(R.string.ai_thinking), 14f, mutedColor, medium).apply {
            setPadding(0, dp(12), 0, dp(8))
        })
    }

    private fun showError(message: String) {
        content.removeAllViews()
        content.addView(text(message, 14f, textColor, medium).apply { setPadding(dp(8), dp(4), dp(8), dp(16)) })
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(cell(chip(ime.getString(R.string.ai_try_another), R.drawable.ic_pp_refresh, false) { showActions() }, 1f, 48))
        content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun showResult(output: String) {
        content.removeAllViews()
        val box = ScrollView(context)
        box.background = rounded(chipColor, 18)
        val outputView = text(output, 15f, textColor, regular, center = false)
        outputView.setTextIsSelectable(true)
        outputView.setPadding(dp(16), dp(14), dp(16), dp(14))
        box.addView(outputView)
        content.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)))

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        val copy = chip(ime.getString(R.string.ai_copy), R.drawable.ic_pp_copy, false) {}
        copy.setOnClickListener { copyResult(copy) }
        val insert = chip(ime.getString(R.string.ai_insert), R.drawable.ic_pp_add, false) { insertResult() }
        val replace = chip(ime.getString(R.string.ai_replace), R.drawable.ic_pp_replace, true) {}
        replace.setOnClickListener { replaceResult(replace) }
        row.addView(cell(copy, 1f, 50, 0, 4))
        row.addView(cell(insert, 1f, 50, 4, 4))
        row.addView(cell(replace, 1.15f, 50, 4, 0))
        val rowParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rowParams.topMargin = dp(14)
        content.addView(row, rowParams)

        val again = chip(ime.getString(R.string.ai_try_another), R.drawable.ic_pp_refresh, false) { showActionsOrEmpty() }
        val againParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46))
        againParams.topMargin = dp(10)
        content.addView(again, againParams)
    }

    private fun showActionsOrEmpty() {
        if (session.sourceText.isBlank()) showEmpty() else showActions()
    }

    private fun runAction(action: AiAction, instruction: String?) {
        pending?.cancel(true)
        val id = ++generation
        result = null
        replaced = false
        showWorking()
        val appContext = ime.applicationContext
        val text = session.sourceText
        pending = executor.submit(Runnable {
            val outcome = runCatching { AiRunner.run(appContext, action, text, instruction) }
            handler.post {
                if (id != generation) return@post
                outcome.onSuccess {
                    result = it
                    showResult(it)
                }
                outcome.onFailure { showError(it.message ?: it.javaClass.simpleName) }
            }
        })
    }

    private fun copyResult(button: LinearLayout) {
        val output = result ?: return
        val clipboard = ime.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("", output))
        (button.getChildAt(button.childCount - 1) as? TextView)?.text = ime.getString(R.string.ai_copied)
    }

    private fun insertResult() {
        val output = result ?: return
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        var prefix = ""
        if (session.hasSelection) {
            val extracted = connection.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null) {
                val end = extracted.startOffset + extracted.selectionEnd
                connection.setSelection(end, end)
                prefix = " "
            }
        }
        connection.commitText(prefix + output, 1)
        connection.endBatchEdit()
        dialog.dismiss()
    }

    private fun replaceResult(button: LinearLayout) {
        val output = result ?: return
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        if (replaced) {
            connection.deleteSurroundingText(output.length, 0)
            connection.commitText(session.sourceText, 1)
            replaced = false
            (button.getChildAt(button.childCount - 1) as? TextView)?.text = ime.getString(R.string.ai_replace)
        } else {
            if (!session.hasSelection) connection.deleteSurroundingText(session.beforeLength, session.afterLength)
            connection.commitText(output, 1)
            replaced = true
            (button.getChildAt(button.childCount - 1) as? TextView)?.text = ime.getString(R.string.ai_restore)
        }
        connection.endBatchEdit()
    }
}
