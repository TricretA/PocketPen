package helium314.keyboard.settings.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.voice.VoiceEngine
import helium314.keyboard.voice.VoicePermissionActivity
import helium314.keyboard.voice.VoiceSettings

private enum class VoiceDialog { ENGINE, MODEL, LANGUAGE, VOCABULARY }

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

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_voice),
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(Modifier.verticalScroll(rememberScrollState()).then(Modifier.padding(innerPadding))) {
                Preference(
                    name = stringResource(R.string.voice_engine),
                    description = stringResource(
                        if (engine == VoiceEngine.GROQ) R.string.voice_engine_groq_hint else R.string.voice_engine_device
                    ),
                    onClick = { dialog = VoiceDialog.ENGINE }
                )
                Preference(
                    name = stringResource(R.string.voice_model),
                    description = stringResource(
                        if (model == VoiceSettings.MODEL_ACCURATE) R.string.voice_model_accurate else R.string.voice_model_turbo
                    ),
                    onClick = { dialog = VoiceDialog.MODEL }
                )
                Preference(
                    name = stringResource(R.string.voice_language),
                    description = language.ifBlank { stringResource(R.string.voice_language_auto) },
                    onClick = { dialog = VoiceDialog.LANGUAGE }
                )
                Preference(
                    name = stringResource(R.string.voice_vocabulary),
                    description = vocabulary.ifBlank { stringResource(R.string.voice_vocabulary_empty) },
                    onClick = { dialog = VoiceDialog.VOCABULARY }
                )
                Preference(
                    name = stringResource(R.string.voice_ai_cleanup),
                    description = stringResource(if (cleanup) R.string.voice_ai_cleanup_on else R.string.voice_ai_cleanup_off),
                    onClick = {
                        VoiceSettings.setAiCleanup(ctx, !cleanup)
                        version++
                    }
                )
                Preference(
                    name = stringResource(R.string.voice_microphone),
                    description = stringResource(if (micGranted) R.string.voice_microphone_granted else R.string.voice_microphone_missing),
                    onClick = {
                        ctx.startActivity(Intent(ctx, VoicePermissionActivity::class.java))
                        version++
                    }
                )
                Text(
                    text = stringResource(R.string.voice_groq_key_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    when (dialog) {
        VoiceDialog.ENGINE -> ChoiceDialog(
            title = stringResource(R.string.voice_engine),
            options = listOf(
                stringResource(R.string.voice_engine_device) to VoiceEngine.DEVICE.id,
                stringResource(R.string.voice_engine_groq) to VoiceEngine.GROQ.id,
            ),
            selected = engine.id,
            onSelect = {
                VoiceSettings.setEngine(ctx, VoiceEngine.fromId(it))
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        VoiceDialog.MODEL -> ChoiceDialog(
            title = stringResource(R.string.voice_model),
            options = listOf(
                stringResource(R.string.voice_model_turbo) to VoiceSettings.MODEL_TURBO,
                stringResource(R.string.voice_model_accurate) to VoiceSettings.MODEL_ACCURATE,
            ),
            selected = model,
            onSelect = {
                VoiceSettings.setModel(ctx, it)
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        VoiceDialog.LANGUAGE -> {
            var text by remember { mutableStateOf(language) }
            VoiceTextDialog(
                title = stringResource(R.string.voice_language),
                hint = stringResource(R.string.voice_language_hint),
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
                title = stringResource(R.string.voice_vocabulary),
                hint = stringResource(R.string.voice_vocabulary_hint),
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
private fun ChoiceDialog(
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
    hint: String,
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
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(hint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.ai_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_cancel)) } }
    )
}
