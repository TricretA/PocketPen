package helium314.keyboard

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.WelcomeWizard
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
    @get:Rule val rule = createComposeRule()

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { Theme(dark = dark) { content() } }
        rule.mainClock.advanceTimeBy(600)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screens")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

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
    @Test fun onboarding() = shoot("onboarding") { WelcomeWizard({}, {}) }
}
