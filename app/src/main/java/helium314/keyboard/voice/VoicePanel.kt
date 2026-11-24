package helium314.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ViewGroup
import android.widget.RelativeLayout
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

object VoiceLauncher {
    private var session: VoiceSession? = null
    private var bar: VoiceBar? = null

    @JvmStatic
    fun handle(ime: LatinIME): Boolean {
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
        val strip = KeyboardSwitcher.getInstance().suggestionStrip ?: return false
        val useGroq = VoiceSettings.engine(ime) == VoiceEngine.GROQ
            && Connectivity.isOnline(ime)
            && AiKeyStore.load(ime, AiProvider.GROQ) != null
        if (!useGroq && !SpeechRecognizer.isRecognitionAvailable(ime)) return false
        cancel()
        val newBar = VoiceBar(ime, { cancel() }, { session?.onMic() })
        strip.addView(newBar, RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        bar = newBar
        val created = if (useGroq) GroqSession(ime, newBar, { cancel() }, { fallToDevice(ime) }) else DeviceSession(ime, newBar) { cancel() }
        session = created
        created.start()
        return true
    }

    @JvmStatic
    fun cancel() {
        session?.cancel()
        session = null
        bar?.let { (it.parent as? ViewGroup)?.removeView(it) }
        bar = null
    }

    private fun fallToDevice(ime: LatinIME) {
        val currentBar = bar ?: return
        session?.cancel()
        if (!SpeechRecognizer.isRecognitionAvailable(ime)) {
            currentBar.setListening(false)
            currentBar.setStatus(ime.getString(R.string.vz_error))
            session = null
            return
        }
        val device = DeviceSession(ime, currentBar) { cancel() }
        session = device
        device.start()
    }
}

private fun commitVoiceText(ime: LatinIME, text: String) {
    val connection = ime.currentInputConnection ?: return
    val before = connection.getTextBeforeCursor(1, 0)
    val prefix = if (!before.isNullOrEmpty() && !before[0].isWhitespace()) " " else ""
    connection.beginBatchEdit()
    connection.finishComposingText()
    connection.commitText(prefix + text, 1)
    connection.endBatchEdit()
}

private abstract class VoiceSession(val ime: LatinIME, val bar: VoiceBar) {
    abstract fun start()
    abstract fun onMic()
    abstract fun cancel()
    protected fun text(res: Int): String = ime.getString(res)
}

private class DeviceSession(ime: LatinIME, bar: VoiceBar, private val finish: () -> Unit) : VoiceSession(ime, bar) {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            bar.setListening(true)
            bar.setStatus(text(R.string.vz_speak_now))
        }

        override fun onBeginningOfSpeech() {
            bar.setStatus(text(R.string.vz_listening))
        }

        override fun onRmsChanged(rmsdB: Float) {
            bar.setLevel((rmsdB + 2f) / 12f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            listening = false
            bar.setListening(false)
            bar.setStatus(text(R.string.vz_transcribing))
        }

        override fun onError(error: Int) {
            listening = false
            bar.setListening(false)
            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.vz_didnt_catch
                else -> R.string.vz_error
            }
            bar.setStatus(text(message))
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val best = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (best.isEmpty()) {
                bar.setListening(false)
                bar.setStatus(text(R.string.vz_didnt_catch))
                return
            }
            commitVoiceText(ime, best)
            finish()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrBlank()) bar.setStatus(partial)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun start() {
        release()
        bar.setListening(false)
        bar.setStatus(text(R.string.vz_getting_ready))
        val created = SpeechRecognizer.createSpeechRecognizer(ime)
        recognizer = created
        created.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ime.packageName)
        val language = VoiceSettings.language(ime)
        if (language.isNotBlank()) intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        created.startListening(intent)
    }

    override fun onMic() {
        if (listening) recognizer?.stopListening() else start()
    }

    override fun cancel() {
        release()
    }

    private fun release() {
        listening = false
        recognizer?.let {
            it.setRecognitionListener(null)
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }
}

