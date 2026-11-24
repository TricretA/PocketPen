package helium314.keyboard.settings.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsDestination
import helium314.keyboard.settings.ui.KeyboardPreview
import helium314.keyboard.settings.ui.NavTile
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.previewColorsFor

private class Destination(val label: Int, val icon: Int, val route: String)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val themeName = if (dark && prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT))
        prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT)
    else
        prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
    val destinations = listOf(
        Destination(R.string.pp_look, R.drawable.ic_settings_appearance, SettingsDestination.Look),
        Destination(R.string.pp_typing, R.drawable.ic_pp_keyboard, SettingsDestination.Typing),
        Destination(R.string.pp_languages, R.drawable.ic_settings_languages, SettingsDestination.Languages),
        Destination(R.string.pp_clipboard, R.drawable.sym_keyboard_clipboard_lxx, SettingsDestination.ClipboardManager),
        Destination(R.string.pp_voice, R.drawable.sym_keyboard_voice_lxx, SettingsDestination.Voice),
        Destination(R.string.pp_write, R.drawable.ic_handwriting, SettingsDestination.Handwriting),
        Destination(R.string.pp_ai, R.drawable.ic_ai, SettingsDestination.Ai),
        Destination(R.string.pp_more, R.drawable.ic_pp_apps, SettingsDestination.More),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(if (dark) R.drawable.pp_logo_reversed else R.drawable.pp_logo_colour),
                contentDescription = stringResource(R.string.english_ime_name),
                modifier = Modifier.height(36.dp)
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KeyboardPreview(
                colors = previewColorsFor(themeName ?: ""),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
                    .clickable(role = Role.Button) { onNavigate(SettingsDestination.Look) }
            )
            TileRows(columns = 2, count = destinations.size) { i ->
                val d = destinations[i]
                NavTile(stringResource(d.label), d.icon, { onNavigate(d.route) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun MoreScreen(onNavigate: (String) -> Unit, onClickBack: () -> Unit) {
    val items = listOfNotNull(
        Destination(R.string.pp_options, R.drawable.ic_settings_preferences, SettingsDestination.Preferences),
        Destination(R.string.pp_toolbar, R.drawable.ic_settings_toolbar, SettingsDestination.Toolbar),
        Destination(R.string.pp_correction, R.drawable.ic_settings_correction, SettingsDestination.TextCorrection),
        Destination(R.string.pp_layouts, R.drawable.ic_settings_layout, SettingsDestination.Layouts),
        Destination(R.string.pp_dictionaries, R.drawable.ic_dictionary, SettingsDestination.Dictionaries),
        if (JniUtils.sHaveGestureLib) Destination(R.string.pp_glide, R.drawable.ic_settings_gesture, SettingsDestination.GestureTyping) else null,
        Destination(R.string.pp_extras, R.drawable.ic_pp_image, SettingsDestination.AppearanceMore),
        Destination(R.string.pp_advanced, R.drawable.ic_settings_advanced, SettingsDestination.Advanced),
        Destination(R.string.pp_about, R.drawable.ic_settings_about, SettingsDestination.About),
    )
    PocketScreen(stringResource(R.string.pp_more), onClickBack) {
        TileRows(columns = 2, count = items.size) { i ->
            val d = items[i]
            NavTile(stringResource(d.label), d.icon, { onNavigate(d.route) }, Modifier.fillMaxWidth())
        }
    }
}
