package helium314.keyboard.keyboard.emoji

import android.content.Context
import helium314.keyboard.latin.utils.prefs

object EmojiSettings {
    const val PREF_SIZE = "pp_emoji_size"
    const val PREF_STAY = "pp_emoji_stay"
    const val PREF_RECENTS = "pp_emoji_recents"

    @JvmStatic
    fun columnRatio(context: Context): Float = when (context.prefs().getString(PREF_SIZE, "medium")) {
        "small" -> 1.25f
        "large" -> 0.75f
        else -> 1f
    }

    @JvmStatic
    fun glyphDp(context: Context): Float = when (context.prefs().getString(PREF_SIZE, "medium")) {
        "small" -> 20f
        "large" -> 30f
        else -> 25f
    }

    @JvmStatic
    fun stay(context: Context): Boolean = context.prefs().getBoolean(PREF_STAY, true)

    @JvmStatic
    fun recents(context: Context): Boolean = context.prefs().getBoolean(PREF_RECENTS, true)
}
