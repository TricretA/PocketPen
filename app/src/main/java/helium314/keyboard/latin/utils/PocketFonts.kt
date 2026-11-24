package helium314.keyboard.latin.utils

import android.content.Context
import helium314.keyboard.latin.R

object PocketFonts {
    const val PREF = "pp_font"

    class Entry(val id: String, val label: String, val res: Int)

    val all = listOf(
        Entry("poppins", "Poppins", R.font.poppins_regular),
        Entry("roboto_slab", "Roboto Slab", R.font.roboto_slab),
        Entry("lora", "Lora", R.font.lora),
        Entry("playfair", "Playfair", R.font.playfair),
        Entry("oswald", "Oswald", R.font.oswald),
        Entry("comfortaa", "Comfortaa", R.font.comfortaa),
        Entry("space_mono", "Space Mono", R.font.space_mono),
        Entry("caveat", "Caveat", R.font.caveat),
        Entry("dancing_script", "Dancing Script", R.font.dancing_script),
        Entry("pacifico", "Pacifico", R.font.pacifico),
        Entry("lobster", "Lobster", R.font.lobster),
        Entry("bangers", "Bangers", R.font.bangers),
        Entry("press_start", "Press Start 2P", R.font.press_start),
    )

    fun selected(context: Context): Entry? {
        val id = context.prefs().getString(PREF, "").orEmpty()
        return all.firstOrNull { it.id == id }
    }
}
