package helium314.keyboard.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.utils.PocketFonts
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.KeyShape
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.PocketThemes
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.LivePreview
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.ThemeSwatch
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.previewColorsFor
import helium314.keyboard.settings.ui.previewShapeFor

private val outlineColors = listOf(
    0,
    0xFF12141B.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFF04A0C.toInt(),
    0xFF4C8DFF.toInt(),
    0xFF1FB28A.toInt(),
    0xFFFFC53D.toInt(),
    0xFFFF4FA3.toInt(),
    0xFF7B5CE0.toInt(),
)

private fun reload() = KeyboardSwitcher.getInstance().setThemeNeedsReload()

@Composable
fun LookScreen(onNavigate: (String) -> Unit, onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val heightKeys = (0 until 4).map { createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, it, 2) }
    val gapKeys = (0 until 4).map { createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, it, 2) }
    val bottomKeys = (0 until 4).map { createPrefKeyForBooleanSettings(Settings.PREF_BOTTOM_PADDING_SCALE_PREFIX, it, 2) }

    var theme by remember { mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) ?: "paper") }
    var filled by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)) }
    var autoDark by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT)) }
    var numberRow by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) }
    var hints by remember { mutableStateOf(prefs.getBoolean(Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS)) }
    var height by remember { mutableFloatStateOf(prefs.getFloat(heightKeys[0], Defaults.PREF_KEYBOARD_HEIGHT_SCALE[0])) }
    var font by remember { mutableFloatStateOf(prefs.getFloat(Settings.PREF_FONT_SCALE, Defaults.PREF_FONT_SCALE)) }
    var gap by remember { mutableFloatStateOf(prefs.getFloat(gapKeys[0], Defaults.PREF_KEY_GAP_SCALE[0])) }
    var bottom by remember { mutableFloatStateOf(prefs.getFloat(bottomKeys[0], Defaults.PREF_BOTTOM_PADDING_SCALE[0])) }
    val shape = KeyShape.readOrDefault(prefs)
    var radius by remember { mutableFloatStateOf(shape.radius) }
    var outline by remember { mutableFloatStateOf(shape.border) }
    var outlineColor by remember { mutableIntStateOf(shape.borderColor) }
    var depth by remember { mutableFloatStateOf(shape.lip) }
    var more by remember { mutableStateOf(false) }
    var fontId by remember { mutableStateOf(prefs.getString(PocketFonts.PREF, "").orEmpty()) }

    fun applyTheme(name: String, auto: Boolean) {
        val night = if (auto && PocketThemes.get(name)?.dark != true) "ink" else name
        prefs.edit {
            putString(Settings.PREF_THEME_COLORS, name)
            putString(Settings.PREF_THEME_COLORS_NIGHT, night)
        }
        reload()
    }

    fun writeShape() {
        prefs.edit {
            putBoolean(KeyShape.PREF_CUSTOM, true)
            putInt(KeyShape.PREF_RADIUS, radius.toInt())
            putFloat(KeyShape.PREF_BORDER, outline)
            putInt(KeyShape.PREF_BORDER_COLOR, outlineColor)
            putFloat(KeyShape.PREF_LIP, depth)
        }
    }

    fun writeAll(keys: List<String>, value: Float) = prefs.edit { keys.forEach { putFloat(it, value) } }

    PocketScreen(stringResource(R.string.pp_look), onClickBack, footer = { LivePreview() }) {
        SectionLabel(stringResource(R.string.lk_themes))
        TileRows(columns = 3, count = PocketThemes.all.size, spacing = 12.dp) { i ->
            val t = PocketThemes.all[i]
            ThemeSwatch(
                label = stringResource(t.label),
                colors = previewColorsFor(t.id),
                shape = previewShapeFor(t.id),
                selected = theme == t.id,
                onClick = {
                    theme = t.id
                    PocketThemes.apply(prefs, t)
                    filled = t.borders
                    radius = t.radius.toFloat()
                    outline = t.border
                    outlineColor = t.borderColor.toInt()
                    depth = t.lip
                    applyTheme(t.id, autoDark)
                }
            )
        }

        SectionLabel(stringResource(R.string.lk_size))
        Card {
            LabeledSlider(
                stringResource(R.string.lk_height), "${(height * 100).toInt()}%", height, 0.7f..1.4f,
                { height = it; writeAll(heightKeys, it) }, { reload() }
            )
            LabeledSlider(
                stringResource(R.string.lk_text), "${(font * 100).toInt()}%", font, 0.7f..1.5f,
                { font = it; prefs.edit { putFloat(Settings.PREF_FONT_SCALE, it) } }, { reload() }
            )
            LabeledSlider(
                stringResource(R.string.lk_spacing), "${(gap * 100).toInt()}%", gap, 0.5f..2.5f,
                { gap = it; writeAll(gapKeys, it) }, { reload() }
            )
        }

        SectionLabel(stringResource(R.string.lk_keys))
        Card {
            SwitchLine(stringResource(R.string.lk_numbers), stringResource(R.string.lk_numbers_sub), numberRow) {
                numberRow = it
                prefs.edit { putBoolean(Settings.PREF_SHOW_NUMBER_ROW, it) }
                reload()
            }
            SwitchLine(stringResource(R.string.lk_fill), stringResource(R.string.lk_fill_sub), filled) {
                filled = it
                prefs.edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, it) }
                reload()
            }
            LabeledSlider(
                stringResource(R.string.lk_round), "${radius.toInt()}", radius, 0f..24f,
                { radius = it; writeShape() }, { reload() }
            )
            LabeledSlider(
                stringResource(R.string.lk_outline),
                if (outline <= 0f) stringResource(R.string.lk_none) else "${"%.1f".format(outline)}",
                outline, 0f..4f,
                { outline = it; writeShape() }, { reload() }
            )
            Text(stringResource(R.string.lk_outline_color), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                outlineColors.forEach { c ->
                    ColorDot(c, outlineColor == c) {
                        outlineColor = c
                        writeShape()
                        reload()
                    }
                }
            }
            LabeledSlider(
                stringResource(R.string.lk_depth), "${"%.1f".format(depth)}", depth, 0f..4f,
                { depth = it; writeShape() }, { reload() }
            )
        }

        SectionLabel(stringResource(R.string.fn_font))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FontCard(stringResource(R.string.fn_default), FontFamily.Default, fontId.isEmpty()) {
                fontId = ""
                prefs.edit { putString(PocketFonts.PREF, "") }
                KeyboardTypeface.clearCache()
                reload()
            }
            PocketFonts.all.forEach { entry ->
                FontCard(entry.label, FontFamily(Font(entry.res)), fontId == entry.id) {
                    fontId = entry.id
                    prefs.edit { putString(PocketFonts.PREF, entry.id) }
                    KeyboardTypeface.clearCache()
                    reload()
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { more = !more }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.lk_more),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painterResource(R.drawable.ic_pp_next),
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).rotate(if (more) 90f else 0f)
            )
        }
        if (more) {
            Card {
                SwitchLine(stringResource(R.string.lk_auto_dark), stringResource(R.string.lk_auto_dark_sub), autoDark) {
                    autoDark = it
                    prefs.edit { putBoolean(Settings.PREF_THEME_DAY_NIGHT, it) }
                    applyTheme(theme, it)
                }
                SwitchLine(stringResource(R.string.lk_hints), stringResource(R.string.lk_hints_sub), hints) {
                    hints = it
                    prefs.edit { putBoolean(Settings.PREF_SHOW_HINTS, it) }
                    reload()
                }
                LabeledSlider(
                    stringResource(R.string.lk_bottom), "${(bottom * 100).toInt()}%", bottom, 0f..3f,
                    { bottom = it; writeAll(bottomKeys, it) }, { reload() }
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = PP.Signal,
                activeTrackColor = PP.Signal,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun SwitchLine(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PP.White,
                checkedTrackColor = PP.Signal,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun ColorDot(color: Int, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (color == 0) MaterialTheme.colorScheme.surfaceVariant else Color(color))
            .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == 0) Text(
            stringResource(R.string.lk_auto).take(1),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FontCard(name: String, family: FontFamily, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) PP.Signal else MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .size(112.dp, 88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(if (selected) 3.dp else 1.dp, ring, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Abc 123",
            fontFamily = family,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
