package helium314.keyboard.settings.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP

private val SignalLip = Color(0xFFB93405)

@Composable
fun PocketFrame(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                KeyCap(onClick = onBack, modifier = Modifier.size(52.dp), corner = 18.dp) {
                    Icon(
                        painterResource(R.drawable.ic_pp_back),
                        contentDescription = stringResource(R.string.pp_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center).size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        content()
    }
}

@Composable
fun PocketScreen(
    title: String,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    PocketFrame(title, onBack) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
        footer?.invoke()
    }
}

@Composable
fun KeyCap(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    lip: Color = MaterialTheme.colorScheme.outlineVariant,
    corner: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel = 4.dp
    val dy by animateDpAsState(if (pressed) travel else 0.dp, label = "keycap")
    Box(
        modifier
            .drawBehind {
                val t = travel.toPx()
                drawRoundRect(
                    color = lip,
                    topLeft = Offset(0f, t),
                    size = Size(size.width, size.height - t),
                    cornerRadius = CornerRadius(corner.toPx())
                )
            }
            .padding(bottom = travel)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .offset(y = dy)
                .clip(RoundedCornerShape(corner))
                .background(color)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            content = content
        )
    }
}

@Composable
fun ToggleTile(label: String, icon: Int, checked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val darkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fill = when {
        !checked -> MaterialTheme.colorScheme.surfaceContainer
        darkMode -> Color(0xFF3A1708)
        else -> PP.SignalSoft
    }
    val foreground = if (checked) PP.Signal else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val lip = if (checked) PP.Signal else MaterialTheme.colorScheme.outlineVariant
    KeyCap(
        onClick = { onToggle(!checked) },
        modifier = modifier.height(108.dp),
        color = fill,
        lip = lip,
        corner = 24.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = foreground, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NavTile(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KeyCap(onClick = onClick, modifier = modifier.height(124.dp), corner = 28.dp) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(PP.Signal),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = PP.Ink, modifier = Modifier.size(26.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ValueTile(label: String, value: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KeyCap(onClick = onClick, modifier = modifier.height(88.dp), corner = 24.dp) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(PP.Signal),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = PP.Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KeyCap(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        color = if (selected) PP.Signal else MaterialTheme.colorScheme.surfaceContainer,
        lip = if (selected) SignalLip else MaterialTheme.colorScheme.outlineVariant,
        corner = 26.dp
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) PP.Ink else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
            maxLines = 1
        )
    }
}

@Composable
fun <T> PillRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (label, value) ->
            Pill(label, selected == value, { onSelect(value) })
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp)
    )
}

@Composable
fun TileRows(columns: Int, count: Int, spacing: Dp = 14.dp, tile: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        var index = 0
        while (index < count) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (column in 0 until columns) {
                    val i = index + column
                    Box(Modifier.weight(1f)) { if (i < count) tile(i) }
                }
            }
            index += columns
        }
    }
}

@Composable
fun VisualSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onDone: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    smallIcon: Int,
    bigIcon: Int,
    modifier: Modifier = Modifier
) {
    KeyCap(onClick = {}, modifier = modifier.fillMaxWidth().height(84.dp), corner = 24.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(smallIcon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onDone,
                valueRange = range,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = PP.Signal,
                    activeTrackColor = PP.Signal,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Icon(painterResource(bigIcon), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
        }
    }
}

data class PreviewColors(
    val background: Color,
    val key: Color,
    val function: Color,
    val accent: Color,
    val ink: Color,
)

data class PreviewShape(
    val radius: Float = 6f,
    val border: Float = 0f,
    val borderColor: Color? = null,
    val lip: Float = 0f,
    val filled: Boolean = true,
)

fun previewColorsFor(theme: String): PreviewColors {
    val pocket = helium314.keyboard.latin.utils.PocketThemes.get(theme)
    if (pocket != null) return PreviewColors(
        Color(pocket.background.toInt()),
        Color(pocket.key.toInt()),
        Color(pocket.functional.toInt()),
        Color(pocket.accent.toInt()),
        Color(pocket.text.toInt())
    )
    return legacyPreviewColorsFor(theme)
}

fun previewShapeFor(theme: String): PreviewShape {
    val pocket = helium314.keyboard.latin.utils.PocketThemes.get(theme) ?: return PreviewShape()
    return PreviewShape(
        pocket.radius.toFloat(),
        pocket.border,
        if (pocket.borderColor != 0L) Color(pocket.borderColor.toInt()) else null,
        pocket.lip,
        pocket.borders
    )
}

private fun legacyPreviewColorsFor(theme: String): PreviewColors = when (theme) {
    "ink" -> PreviewColors(PP.Ink, Color(0xFF262A36), Color(0xFF1C1F29), PP.Signal, PP.Paper)
    "signal" -> PreviewColors(PP.Signal, PP.Paper, Color(0xFFFFCEBA), PP.Ink, PP.Ink)
    "black" -> PreviewColors(Color.Black, Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFF4F83FF), Color.White)
    "dark" -> PreviewColors(Color(0xFF263238), Color(0xFF364248), Color(0xFF2D393F), Color(0xFF4F83FF), Color.White)
    "darker" -> PreviewColors(Color(0xFF1F1F1F), Color(0xFF2E2E2E), Color(0xFF282828), Color(0xFF4F83FF), Color.White)
    "ocean" -> PreviewColors(Color(89, 109, 155), Color(132, 157, 212), Color(81, 116, 194), Color(255, 124, 0), Color.White)
    "forest" -> PreviewColors(Color(181, 125, 88), Color(228, 212, 191), Color(212, 186, 153), Color(75, 110, 75), Color(0, 50, 0))
    "violette" -> PreviewColors(Color(112, 112, 174), Color(150, 150, 216), Color(123, 123, 206), Color(255, 96, 255), Color.White)
    "pink" -> PreviewColors(Color(252, 228, 236), Color.White, Color(248, 187, 208), Color(236, 64, 122), Color.Black)
    "sand" -> PreviewColors(Color(242, 232, 218), Color.White, Color(234, 211, 185), Color(110, 155, 255), Color.Black)
    "light" -> PreviewColors(Color(0xFFECEFF1), Color.White, Color(0xFFCFD8DC), Color(0xFF4F83FF), Color.Black)
    else -> PreviewColors(PP.Paper, Color.White, PP.Mist, PP.Signal, PP.Ink)
}