private class GroqSession(
    ime: LatinIME,
    bar: VoiceBar,
    private val finish: () -> Unit,
    private val fallback: () -> Unit,
) : VoiceSession(ime, bar) {
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recorder = VoiceRecorder(ime)
    private var pending: Future<*>? = null
    private var generation = 0
    private var recording = false
    private var working = false
    private var heardSpeech = false
    private var recordingStart = 0L
    private var lastLoud = 0L

    private val ticker = object : Runnable {
        override fun run() {
            if (!recording) return
            val now = SystemClock.uptimeMillis()
            val elapsed = now - recordingStart
            val amplitude = recorder.amplitude()
            bar.setLevel(amplitude / LEVEL_SCALE)
            if (amplitude > SPEECH_AMPLITUDE) {
                if (!heardSpeech) bar.setStatus(text(R.string.vz_listening))
                heardSpeech = true
                lastLoud = now
            }
            when {
                elapsed >= VoiceRecorder.MAX_DURATION_MS -> stopAndSend()
                heardSpeech && now - lastLoud > SILENCE_MS -> stopAndSend()
                !heardSpeech && elapsed > NO_SPEECH_MS -> giveUp()
                else -> handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun start() {
        generation++
        working = false
        heardSpeech = false
        bar.setStatus(text(R.string.vz_getting_ready))
        try {
            recorder.start()
        } catch (e: Exception) {
            bar.setListening(false)
            bar.setStatus(text(R.string.vz_start_error))
            return
        }
        recording = true
        recordingStart = SystemClock.uptimeMillis()
        lastLoud = recordingStart
        bar.setListening(true)
        bar.setStatus(text(R.string.vz_speak_now))
        handler.post(ticker)
    }

    override fun onMic() {
        if (recording) stopAndSend() else if (!working) start()
    }

    private fun giveUp() {
        recording = false
        handler.removeCallbacks(ticker)
        recorder.cancel()
        bar.setListening(false)
        bar.setStatus(text(R.string.vz_didnt_catch))
    }

    private fun stopAndSend() {
        if (!recording) return
        recording = false
        handler.removeCallbacks(ticker)
        bar.setListening(false)
        val file = recorder.stop()
        if (file == null || file.length() < MIN_BYTES) {
            bar.setStatus(text(R.string.vz_didnt_catch))
            return
        }
        working = true
        bar.setStatus(text(R.string.vz_transcribing))
        val id = ++generation
        val appContext = ime.applicationContext
        pending = executor.submit(Runnable {
            val outcome = runCatching {
                val apiKey = AiKeyStore.load(appContext, AiProvider.GROQ) ?: throw AiNetworkException("no key")
                var result = GroqSpeech.transcribe(
                    apiKey,
                    file,
                    VoiceSettings.model(appContext),
                    VoiceSettings.language(appContext),
                    VoiceSettings.vocabulary(appContext)
                )
                if (VoiceSettings.aiCleanup(appContext)) {
                    result = runCatching { AiRunner.run(appContext, AiAction.FIX_GRAMMAR, result) }.getOrDefault(result)
                }
                result
            }
            file.delete()
            handler.post {
                if (id != generation) return@post
                working = false
                outcome.onSuccess {
                    commitVoiceText(ime, it)
                    finish()
                }
                outcome.onFailure {
                    if (it is AiNetworkException) fallback()
                    else bar.setStatus(it.message ?: text(R.string.vz_error))
                }
            }
        })
    }

    override fun cancel() {
        generation++
        recording = false
        working = false
        handler.removeCallbacksAndMessages(null)
        pending?.cancel(true)
        recorder.cancel()
        executor.shutdownNow()
    }

    companion object {
        private const val TICK_MS = 80L
        private const val LEVEL_SCALE = 12000f
        private const val SPEECH_AMPLITUDE = 2500
        private const val SILENCE_MS = 1800L
        private const val NO_SPEECH_MS = 8000L
        private const val MIN_BYTES = 1500L
    }
}
