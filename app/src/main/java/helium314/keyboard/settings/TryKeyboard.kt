package helium314.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.delay

@Composable
fun TryKeyboardHost(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    DisposableEffect(open) {
        if (!open) return@DisposableEffect onDispose { }
        val prefs = ctx.prefs()
        val handler = Handler(Looper.getMainLooper())
        val reload = Runnable { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || !key.startsWith("pp_try")) {
                handler.removeCallbacks(reload)
                handler.postDelayed(reload, 600)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            handler.removeCallbacks(reload)
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(open) {
        if (open) {
            delay(200)
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)) {
                imm.showInputMethodPicker()
            } else {
                runCatching { focusRequester.requestFocus() }
                keyboard?.show()
            }
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { content() }
            if (open) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(stringResource(R.string.try_hint)) },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).clickable { open = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_pp_close),
                            contentDescription = stringResource(R.string.try_close),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        if (!open) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 22.dp)
                    .shadow(8.dp, CircleShape)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PP.Signal)
                    .clickable { open = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_pp_keyboard),
                    contentDescription = stringResource(R.string.try_open),
                    tint = PP.Ink,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
