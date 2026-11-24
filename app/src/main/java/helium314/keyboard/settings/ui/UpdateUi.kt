package helium314.keyboard.settings.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.update.UpdateChecker
import helium314.keyboard.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun rememberUpdater(checkOnStart: Boolean): UpdaterState {
    val ctx = LocalContext.current
    val state = remember { UpdaterState() }
    LaunchedEffect(checkOnStart) {
        if (checkOnStart && UpdateChecker.isDue(ctx)) {
            state.checking = true
            state.info = runCatching { withContext(Dispatchers.IO) { UpdateChecker.fetch() } }.getOrNull()
            UpdateChecker.markChecked(ctx)
            state.checking = false
            state.checked = true
        }
    }
    return state
}

private class UpdaterState {
    var info by mutableStateOf<UpdateInfo?>(null)
    var checking by mutableStateOf(false)
    var checked by mutableStateOf(false)
    var progress by mutableIntStateOf(-1)
}

@Composable
fun UpdateBanner() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberUpdater(true)
    val info = state.info ?: return
    SettingsCard {
        SettingsRow(
            stringResource(R.string.up_available),
            stringResource(R.string.up_available_sub, info.version),
            R.drawable.ic_pp_refresh,
            {
                if (state.progress < 0) startInstall(scope, ctx, state, info)
            }
        ) {
            Text(
                if (state.progress >= 0) "${state.progress}%" else stringResource(R.string.up_install),
                color = PP.Signal
            )
        }
    }
}

@Composable
fun UpdateSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberUpdater(false)
    var auto by remember { mutableStateOf(UpdateChecker.autoEnabled(ctx)) }
    val info = state.info
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
        SettingsCard {
            SettingsRow(
                stringResource(R.string.up_title),
                when {
                    !UpdateChecker.supported -> stringResource(R.string.up_debug)
                    state.checking -> stringResource(R.string.up_checking)
                    info != null -> stringResource(R.string.up_available_sub, info.version)
                    state.checked -> stringResource(R.string.up_latest, BuildConfig.VERSION_NAME)
                    else -> stringResource(R.string.up_idle)
                },
                R.drawable.ic_pp_refresh,
                {
                    if (!UpdateChecker.supported || state.checking) {
                    } else if (info != null) {
                        if (state.progress < 0) startInstall(scope, ctx, state, info)
                    } else {
                        scope.launch {
                            state.checking = true
                            val result = runCatching { withContext(Dispatchers.IO) { UpdateChecker.fetch() } }
                            state.info = result.getOrNull()
                            if (result.isFailure) Toast.makeText(ctx, R.string.up_failed, Toast.LENGTH_SHORT).show()
                            UpdateChecker.markChecked(ctx)
                            state.checking = false
                            state.checked = result.isSuccess
                        }
                    }
                }
            ) {
                Text(
                    when {
                        state.progress >= 0 -> "${state.progress}%"
                        info != null -> stringResource(R.string.up_install)
                        else -> stringResource(R.string.up_check)
                    },
                    color = PP.Signal
                )
            }
            RowDivider()
            SwitchRow(stringResource(R.string.up_auto_t), stringResource(R.string.up_auto_d), null, auto) {
                auto = it
                UpdateChecker.setAuto(ctx, it)
            }
        }
    }
}

private fun startInstall(scope: kotlinx.coroutines.CoroutineScope, ctx: android.content.Context, state: UpdaterState, info: UpdateInfo) {
    scope.launch {
        state.progress = 0
        val result = runCatching {
            withContext(Dispatchers.IO) { UpdateChecker.download(ctx, info) { p -> state.progress = p } }
        }
        state.progress = -1
        result.onSuccess { UpdateChecker.install(ctx, it) }
        result.onFailure { Toast.makeText(ctx, R.string.up_failed, Toast.LENGTH_LONG).show() }
    }
}
