package helium314.keyboard.latin.utils

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.DefaultColors
import helium314.keyboard.latin.common.KeyShape
import helium314.keyboard.latin.settings.Settings

class PocketTheme(
    val id: String,
    @StringRes val label: Int,
    val dark: Boolean,
    val accent: Long,
    val background: Long,
    val key: Long,
    val functional: Long,
    val space: Long,
    val text: Long,
    val hint: Long,
    val borders: Boolean,
    val radius: Int,
    val border: Float,
    val borderColor: Long,
    val lip: Float,
)

object PocketThemes {
    val all: List<PocketTheme> = listOf(
        PocketTheme("paper", R.string.theme_name_paper, false, 0xFFF04A0C, 0xFFF7F4ED, 0xFFFFFFFF, 0xFFE3DFD5, 0xFFFFFFFF, 0xFF12141B, 0xFF3B404C, true, 9, 0f, 0, 1.5f),
        PocketTheme("ink", R.string.theme_name_ink, true, 0xFFF04A0C, 0xFF12141B, 0xFF262A36, 0xFF1C1F29, 0xFF262A36, 0xFFF7F4ED, 0xFF9AA0AE, true, 10, 0f, 0, 0f),
        PocketTheme("signal", R.string.theme_name_signal, false, 0xFF12141B, 0xFFF04A0C, 0xFFF7F4ED, 0xFFFFCEBA, 0xFFF7F4ED, 0xFF12141B, 0xFF3B404C, true, 14, 0f, 0, 2.5f),
        PocketTheme("black", R.string.theme_name_black, true, 0xFF4C8DFF, 0xFF000000, 0xFF161618, 0xFF222226, 0xFF161618, 0xFFFFFFFF, 0xFF9AA0A6, false, 6, 0f, 0, 0f),
        PocketTheme("ocean", R.string.theme_name_ocean, true, 0xFFFF7C00, 0xFF596D9B, 0xFF849DD4, 0xFF5174C2, 0xFF849DD4, 0xFFFFFFFF, 0xFFE3EAFB, true, 12, 0f, 0, 1.5f),
        PocketTheme("forest", R.string.theme_name_forest, true, 0xFFB7E37A, 0xFF16261B, 0xFF24402C, 0xFF1C3322, 0xFF24402C, 0xFFE6F4DD, 0xFF8DB397, true, 6, 1f, 0xFF3F6B4C, 0f),
        PocketTheme("violette", R.string.theme_name_violette, true, 0xFFFF60FF, 0xFF7070AE, 0xFF9696D8, 0xFF7B7BCE, 0xFF9696D8, 0xFFFFFFFF, 0xFFE6E6FA, true, 18, 0f, 0, 0f),
        PocketTheme("pink", R.string.theme_name_pink, false, 0xFFEC407A, 0xFFFCE4EC, 0xFFFFFFFF, 0xFFF8BBD0, 0xFFFFFFFF, 0xFF000000, 0xFF6B4452, true, 22, 0f, 0, 0f),
        PocketTheme("sand", R.string.theme_name_sand, false, 0xFF6E9BFF, 0xFFF2E8DA, 0xFFFFFFFF, 0xFFEAD3B9, 0xFFFFFFFF, 0xFF000000, 0xFF5E5240, true, 8, 1f, 0xFFC9AE8A, 0f),
        PocketTheme("midnight", R.string.pp_theme_midnight, true, 0xFF4FC3F7, 0xFF0B1026, 0xFF1A2145, 0xFF121836, 0xFF1A2145, 0xFFE8EEFF, 0xFF8C99CC, true, 12, 1f, 0xFF2E3A73, 0f),
        PocketTheme("sunset", R.string.pp_theme_sunset, true, 0xFFFF8A5B, 0xFF3D1E3A, 0xFF5C2A4F, 0xFF4A2142, 0xFF5C2A4F, 0xFFFFE9D6, 0xFFE0A9A0, true, 18, 0f, 0, 2.5f),
        PocketTheme("mint", R.string.pp_theme_mint, false, 0xFF1FB28A, 0xFFDDF5EA, 0xFFFFFFFF, 0xFFBFE8D6, 0xFFFFFFFF, 0xFF0E3B2E, 0xFF3F7A66, true, 10, 0f, 0, 1.5f),
        PocketTheme("lavender", R.string.pp_theme_lavender, false, 0xFF7B5CE0, 0xFFEDE7FA, 0xFFFFFFFF, 0xFFD6CBF2, 0xFFFFFFFF, 0xFF2A1F4D, 0xFF6B5E99, true, 18, 0f, 0, 0f),
        PocketTheme("candy", R.string.pp_theme_candy, false, 0xFFFF4FA3, 0xFFFFF3B0, 0xFFFFFFFF, 0xFFFFC2E2, 0xFFFFFFFF, 0xFF3A1230, 0xFF8A3E78, true, 22, 2f, 0xFFFF4FA3, 0f),
        PocketTheme("neon", R.string.pp_theme_neon, true, 0xFF00F5A0, 0xFF0A0A0F, 0xFF14141F, 0xFF0F0F18, 0xFF14141F, 0xFFE6FFF5, 0xFF59C9A0, true, 6, 1.5f, 0xFF00F5A0, 0f),
        PocketTheme("mono", R.string.pp_theme_mono, false, 0xFF111111, 0xFFF2F2F2, 0xFFFFFFFF, 0xFFDADADA, 0xFFFFFFFF, 0xFF111111, 0xFF555555, true, 0, 1f, 0xFF111111, 0f),
        PocketTheme("coffee", R.string.pp_theme_coffee, true, 0xFFD9A066, 0xFF2B1D14, 0xFF4A3426, 0xFF3A281C, 0xFF4A3426, 0xFFF5E6D3, 0xFFC4A98B, true, 10, 0f, 0, 3f),
        PocketTheme("rose", R.string.pp_theme_rose, false, 0xFFC2566B, 0xFFF7D9D4, 0xFFFFF5F3, 0xFFEBB9B0, 0xFFFFF5F3, 0xFF3D1F26, 0xFF8A5560, true, 14, 0f, 0, 1.5f),
        PocketTheme("slate", R.string.pp_theme_slate, true, 0xFF80CBC4, 0xFF263238, 0xFF37474F, 0xFF2E3C43, 0xFF37474F, 0xFFECEFF1, 0xFF90A4AE, true, 3, 0f, 0, 0f),
        PocketTheme("aurora", R.string.pp_theme_aurora, true, 0xFF7CFFCB, 0xFF0F2027, 0xFF1D3B4A, 0xFF16303C, 0xFF1D3B4A, 0xFFE8FFF8, 0xFF8CC9B8, true, 16, 1f, 0xFF3B8C86, 0f),
        PocketTheme("gold", R.string.pp_theme_gold, true, 0xFFFFC53D, 0xFF14110A, 0xFF241E10, 0xFF1B160B, 0xFF241E10, 0xFFFFE9B0, 0xFFB89B4E, true, 8, 1f, 0xFFFFC53D, 0f),
    )

    val ids: Set<String> = all.map { it.id }.toSet()

    fun get(id: String?): PocketTheme? = all.firstOrNull { it.id == id }

    fun colors(theme: PocketTheme, themeStyle: String, hasBorders: Boolean, background: Drawable?) = DefaultColors(
        themeStyle,
        hasBorders,
        theme.accent.toInt(),
        theme.background.toInt(),
        theme.key.toInt(),
        theme.functional.toInt(),
        theme.space.toInt(),
        theme.text.toInt(),
        theme.hint.toInt(),
        keyboardBackground = background
    )

    fun apply(prefs: SharedPreferences, theme: PocketTheme) {
        prefs.edit {
            putString(Settings.PREF_THEME_STYLE, KeyboardTheme.STYLE_MATERIAL)
            putBoolean(Settings.PREF_THEME_KEY_BORDERS, theme.borders)
            putBoolean(KeyShape.PREF_CUSTOM, true)
            putInt(KeyShape.PREF_RADIUS, theme.radius)
            putFloat(KeyShape.PREF_BORDER, theme.border)
            putInt(KeyShape.PREF_BORDER_COLOR, theme.borderColor.toInt())
            putFloat(KeyShape.PREF_LIP, theme.lip)
        }
    }
}
