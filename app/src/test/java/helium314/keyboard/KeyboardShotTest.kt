package helium314.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-xxhdpi",
    shadows = [ShadowInputMethodService::class, ShadowLooper::class]
)
class KeyboardShotTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun shoot(name: String, theme: String) {
        latinIME.prefs().edit {
            putString(Settings.PREF_THEME_COLORS, theme)
            putString(Settings.PREF_THEME_COLORS_NIGHT, theme)
            putBoolean(Settings.PREF_THEME_DAY_NIGHT, false)
        }
        val switcher = KeyboardSwitcher.getInstance()
        val view = switcher.onCreateInputView(latinIME, true)
        switcher.reloadMainKeyboard()
        val metrics = latinIME.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.AT_MOST)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screens")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun paper() = shoot("keyboard_paper", "paper")
    @Test fun ink() = shoot("keyboard_ink", "ink")
    @Test fun signal() = shoot("keyboard_signal", "signal")
}
