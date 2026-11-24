package helium314.keyboard.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.clipboardplus.ScreenshotAccess
import helium314.keyboard.settings.ui.KeyCap
import androidx.compose.foundation.layout.width
import helium314.keyboard.settings.ui.PillRow
import helium314.keyboard.settings.ui.PocketFrame
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SwitchRow
import helium314.keyboard.settings.ui.ValueRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.R
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference

private sealed interface ClipDialog {
    data object Retention : ClipDialog
    data class Edit(val entry: ClipboardHistoryEntry) : ClipDialog
    data class Delete(val entry: ClipboardHistoryEntry) : ClipDialog
}

private val retentionPresets = listOf(
    R.string.clipboard_retention_1_hour to 60,
    R.string.clipboard_retention_1_day to 1440,
    R.string.clipboard_retention_7_days to 10080,
    R.string.clipboard_retention_30_days to 43200,
    R.string.clipboard_retention_90_days to 129600,
    R.string.clipboard_retention_never to 0,
)

@Composable
fun ClipboardManagerScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { ClipboardDao.getInstance(ctx) }
    var version by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(ClipboardDao.Tab.ALL) }
    var dialog by remember { mutableStateOf<ClipDialog?>(null) }
    val screenshots = remember(version) { ScreenshotAccess.active(ctx) }
    val retention = remember(version) {
        ctx.prefs().getInt(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME)
    }
    val entries = remember(version, query, tab) {
        dao?.getAll().orEmpty()
            .filter { tab.accepts(it) }
            .filter { query.isBlank() || it.text?.contains(query.trim(), ignoreCase = true) == true }
    }

    PocketFrame(stringResource(R.string.pp_clipboard), onClickBack) {
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCard {
                ValueRow(
                    stringResource(R.string.sb_clip_keep_t),
                    stringResource(R.string.sb_clip_keep_d),
                    retentionLabel(retention),
                    R.drawable.ic_pp_clock
                ) { dialog = ClipDialog.Retention }
                RowDivider()
                SwitchRow(
                    stringResource(R.string.sb_clip_screens_t),
                    stringResource(R.string.sb_clip_screens_d),
                    R.drawable.ic_pp_image,
                    screenshots
                ) { on ->
                    ScreenshotAccess.setEnabled(ctx, on)
                    if (on && !ScreenshotAccess.hasPermission(ctx)) ScreenshotAccess.requestPermission(ctx)
                    version++
                }
            }
            SectionLabel(stringResource(R.string.sb_clip_list))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pp_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_pp_search), contentDescription = null) },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            )
            PillRow(
                options = listOf(
                    stringResource(R.string.clipboard_tab_all) to ClipboardDao.Tab.ALL,
                    stringResource(R.string.clipboard_tab_recent) to ClipboardDao.Tab.RECENT,
                    stringResource(R.string.clipboard_tab_pinned) to ClipboardDao.Tab.PINNED,
                    stringResource(R.string.clipboard_tab_images) to ClipboardDao.Tab.IMAGES,
                ),
                selected = tab,
                onSelect = { tab = it }
            )
            if (entries.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.sym_keyboard_clipboard_lxx),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(stringResource(R.string.pp_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (entry.filename != null) {
                            ClipThumb(entry.filename!!)
                        } else Icon(
                            painterResource(if (entry.isPinned) R.drawable.ic_pp_pin else R.drawable.ic_pp_clock),
                            contentDescription = null,
                            tint = if (entry.isPinned || entry.isSnippet) PP.Signal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = entry.text ?: stringResource(R.string.clipboard_manager_file),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = {
                            dao?.setPinned(entry.id, !entry.isPinned)
                            version++
                        }) {
                            Icon(painterResource(R.drawable.ic_pp_pin), contentDescription = stringResource(R.string.pp_pin), modifier = Modifier.size(22.dp))
                        }
                        if (entry.filename == null) IconButton(onClick = { dialog = ClipDialog.Edit(entry) }) {
                            Icon(painterResource(R.drawable.ic_pp_edit), contentDescription = stringResource(R.string.pp_edit), modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { dialog = ClipDialog.Delete(entry) }) {
                            Icon(painterResource(R.drawable.ic_pp_delete), contentDescription = stringResource(R.string.pp_delete), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        ClipDialog.Retention -> RetentionDialog(
            current = retention,
            onSelect = { minutes ->
                ctx.prefs().edit { putInt(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, minutes) }
                dao?.clearOldClips(true)
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is ClipDialog.Edit -> {
            var text by remember { mutableStateOf(current.entry.text.orEmpty()) }
            ClipTextDialog(
                title = stringResource(R.string.clipboard_manager_edit),
                value = text,
                onValueChange = { text = it },
                onSave = {
                    dao?.updateText(current.entry.id, text)
                    version++
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }

        is ClipDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.clipboard_manager_delete)) },
            text = { Text(current.entry.text ?: stringResource(R.string.clipboard_manager_file), maxLines = 4, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    dao?.remove(current.entry.id)
                    version++
                    dialog = null
                }) { Text(stringResource(R.string.clipboard_manager_delete)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
        )

        null -> {}
    }
}

@Composable
private fun retentionLabel(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.clipboard_retention_never)
    minutes % 1440 == 0 -> stringResource(R.string.clipboard_manager_retention_days, minutes / 1440)
    minutes % 60 == 0 -> stringResource(R.string.clipboard_manager_retention_hours, minutes / 60)
    else -> stringResource(R.string.clipboard_manager_retention_minutes, minutes)
}

@Composable
private fun RetentionDialog(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clipboard_manager_retention)) },
        text = {
            Column {
                retentionPresets.forEach { (label, minutes) ->
                    Row(Modifier.fillMaxWidth().clickable { onSelect(minutes) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = minutes == current, onClick = null)
                        Text(stringResource(label), Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter { char -> char.isDigit() }.take(5) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.clipboard_manager_retention_custom)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = (custom.toIntOrNull() ?: 0) > 0,
                onClick = { onSelect((custom.toIntOrNull() ?: 1).coerceAtMost(36500) * 1440) }
            ) { Text(stringResource(R.string.ai_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_cancel)) } }
    )
}

@Composable
private fun ClipTextDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = onValueChange, minLines = 3, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = onSave) { Text(stringResource(R.string.ai_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_cancel)) } }
    )
}


@Composable
private fun ClipThumb(filename: String) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, filename) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(ClipboardDao.clipFilesDir, filename)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 320).coerceAtLeast(1)
                }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(painterResource(R.drawable.ic_pp_image), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
    }
}
