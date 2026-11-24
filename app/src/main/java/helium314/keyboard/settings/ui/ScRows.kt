package helium314.keyboard.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.LayoutType
import helium314.keyboard.latin.utils.LayoutUtils
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

@Composable
fun PrefSwitch(
    key: String,
    default: Boolean,
    title: String,
    subtitle: String?,
    icon: Int?,
    onChanged: (Boolean) -> Unit = {},
) {
    val prefs = LocalContext.current.prefs()
    var checked by remember(key) { mutableStateOf(prefs.getBoolean(key, default)) }
    SwitchRow(title, subtitle, icon, checked) {
        checked = it
        prefs.edit { putBoolean(key, it) }
        onChanged(it)
    }
}

@Composable
fun PrefChoice(
    key: String,
    default: String,
    options: List<Pair<String, String>>,
    title: String,
    subtitle: String?,
    icon: Int?,
    onChanged: (String) -> Unit = {},
) {
    val prefs = LocalContext.current.prefs()
    var value by remember(key) { mutableStateOf(prefs.getString(key, default) ?: default) }
    var open by rememberSaveable(key) { mutableStateOf(false) }
    ValueRow(title, subtitle, options.firstOrNull { it.second == value }?.first ?: value, icon) { open = true }
    if (open) {
        ScChoiceDialog(
            title = title,
            options = options,
            selected = value,
            onSelect = {
                value = it
                prefs.edit { putString(key, it) }
                onChanged(it)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun PrefIntChoice(
    key: String,
    default: Int,
    options: List<Pair<String, Int>>,
    title: String,
    subtitle: String?,
    icon: Int?,
    onChanged: (Int) -> Unit = {},
) {
    val prefs = LocalContext.current.prefs()
    var value by remember(key) { mutableStateOf(prefs.getInt(key, default)) }
    var open by rememberSaveable(key) { mutableStateOf(false) }
    ValueRow(title, subtitle, options.firstOrNull { it.second == value }?.first ?: value.toString(), icon) { open = true }
    if (open) {
        ScChoiceDialog(
            title = title,
            options = options.map { it.first to it.second.toString() },
            selected = value.toString(),
            onSelect = { chosen ->
                val number = chosen.toInt()
                value = number
                prefs.edit { putInt(key, number) }
                onChanged(number)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun ScChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PP.Signal)
                        )
                        Text(label, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } }
    )
}

@Composable
fun ScSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionLabel(title)
    SettingsCard(content)
}

@Composable
fun ScCollapsible(title: String, subtitle: String?, icon: Int?, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    SettingsCard {
        SettingsRow(title, subtitle, icon, { open = !open }) {
            Icon(
                painterResource(R.drawable.ic_pp_next),
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).rotate(if (open) 90f else 0f)
            )
        }
        if (open) {
            RowDivider()
            content()
        }
    }
}

@Composable
fun EmbeddedSetting(key: String) {
    val container = SettingsActivity.settingsContainer
    container[key]?.Preference()
}

object LayoutPreviewData {
    private val cache = ConcurrentHashMap<String, List<List<String>>>()

    fun rows(context: android.content.Context, type: LayoutType, name: String): List<List<String>> {
        val cacheKey = type.name + "/" + name
        return cache.getOrPut(cacheKey) {
            runCatching {
                val content = if (LayoutUtilsCustom.isCustomLayout(name))
                    LayoutUtilsCustom.getLayoutFile(name, type, context).readText()
                else LayoutUtils.getContent(type, name, context)
                parse(content)
            }.getOrDefault(emptyList())
        }
    }

    fun parse(content: String): List<List<String>> {
        val text = content.replace("\r\n", "\n")
        val trimmed = text.trimStart()
        if (trimmed.startsWith("[") || trimmed.startsWith("//")) {
            val parsed = runCatching { parseJson(text) }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }
        return parseSimple(text)
    }

    private fun parseSimple(text: String): List<List<String>> =
        text.split(Regex("\\n\\s*\\n")).map { block ->
            block.split("\n").map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("//") }
                .map { line -> clean(line.split(Regex("\\s+")).first().substringBefore("|")) }
                .filter { it.isNotEmpty() }
        }.filter { it.isNotEmpty() }.take(6)

    private fun parseJson(text: String): List<List<String>> {
        val stripped = text.split("\n").filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
        val array = JSONArray(stripped)
        val rows = ArrayList<List<String>>()
        for (i in 0 until array.length()) {
            val row = array.optJSONArray(i) ?: continue
            val labels = ArrayList<String>()
            for (j in 0 until row.length()) {
                val key = row.opt(j)
                val label = when (key) {
                    is JSONObject -> keyLabel(key)
                    is String -> clean(key)
                    else -> ""
                }
                if (label.isNotEmpty()) labels.add(label)
            }
            if (labels.isNotEmpty()) rows.add(labels)
        }
        return rows.take(6)
    }

    private fun keyLabel(key: JSONObject): String {
        val label = key.optString("label", "")
        if (label.isNotEmpty()) return clean(label.substringBefore("|").substringBefore(" "))
        val code = key.optInt("code", 0)
        if (code > 32) return String(Character.toChars(code))
        val type = key.optString("$", "")
        return when {
            type.contains("shift") -> "⇧"
            type.contains("delete") -> "⌫"
            type.contains("enter") || type.contains("action") -> "⏎"
            type.contains("space") -> ""
            else -> ""
        }
    }

    private fun clean(token: String): String {
        if (token.startsWith("!icon/")) {
            val name = token.removePrefix("!icon/").substringBefore("|")
            return when {
                name.contains("shift") -> "⇧"
                name.contains("delete") -> "⌫"
                name.contains("enter") || name.contains("action") -> "⏎"
                name.contains("space") -> "␣"
                name.contains("emoji") -> "☺"
                name.contains("language") -> "◍"
                else -> "•"
            }
        }
        if (token.startsWith("!")) return "•"
        if (token.startsWith("$$$")) return "$"
        if (token.startsWith("$")) return "•"
        return if (token.length <= 3) token else token.take(3)
    }
}

@Composable
fun LayoutPicture(type: LayoutType, name: String, modifier: Modifier = Modifier, height: Dp = 72.dp) {
    val context = LocalContext.current
    val rows = remember(type, name) { LayoutPreviewData.rows(context, type, name) }
    val keyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val frameColor = MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(frameColor, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(6.dp)
    ) {
        if (rows.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val widest = rows.maxOf { it.size }.coerceAtLeast(1)
        val unit = (size.width - gap * (widest - 1)) / widest
        val keyHeight = (size.height - gap * (rows.size - 1)) / rows.size
        val corner = CornerRadius(keyHeight * 0.22f, keyHeight * 0.22f)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = (minOf(keyHeight * 0.58f, unit * 0.62f)).coerceAtLeast(6f)
        }
        rows.forEachIndexed { r, row ->
            val rowWidth = row.size * unit + (row.size - 1) * gap
            val startX = (size.width - rowWidth) / 2f
            val top = r * (keyHeight + gap)
            row.forEachIndexed { c, label ->
                val left = startX + c * (unit + gap)
                drawRoundRect(keyColor, Offset(left, top), Size(unit, keyHeight), corner)
                drawIntoCanvas {
                    val baseline = top + keyHeight / 2f - (paint.descent() + paint.ascent()) / 2f
                    it.nativeCanvas.drawText(label, left + unit / 2f, baseline, paint)
                }
            }
        }
    }
}
