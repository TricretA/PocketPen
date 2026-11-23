package helium314.keyboard.voice

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import helium314.keyboard.ai.AiAction
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiNetworkException
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.ai.AiRunner
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

object VoiceLauncher {
    @JvmStatic
    fun handle(ime: LatinIME): Boolean {
        if (VoiceSettings.engine(ime) != VoiceEngine.GROQ) return false
        if (!Connectivity.isOnline(ime)) {
            Toast.makeText(ime, R.string.voice_offline_using_device, Toast.LENGTH_SHORT).show()
            return false
        }
        if (AiKeyStore.load(ime, AiProvider.GROQ) == null) {
            Toast.makeText(ime, R.string.voice_no_key_using_device, Toast.LENGTH_LONG).show()
            return false
        }
        val inputType = ime.currentInputEditorInfo?.inputType ?: 0
        if (InputTypeUtils.isAnyPasswordInputType(inputType)) {
            Toast.makeText(ime, R.string.voice_error_password, Toast.LENGTH_SHORT).show()
            return true
        }
        if (ContextCompat.checkSelfPermission(ime, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(ime, VoicePermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ime.startActivity(intent)
            Toast.makeText(ime, R.string.voice_permission_needed, Toast.LENGTH_LONG).show()
            return true
        }
        val windowToken = KeyboardSwitcher.getInstance().mainKeyboardView?.windowToken ?: return false
        ime.showOptionsDialog(VoicePanel(ime).create(windowToken))
        return true
    }
}

private class VoicePanel(private val ime: LatinIME) {
    private val context = getPlatformDialogThemeContext(ime)
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recorder = VoiceRecorder(ime)
    private var pending: Future<*>? = null
    private var generation = 0
    private var recordingStart = 0L
    private var recording = false
    private var transcript: String? = null
    private lateinit var dialog: AlertDialog
    private lateinit var indicator: RecordingIndicatorView
    private lateinit var timerView: TextView
    private lateinit var statusView: TextView
    private lateinit var transcriptView: TextView
    private lateinit var stopButton: Button
    private lateinit var insertButton: Button
    private lateinit var cleanupButton: Button
    private lateinit var againButton: Button
    private lateinit var deviceButton: Button

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private val ticker = object : Runnable {
        override fun run() {
            if (!recording) return
            val elapsed = SystemClock.uptimeMillis() - recordingStart
            val seconds = (elapsed / 1000).toInt()
            timerView.text = String.format("%d:%02d", seconds / 60, seconds % 60)
            indicator.setLevel(recorder.amplitude() / LEVEL_SCALE)
            if (elapsed >= VoiceRecorder.MAX_DURATION_MS) stopRecording()
            else handler.postDelayed(this, TICK_MS)
        }
    }

    fun create(windowToken: IBinder): AlertDialog {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        indicator = RecordingIndicatorView(context)
        root.addView(indicator, LinearLayout.LayoutParams(dp(140), dp(140)))
        timerView = TextView(context).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            text = "0:00"
        }
        root.addView(timerView)
        statusView = TextView(context).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(statusView)
        transcriptView = TextView(context).apply {
            textSize = 16f
            setTextIsSelectable(true)
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(transcriptView)

        stopButton = button(R.string.voice_stop) { stopRecording() }
        insertButton = button(R.string.voice_insert) { insertTranscript() }
        cleanupButton = button(R.string.voice_cleanup) { cleanupTranscript() }
        againButton = button(R.string.voice_again) { startRecording() }
        deviceButton = button(R.string.voice_use_device) {
            dialog.dismiss()
            ime.switchToDeviceVoiceInput()
        }
        listOf(stopButton, insertButton, cleanupButton, againButton, deviceButton).forEach {
            root.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.voice_title)
            .setView(android.widget.ScrollView(context).apply { addView(root) })
            .setNegativeButton(R.string.ai_close, null)
            .create()
        dialog.setOnShowListener { startRecording() }
        dialog.setOnDismissListener {
            generation++
            recording = false
            handler.removeCallbacks(ticker)
            pending?.cancel(true)
            recorder.cancel()
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

    private fun button(textRes: Int, onClick: () -> Unit) = Button(context).apply {
        text = ime.getString(textRes)
        isAllCaps = false
        visibility = View.GONE
        setOnClickListener { onClick() }
    }

    private fun showButtons(vararg visible: Button) {
        listOf(stopButton, insertButton, cleanupButton, againButton, deviceButton).forEach {
            it.visibility = if (it in visible) View.VISIBLE else View.GONE
        }
    }

    private fun startRecording() {
        generation++
        transcript = null
        transcriptView.visibility = View.GONE
        try {
            recorder.start()
        } catch (e: Exception) {
            indicator.setRunning(false)
            statusView.text = ime.getString(R.string.voice_error_start, e.message ?: e.javaClass.simpleName)
            showButtons(againButton, deviceButton)
            return
        }
        recording = true
        recordingStart = SystemClock.uptimeMillis()
        indicator.setRunning(true)
        statusView.text = ime.getString(R.string.voice_recording)
        showButtons(stopButton)
        handler.post(ticker)
    }

    private fun stopRecording() {
        if (!recording) return
        recording = false
        handler.removeCallbacks(ticker)
        indicator.setRunning(false)
        indicator.setLevel(0f)
        val file = recorder.stop()
        if (file == null || file.length() == 0L) {
            statusView.text = ime.getString(R.string.voice_too_short)
            showButtons(againButton)
            return
        }
        statusView.text = ime.getString(R.string.voice_transcribing)
        showButtons()
        val id = ++generation
        val appContext = ime.applicationContext
        pending = executor.submit(Runnable {
            val outcome = runCatching {
                val apiKey = AiKeyStore.load(appContext, AiProvider.GROQ)
                    ?: throw IllegalStateException(appContext.getString(R.string.voice_no_key_using_device))
                var text = GroqSpeech.transcribe(
                    apiKey,
                    file,
                    VoiceSettings.model(appContext),
                    VoiceSettings.language(appContext),
                    VoiceSettings.vocabulary(appContext)
                )
                if (VoiceSettings.aiCleanup(appContext)) {
                    text = runCatching { AiRunner.run(appContext, AiAction.FIX_GRAMMAR, text) }.getOrDefault(text)
                }
                text
            }
            file.delete()
            handler.post { if (id == generation) showTranscript(outcome) }
        })
    }

    private fun showTranscript(outcome: Result<String>) {
        outcome.onSuccess { text ->
            transcript = text
            transcriptView.text = text
            transcriptView.visibility = View.VISIBLE
            statusView.text = ime.getString(R.string.voice_review)
            showButtons(insertButton, cleanupButton, againButton)
        }
        outcome.onFailure { error ->
            statusView.text = error.message ?: error.javaClass.simpleName
            if (error is AiNetworkException) showButtons(againButton, deviceButton) else showButtons(againButton)
        }
    }

    private fun cleanupTranscript() {
        val text = transcript ?: return
        val id = ++generation
        val appContext = ime.applicationContext
        statusView.text = ime.getString(R.string.ai_working)
        showButtons()
        pending = executor.submit(Runnable {
            val outcome = runCatching { AiRunner.run(appContext, AiAction.FIX_GRAMMAR, text) }
            handler.post {
                if (id != generation) return@post
                outcome.onSuccess {
                    transcript = it
                    transcriptView.text = it
                    statusView.text = ime.getString(R.string.voice_review)
                }
                outcome.onFailure { statusView.text = it.message ?: it.javaClass.simpleName }
                showButtons(insertButton, cleanupButton, againButton)
            }
        })
    }

    private fun insertTranscript() {
        val text = transcript ?: return
        val connection = ime.currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.finishComposingText()
        connection.commitText(text, 1)
        connection.endBatchEdit()
        dialog.dismiss()
    }

    companion object {
        private const val TICK_MS = 80L
        private const val LEVEL_SCALE = 12000f
    }
}
