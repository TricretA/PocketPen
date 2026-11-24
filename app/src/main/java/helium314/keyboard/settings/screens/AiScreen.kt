package helium314.keyboard.settings.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import helium314.keyboard.ai.AiAction
import helium314.keyboard.ai.AiClient
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.ai.AiRunner
import helium314.keyboard.ai.AiSettings
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.settings.ui.KeyCap
import helium314.keyboard.settings.ui.Pill
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.ValueTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProviderLogo(provider: AiProvider, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(provider.brandColor)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(provider.logo),
            contentDescription = provider.label,
            tint = Color.White,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
private fun ProviderTile(provider: AiProvider, ready: Boolean, isDefault: Boolean, onClick: () -> Unit, modifier: Modifier) {
    KeyCap(
        onClick = onClick,
        modifier = modifier.height(128.dp),
        lip = if (isDefault) PP.Signal else MaterialTheme.colorScheme.outlineVariant,
        corner = 26.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProviderLogo(provider, 52.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                provider.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (ready) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(PP.Signal),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_pp_check), contentDescription = null, tint = PP.Ink, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DefaultRow(provider: AiProvider, model: String, selected: Boolean, onSelect: () -> Unit) {
    KeyCap(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        lip = if (selected) PP.Signal else MaterialTheme.colorScheme.outlineVariant,
        corner = 24.dp
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProviderLogo(provider, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(provider.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    model,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painterResource(R.drawable.ic_pp_star),
                contentDescription = stringResource(R.string.pp_is_default),
                tint = if (selected) PP.Signal else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun ProviderDetail(
    provider: AiProvider,
    version: Int,
    onChanged: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyText by remember(provider) { mutableStateOf("") }
    val hasKey = remember(provider, version) { AiKeyStore.hasKey(ctx, provider) }
    val isDefault = remember(provider, version) { AiSettings.provider(ctx) == provider }
    var model by remember(provider, version) { mutableStateOf(AiSettings.model(ctx, provider)) }
    var models by remember(provider) { mutableStateOf((listOf(provider.defaultModel) + provider.suggestedModels).distinct()) }
    var menu by remember { mutableStateOf(false) }
    var status by remember(provider) { mutableStateOf<String?>(null) }
    var loading by remember(provider) { mutableStateOf(false) }

    PocketScreen(provider.label, onBack) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            ProviderLogo(provider, 84.dp)
        }
        if (hasKey) {
            ValueTile(
                stringResource(R.string.pp_key),
                stringResource(R.string.pp_key_saved_msg),
                R.drawable.ic_pp_lock,
                {},
                Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = keyText,
            onValueChange = { keyText = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.pp_paste_key)) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(28.dp),
            trailingIcon = {
                IconButton(onClick = {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0)
                        keyText = clip.getItemAt(0).coerceToText(ctx).toString().trim()
                }) {
                    Icon(painterResource(R.drawable.ic_pp_paste), contentDescription = stringResource(R.string.pp_paste_key))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(
                stringResource(R.string.pp_save_key),
                keyText.isNotBlank(),
                {
                    if (keyText.isNotBlank()) {
                        val firstReady = AiProvider.entries.none { it != provider && AiKeyStore.hasKey(ctx, it) }
                        AiKeyStore.save(ctx, provider, keyText)
                        if (firstReady) AiSettings.setProvider(ctx, provider)
                        keyText = ""
                        onChanged()
                    }
                },
                Modifier.weight(1f)
            )
            if (hasKey) {
                Pill(
                    stringResource(R.string.pp_remove_key),
                    false,
                    {
                        AiKeyStore.clear(ctx, provider)
                        onChanged()
                    },
                    Modifier.weight(1f)
                )
            }
        }
        Box(Modifier.fillMaxWidth()) {
            ValueTile(stringResource(R.string.pp_model), model, R.drawable.ic_ai, { menu = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                models.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(id) },
                        onClick = {
                            AiSettings.setModel(ctx, provider, id)
                            model = id
                            menu = false
                            onChanged()
                        }
                    )
                }
            }
        }
        Pill(
            stringResource(if (loading) R.string.pp_loading else R.string.pp_load_models),
            false,
            {
                val apiKey = AiKeyStore.load(ctx, provider)
                if (apiKey == null) {
                    status = ctx.getString(R.string.pp_no_key_msg)
                } else if (!loading) {
                    loading = true
                    status = null
                    scope.launch(Dispatchers.IO) {
                        val outcome = runCatching { AiClient.listModels(provider, apiKey) }
                        outcome.onSuccess { list ->
                            models = (listOf(model) + list).distinct()
                            status = ctx.getString(R.string.pp_models_loaded)
                        }
                        outcome.onFailure { status = it.message }
                        loading = false
                    }
                }
            },
            Modifier.fillMaxWidth()
        )
        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
        }
        Pill(
            stringResource(if (isDefault) R.string.pp_is_default else R.string.pp_make_default),
            isDefault,
            {
                AiSettings.setProvider(ctx, provider)
                onChanged()
            },
            Modifier.fillMaxWidth()
        )
        Pill(stringResource(R.string.pp_test), false, onTest, Modifier.fillMaxWidth())
    }
}

@Composable
fun AiScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<AiProvider?>(null) }
    var testing by remember { mutableStateOf<AiProvider?>(null) }
    var languageDialog by remember { mutableStateOf(false) }
    val default = remember(version) { AiSettings.provider(ctx) }
    val language = remember(version) { AiSettings.targetLanguage(ctx) }
    val ready = remember(version) { AiProvider.entries.filter { AiKeyStore.hasKey(ctx, it) } }

    BackHandler(enabled = editing != null) { editing = null }

    val current = editing
    if (current == null) {
        PocketScreen(stringResource(R.string.pp_ai), onClickBack) {
            SectionLabel(stringResource(R.string.pp_providers))
            TileRows(columns = 3, count = AiProvider.entries.size, spacing = 12.dp) { i ->
                val provider = AiProvider.entries[i]
                ProviderTile(provider, provider in ready, provider == default, { editing = provider }, Modifier.fillMaxWidth())
            }
            if (ready.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pp_default))
                ready.forEach { provider ->
                    DefaultRow(provider, AiSettings.model(ctx, provider), provider == default) {
                        AiSettings.setProvider(ctx, provider)
                        version++
                    }
                }
            }
            ValueTile(
                stringResource(R.string.pp_translate_to),
                language,
                R.drawable.ic_settings_languages,
                { languageDialog = true },
                Modifier.fillMaxWidth()
            )
        }
    } else {
        ProviderDetail(
            provider = current,
            version = version,
            onChanged = { version++ },
            onTest = { testing = current },
            onBack = { editing = null }
        )
    }

    if (languageDialog) {
        var text by remember { mutableStateOf(language) }
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(stringResource(R.string.pp_translate_to)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    AiSettings.setTargetLanguage(ctx, text)
                    version++
                    languageDialog = false
                }) { Text(stringResource(R.string.ai_save)) }
            },
            dismissButton = { TextButton(onClick = { languageDialog = false }) { Text(stringResource(R.string.ai_cancel)) } }
        )
    }

    testing?.let { target ->
        var outcome by remember(target) { mutableStateOf<String?>(null) }
        LaunchedEffect(target) {
            outcome = withContext(Dispatchers.IO) {
                runCatching { AiRunner.runWith(ctx, target, AiAction.ASK, "Reply with the single word: ok") }
                    .fold({ "OK: $it" }, { "Failed: " + (it.message ?: it.javaClass.simpleName) })
            }
        }
        AlertDialog(
            onDismissRequest = { testing = null },
            title = { Text(stringResource(R.string.pp_test)) },
            text = { Text(outcome ?: stringResource(R.string.ai_testing)) },
            confirmButton = { TextButton(onClick = { testing = null }) { Text(stringResource(R.string.ai_close)) } }
        )
    }
}
