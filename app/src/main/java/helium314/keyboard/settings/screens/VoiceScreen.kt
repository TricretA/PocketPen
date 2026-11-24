package helium314.keyboard.settings.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ui.ChoiceRow
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SwitchRow
import helium314.keyboard.settings.ui.ValueRow
import helium314.keyboard.voice.VoiceEngine
import helium314.keyboard.voice.VoicePermissionActivity
import helium314.keyboard.voice.VoiceSettings
import kotlinx.coroutines.delay

private enum class VoiceDialog { LANGUAGE, VOCABULARY }

private val voiceLanguages = listOf(
    "en" to "English", "sw" to "Swahili", "fr" to "French", "es" to "Spanish", "de" to "German",
    "pt" to "Portuguese", "it" to "Italian", "ar" to "Arabic", "hi" to "Hindi", "zh" to "Chinese",
    "ja" to "Japanese", "ko" to "Korean", "ru" to "Russian", "tr" to "Turkish", "nl" to "Dutch"
)

private fun micAllowed(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@Composable
fun VoiceScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<VoiceDialog?>(null) }
    var micGranted by remember { mutableStateOf(micAllowed(ctx)) }
    var keyInput by remember { mutableStateOf("") }
    val engine = remember(version) { VoiceSettings.engine(ctx) }
    val model = remember(version) { VoiceSettings.model(ctx) }
    val language = remember(version) { VoiceSettings.language(ctx) }
    val vocabulary = remember(version) { VoiceSettings.vocabulary(ctx) }
    val cleanup = remember(version) { VoiceSettings.aiCleanup(ctx) }
    val hasKey = remember(version) { AiKeyStore.hasKey(ctx, AiProvider.GROQ) }

    LaunchedEffect(Unit) {
        while (true) {
            micGranted = micAllowed(ctx)
            delay(800)
        }
    }

    PocketScreen(stringResource(R.string.pp_voice), onClickBack) {
        SectionLabel(stringResource(R.string.vz_listen_section))
        SettingsCard {
            ChoiceRow(
                stringResource(R.string.vz_builtin),
                stringResource(R.string.vz_builtin_sub),
                R.drawable.sym_keyboard_voice_lxx,
                engine == VoiceEngine.DEVICE
            ) {
                VoiceSettings.setEngine(ctx, VoiceEngine.DEVICE)
                version++
            }
            RowDivider()
            ChoiceRow(
                stringResource(R.string.vz_whisper),
                stringResource(R.string.vz_whisper_sub),
                R.drawable.ic_pp_cloud,
                engine == VoiceEngine.GROQ
            ) {
                VoiceSettings.setEngine(ctx, VoiceEngine.GROQ)
                version++
            }
        }

        if (engine == VoiceEngine.GROQ) {
            SectionLabel(stringResource(R.string.vz_whisper_section))
            SettingsCard {
                if (hasKey) {
                    ValueRow(
                        stringResource(R.string.vz_key_title),
                        stringResource(R.string.vz_key_saved),
                        stringResource(R.string.vz_remove),
                        R.drawable.ic_pp_lock
                    ) {
                        AiKeyStore.clear(ctx, AiProvider.GROQ)
                        version++
                    }
                } else {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.vz_key_missing), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.vz_key_hint)) },
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = ctx.getSystemService(ClipboardManager::class.java)?.primaryClip
                                    val pasted = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
                                    if (!pasted.isNullOrBlank()) keyInput = pasted.trim()
                                }) {
                                    Icon(painterResource(R.drawable.ic_pp_paste), contentDescription = stringResource(R.string.vz_paste))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (keyInput.isNotBlank() && AiKeyStore.save(ctx, AiProvider.GROQ, keyInput)) {
                                    keyInput = ""
                                    version++
                                }
                            },
                            enabled = keyInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.vz_save)) }
                    }
                }
                RowDivider()
                SwitchRow(
                    stringResource(R.string.vz_accurate),
                    stringResource(R.string.vz_accurate_sub),
                    R.drawable.ic_pp_star,
                    model == VoiceSettings.MODEL_ACCURATE
                ) {
                    VoiceSettings.setModel(ctx, if (it) VoiceSettings.MODEL_ACCURATE else VoiceSettings.MODEL_TURBO)
                    version++
                }
                RowDivider()
                ValueRow(
                    stringResource(R.string.vz_language),
                    null,
                    voiceLanguages.firstOrNull { it.first == language }?.second
                        ?: if (language.isBlank()) stringResource(R.string.vz_language_auto) else language,
                    R.drawable.ic_settings_languages
                ) { dialog = VoiceDialog.LANGUAGE }
                RowDivider()
                SwitchRow(
                    stringResource(R.string.vz_cleanup),
                    stringResource(R.string.vz_cleanup_sub),
                    R.drawable.ic_ai,
                    cleanup
                ) {
                    VoiceSettings.setAiCleanup(ctx, it)
                    version++
                }
                RowDivider()
                ValueRow(
                    stringResource(R.string.vz_words),
                    stringResource(R.string.vz_words_sub),
                    if (vocabulary.isBlank()) stringResource(R.string.vz_words_none) else vocabulary,
                    R.drawable.ic_pp_snippet
                ) { dialog = VoiceDialog.VOCABULARY }
            }
        }

        SectionLabel(stringResource(R.string.vz_mic_section))
        SettingsCard {
            ValueRow(
                stringResource(R.string.vz_mic),
                null,
                stringResource(if (micGranted) R.string.vz_mic_allowed else R.string.vz_mic_tap),
                R.drawable.sym_keyboard_voice_lxx
            ) {
                if (!micGranted) ctx.startActivity(Intent(ctx, VoicePermissionActivity::class.java))
            }
        }
    }

    when (dialog) {
        VoiceDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.vz_language),
            options = listOf(stringResource(R.string.vz_language_auto) to "") + voiceLanguages.map { it.second to it.first },
            selected = language,
            onSelect = {
                VoiceSettings.setLanguage(ctx, it)
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        VoiceDialog.VOCABULARY -> {
            var text by remember { mutableStateOf(vocabulary) }
            VoiceTextDialog(
                title = stringResource(R.string.vz_words),
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
