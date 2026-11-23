package helium314.keyboard.handwriting

import android.content.Context
import androidx.core.content.edit

object HandwritingSettings {
    private const val FILE = "handwriting_settings"
    const val DEFAULT_LANGUAGE = "en-US"
    const val PAUSE_FAST = 450
    const val PAUSE_NORMAL = 700
    const val PAUSE_SLOW = 1000

    val languages = listOf(
        "English" to "en-US",
        "Kiswahili" to "sw",
        "Français" to "fr",
        "Español" to "es",
        "Deutsch" to "de",
        "Português" to "pt",
        "Italiano" to "it",
        "العربية" to "ar",
        "हिन्दी" to "hi",
        "Русский" to "ru",
        "Türkçe" to "tr",
        "Bahasa Indonesia" to "id",
    )

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun languageTag(context: Context): String = prefs(context).getString("language", null) ?: DEFAULT_LANGUAGE

    fun setLanguageTag(context: Context, tag: String) = prefs(context).edit { putString("language", tag) }

    fun autoInsert(context: Context) = prefs(context).getBoolean("auto_insert", true)

    fun setAutoInsert(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean("auto_insert", enabled) }

    fun addSpace(context: Context) = prefs(context).getBoolean("add_space", true)

    fun setAddSpace(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean("add_space", enabled) }

    fun pauseMs(context: Context) = prefs(context).getInt("pause_ms", PAUSE_NORMAL)

    fun setPauseMs(context: Context, millis: Int) = prefs(context).edit { putInt("pause_ms", millis) }

    fun palmRejection(context: Context) = prefs(context).getBoolean("palm_rejection", true)

    fun setPalmRejection(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean("palm_rejection", enabled) }
}
