package helium314.keyboard.settings.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.KeyboardPreview
import helium314.keyboard.settings.ui.NavTile
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.ThemeSwatch
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.ToggleTile
import helium314.keyboard.settings.ui.VisualSlider
import helium314.keyboard.settings.ui.previewColorsFor

private class Swatch(val id: String, val label: Int)

private val darkThemes = setOf("ink", "black", "dark", "darker")

private val swatches = listOf(
    Swatch("paper", R.string.theme_name_paper),
    Swatch("ink", R.string.theme_name_ink),
    Swatch("signal", R.string.theme_name_signal),
    Swatch("black", R.string.theme_name_black),
    Swatch("ocean", R.string.theme_name_ocean),
    Swatch("forest", R.string.theme_name_forest),
    Swatch("violette", R.string.theme_name_violette),
    Swatch("pink", R.string.theme_name_pink),
    Swatch("sand", R.string.theme_name_sand),
)

@Composable
fun LookScreen(onNavigate: (String) -> Unit, onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var theme by remember { mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) ?: "paper") }
    var borders by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)) }
    var autoDark by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT)) }
    var numberRow by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) }
    var height by remember {
        mutableFloatStateOf(prefs.getFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, 0, 2), Defaults.PREF_KEYBOARD_HEIGHT_SCALE[0]))
    }
    var font by remember { mutableFloatStateOf(prefs.getFloat(Settings.PREF_FONT_SCALE, Defaults.PREF_FONT_SCALE)) }

    fun applyTheme(name: String, auto: Boolean) {
        val night = if (auto && name !in darkThemes) "ink" else name
        prefs.edit {
            putString(Settings.PREF_THEME_COLORS, name)
            putString(Settings.PREF_THEME_COLORS_NIGHT, night)
        }
        KeyboardSwitcher.getInstance().setThemeNeedsReload()
    }

    PocketScreen(stringResource(R.string.pp_look), onClickBack) {
        KeyboardPreview(
            colors = previewColorsFor(theme),
            bordered = borders,
            modifier = Modifier
                .fillMaxWidth()
                .height((120 + (height - 1f) * 90f).coerceIn(90f, 190f).dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
        )
        TileRows(columns = 3, count = swatches.size, spacing = 12.dp) { i ->
            val s = swatches[i]
            ThemeSwatch(
                label = stringResource(s.label),
                colors = previewColorsFor(s.id),
                selected = theme == s.id,
                onClick = {
                    theme = s.id
                    applyTheme(s.id, autoDark)
                }
            )
        }
        SectionLabel(stringResource(R.string.pp_height))
        VisualSlider(
            value = height,
            onValueChange = { height = it },
            onDone = {
                prefs.edit {
                    for (variant in 0 until 4)
                        putFloat(createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, variant, 2), height)
                }
            },
            range = 0.7f..1.4f,
            smallIcon = R.drawable.ic_pp_size,
            bigIcon = R.drawable.ic_pp_size
        )
        SectionLabel(stringResource(R.string.pp_text_size))
        VisualSlider(
            value = font,
            onValueChange = { font = it },
            onDone = { prefs.edit { putFloat(Settings.PREF_FONT_SCALE, font) } },
            range = 0.7f..1.5f,
            smallIcon = R.drawable.ic_pp_abc,
            bigIcon = R.drawable.ic_pp_abc
        )
        TileRows(columns = 3, count = 3) { i ->
            when (i) {
                0 -> ToggleTile(stringResource(R.string.pp_borders), R.drawable.ic_pp_border, borders, {
                    borders = it
                    prefs.edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, it) }
                    KeyboardSwitcher.getInstance().setThemeNeedsReload()
                }, Modifier.fillMaxWidth())
                1 -> ToggleTile(stringResource(R.string.pp_auto_dark), R.drawable.ic_pp_dark, autoDark, {
                    autoDark = it
                    prefs.edit { putBoolean(Settings.PREF_THEME_DAY_NIGHT, it) }
                    applyTheme(theme, it)
                }, Modifier.fillMaxWidth())
                else -> ToggleTile(stringResource(R.string.pp_numbers), R.drawable.ic_pp_keyboard, numberRow, {
                    numberRow = it
                    prefs.edit { putBoolean(Settings.PREF_SHOW_NUMBER_ROW, it) }
                }, Modifier.fillMaxWidth())
            }
        }
        NavTile(stringResource(R.string.pp_extras), R.drawable.ic_pp_image, { onNavigate(helium314.keyboard.settings.SettingsDestination.AppearanceMore) }, Modifier.fillMaxWidth())
    }
}
