package helium314.keyboard.settings.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.handwriting.HandwritingEngine
import helium314.keyboard.handwriting.HandwritingSettings
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ui.ChoiceRow
import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SettingsRow
import helium314.keyboard.settings.ui.SwitchRow
import helium314.keyboard.settings.ui.ValueRow

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
        SettingsCard {
            ValueRow(
                stringResource(R.string.sb_hw_lang_t),
                stringResource(R.string.sb_hw_lang_d),
                languageName,
                R.drawable.ic_settings_languages
            ) { dialog = HandwritingDialog.LANGUAGE }
            RowDivider()
            SettingsRow(
                stringResource(R.string.sb_hw_pack_t, languageName),
                stringResource(R.string.sb_hw_pack_d),
                R.drawable.ic_pp_cloud,
                { dialog = HandwritingDialog.DOWNLOAD }
            ) {
                Text(stringResource(R.string.sb_hw_pack_go), color = helium314.keyboard.latin.utils.PP.Signal)
            }
        }

        SectionLabel(stringResource(R.string.sb_hw_sec_insert))
        SettingsCard {
            SwitchRow(
                stringResource(R.string.sb_hw_auto_t),
                stringResource(R.string.sb_hw_auto_d),
                R.drawable.ic_handwriting,
                autoInsert
            ) {
                HandwritingSettings.setAutoInsert(ctx, it)
                version++
            }
            RowDivider()
            SwitchRow(
                stringResource(R.string.sb_hw_space_t),
                stringResource(R.string.sb_hw_space_d),
                R.drawable.ic_pp_dot,
                addSpace
            ) {
                HandwritingSettings.setAddSpace(ctx, it)
                version++
            }
        }

        SectionLabel(stringResource(R.string.sb_hw_wait_t))
        SettingsCard {
            val choices = listOf(
                Triple(R.string.sb_hw_fast, R.string.sb_hw_fast_d, HandwritingSettings.PAUSE_FAST),
                Triple(R.string.sb_hw_normal, R.string.sb_hw_normal_d, HandwritingSettings.PAUSE_NORMAL),
                Triple(R.string.sb_hw_slow, R.string.sb_hw_slow_d, HandwritingSettings.PAUSE_SLOW),
            )
            choices.forEachIndexed { index, (title, description, value) ->
                if (index > 0) RowDivider()
                ChoiceRow(stringResource(title), stringResource(description), null, pause == value) {
                    HandwritingSettings.setPauseMs(ctx, value)
                    version++
                }
            }
        }

        SectionLabel(stringResource(R.string.sb_hw_sec_touch))
        SettingsCard {
            SwitchRow(
                stringResource(R.string.sb_hw_palm_t),
                stringResource(R.string.sb_hw_palm_d),
                R.drawable.ic_pp_phone,
                palm
            ) {
                HandwritingSettings.setPalmRejection(ctx, it)
                version++
            }
        }
    }

    when (dialog) {
        HandwritingDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.sb_hw_lang_t),
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
                title = { Text(stringResource(R.string.sb_hw_pack_go)) },
                text = { Text(status ?: "") },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ai_close)) } }
            )
        }

        null -> {}
    }
}
