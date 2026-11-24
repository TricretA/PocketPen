package helium314.keyboard.settings.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.KeyCap
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.toolspanel.ToolsLayout
import helium314.keyboard.toolspanel.ToolsState

@Composable
fun ToolbarScreen(onClickBack: () -> Unit, onAdvanced: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var version by remember { mutableIntStateOf(0) }
    val state = remember(version) { ToolsLayout.read(prefs) }
    val autoShow = remember(version) { prefs.getBoolean(Settings.PREF_AUTO_SHOW_TOOLBAR, true) }
    remember { KeyboardIconsSet.instance.loadIcons(ctx) }

    fun apply(next: ToolsState) {
        ToolsLayout.write(prefs, next)
        runCatching { KeyboardSwitcher.getInstance().suggestionStrip?.rebuildToolbarKeys() }
        version++
    }

    fun iconOf(key: ToolbarKey) = KeyboardIconsSet.instance.getNewDrawable(key.name, ctx)?.mutate()?.toBitmap(96, 96)?.asImageBitmap()

    PocketScreen(stringResource(R.string.pp_toolbar), onClickBack, footer = { helium314.keyboard.settings.ui.LivePreview() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(painterResource(R.drawable.ic_pp_grid), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            state.toolbar.forEach { key ->
                iconOf(key)?.let {
                    Image(it, null, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface), modifier = Modifier.size(26.dp))
                }
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                state.pinned?.let { key ->
                    iconOf(key)?.let {
                        Image(it, null, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface), modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.pp_tb_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        SectionLabel(stringResource(R.string.pp_tb_on))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.toolbar.forEachIndexed { index, key ->
                ToolRow(
                    label = ToolsLayout.label(ctx, key),
                    icon = iconOf(key),
                    checked = true,
                    onChange = { apply(state.copy(toolbar = state.toolbar - key, panel = listOf(key) + state.panel)) },
                    canUp = index > 0,
                    canDown = index < state.toolbar.size - 1,
                    onUp = { apply(state.copy(toolbar = state.toolbar.toMutableList().apply { removeAt(index); add(index - 1, key) })) },
                    onDown = { apply(state.copy(toolbar = state.toolbar.toMutableList().apply { removeAt(index); add(index + 1, key) })) }
                )
            }
        }

        SectionLabel(stringResource(R.string.pp_tb_off))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.panel.forEach { key ->
                ToolRow(
                    label = ToolsLayout.label(ctx, key),
                    icon = iconOf(key),
                    checked = false,
                    onChange = {
                        if (state.toolbar.size >= ToolsLayout.MAX_TOOLBAR)
                            Toast.makeText(ctx, R.string.pp_toolbar_full, Toast.LENGTH_SHORT).show()
                        else apply(state.copy(toolbar = state.toolbar + key, panel = state.panel - key))
                    }
                )
            }
        }

        SectionLabel(stringResource(R.string.pp_tb_mic))
        SwitchLine(
            stringResource(R.string.pp_tb_mic),
            stringResource(R.string.pp_tb_mic_sub),
            state.pinned != null
        ) { on ->
            val voice = ToolbarKey.VOICE
            if (on) apply(state.copy(toolbar = state.toolbar - voice, pinned = voice, panel = state.panel - voice))
            else state.pinned?.let { apply(state.copy(pinned = null, panel = listOf(it) + state.panel)) }
        }
        SwitchLine(
            stringResource(R.string.pp_tb_idle),
            stringResource(R.string.pp_tb_idle_sub),
            autoShow
        ) { on ->
            prefs.edit { putBoolean(Settings.PREF_AUTO_SHOW_TOOLBAR, on) }
            version++
        }

        KeyCap(
            onClick = { ToolsLayout.reset(prefs); apply(ToolsLayout.read(prefs)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            corner = 28.dp
        ) {
            Text(
                stringResource(R.string.pp_tb_reset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        KeyCap(
            onClick = onAdvanced,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            corner = 28.dp
        ) {
            Text(
                stringResource(R.string.pp_tb_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ToolRow(
    label: String,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    checked: Boolean,
    onChange: () -> Unit,
    canUp: Boolean = false,
    canDown: Boolean = false,
    onUp: () -> Unit = {},
    onDown: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null)
            Image(icon, null, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (checked) {
            Arrow(rotation = 90f, enabled = canUp, onClick = onUp)
            Arrow(rotation = -90f, enabled = canDown, onClick = onDown)
        }
        Switch(checked = checked, onCheckedChange = { onChange() }, colors = switchColors())
    }
}

@Composable
private fun Arrow(rotation: Float, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_pp_back),
            null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.2f),
            modifier = Modifier.size(22.dp).rotate(rotation)
        )
    }
}

@Composable
private fun SwitchLine(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = switchColors())
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = PP.White,
    checkedTrackColor = PP.Signal,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
)
