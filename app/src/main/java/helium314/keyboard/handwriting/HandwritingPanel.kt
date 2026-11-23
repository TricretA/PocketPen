package helium314.keyboard.handwriting

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext

fun createHandwritingDialog(latinIme: LatinIME, anchor: View): Dialog? {
    val inputType = latinIme.currentInputEditorInfo?.inputType ?: 0
    if (InputTypeUtils.isAnyPasswordInputType(inputType)) {
        Toast.makeText(latinIme, R.string.hw_error_password, Toast.LENGTH_SHORT).show()
        return null
    }
    return HandwritingPanel(latinIme).create(anchor)
}

private class HandwritingPanel(private val ime: LatinIME) : HandwritingView.Listener {
    private val context = getPlatformDialogThemeContext(ime)
    private val handler = Handler(Looper.getMainLooper())
    private val engine = HandwritingEngine(HandwritingSettings.languageTag(ime))
    private var ready = false
    private var generation = 0
    private lateinit var canvas: HandwritingView
    private lateinit var statusView: TextView
    private lateinit var chipRow: LinearLayout
    private lateinit var dialog: Dialog

    private val recognizeRunnable = Runnable { recognize() }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun create(anchor: View): Dialog {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val strip = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        strip.addView(chipRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        statusView = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.rgb(95, 99, 104))
            setPadding(dp(12), 0, dp(12), 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        top.addView(strip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(TOP_BAR_DP)))

        canvas = HandwritingView(context).apply {
            listener = this@HandwritingPanel
            palmRejection = HandwritingSettings.palmRejection(ime)
            hint = ime.getString(R.string.hw_hint)
        }
        root.addView(canvas, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(barButton(R.string.hw_back) { dialog.dismiss() }, barParams(1.2f))
        bottom.addView(barButton(R.string.hw_undo) { canvas.undoLast() }, barParams(1f))
        bottom.addView(barButton(R.string.hw_clear) { clearCanvas() }, barParams(1f))
        bottom.addView(barButton(R.string.hw_space) { commitRaw(" ") }, barParams(1.6f))
        bottom.addView(barButton(R.string.hw_backspace) { sendKey(KeyEvent.KEYCODE_DEL) }, barParams(1f))
        bottom.addView(barButton(R.string.hw_enter) { sendKey(KeyEvent.KEYCODE_ENTER) }, barParams(1f))
        root.addView(bottom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(BOTTOM_BAR_DP)))

        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        dialog.setOnDismissListener {
            generation++
            handler.removeCallbacks(recognizeRunnable)
            engine.close()
        }
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val extra = dp(EXTRA_TOP_DP)
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.token = anchor.windowToken
        layoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        layoutParams?.gravity = Gravity.TOP or Gravity.START
        layoutParams?.x = location[0]
        layoutParams?.y = location[1] - extra
        layoutParams?.width = anchor.width
        layoutParams?.height = anchor.height + extra
        window?.attributes = layoutParams
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        prepareEngine()
        return dialog
    }

    private fun barParams(weight: Float) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)

    private fun barButton(textRes: Int, onClick: () -> Unit) = Button(context).apply {
        text = ime.getString(textRes)
        isAllCaps = false
        textSize = 13f
        setOnClickListener { onClick() }
    }

    private fun prepareEngine() {
        statusView.text = ime.getString(R.string.hw_preparing)
        engine.prepare(
            onDownloading = { statusView.text = ime.getString(R.string.hw_downloading) },
            onReady = {
                ready = true
                statusView.text = ""
                if (canvas.hasInk()) scheduleRecognition()
            },
            onFailed = { reason ->
                ready = false
                statusView.text = ime.getString(R.string.hw_model_failed)
                showChips(listOf(ime.getString(R.string.hw_retry)), retry = true)
                Toast.makeText(ime, reason, Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onStrokeStarted() {
        generation++
        handler.removeCallbacks(recognizeRunnable)
        chipRow.removeAllViews()
        statusView.text = ""
    }

    override fun onStrokeFinished() {
        scheduleRecognition()
    }

    private fun scheduleRecognition() {
        handler.removeCallbacks(recognizeRunnable)
        if (ready && canvas.hasInk()) handler.postDelayed(recognizeRunnable, HandwritingSettings.pauseMs(ime).toLong())
    }

    private fun recognize() {
        if (!ready || !canvas.hasInk()) return
        val id = ++generation
        val geometry = canvas.geometry()
        val preContext = ime.currentInputConnection?.getTextBeforeCursor(PRE_CONTEXT_CHARS, 0)?.toString().orEmpty()
        val recognitionContext = RecognitionContext.builder()
            .setPreContext(preContext)
            .setWritingArea(WritingArea(canvas.width.toFloat(), canvas.height.toFloat()))
            .build()
        statusView.text = ime.getString(R.string.hw_reading)
        engine.recognize(canvas.buildInk(), recognitionContext) { outcome ->
            if (id == generation) showOutcome(outcome, geometry)
        }
    }

    private fun showOutcome(outcome: Result<RecognitionOutcome>, geometry: InkGeometry) {
        statusView.text = ""
        val result = outcome.getOrNull()
        if (result == null || result.candidates.isEmpty()) {
            statusView.text = ime.getString(R.string.hw_no_text)
            canvas.markRejected(true)
            return
        }
        val top = result.candidates.first()
        val isText = InkJudge.isText(top, result.shapeConfident, geometry)
        if (!isText) {
            canvas.markRejected(true)
            statusView.text = ime.getString(R.string.hw_not_text)
            showChips(result.candidates, retry = false)
            return
        }
        if (HandwritingSettings.autoInsert(ime)) {
            insertText(top)
        } else {
            showChips(result.candidates, retry = false)
        }
    }

    private fun showChips(items: List<String>, retry: Boolean) {
        chipRow.removeAllViews()
        items.forEach { item ->
            val chip = Button(context).apply {
                text = item
                isAllCaps = false
                textSize = 14f
                setOnClickListener {
                    if (retry) {
                        chipRow.removeAllViews()
                        prepareEngine()
                    } else {
                        insertText(item)
                    }
                }
            }
            chipRow.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun insertText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val addSpace = HandwritingSettings.addSpace(ime) && trimmed.last().isLetterOrDigit()
        commitRaw(if (addSpace) "$trimmed " else trimmed)
        clearCanvas()
    }

    private fun commitRaw(text: String) {
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        connection.commitText(text, 1)
        connection.endBatchEdit()
    }

    private fun sendKey(keyCode: Int) {
        ime.sendDownUpKeyEvents(keyCode)
    }

    private fun clearCanvas() {
        generation++
        handler.removeCallbacks(recognizeRunnable)
        canvas.clear()
        chipRow.removeAllViews()
        statusView.text = ""
    }

    companion object {
        private const val TOP_BAR_DP = 44
        private const val BOTTOM_BAR_DP = 48
        private const val EXTRA_TOP_DP = 48
        private const val PRE_CONTEXT_CHARS = 20
    }
}
