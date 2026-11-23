package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import helium314.keyboard.handwriting.HandwritingEngine
import helium314.keyboard.handwriting.HandwritingSettings
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ui.Pill
import helium314.keyboard.settings.ui.PillRow
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.ToggleTile
import helium314.keyboard.settings.ui.ValueTile

private enum class HandwritingDialog { LANGUAGE, DOWNLOAD }

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

    PocketScreen(stringResource(R.string.pp_write), onClickBack) {
        ValueTile(
            stringResource(R.string.pp_language),
            languageName,
            R.drawable.ic_settings_languages,
            { dialog = HandwritingDialog.LANGUAGE },
            Modifier.fillMaxWidth()
        )
        TileRows(columns = 3, count = 3) { i ->
            when (i) {
                0 -> ToggleTile(stringResource(R.string.pp_auto_insert), R.drawable.ic_handwriting, autoInsert, {
                    HandwritingSettings.setAutoInsert(ctx, it)
                    version++
                }, Modifier.fillMaxWidth())
                1 -> ToggleTile(stringResource(R.string.pp_space_after), R.drawable.ic_pp_dot, addSpace, {
                    HandwritingSettings.setAddSpace(ctx, it)
                    version++
                }, Modifier.fillMaxWidth())
                else -> ToggleTile(stringResource(R.string.pp_palm), R.drawable.ic_pp_phone, palm, {
                    HandwritingSettings.setPalmRejection(ctx, it)
                    version++
                }, Modifier.fillMaxWidth())
            }
        }
        PillRow(
            options = listOf(
                stringResource(R.string.pp_fast) to HandwritingSettings.PAUSE_FAST,
                stringResource(R.string.pp_normal) to HandwritingSettings.PAUSE_NORMAL,
                stringResource(R.string.pp_slow) to HandwritingSettings.PAUSE_SLOW,
            ),
            selected = pause,
            onSelect = {
                HandwritingSettings.setPauseMs(ctx, it)
                version++
            }
        )
        Pill(stringResource(R.string.pp_download), true, { dialog = HandwritingDialog.DOWNLOAD }, Modifier.fillMaxWidth())
    }

    when (dialog) {
        HandwritingDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.pp_language),
            options = HandwritingSettings.languages,
            selected = languageTag,
            onSelect = {
                HandwritingSettings.setLanguageTag(ctx, it)
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
                title = { Text(stringResource(R.string.pp_download)) },
                text = { Text(status ?: "") },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_close)) } }
            )
        }

        null -> {}
    }
}
