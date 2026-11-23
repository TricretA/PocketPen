package helium314.keyboard.voice

import android.content.Context
import androidx.core.content.edit

enum class VoiceEngine(val id: String) {
    DEVICE("device"),
    GROQ("groq");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: GROQ
    }
}

object VoiceSettings {
    private const val FILE = "voice_settings"
    const val MODEL_TURBO = "whisper-large-v3-turbo"
    const val MODEL_ACCURATE = "whisper-large-v3"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun engine(context: Context) = VoiceEngine.fromId(prefs(context).getString("engine", null))

    fun setEngine(context: Context, engine: VoiceEngine) = prefs(context).edit { putString("engine", engine.id) }

    fun model(context: Context): String =
        prefs(context).getString("model", null)?.takeIf { it == MODEL_ACCURATE || it == MODEL_TURBO } ?: MODEL_TURBO

    fun setModel(context: Context, model: String) = prefs(context).edit { putString("model", model) }

    fun language(context: Context): String = prefs(context).getString("language", "").orEmpty()

    fun setLanguage(context: Context, language: String) = prefs(context).edit { putString("language", language.trim().lowercase()) }

    fun vocabulary(context: Context): String = prefs(context).getString("vocabulary", "").orEmpty()

    fun setVocabulary(context: Context, vocabulary: String) = prefs(context).edit { putString("vocabulary", vocabulary.trim()) }

    fun aiCleanup(context: Context) = prefs(context).getBoolean("ai_cleanup", false)

    fun setAiCleanup(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean("ai_cleanup", enabled) }
}
