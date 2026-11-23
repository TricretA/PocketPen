package helium314.keyboard.ai

import android.content.Context
import androidx.core.content.edit

object AiSettings {
    private const val FILE = "ai_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_LANGUAGE = "target_language"
    private const val KEY_CUSTOM_PROMPT = "custom_prompt"
    const val DEFAULT_LANGUAGE = "English"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun provider(context: Context) = AiProvider.fromId(prefs(context).getString(KEY_PROVIDER, null))

    fun setProvider(context: Context, provider: AiProvider) =
        prefs(context).edit { putString(KEY_PROVIDER, provider.id) }

    fun model(context: Context, provider: AiProvider): String =
        prefs(context).getString("model_${provider.id}", null)?.trim()?.takeIf { it.isNotEmpty() } ?: provider.defaultModel

    fun setModel(context: Context, provider: AiProvider, model: String) =
        prefs(context).edit { putString("model_${provider.id}", model.trim()) }

    fun targetLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, null)?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_LANGUAGE

    fun setTargetLanguage(context: Context, language: String) =
        prefs(context).edit { putString(KEY_LANGUAGE, language.trim()) }

    fun customPrompt(context: Context): String = prefs(context).getString(KEY_CUSTOM_PROMPT, "").orEmpty()

    fun setCustomPrompt(context: Context, prompt: String) =
        prefs(context).edit { putString(KEY_CUSTOM_PROMPT, prompt.trim()) }
}
