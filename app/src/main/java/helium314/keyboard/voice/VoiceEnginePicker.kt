package helium314.keyboard.voice

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

object VoiceEnginePicker {
    fun show(anchor: View) {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        val colors = Settings.getValues().mColors
        val background = ColorUtils.setAlphaComponent(colors.get(ColorType.MAIN_BACKGROUND), 255)
        val text = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 255)
        val accent = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 255)
        val card = ColorUtils.blendARGB(background, text, 0.14f)
        val current = VoiceSettings.engine(context)

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
        root.background = GradientDrawable().apply {
            setColor(card)
            cornerRadius = 20 * density
            setStroke((1 * density).toInt(), ColorUtils.blendARGB(card, text, 0.2f))
        }
        val popup = PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 12 * density

        fun row(engine: VoiceEngine, titleRes: Int, subRes: Int) {
            val selected = engine == current
            val item = LinearLayout(context)
            item.orientation = LinearLayout.HORIZONTAL
            item.gravity = Gravity.CENTER_VERTICAL
            item.setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            item.background = GradientDrawable().apply {
                setColor(if (selected) ColorUtils.setAlphaComponent(accent, 60) else Color.TRANSPARENT)
                cornerRadius = 14 * density
            }
            val labels = LinearLayout(context)
            labels.orientation = LinearLayout.VERTICAL
            val title = TextView(context)
            title.text = context.getString(titleRes)
            title.setTextColor(text)
            title.textSize = 15f
            val sub = TextView(context)
            sub.text = context.getString(subRes)
            sub.setTextColor(ColorUtils.setAlphaComponent(text, 170))
            sub.textSize = 12f
            labels.addView(title)
            labels.addView(sub)
            item.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val mark = TextView(context)
            mark.text = if (selected) "✓" else ""
            mark.setTextColor(accent)
            mark.textSize = 18f
            mark.setPadding((14 * density).toInt(), 0, 0, 0)
            item.addView(mark)
            item.setOnClickListener {
                VoiceSettings.setEngine(context, engine)
                if (engine == VoiceEngine.GROQ && AiKeyStore.load(context, AiProvider.GROQ) == null)
                    Toast.makeText(context, R.string.voice_no_key_using_device, Toast.LENGTH_LONG).show()
                popup.dismiss()
            }
            root.addView(item, LinearLayout.LayoutParams((240 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        row(VoiceEngine.GROQ, R.string.vz_whisper, R.string.vz_whisper_sub)
        row(VoiceEngine.DEVICE, R.string.vz_builtin, R.string.vz_builtin_sub)

        root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(anchor, -(root.measuredWidth - anchor.width), -(root.measuredHeight + anchor.height + (6 * density).toInt()))
    }
}
