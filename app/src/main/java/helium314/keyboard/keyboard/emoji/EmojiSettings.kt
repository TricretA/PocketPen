package helium314.keyboard.keyboard.emoji

import android.content.Context
import helium314.keyboard.latin.utils.prefs

object EmojiSettings {
    const val PREF_SIZE = "pp_emoji_size"
    const val PREF_STAY = "pp_emoji_stay"
    const val PREF_RECENTS = "pp_emoji_recents"

    @JvmStatic
    fun scale(context: Context): Float = when (context.prefs().getString(PREF_SIZE, "medium")) {
        "small" -> 0.72f
        "large" -> 1f
        else -> 0.84f
    }

    @JvmStatic
    fun stay(context: Context): Boolean = context.prefs().getBoolean(PREF_STAY, true)

    @JvmStatic
    fun recents(context: Context): Boolean = context.prefs().getBoolean(PREF_RECENTS, true)
}