@Composable
fun KeyboardPreview(
    colors: PreviewColors,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    shape: PreviewShape? = null,
    numberRow: Boolean = false,
    textScale: Float = 1f,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val unit = w / 360f
        drawRect(colors.background)
        val pad = w * 0.03f
        val stripH = h * 0.14f
        val words = 3
        for (i in 0 until words) {
            val cw = (w - pad * 2) / words
            drawRoundRect(
                color = colors.ink.copy(alpha = 0.22f),
                topLeft = Offset(pad + cw * i + cw * 0.28f, stripH * 0.38f),
                size = Size(cw * 0.44f, stripH * 0.24f),
                cornerRadius = CornerRadius(stripH * 0.12f)
            )
        }
        val rows = if (numberRow) 5 else 4
        val gap = w * 0.012f
        val top = stripH + gap
        val rowH = (h - top - pad * 0.6f - gap * (rows - 1)) / rows
        val radius = if (shape != null) (shape.radius * unit).coerceAtMost(rowH / 2f) else rowH * 0.28f
        val lip = if (shape != null) shape.lip * unit else 0f
        val strokeW = if (shape != null) shape.border * unit else if (bordered) 2f else 0f
        val strokeColor = shape?.borderColor ?: colors.ink.copy(alpha = 0.3f)
        val filled = shape?.filled ?: true
        val markScale = textScale.coerceIn(0.6f, 1.6f)
        fun key(x: Float, y: Float, kw: Float, color: Color, mark: Boolean) {
            val kh = rowH - lip
            if (lip > 0f) drawRoundRect(color.copy(alpha = 1f).let { Color(it.red * 0.7f, it.green * 0.7f, it.blue * 0.7f, 1f) }, Offset(x, y + lip), Size(kw, kh), CornerRadius(radius))
            drawRoundRect(if (filled) color else color.copy(alpha = 0.35f), Offset(x, y), Size(kw, kh), CornerRadius(radius))
            if (strokeW > 0f) drawRoundRect(strokeColor, Offset(x, y), Size(kw, kh), CornerRadius(radius), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
            if (mark) drawRoundRect(
                colors.ink.copy(alpha = 0.55f),
                Offset(x + kw * (0.5f - 0.14f * markScale), y + kh * (0.5f - 0.14f * markScale)),
                Size(kw * 0.28f * markScale, kh * 0.28f * markScale),
                CornerRadius(kh * 0.1f)
            )
        }
        fun letters(row: Int, count: Int, inset: Float) {
            val kw = (w - pad * 2 - gap * 9) / 10f
            val y = top + (rowH + gap) * row
            val rowWidth = count * kw + (count - 1) * gap
            val start = pad + inset + ((w - pad * 2 - inset * 2) - rowWidth) / 2f
            for (i in 0 until count) key(start + i * (kw + gap), y, kw, colors.key, true)
        }
        val first = if (numberRow) 1 else 0
        if (numberRow) letters(0, 10, 0f)
        letters(first, 10, 0f)
        letters(first + 1, 9, 0f)
        val kw = (w - pad * 2 - gap * 9) / 10f
        val y3 = top + (rowH + gap) * (first + 2)
        key(pad, y3, kw * 1.4f, colors.function, false)
        letters(first + 2, 7, kw * 1.4f + gap)
        key(w - pad - kw * 1.4f, y3, kw * 1.4f, colors.function, false)
        val y4 = top + (rowH + gap) * (first + 3)
        key(pad, y4, kw * 1.4f, colors.function, false)
        key(pad + kw * 1.4f + gap, y4, kw, colors.function, false)
        val spaceStart = pad + kw * 2.4f + gap * 2
        val enterW = kw * 1.6f
        val spaceEnd = w - pad - enterW - gap - kw - gap
        key(spaceStart, y4, spaceEnd - spaceStart, colors.key, false)
        key(spaceEnd + gap, y4, kw, colors.function, false)
        key(w - pad - enterW, y4, enterW, colors.accent, false)
    }
}

@Composable
fun ThemeSwatch(
    label: String,
    colors: PreviewColors,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: PreviewShape? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(20.dp)
                )
                .clickable(role = Role.RadioButton, onClick = onClick)
        ) {
            KeyboardPreview(colors, Modifier.fillMaxSize(), shape = shape)
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(PP.Signal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_pp_check),
                        contentDescription = null,
                        tint = PP.Ink,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String?,
    icon: Int?,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    val darkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconFill = if (darkMode) Color(0xFF3A1708) else PP.SignalSoft
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(iconFill), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = PP.Signal, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, icon: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingsRow(title, subtitle, icon, { onChange(!checked) }) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PP.Signal,
                checkedBorderColor = PP.Signal,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun ChoiceRow(title: String, subtitle: String?, icon: Int?, selected: Boolean, onClick: () -> Unit) {
    SettingsRow(title, subtitle, icon, onClick) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = null,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = PP.Signal,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun ValueRow(title: String, subtitle: String?, value: String, icon: Int?, onClick: () -> Unit) {
    SettingsRow(title, subtitle, icon, onClick) {
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = PP.Signal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )
    }
}
