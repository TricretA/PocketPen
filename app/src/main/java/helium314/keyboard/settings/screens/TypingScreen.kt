package helium314.keyboard.settings.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.KeySounds
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.LivePreview
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SettingsRow
import helium314.keyboard.settings.ui.SwitchRow
import helium314.keyboard.settings.ui.ValueRow

@Composable
private fun PrefSwitch(key: String, default: Boolean, title: Int, subtitle: Int, icon: Int, onChanged: () -> Unit = {}) {
    val reload = { helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    val prefs = LocalContext.current.prefs()
    var checked by remember(key) { mutableStateOf(prefs.getBoolean(key, default)) }
    SwitchRow(stringResource(title), stringResource(subtitle), icon, checked) {
        checked = it
        prefs.edit { putBoolean(key, it) }
        onChanged()
        reload()
    }
}

private fun soundLabel(ctx: android.content.Context, choice: String): String = ctx.getString(
    when (choice) {
        "soft" -> R.string.sb_sound_soft
        "click" -> R.string.sb_sound_click
        "pop" -> R.string.sb_sound_pop
        "typewriter" -> R.string.sb_sound_typewriter
        "bubble" -> R.string.sb_sound_bubble
        "wood" -> R.string.ks_wood
        "glass" -> R.string.ks_glass
        "laser" -> R.string.ks_laser
        "drop" -> R.string.ks_drop
        "marble" -> R.string.ks_marble
        "blue" -> R.string.ks_blue
        "brown" -> R.string.ks_brown
        "bell" -> R.string.ks_bell
        "kalimba" -> R.string.ks_kalimba
        "water" -> R.string.ks_water
        "heartbeat" -> R.string.ks_heartbeat
        "tap" -> R.string.ks_tap
        "snap" -> R.string.ks_snap
        "chirp" -> R.string.ks_chirp
        "thock" -> R.string.ks_thock
        "piano" -> R.string.ks_piano
        KeySounds.CUSTOM -> R.string.sb_sound_custom
        else -> R.string.sb_sound_system
    }
)

@Composable
fun TypingScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var version by remember { mutableIntStateOf(0) }
    var showStrength by remember { mutableStateOf(false) }
    var showSounds by remember { mutableStateOf(false) }
    val vibrateOn = remember(version) { prefs.getBoolean(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON) }
    val soundOn = remember(version) { prefs.getBoolean(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON) }
    val strength = remember(version) { prefs.getInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, Defaults.PREF_VIBRATION_DURATION_SETTINGS) }
    val choice = remember(version) { KeySounds.choice(ctx) }
    val savedVolume = remember(version) { prefs.getFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, Defaults.PREF_KEYPRESS_SOUND_VOLUME) }
    var volume by remember(savedVolume) { mutableStateOf(if (savedVolume < 0f) 0.5f else savedVolume) }

    val strengthOptions = listOf(
        stringResource(R.string.sb_strength_default) to "-1",
        stringResource(R.string.sb_strength_light) to "10",
        stringResource(R.string.sb_strength_medium) to "20",
        stringResource(R.string.sb_strength_strong) to "40",
    )
    val strengthLabel = strengthOptions.firstOrNull { it.second == strength.toString() }?.first ?: strengthOptions[0].first

    PocketScreen(stringResource(R.string.pp_typing), onClickBack, footer = { LivePreview() }) {
        SectionLabel(stringResource(R.string.sb_sec_suggestions))
        SettingsCard {
            PrefSwitch(Settings.PREF_SHOW_SUGGESTIONS, Defaults.PREF_SHOW_SUGGESTIONS, R.string.sb_suggest_t, R.string.sb_suggest_d, R.drawable.ic_pp_strip)
            RowDivider()
            PrefSwitch(Settings.PREF_AUTO_CORRECTION, Defaults.PREF_AUTO_CORRECTION, R.string.sb_autocorrect_t, R.string.sb_autocorrect_d, R.drawable.ic_pp_spellcheck)
            RowDivider()
            PrefSwitch(Settings.PREF_BIGRAM_PREDICTIONS, Defaults.PREF_BIGRAM_PREDICTIONS, R.string.sb_nextword_t, R.string.sb_nextword_d, R.drawable.ic_pp_next)
            RowDivider()
            PrefSwitch(Settings.PREF_SUGGEST_EMOJIS, Defaults.PREF_SUGGEST_EMOJIS, R.string.sb_emoji_t, R.string.sb_emoji_d, R.drawable.ic_pp_emoji)
        }

        SectionLabel(stringResource(R.string.sb_sec_writing))
        SettingsCard {
            PrefSwitch(Settings.PREF_AUTO_CAP, Defaults.PREF_AUTO_CAP, R.string.sb_autocaps_t, R.string.sb_autocaps_d, R.drawable.ic_pp_abc)
            RowDivider()
            PrefSwitch(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, Defaults.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, R.string.sb_dspace_t, R.string.sb_dspace_d, R.drawable.ic_pp_dot)
            RowDivider()
            PrefSwitch(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW, R.string.sb_numrow_t, R.string.sb_numrow_d, R.drawable.ic_pp_keyboard)
        }

        SectionLabel(stringResource(R.string.sb_sec_feel))
        SettingsCard {
            PrefSwitch(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON, R.string.sb_vibrate_t, R.string.sb_vibrate_d, R.drawable.ic_pp_vibrate) { version++ }
            if (vibrateOn) {
                RowDivider()
                ValueRow(
                    stringResource(R.string.sb_vibrate_strength_t),
                    stringResource(R.string.sb_vibrate_strength_d),
                    strengthLabel,
                    null
                ) { showStrength = true }
            }
            RowDivider()
            PrefSwitch(Settings.PREF_POPUP_ON, Defaults.PREF_POPUP_ON, R.string.sb_popup_t, R.string.sb_popup_d, R.drawable.ic_pp_popup)
            RowDivider()
            PrefSwitch(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON, R.string.sb_sound_t, R.string.sb_sound_d, R.drawable.ic_pp_sound) { version++ }
            if (soundOn) {
                RowDivider()
                ValueRow(
                    stringResource(R.string.sb_sound_choose_t),
                    stringResource(R.string.sb_sound_choose_d),
                    soundLabel(ctx, choice),
                    null
                ) { showSounds = true }
                RowDivider()
                SettingsRow(stringResource(R.string.sb_sound_volume_t), null, null, null) {}
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = {
                        prefs.edit { putFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, volume) }
                        KeySounds.preview(ctx, if (choice == KeySounds.SYSTEM) "click" else choice, volume)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PP.Signal,
                        activeTrackColor = PP.Signal,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    if (showStrength) ChoiceDialog(
        title = stringResource(R.string.sb_vibrate_strength_t),
        options = strengthOptions,
        selected = strength.toString(),
        onSelect = {
            val ms = it.toInt()
            prefs.edit { putInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, ms) }
            if (ms > 0) AudioAndHapticFeedbackManager.getInstance().vibrate(ms.toLong())
            version++
            showStrength = false
        },
        onDismiss = { showStrength = false }
    )

    if (showSounds) SoundDialog(
        current = choice,
        volume = volume,
        onChosen = {
            KeySounds.setChoice(ctx, it)
            version++
        },
        onDismiss = { showSounds = false }
    )
}

@Composable
private fun SoundDialog(current: String, volume: Float, onChosen: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(current) }
    var hasCustom by remember { mutableStateOf(KeySounds.hasCustom(ctx)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            if (KeySounds.importCustom(ctx, uri)) {
                hasCustom = true
                selected = KeySounds.CUSTOM
                onChosen(KeySounds.CUSTOM)
                KeySounds.preview(ctx, KeySounds.CUSTOM, volume)
            } else {
                Toast.makeText(ctx, R.string.sb_sound_pick_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val options = buildList {
        add(KeySounds.SYSTEM)
        addAll(KeySounds.builtIn.map { it.first })
        if (hasCustom) add(KeySounds.CUSTOM)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sb_sound_choose_t)) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = option
                                onChosen(option)
                                KeySounds.preview(ctx, option, volume)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PP.Signal)
                        )
                        Text(soundLabel(ctx, option), Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
                TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Text(stringResource(R.string.sb_sound_pick), color = PP.Signal)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sb_done)) } }
    )
}
