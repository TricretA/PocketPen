package helium314.keyboard.ai

import android.app.AlertDialog
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

private const val MAX_FIELD_CHARS = 6000
private const val COLUMNS = 3

fun createAiPanelDialog(latinIme: LatinIME, windowToken: IBinder): AlertDialog? = AiPanel(latinIme).create(windowToken)

private class AiPanel(private val ime: LatinIME) {
    private val context = getPlatformDialogThemeContext(ime)
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var pending: Future<*>? = null
    private var generation = 0
    private var sourceText = ""
    private var hasSelection = false
    private var beforeLength = 0
    private var afterLength = 0
    private var result: String? = null
    private lateinit var resultView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var applyButton: Button
    private lateinit var restoreButton: Button

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun toast(resId: Int) = Toast.makeText(ime, resId, Toast.LENGTH_SHORT).show()

    fun create(windowToken: IBinder): AlertDialog? {
        val inputType = ime.currentInputEditorInfo?.inputType ?: 0
        if (InputTypeUtils.isAnyPasswordInputType(inputType)) {
            toast(R.string.ai_error_password)
            return null
        }
        if (!captureText()) {
            toast(R.string.ai_error_empty)
            return null
        }

        val provider = AiSettings.provider(ime)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        root.addView(TextView(context).apply {
            val source = ime.getString(if (hasSelection) R.string.ai_source_selection else R.string.ai_source_field, sourceText.length)
            text = ime.getString(R.string.ai_header, provider.label, AiSettings.model(ime, provider), source)
            textSize = 12f
        })

        AiAction.entries.chunked(COLUMNS).forEach { actions ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            actions.forEach { action ->
                val button = Button(context).apply {
                    text = ime.getString(action.labelRes)
                    isAllCaps = false
                    textSize = 12f
                    setOnClickListener { runAction(action) }
                }
                row.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            repeat(COLUMNS - actions.size) {
                row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            }
            root.addView(row)
        }

        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.INVISIBLE
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        resultView = TextView(context).apply {
            text = ime.getString(R.string.ai_choose_action)
            textSize = 15f
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(resultView)

        val applyRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        applyButton = Button(context).apply {
            text = ime.getString(R.string.ai_apply)
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { applyResult() }
        }
        restoreButton = Button(context).apply {
            text = ime.getString(R.string.ai_restore)
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { restoreOriginal() }
        }
        applyRow.addView(applyButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        applyRow.addView(restoreButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(applyRow)

        val scroll = ScrollView(context).apply { addView(root) }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.settings_screen_ai)
            .setView(scroll)
            .setNegativeButton(R.string.ai_close, null)
            .create()
        dialog.setOnDismissListener {
            generation++
            pending?.cancel(true)
            executor.shutdownNow()
        }
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.token = windowToken
        layoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window?.attributes = layoutParams
        window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        return dialog
    }

    private fun captureText(): Boolean {
        val connection = ime.currentInputConnection ?: return false
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            sourceText = selected.toString()
            hasSelection = true
            return true
        }
        val before = connection.getTextBeforeCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(MAX_FIELD_CHARS, 0)?.toString().orEmpty()
        sourceText = before + after
        beforeLength = before.length
        afterLength = after.length
        return sourceText.isNotBlank()
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
    }

    private fun runAction(action: AiAction) {
        pending?.cancel(true)
        val id = ++generation
        result = null
        applyButton.visibility = View.GONE
        restoreButton.visibility = View.GONE
        setBusy(true)
        resultView.text = ime.getString(R.string.ai_working)
        val appContext = ime.applicationContext
        val text = sourceText
        pending = executor.submit(Runnable {
            val outcome = runCatching { AiRunner.run(appContext, action, text) }
            handler.post { if (id == generation) showOutcome(outcome) }
        })
    }

    private fun showOutcome(outcome: Result<String>) {
        setBusy(false)
        outcome.onSuccess { text ->
            result = text
            resultView.text = text
            applyButton.visibility = View.VISIBLE
        }
        outcome.onFailure { error ->
            resultView.text = error.message ?: error.javaClass.simpleName
        }
    }

    private fun applyResult() {
        val text = result ?: return
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        if (!hasSelection) connection.deleteSurroundingText(beforeLength, afterLength)
        connection.commitText(text, 1)
        connection.endBatchEdit()
        applyButton.visibility = View.GONE
        restoreButton.visibility = View.VISIBLE
    }

    private fun restoreOriginal() {
        val text = result ?: return
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        connection.deleteSurroundingText(text.length, 0)
        connection.commitText(sourceText, 1)
        connection.endBatchEdit()
        restoreButton.visibility = View.GONE
        applyButton.visibility = View.VISIBLE
    }
}
