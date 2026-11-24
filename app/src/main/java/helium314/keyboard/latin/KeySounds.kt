package helium314.keyboard.latin

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import androidx.core.content.edit
import helium314.keyboard.latin.utils.prefs
import java.io.File

object KeySounds {
    const val PREF_CHOICE = "pp_sound_choice"
    const val SYSTEM = "system"
    const val CUSTOM = "custom"
    private const val CUSTOM_FILE = "custom_key_sound"
    private const val MAX_BYTES = 1_500_000L
    private const val DEFAULT_VOLUME = 0.5f

    val builtIn = listOf(
        "soft" to R.raw.key_soft,
        "click" to R.raw.key_click,
        "pop" to R.raw.key_pop,
        "typewriter" to R.raw.key_typewriter,
        "bubble" to R.raw.key_bubble,
    )

    private var pool: SoundPool? = null
    private var loadedChoice: String? = null
    private var loadedStamp = 0L
    private var soundId = 0
    private var ready = false
    private var pendingVolume: Float? = null

    fun choice(context: Context): String = context.prefs().getString(PREF_CHOICE, SYSTEM) ?: SYSTEM

    fun setChoice(context: Context, choice: String) = context.prefs().edit { putString(PREF_CHOICE, choice) }

    fun customFile(context: Context) = File(context.filesDir, CUSTOM_FILE)

    fun hasCustom(context: Context) = customFile(context).let { it.exists() && it.length() > 0 }

    fun importCustom(context: Context, uri: Uri): Boolean = try {
        val target = customFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) throw IllegalStateException("too large")
                    out.write(buffer, 0, read)
                }
            }
        } ?: throw IllegalStateException("no stream")
        loadedChoice = null
        true
    } catch (e: Exception) {
        customFile(context).delete()
        false
    }

    private fun effective(volume: Float) = if (volume < 0f) DEFAULT_VOLUME else volume.coerceIn(0f, 1f)

    private fun ensureLoaded(context: Context, choice: String): Boolean {
        val stamp = if (choice == CUSTOM) customFile(context).lastModified() else 0L
        if (pool != null && loadedChoice == choice && loadedStamp == stamp) return true
        val resId = builtIn.firstOrNull { it.first == choice }?.second
        if (resId == null && !(choice == CUSTOM && hasCustom(context))) return false
        pool?.release()
        ready = false
        val created = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        created.setOnLoadCompleteListener { sp, id, status ->
            if (status == 0 && id == soundId) {
                ready = true
                pendingVolume?.let { sp.play(id, it, it, 1, 0, 1f) }
            }
            pendingVolume = null
        }
        pool = created
        loadedChoice = choice
        loadedStamp = stamp
        soundId = if (resId != null) created.load(context.applicationContext, resId, 1) else created.load(customFile(context).absolutePath, 1)
        return true
    }

    fun play(context: Context, volume: Float): Boolean {
        val choice = choice(context)
        if (choice == SYSTEM) return false
        if (!ensureLoaded(context, choice)) return false
        if (!ready) return true
        val v = effective(volume)
        pool?.play(soundId, v, v, 1, 0, 1f)
        return true
    }

    fun preview(context: Context, choice: String, volume: Float) {
        if (choice == SYSTEM) return
        val v = effective(volume)
        pendingVolume = v
        if (!ensureLoaded(context, choice)) return
        if (ready) {
            pendingVolume = null
            pool?.play(soundId, v, v, 1, 0, 1f)
        }
    }
}
