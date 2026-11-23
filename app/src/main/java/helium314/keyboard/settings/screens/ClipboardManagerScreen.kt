package helium314.keyboard.settings.screens

import androidx.compose.foundation.clickable
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
    data object Add : ClipDialog
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
    val retention = remember(version) {
        ctx.prefs().getInt(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME)
    }
    val entries = remember(version, query, tab) {
        dao?.getAll().orEmpty()
            .filter { tab.accepts(it) }
            .filter { query.isBlank() || it.text?.contains(query.trim(), ignoreCase = true) == true }
    }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_clipboard),
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                Preference(
                    name = stringResource(R.string.clipboard_manager_retention),
                    description = retentionLabel(retention),
                    onClick = { dialog = ClipDialog.Retention }
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.clipboard_manager_search)) },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { dialog = ClipDialog.Add }) { Text(stringResource(R.string.clipboard_manager_add_snippet)) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(
                        ClipboardDao.Tab.ALL to R.string.clipboard_tab_all,
                        ClipboardDao.Tab.RECENT to R.string.clipboard_tab_recent,
                        ClipboardDao.Tab.PINNED to R.string.clipboard_tab_pinned,
                        ClipboardDao.Tab.SNIPPETS to R.string.clipboard_tab_snippets,
                    ).forEach { (entryTab, label) ->
                        TextButton(onClick = { tab = entryTab }) {
                            Text(
                                stringResource(label),
                                color = if (tab == entryTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()
                if (entries.isEmpty()) {
                    Text(stringResource(R.string.clipboard_manager_empty), Modifier.padding(16.dp))
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.id }) { entry ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = entry.text ?: stringResource(R.string.clipboard_manager_file),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(
                                        when {
                                            entry.isSnippet -> R.string.clipboard_manager_kind_snippet
                                            entry.isPinned -> R.string.clipboard_manager_kind_pinned
                                            else -> R.string.clipboard_manager_kind_recent
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    dao?.cycleKind(entry.id)
                                    version++
                                }) { Text(stringResource(R.string.clipboard_manager_pin_cycle)) }
                                if (entry.filename == null) TextButton(onClick = { dialog = ClipDialog.Edit(entry) }) {
                                    Text(stringResource(R.string.clipboard_manager_edit))
                                }
                                TextButton(onClick = { dialog = ClipDialog.Delete(entry) }) {
                                    Text(stringResource(R.string.clipboard_manager_delete))
                                }
                            }
                        }
                        HorizontalDivider()
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

        ClipDialog.Add -> {
            var text by remember { mutableStateOf("") }
            ClipTextDialog(
                title = stringResource(R.string.clipboard_manager_add_snippet),
                value = text,
                onValueChange = { text = it },
                onSave = {
                    dao?.addSnippet(text)
                    version++
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }

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
