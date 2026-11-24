package helium314.keyboard.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings as KeyboardSettings
import helium314.keyboard.latin.utils.PP
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ui.KeyCap
import helium314.keyboard.settings.ui.KeyboardPreview
import helium314.keyboard.settings.ui.Pill
import helium314.keyboard.settings.ui.ThemeSwatch
import helium314.keyboard.settings.ui.TileRows
import helium314.keyboard.settings.ui.previewColorsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val lookChoices = listOf(
    "paper" to R.string.theme_name_paper,
    "ink" to R.string.theme_name_ink,
    "signal" to R.string.theme_name_signal,
    "ocean" to R.string.theme_name_ocean,
    "forest" to R.string.theme_name_forest,
    "pink" to R.string.theme_name_pink,
)

@Composable
fun WelcomeWizard(
    close: () -> Unit,
    finish: () -> Unit,
    startStep: Int? = null,
    animate: Boolean = true
) {
    val ctx = LocalContext.current
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    fun determineStep(): Int = when {
        !UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm) -> 0
        !UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm) -> 1
        else -> 2
    }
    var step by rememberSaveable { mutableIntStateOf(startStep ?: determineStep()) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    LaunchedEffect(step) {
        if (step == 1 && startStep == null)
            scope.launch {
                while (step == 1 && !UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)) {
                    delay(50)
                }
                step = 2
            }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        step = determineStep()
    }
    var chosen by remember { mutableStateOf("paper") }

    Theme(dark = true) {
        Column(
            Modifier
                .fillMaxSize()
                .background(PP.Ink)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..2) {
                    Box(
                        Modifier
                            .width(if (i == step) 36.dp else 12.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (i <= step) PP.Signal else PP.InkLine)
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (step) {
                    0 -> ToggleIllustration(animate)
                    1 -> SwitchIllustration(animate)
                    else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TileRows(columns = 3, count = lookChoices.size, spacing = 12.dp) { i ->
                            val (id, label) = lookChoices[i]
                            ThemeSwatch(
                                label = stringResource(label),
                                colors = previewColorsFor(id),
                                selected = chosen == id,
                                onClick = {
                                    chosen = id
                                    ctx.prefs().edit {
                                        putString(KeyboardSettings.PREF_THEME_COLORS, id)
                                        putString(KeyboardSettings.PREF_THEME_COLORS_NIGHT, id)
                                    }
                                    KeyboardSwitcher.getInstance().setThemeNeedsReload()
                                }
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(
                    when (step) {
                        0 -> R.string.pp_ob_enable
                        1 -> R.string.pp_ob_switch
                        else -> R.string.pp_ob_look
                    }
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = PP.Paper,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 22.dp)
            )
            Pill(
                label = stringResource(
                    when (step) {
                        0 -> R.string.pp_ob_enable_button
                        1 -> R.string.pp_ob_switch_button
                        else -> R.string.pp_ob_done
                    }
                ),
                selected = true,
                onClick = {
                    when (step) {
                        0 -> {
                            val intent = Intent()
                            intent.action = Settings.ACTION_INPUT_METHOD_SETTINGS
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            launcher.launch(intent)
                        }
                        1 -> imm.showInputMethodPicker()
                        else -> finish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            )
        }
    }
}

@Composable
private fun ToggleIllustration(animate: Boolean) {
    val t = if (animate) {
        val transition = rememberInfiniteTransition(label = "toggle")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "knob"
        )
        animated
    } else 1f
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Image(painterResource(R.drawable.pp_mark), contentDescription = null, modifier = Modifier.size(132.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(PP.InkRaised)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.pp_mark), contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.width(76.dp).height(44.dp)) {
                val trackColor = if (t > 0.5f) PP.Signal else PP.InkLine
                drawRoundRect(trackColor, Offset.Zero, size, CornerRadius(size.height / 2f))
                val knobR = size.height * 0.36f
                val cx = size.height / 2f + (size.width - size.height) * t
                drawCircle(if (t > 0.5f) PP.Ink else PP.Paper, knobR, Offset(cx, size.height / 2f))
            }
        }
    }
}

@Composable
private fun SwitchIllustration(animate: Boolean) {
    val t = if (animate) {
        val transition = rememberInfiniteTransition(label = "bob")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "bob"
        )
        animated
    } else 0.5f
    Box(contentAlignment = Alignment.TopEnd) {
        KeyboardPreview(
            colors = previewColorsFor("paper"),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(32.dp))
        )
        KeyCap(
            onClick = {},
            modifier = Modifier.padding(14.dp).offset(y = (t * 8f).dp).size(72.dp),
            color = PP.Signal,
            lip = Color(0xFFB93405),
            corner = 22.dp
        ) {
            Icon(
                painterResource(R.drawable.ic_pp_keyboard),
                contentDescription = null,
                tint = PP.Ink,
                modifier = Modifier.align(Alignment.Center).size(34.dp)
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WelcomeWizard({}) {}
}
