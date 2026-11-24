// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.runtime.mutableStateOf as scMutableStateOf
import helium314.keyboard.settings.ui.PocketScreen as ScPocketScreen
import helium314.keyboard.settings.ui.LivePreview
import helium314.keyboard.settings.ui.PrefSwitch
import helium314.keyboard.settings.ui.PrefChoice
import helium314.keyboard.settings.ui.PrefIntChoice
import helium314.keyboard.settings.ui.ScSection
import helium314.keyboard.settings.ui.ScCollapsible
import helium314.keyboard.settings.ui.EmbeddedSetting
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SettingsRow
import helium314.keyboard.settings.ui.ValueRow
import helium314.keyboard.settings.ui.SectionLabel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.Context
import android.media.AudioManager
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.ReorderSwitchPreference
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.SwitchPreferenceWithEmojiDictWarning
import helium314.keyboard.latin.utils.previewDark

@Composable
fun PreferencesScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var clipboardOn by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_CLIPBOARD_HISTORY, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY))
    }
    ScPocketScreen(stringResource(R.string.pp_options), onClickBack, footer = { LivePreview() }) {
        ScSection(stringResource(R.string.sc_keys)) {
            PrefSwitch(
                Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW,
                stringResource(R.string.sc_number_row), stringResource(R.string.sc_number_row_sub), R.drawable.ic_pp_keyboard
            ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
            RowDivider()
            PrefSwitch(
                Settings.PREF_SHOW_EMOJI_KEY, Defaults.PREF_SHOW_EMOJI_KEY,
                stringResource(R.string.sc_emoji_key), stringResource(R.string.sc_emoji_key_sub), R.drawable.ic_pp_emoji
            ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
            RowDivider()
            PrefSwitch(
                Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY, Defaults.PREF_SHOW_LANGUAGE_SWITCH_KEY,
                stringResource(R.string.sc_lang_key), stringResource(R.string.sc_lang_key_sub), R.drawable.ic_settings_languages
            ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
            RowDivider()
            PrefSwitch(
                Settings.PREF_SHOW_DPAD_KEY, Defaults.PREF_SHOW_DPAD_KEY,
                stringResource(R.string.sc_arrow_keys), stringResource(R.string.sc_arrow_keys_sub), R.drawable.ic_pp_next
            ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
            RowDivider()
            PrefSwitch(
                Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS,
                stringResource(R.string.sc_hints), stringResource(R.string.sc_hints_sub), R.drawable.ic_pp_dot
            ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
        }
        ScSection(stringResource(R.string.sc_clipboard)) {
            PrefSwitch(
                Settings.PREF_ENABLE_CLIPBOARD_HISTORY, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY,
                stringResource(R.string.sc_clip_on), stringResource(R.string.sc_clip_on_sub), R.drawable.ic_pp_paste
            ) {
                clipboardOn = it
                if (!it) ClipboardDao.getInstance(ctx)?.clearNonPinned()
            }
            if (clipboardOn) {
                RowDivider()
                PrefIntChoice(
                    Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME,
                    listOf(
                        stringResource(R.string.clipboard_retention_1_hour) to 60,
                        stringResource(R.string.clipboard_retention_1_day) to 1440,
                        stringResource(R.string.clipboard_retention_7_days) to 10080,
                        stringResource(R.string.clipboard_retention_30_days) to 43200,
                        stringResource(R.string.clipboard_retention_90_days) to 129600,
                        stringResource(R.string.clipboard_retention_never) to 0,
                    ),
                    stringResource(R.string.sc_keep_for), stringResource(R.string.sc_keep_for_sub), R.drawable.ic_pp_clock
                ) { ClipboardDao.getInstance(ctx)?.clearOldClips(true) }
                RowDivider()
                PrefSwitch(
                    Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST, Defaults.PREF_CLIPBOARD_HISTORY_PINNED_FIRST,
                    stringResource(R.string.sc_pinned_first), stringResource(R.string.sc_pinned_first_sub), R.drawable.ic_pp_pin
                )
            }
        }
        ScSection(stringResource(R.string.sc_apps)) {
            PrefSwitch(
                Settings.PREF_SAVE_SUBTYPE_PER_APP, Defaults.PREF_SAVE_SUBTYPE_PER_APP,
                stringResource(R.string.sc_per_app), stringResource(R.string.sc_per_app_sub), R.drawable.ic_pp_apps
            )
        }
        ScCollapsible(stringResource(R.string.sc_more), stringResource(R.string.sc_more_sub), R.drawable.ic_pp_all) {
            EmbeddedSetting(Settings.PREF_LOCALIZED_NUMBER_ROW)
            EmbeddedSetting(Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS)
            EmbeddedSetting(Settings.PREF_SHOW_NUMBER_ROW_HINTS)
            EmbeddedSetting(Settings.PREF_LANGUAGE_SWITCH_KEY)
            EmbeddedSetting(Settings.PREF_SHOW_POPUP_HINTS)
            EmbeddedSetting(Settings.PREF_POPUP_KEYS_HINT_ORDER)
            EmbeddedSetting(Settings.PREF_POPUP_KEYS_ORDER)
            EmbeddedSetting(Settings.PREF_SHOW_TLD_POPUP_KEYS)
            EmbeddedSetting(Settings.PREF_REMOVE_REDUNDANT_POPUPS)
            EmbeddedSetting(Settings.PREF_SHOW_EMOJI_DESCRIPTIONS)
            EmbeddedSetting(Settings.PREF_CLIPBOARD_USE_FILES)
            EmbeddedSetting(Settings.PREF_CLIPBOARD_FILES_SIZE_LIMIT)
        }
    }
}

fun createPreferencesSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_SAVE_SUBTYPE_PER_APP, R.string.save_subtype_per_app) {
        SwitchPreference(it, Defaults.PREF_SAVE_SUBTYPE_PER_APP)
    },
    Setting(context, Settings.PREF_SHOW_HINTS, R.string.show_hints, R.string.show_hints_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_HINTS) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_POPUP_KEYS_HINT_ORDER, R.string.hint_source) {
        ReorderSwitchPreference(it, Defaults.PREF_POPUP_KEYS_HINT_ORDER)
    },
    Setting(context, Settings.PREF_POPUP_KEYS_ORDER, R.string.popup_order) {
        ReorderSwitchPreference(it, Defaults.PREF_POPUP_KEYS_ORDER)
    },
    Setting(
        context, Settings.PREF_SHOW_TLD_POPUP_KEYS, R.string.show_tld_popup_keys,
        R.string.show_tld_popup_keys_summary
    ) {
        SwitchPreference(it, Defaults.PREF_SHOW_TLD_POPUP_KEYS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_POPUP_HINTS, R.string.show_popup_hints, R.string.show_popup_hints_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_POPUP_HINTS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_POPUP_ON, R.string.popup_on_keypress) {
        SwitchPreference(it, Defaults.PREF_POPUP_ON) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_VIBRATE_ON, R.string.vibrate_on_keypress) {
        SwitchPreference(it, Defaults.PREF_VIBRATE_ON)
    },
    Setting(context, Settings.PREF_VIBRATE_IN_DND_MODE, R.string.vibrate_in_dnd_mode) {
        SwitchPreference(it, Defaults.PREF_VIBRATE_IN_DND_MODE)
    },
    Setting(context, Settings.PREF_SOUND_ON, R.string.sound_on_keypress) {
        SwitchPreference(it, Defaults.PREF_SOUND_ON)
    },
    Setting(context, Settings.PREF_SHOW_EMOJI_DESCRIPTIONS, R.string.show_emoji_descriptions) {
        SwitchPreferenceWithEmojiDictWarning(it, Defaults.PREF_SHOW_EMOJI_DESCRIPTIONS)
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW, R.string.number_row, R.string.number_row_summary) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS, R.string.number_row_in_symbols) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_LOCALIZED_NUMBER_ROW, R.string.localized_number_row, R.string.localized_number_row_summary) {
        SwitchPreference(it, Defaults.PREF_LOCALIZED_NUMBER_ROW) {
            KeyboardLayoutSet.onSystemLocaleChanged()
            KeyboardSwitcher.getInstance().reloadKeyboard()
        }
    },
    Setting(context, Settings.PREF_SHOW_NUMBER_ROW_HINTS, R.string.number_row_hints) {
        SwitchPreference(it, Defaults.PREF_SHOW_NUMBER_ROW_HINTS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY, R.string.show_language_switch_key) {
        SwitchPreference(it, Defaults.PREF_SHOW_LANGUAGE_SWITCH_KEY) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_LANGUAGE_SWITCH_KEY, R.string.language_switch_key_behavior) {
        ListPreference(
            it,
            listOf(
                stringResource(R.string.switch_language) to "internal",
                stringResource(R.string.language_switch_key_switch_input_method) to "input_method",
                stringResource(R.string.language_switch_key_switch_both) to "both"
            ),
            Defaults.PREF_LANGUAGE_SWITCH_KEY
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SHOW_EMOJI_KEY, R.string.show_emoji_key) {
        SwitchPreference(it, Defaults.PREF_SHOW_EMOJI_KEY) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_SHOW_DPAD_KEY, R.string.show_dpad_key) {
        SwitchPreference(it, Defaults.PREF_SHOW_DPAD_KEY) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_REMOVE_REDUNDANT_POPUPS,
        R.string.remove_redundant_popups, R.string.remove_redundant_popups_summary)
    {
        SwitchPreference(it, Defaults.PREF_REMOVE_REDUNDANT_POPUPS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        R.string.enable_clipboard_history, R.string.enable_clipboard_history_summary)
    {
        val ctx = LocalContext.current
        SwitchPreference(it, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY) { ClipboardDao.getInstance(ctx)?.clearNonPinned() }
    },
    Setting(context, Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, R.string.clipboard_history_retention_time) { setting ->
        val ctx = LocalContext.current
        ListPreference(
            setting = setting,
            items = listOf(
                stringResource(R.string.clipboard_retention_1_hour) to 60,
                stringResource(R.string.clipboard_retention_1_day) to 1440,
                stringResource(R.string.clipboard_retention_7_days) to 10080,
                stringResource(R.string.clipboard_retention_30_days) to 43200,
                stringResource(R.string.clipboard_retention_90_days) to 129600,
                stringResource(R.string.clipboard_retention_never) to 0,
            ),
            default = Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME,
            description = stringResource(R.string.clipboard_retention_pinned_note),
        ) { ClipboardDao.getInstance(ctx)?.clearOldClips(true) }
    },
    Setting(context, Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST, R.string.clipboard_history_pinned_first) {
        SwitchPreference(it, Defaults.PREF_CLIPBOARD_HISTORY_PINNED_FIRST)
    },
    Setting(context, Settings.PREF_CLIPBOARD_USE_FILES, R.string.clipboard_history_files) {
        val ctx = LocalContext.current
        SwitchPreference(it, Defaults.PREF_CLIPBOARD_USE_FILES) {
            ClipboardDao.getInstance(ctx)?.cleanupFiles(ctx.prefs())
        }
    },
    Setting(context, Settings.PREF_CLIPBOARD_FILES_SIZE_LIMIT, R.string.clipboard_history_max_file_size) { setting ->
        val ctx = LocalContext.current
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_CLIPBOARD_FILES_SIZE_LIMIT,
            description = {
                if (it > 1000) stringResource(R.string.settings_no_limit)
                else stringResource(R.string.abbreviation_unit_mb, it.toString())
            },
            range = 1f..1001f,
        ) { ClipboardDao.getInstance(ctx)?.cleanupFiles(ctx.prefs()) }
    },
    Setting(context, Settings.PREF_VIBRATION_DURATION_SETTINGS, R.string.prefs_keypress_vibration_duration_settings) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_VIBRATION_DURATION_SETTINGS,
            description = {
                if (it < 0) stringResource(R.string.settings_system_default)
                else stringResource(R.string.abbreviation_unit_milliseconds, it.toString())
            },
            range = -1f..100f,
            onValueChanged = { it?.let { AudioAndHapticFeedbackManager.getInstance().vibrate(it.toLong()) } }
        )
    },
    Setting(context, Settings.PREF_KEYPRESS_SOUND_VOLUME, R.string.prefs_keypress_sound_volume_settings) { setting ->
        val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_KEYPRESS_SOUND_VOLUME,
            description = {
                if (it < 0) stringResource(R.string.settings_system_default)
                else (it * 100).toInt().toString()
            },
            range = -0.01f..1f,
            onValueChanged = { it?.let { audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, it) } }
        )
    },
)

private val localesWithLocalizedNumberRow = listOf("ar", "bn", "ckb", "fa", "gu", "hi", "kn", "mr", "ne", "th", "ur")

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            PreferencesScreen { }
        }
    }
}
