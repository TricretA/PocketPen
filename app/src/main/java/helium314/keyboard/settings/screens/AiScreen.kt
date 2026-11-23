package helium314.keyboard.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import helium314.keyboard.ai.AiAction
import helium314.keyboard.ai.AiClient
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.ai.AiRunner
import helium314.keyboard.ai.AiSettings
import helium314.keyboard.latin.R
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AiDialog { PROVIDER, KEY, MODEL, LANGUAGE, PROMPT, TEST }

@Composable
fun AiScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var provider by remember { mutableStateOf(AiSettings.provider(ctx)) }
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<AiDialog?>(null) }
    val hasKey = remember(provider, version) { AiKeyStore.hasKey(ctx, provider) }
    val model = remember(provider, version) { AiSettings.model(ctx, provider) }
    val language = remember(version) { AiSettings.targetLanguage(ctx) }
    val customPrompt = remember(version) { AiSettings.customPrompt(ctx) }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_ai),
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(Modifier.verticalScroll(rememberScrollState()).then(Modifier.padding(innerPadding))) {
                Preference(
                    name = stringResource(R.string.ai_provider),
                    description = provider.label,
                    onClick = { dialog = AiDialog.PROVIDER }
                )
                Preference(
                    name = stringResource(R.string.ai_api_key),
                    description = stringResource(if (hasKey) R.string.ai_api_key_saved else R.string.ai_api_key_missing),
                    onClick = { dialog = AiDialog.KEY }
                )
                Preference(
                    name = stringResource(R.string.ai_model),
                    description = model,
                    onClick = { dialog = AiDialog.MODEL }
                )
                Preference(
                    name = stringResource(R.string.ai_target_language),
                    description = language,
                    onClick = { dialog = AiDialog.LANGUAGE }
                )
                Preference(
                    name = stringResource(R.string.ai_custom_prompt),
                    description = customPrompt.ifBlank { stringResource(R.string.ai_custom_prompt_empty) },
                    onClick = { dialog = AiDialog.PROMPT }
                )
                Preference(
                    name = stringResource(R.string.ai_test),
                    onClick = { dialog = AiDialog.TEST }
                )
                Text(
                    text = stringResource(R.string.ai_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    when (dialog) {
        AiDialog.PROVIDER -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.ai_provider)) },
            text = {
                Column {
                    AiProvider.entries.forEach { entry ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                AiSettings.setProvider(ctx, entry)
                                provider = entry
                                dialog = null
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = entry == provider, onClick = null)
                            Text(entry.label, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
        )

        AiDialog.KEY -> {
            var text by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.ai_api_key) + " · " + provider.label) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (hasKey) TextButton(onClick = {
                            AiKeyStore.clear(ctx, provider)
                            version++
                            dialog = null
                        }) { Text(stringResource(R.string.ai_api_key_remove)) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (text.isNotBlank()) AiKeyStore.save(ctx, provider, text)
                        version++
                        dialog = null
                    }) { Text(stringResource(R.string.ai_save)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
            )
        }

        AiDialog.MODEL -> {
            var text by remember { mutableStateOf(model) }
            var models by remember { mutableStateOf<List<String>>(emptyList()) }
            var status by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.ai_model) + " · " + provider.label) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            val apiKey = AiKeyStore.load(ctx, provider)
                            if (apiKey == null) {
                                status = ctx.getString(R.string.ai_error_no_key, provider.label)
                            } else {
                                status = ctx.getString(R.string.ai_fetching_models)
                                val current = provider
                                scope.launch(Dispatchers.IO) {
                                    val result = runCatching { AiClient.listModels(current, apiKey) }
                                    result.onSuccess {
                                        models = it
                                        status = null
                                    }
                                    result.onFailure { status = it.message }
                                }
                            }
                        }) { Text(stringResource(R.string.ai_fetch_models)) }
                        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        val query = text.trim()
                        val shown = if (query.isEmpty() || query == model) models
                            else models.filter { it.contains(query, ignoreCase = true) }
                        LazyColumn(Modifier.heightIn(max = 220.dp)) {
                            items(shown) { id ->
                                Text(id, Modifier.fillMaxWidth().clickable { text = id }.padding(vertical = 10.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        AiSettings.setModel(ctx, provider, text)
                        version++
                        dialog = null
                    }) { Text(stringResource(R.string.ai_save)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
            )
        }

        AiDialog.LANGUAGE -> {
            var text by remember { mutableStateOf(language) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.ai_target_language)) },
                text = {
                    OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    TextButton(onClick = {
                        AiSettings.setTargetLanguage(ctx, text)
                        version++
                        dialog = null
                    }) { Text(stringResource(R.string.ai_save)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
            )
        }

        AiDialog.PROMPT -> {
            var text by remember { mutableStateOf(customPrompt) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.ai_custom_prompt)) },
                text = {
                    OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    TextButton(onClick = {
                        AiSettings.setCustomPrompt(ctx, text)
                        version++
                        dialog = null
                    }) { Text(stringResource(R.string.ai_save)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_cancel)) } }
            )
        }

        AiDialog.TEST -> {
            var outcome by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                outcome = withContext(Dispatchers.IO) {
                    runCatching { AiRunner.run(ctx, AiAction.ASK, "Reply with the single word: ok") }
                        .fold({ "OK: $it" }, { "Failed: " + (it.message ?: it.javaClass.simpleName) })
                }
            }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.ai_test)) },
                text = { Text(outcome ?: stringResource(R.string.ai_testing)) },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_close)) } }
            )
        }

        null -> {}
    }
}
