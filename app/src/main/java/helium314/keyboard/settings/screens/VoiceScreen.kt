package helium314.keyboard.settings.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ui.PillRow
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.ToggleTile
import helium314.keyboard.settings.ui.ValueTile
import helium314.keyboard.voice.VoiceEngine
import helium314.keyboard.voice.VoicePermissionActivity
import helium314.keyboard.voice.VoiceSettings

private enum class VoiceDialog { LANGUAGE, VOCABULARY }

@Composable
fun VoiceScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<VoiceDialog?>(null) }
    val engine = remember(version) { VoiceSettings.engine(ctx) }
    val model = remember(version) { VoiceSettings.model(ctx) }
    val language = remember(version) { VoiceSettings.language(ctx) }
    val vocabulary = remember(version) { VoiceSettings.vocabulary(ctx) }
    val cleanup = remember(version) { VoiceSettings.aiCleanup(ctx) }
    val micGranted = remember(version) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    PocketScreen(stringResource(R.string.pp_voice), onClickBack) {
        TileRows(columns = 2, count = 2) { i ->
            if (i == 0) ToggleTile(
                stringResource(R.string.pp_device),
                R.drawable.sym_keyboard_voice_lxx,
                engine == VoiceEngine.DEVICE,
                {
                    VoiceSettings.setEngine(ctx, VoiceEngine.DEVICE)
                    version++
                },
                Modifier.fillMaxWidth()
            ) else ToggleTile(
                stringResource(R.string.pp_whisper),
                R.drawable.ic_pp_cloud,
                engine == VoiceEngine.GROQ,
                {
                    VoiceSettings.setEngine(ctx, VoiceEngine.GROQ)
                    version++
                },
                Modifier.fillMaxWidth()
            )
        }
        PillRow(
            options = listOf(
                stringResource(R.string.pp_fast) to VoiceSettings.MODEL_TURBO,
                stringResource(R.string.pp_accurate) to VoiceSettings.MODEL_ACCURATE,
            ),
            selected = model,
            onSelect = {
                VoiceSettings.setModel(ctx, it)
                version++
            }
        )
        ValueTile(
            stringResource(R.string.pp_language),
            language.ifBlank { stringResource(R.string.pp_auto) },
            R.drawable.ic_settings_languages,
            { dialog = VoiceDialog.LANGUAGE },
            Modifier.fillMaxWidth()
        )
        ValueTile(
            stringResource(R.string.pp_hints),
            vocabulary.ifBlank { stringResource(R.string.pp_none) },
            R.drawable.ic_pp_snippet,
            { dialog = VoiceDialog.VOCABULARY },
            Modifier.fillMaxWidth()
        )
        TileRows(columns = 2, count = 2) { i ->
            if (i == 0) ToggleTile(
                stringResource(R.string.pp_cleanup),
                R.drawable.ic_ai,
                cleanup,
                {
                    VoiceSettings.setAiCleanup(ctx, it)
                    version++
                },
                Modifier.fillMaxWidth()
            ) else ToggleTile(
                stringResource(R.string.pp_mic),
                R.drawable.sym_keyboard_voice_lxx,
                micGranted,
                {
                    ctx.startActivity(Intent(ctx, VoicePermissionActivity::class.java))
                    version++
                },
                Modifier.fillMaxWidth()
            )
        }
    }

    when (dialog) {
        VoiceDialog.LANGUAGE -> {
            var text by remember { mutableStateOf(language) }
            VoiceTextDialog(
                title = stringResource(R.string.pp_language),
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                onSave = {
                    VoiceSettings.setLanguage(ctx, text)
                    version++
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }

        VoiceDialog.VOCABULARY -> {
            var text by remember { mutableStateOf(vocabulary) }
            VoiceTextDialog(
                title = stringResource(R.string.pp_hints),
                value = text,
                onValueChange = { text = it },
                singleLine = false,
                onSave = {
                    VoiceSettings.setVocabulary(ctx, text)
                    version++
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }

        null -> {}
    }
}

@Composable
internal fun ChoiceDialog(
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
                    Row(Modifier.fillMaxWidth().clickable { onSelect(value) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_cancel)) } }
    )
}

@Composable
private fun VoiceTextDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.ai_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_cancel)) } }
    )
}
