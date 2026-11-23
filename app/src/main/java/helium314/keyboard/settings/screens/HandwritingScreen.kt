package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import helium314.keyboard.handwriting.HandwritingEngine
import helium314.keyboard.handwriting.HandwritingSettings
import helium314.keyboard.latin.R
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference

private enum class HandwritingDialog { LANGUAGE, PAUSE, DOWNLOAD }

@Composable
fun HandwritingScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<HandwritingDialog?>(null) }
    val languageTag = remember(version) { HandwritingSettings.languageTag(ctx) }
    val autoInsert = remember(version) { HandwritingSettings.autoInsert(ctx) }
    val addSpace = remember(version) { HandwritingSettings.addSpace(ctx) }
    val pause = remember(version) { HandwritingSettings.pauseMs(ctx) }
    val palm = remember(version) { HandwritingSettings.palmRejection(ctx) }
    val languageName = HandwritingSettings.languages.firstOrNull { it.second == languageTag }?.first ?: languageTag

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_handwriting),
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(Modifier.verticalScroll(rememberScrollState()).then(Modifier.padding(innerPadding))) {
                Preference(
                    name = stringResource(R.string.hw_language),
                    description = languageName,
                    onClick = { dialog = HandwritingDialog.LANGUAGE }
                )
                Preference(
                    name = stringResource(R.string.hw_auto_insert),
                    description = stringResource(if (autoInsert) R.string.hw_auto_insert_on else R.string.hw_auto_insert_off),
                    onClick = {
                        HandwritingSettings.setAutoInsert(ctx, !autoInsert)
                        version++
                    }
                )
                Preference(
                    name = stringResource(R.string.hw_add_space),
                    description = stringResource(if (addSpace) R.string.hw_on else R.string.hw_off),
                    onClick = {
                        HandwritingSettings.setAddSpace(ctx, !addSpace)
                        version++
                    }
                )
                Preference(
                    name = stringResource(R.string.hw_pause),
                    description = stringResource(pauseLabel(pause)),
                    onClick = { dialog = HandwritingDialog.PAUSE }
                )
                Preference(
                    name = stringResource(R.string.hw_palm_rejection),
                    description = stringResource(if (palm) R.string.hw_on else R.string.hw_off),
                    onClick = {
                        HandwritingSettings.setPalmRejection(ctx, !palm)
                        version++
                    }
                )
                Preference(
                    name = stringResource(R.string.hw_download_model),
                    onClick = { dialog = HandwritingDialog.DOWNLOAD }
                )
                Text(
                    text = stringResource(R.string.hw_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    when (dialog) {
        HandwritingDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.hw_language),
            options = HandwritingSettings.languages,
            selected = languageTag,
            onSelect = {
                HandwritingSettings.setLanguageTag(ctx, it)
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        HandwritingDialog.PAUSE -> ChoiceDialog(
            title = stringResource(R.string.hw_pause),
            options = listOf(
                stringResource(R.string.hw_pause_fast) to HandwritingSettings.PAUSE_FAST.toString(),
                stringResource(R.string.hw_pause_normal) to HandwritingSettings.PAUSE_NORMAL.toString(),
                stringResource(R.string.hw_pause_slow) to HandwritingSettings.PAUSE_SLOW.toString(),
            ),
            selected = pause.toString(),
            onSelect = {
                HandwritingSettings.setPauseMs(ctx, it.toInt())
                version++
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        HandwritingDialog.DOWNLOAD -> {
            var status by remember { mutableStateOf<String?>(null) }
            val engine = remember { HandwritingEngine(languageTag) }
            DisposableEffect(Unit) {
                status = ctx.getString(R.string.hw_preparing)
                engine.prepare(
                    onDownloading = { status = ctx.getString(R.string.hw_downloading) },
                    onReady = { status = ctx.getString(R.string.hw_download_done) },
                    onFailed = { status = it }
                )
                onDispose { engine.close() }
            }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.hw_download_model)) },
                text = { Text(status ?: "") },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_close)) } }
            )
        }

        null -> {}
    }
}

private fun pauseLabel(millis: Int) = when {
    millis <= HandwritingSettings.PAUSE_FAST -> R.string.hw_pause_fast
    millis >= HandwritingSettings.PAUSE_SLOW -> R.string.hw_pause_slow
    else -> R.string.hw_pause_normal
}
