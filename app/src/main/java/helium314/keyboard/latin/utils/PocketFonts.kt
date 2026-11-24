package helium314.keyboard.latin.utils

import android.content.Context
import helium314.keyboard.latin.R

object PocketFonts {
    const val PREF = "pp_font"

    class Entry(val id: String, val label: String, val res: Int)

    val all = listOf(
        Entry("poppins", "Poppins", R.font.poppins_regular),
        Entry("outfit", "Outfit", R.font.outfit),
        Entry("nunito", "Nunito", R.font.nunito),
        Entry("quicksand", "Quicksand", R.font.quicksand),
        Entry("raleway", "Raleway", R.font.raleway),
        Entry("josefin", "Josefin Sans", R.font.josefin),
        Entry("montserrat_alt", "Montserrat Alt", R.font.montserrat_alt),
        Entry("fredoka", "Fredoka", R.font.fredoka),
        Entry("comfortaa", "Comfortaa", R.font.comfortaa),
        Entry("righteous", "Righteous", R.font.righteous),
        Entry("orbitron", "Orbitron", R.font.orbitron),
        Entry("audiowide", "Audiowide", R.font.audiowide),
        Entry("oswald", "Oswald", R.font.oswald),
        Entry("bungee", "Bungee", R.font.bungee),
        Entry("sigmar", "Sigmar One", R.font.sigmar),
        Entry("chewy", "Chewy", R.font.chewy),
        Entry("bangers", "Bangers", R.font.bangers),
        Entry("marker", "Permanent Marker", R.font.marker),
        Entry("shadows", "Shadows Into Light", R.font.shadows),
        Entry("caveat", "Caveat", R.font.caveat),
        Entry("satisfy", "Satisfy", R.font.satisfy),
        Entry("dancing_script", "Dancing Script", R.font.dancing_script),
        Entry("pacifico", "Pacifico", R.font.pacifico),
        Entry("lobster", "Lobster", R.font.lobster),
        Entry("roboto_slab", "Roboto Slab", R.font.roboto_slab),
        Entry("lora", "Lora", R.font.lora),
        Entry("playfair", "Playfair", R.font.playfair),
        Entry("abril", "Abril Fatface", R.font.abril),
        Entry("cinzel", "Cinzel", R.font.cinzel),
        Entry("elite", "Special Elite", R.font.elite),
        Entry("space_mono", "Space Mono", R.font.space_mono),
        Entry("vt323", "VT323", R.font.vt323),
        Entry("press_start", "Press Start 2P", R.font.press_start),
    )

    fun selected(context: Context): Entry? {
        val id = context.prefs().getString(PREF, "").orEmpty()
        return all.firstOrNull { it.id == id }
    }
}
