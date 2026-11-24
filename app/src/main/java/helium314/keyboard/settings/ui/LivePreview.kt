package helium314.keyboard.settings.ui

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.common.KeyShape
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.prefs

data class LiveSpec(
    val colors: PreviewColors,
    val shape: PreviewShape,
    val heightScale: Float,
    val textScale: Float,
    val numberRow: Boolean,
)

@Composable
fun rememberLiveSpec(): LiveSpec {
    val ctx = LocalContext.current
    val prefs = remember { ctx.prefs() }
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tick++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(tick, dark) { readLiveSpec(prefs, dark) }
}

private fun readLiveSpec(prefs: SharedPreferences, dark: Boolean): LiveSpec {
    val night = dark && prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT)
    val themeName = (if (night) prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT)
        else prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)) ?: "paper"
    val borders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
    val custom = KeyShape.read(prefs)
    val shape = if (custom != null) PreviewShape(
        custom.radius,
        custom.border,
        if (custom.borderColor != 0) Color(custom.borderColor) else null,
        custom.lip,
        borders
    ) else PreviewShape(radius = 6f, border = if (borders) 0.8f else 0f, filled = true)
    val height = prefs.getFloat(
        createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, 0, 2),
        Defaults.PREF_KEYBOARD_HEIGHT_SCALE[0]
    )
    return LiveSpec(
        previewColorsFor(themeName),
        shape,
        height,
        prefs.getFloat(Settings.PREF_FONT_SCALE, Defaults.PREF_FONT_SCALE),
        prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)
    )
}

@Composable
fun LiveKeyboard(spec: LiveSpec, modifier: Modifier = Modifier, baseHeight: Int = 120) {
    val dp = (baseHeight + (spec.heightScale - 1f) * baseHeight * 0.75f + if (spec.numberRow) baseHeight * 0.16f else 0f)
        .coerceIn(baseHeight * 0.55f, baseHeight * 1.7f)
    KeyboardPreview(
        colors = spec.colors,
        shape = spec.shape,
        numberRow = spec.numberRow,
        textScale = spec.textScale,
        modifier = modifier
            .fillMaxWidth()
            .height(dp.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
    )
}

@Composable
fun LivePreview(modifier: Modifier = Modifier) {
}
