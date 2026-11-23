package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.ToggleTile

private class Toggle(val key: String, val default: Boolean, val label: Int, val icon: Int)

@Composable
private fun ToggleGrid(items: List<Toggle>) {
    val prefs = LocalContext.current.prefs()
    TileRows(columns = 3, count = items.size) { i ->
        val item = items[i]
        var checked by remember { mutableStateOf(prefs.getBoolean(item.key, item.default)) }
        ToggleTile(
            label = stringResource(item.label),
            icon = item.icon,
            checked = checked,
            onToggle = {
                checked = it
                prefs.edit { putBoolean(item.key, it) }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TypingScreen(onClickBack: () -> Unit) {
    PocketScreen(stringResource(R.string.pp_typing), onClickBack) {
        SectionLabel(stringResource(R.string.pp_section_suggest))
        ToggleGrid(
            listOf(
                Toggle(Settings.PREF_SHOW_SUGGESTIONS, Defaults.PREF_SHOW_SUGGESTIONS, R.string.pp_suggest, R.drawable.ic_pp_strip),
                Toggle(Settings.PREF_AUTO_CORRECTION, Defaults.PREF_AUTO_CORRECTION, R.string.pp_autocorrect, R.drawable.ic_pp_spellcheck),
                Toggle(Settings.PREF_BIGRAM_PREDICTIONS, Defaults.PREF_BIGRAM_PREDICTIONS, R.string.pp_next_word, R.drawable.ic_pp_next),
                Toggle(Settings.PREF_SUGGEST_EMOJIS, Defaults.PREF_SUGGEST_EMOJIS, R.string.pp_emoji, R.drawable.ic_pp_emoji),
            )
        )
        SectionLabel(stringResource(R.string.pp_section_write))
        ToggleGrid(
            listOf(
                Toggle(Settings.PREF_AUTO_CAP, Defaults.PREF_AUTO_CAP, R.string.pp_auto_caps, R.drawable.ic_pp_abc),
                Toggle(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, Defaults.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, R.string.pp_space_space, R.drawable.ic_pp_dot),
                Toggle(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW, R.string.pp_numbers, R.drawable.ic_pp_keyboard),
            )
        )
        SectionLabel(stringResource(R.string.pp_section_feel))
        ToggleGrid(
            listOf(
                Toggle(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON, R.string.pp_vibrate, R.drawable.ic_pp_vibrate),
                Toggle(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON, R.string.pp_sound, R.drawable.ic_pp_sound),
                Toggle(Settings.PREF_POPUP_ON, Defaults.PREF_POPUP_ON, R.string.pp_popup, R.drawable.ic_pp_popup),
            )
        )
    }
}
