package helium314.keyboard.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    val file = File(context.cacheDir, "voice_input.m4a")

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    fun start() {
        file.delete()
        val created = newRecorder()
        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioChannels(1)
            created.setAudioSamplingRate(SAMPLE_RATE)
            created.setAudioEncodingBitRate(BIT_RATE)
            created.setMaxDuration(MAX_DURATION_MS)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
        } catch (e: Exception) {
            created.release()
            throw e
        }
        recorder = created
    }

    fun amplitude(): Int = try {
        recorder?.maxAmplitude ?: 0
    } catch (e: RuntimeException) {
        0
    }

    fun stop(): File? {
        val active = recorder ?: return null
        recorder = null
        return try {
            active.stop()
            file
        } catch (e: RuntimeException) {
            file.delete()
            null
        } finally {
            active.release()
        }
    }

    fun cancel() {
        val active = recorder
        recorder = null
        if (active != null) {
            try {
                active.stop()
            } catch (e: RuntimeException) {
            }
            active.release()
        }
        file.delete()
    }

    companion object {
        const val MAX_DURATION_MS = 10 * 60 * 1000
        private const val SAMPLE_RATE = 16000
        private const val BIT_RATE = 48000
    }
}
