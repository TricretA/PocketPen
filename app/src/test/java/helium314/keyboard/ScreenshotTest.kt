package helium314.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.screens.AiScreen
import helium314.keyboard.settings.screens.ClipboardManagerScreen
import helium314.keyboard.settings.screens.HandwritingScreen
import helium314.keyboard.settings.screens.HomeScreen
import helium314.keyboard.settings.screens.LookScreen
import helium314.keyboard.settings.screens.MoreScreen
import helium314.keyboard.settings.screens.TypingScreen
import helium314.keyboard.settings.screens.VoiceScreen
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { Theme(dark = dark) { content() } }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val metrics = view.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        rule.waitForIdle()
        val bitmap = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screens")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun baseline() = shoot("baseline") { androidx.compose.material3.Text("hello") }
    @Test fun home() = shoot("home") { HomeScreen({}, {}) }
    @Test fun homeDark() = shoot("home_dark", dark = true) { HomeScreen({}, {}) }
    @Test fun look() = shoot("look") { LookScreen({}, {}) }
    @Test fun lookDark() = shoot("look_dark", dark = true) { LookScreen({}, {}) }
    @Test fun typing() = shoot("typing") { TypingScreen {} }
    @Test fun more() = shoot("more") { MoreScreen({}, {}) }
    @Test fun ai() = shoot("ai") { AiScreen {} }
    @Test fun voice() = shoot("voice") { VoiceScreen {} }
    @Test fun write() = shoot("write") { HandwritingScreen {} }
    @Test fun clipboard() = shoot("clipboard") { ClipboardManagerScreen {} }
}
