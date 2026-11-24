// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.toolspanel

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.ai.AiKeyStore
import helium314.keyboard.ai.AiProvider
import helium314.keyboard.ai.AiSettings
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.PocketTheme
import helium314.keyboard.latin.utils.PocketThemes
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
import kotlin.math.hypot

@SuppressLint("ClickableViewAccessibility")
class ToolsPanelView(context: Context, private val host: Host) : FrameLayout(context) {

    interface Host {
        fun onTool(key: ToolbarKey)
        fun onToolKeep(key: ToolbarKey)
        fun onEditMode(editing: Boolean)
        fun onToolbarChanged()
        fun onToolbarFull()
    }

    private sealed class Drop {
        object Pinned : Drop()
        data class Toolbar(val index: Int, val lineX: Float) : Drop()
        data class Grid(val target: Int) : Drop()
    }

    private enum class Sub { TEXT, THEME, RESIZE, AI_MODEL }

    private val colors = Settings.getValues().mColors
    private val prefs = context.prefs()
    private val density = resources.displayMetrics.density
    private val background = ColorUtils.setAlphaComponent(colors.get(ColorType.MAIN_BACKGROUND), 255)
    private val textColor = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 255)
    private val pillColor = ColorUtils.blendARGB(background, textColor, 0.14f)
    private val activeColor = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 255)
    private val lineColor = ColorUtils.blendARGB(background, textColor, 0.35f)
    private val accent = activeColor
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var state = ToolsLayout.read(prefs)
    var editing = false
        private set

    private val tileViews = LinkedHashMap<ToolbarKey, View>()
    private val slotViews = LinkedHashMap<ToolbarKey, View>()
    private var pinnedSlot: View? = null
    private var slotRow: View? = null
    private var gridScroll: ScrollView? = null
    private var dragLayer: FrameLayout? = null
    private var ghost: View? = null
    private var indicator: View? = null
    private var dragKey: ToolbarKey? = null
    private var highlighted: View? = null
    private var highlightedBackground: Drawable? = null
    private var dots: DotsView? = null

    init {
        isClickable = true
        showNormal()
    }

    fun setBottomReserve(px: Int) {
        setPadding(0, 0, 0, px)
    }

    private fun dp(value: Int) = (value * density).toInt()

    private fun round(color: Int, radius: Int, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    private fun circle(color: Int, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    private fun tinted(res: Int, color: Int = textColor): Drawable? = context.getDrawable(res)?.mutate()?.apply { setTint(color) }

    private fun iconFor(key: ToolbarKey) = KeyboardIconsSet.instance.getNewDrawable(key.name, context)?.mutate()?.apply { setTint(textColor) }

    private fun labelFor(key: ToolbarKey) = ToolsLayout.label(context, key)

    private fun feedback(view: View) =
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS)

    private fun tile(icon: Drawable?, label: String, hint: String? = null, active: Boolean = false): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val fill = if (active) ColorUtils.blendARGB(pillColor, accent, 0.45f) else pillColor
        row.background = round(fill, dp(22))
        row.setPadding(dp(12), 0, dp(10), 0)
        val iconView = ImageView(context)
        iconView.setImageDrawable(icon)
        row.addView(iconView, LinearLayout.LayoutParams(dp(20), dp(20)))
        val texts = LinearLayout(context)
        texts.orientation = LinearLayout.VERTICAL
        texts.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(context)
        title.text = label
        title.setTextColor(textColor)
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        title.maxLines = 1
        title.ellipsize = TextUtils.TruncateAt.END
        texts.addView(title)
        if (hint != null) {
            val sub = TextView(context)
            sub.text = hint
            sub.setTextColor(ColorUtils.setAlphaComponent(textColor, 160))
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            sub.maxLines = 1
            sub.ellipsize = TextUtils.TruncateAt.END
            texts.addView(sub)
        }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = dp(10)
        row.addView(texts, lp)
        return row
    }

    private fun tile(key: ToolbarKey): View =
        tile(iconFor(key), labelFor(key), if (key == ToolbarKey.INCOGNITO) context.getString(R.string.tp_incognito_hint) else null)

    private fun specialTile(special: ToolSpecial): View = when (special) {
        ToolSpecial.TEXT -> tile(tinted(R.drawable.ic_pp_snippet), context.getString(R.string.pp_text_editing))
        ToolSpecial.THEME -> tile(tinted(R.drawable.ic_pp_drop), context.getString(R.string.tp_theme))
        ToolSpecial.RESIZE -> tile(tinted(R.drawable.ic_pp_size), context.getString(R.string.tp_resize))
        ToolSpecial.AI_MODEL -> tile(tinted(R.drawable.ic_ai), context.getString(R.string.tp_ai_model))
        ToolSpecial.SOUND -> tile(tinted(R.drawable.ic_pp_sound), context.getString(R.string.tp_sound), null, soundOn())
    }

    private fun soundOn() = prefs.getBoolean(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON)

    private fun resetState() {
        removeAllViews()
        tileViews.clear()
        slotViews.clear()
        pinnedSlot = null
        slotRow = null
        gridScroll = null
        dragLayer = null
        ghost = null
        indicator = null
        highlighted = null
        highlightedBackground = null
        dots = null
        setBackgroundColor(background)
    }

    private fun showNormal() {
        resetState()
        editing = false
        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL
        val pages = ToolsLayout.normalItems(state).chunked(ToolsLayout.PAGE_SIZE)

        val pager = RecyclerView(context)
        val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        pager.layoutManager = manager
        pager.adapter = PageAdapter(pages)
        pager.overScrollMode = OVER_SCROLL_NEVER
        PagerSnapHelper().attachToRecyclerView(pager)

        val bottom = FrameLayout(context)
        val dotsView = DotsView(context, pages.size.coerceAtLeast(1), accent, lineColor)
        dots = dotsView
        bottom.addView(dotsView, FrameLayout.LayoutParams(dp(80), dp(16), Gravity.CENTER))
        val pencil = FrameLayout(context)
        pencil.background = round(Color.TRANSPARENT, dp(16), dp(1), lineColor)
        val pencilIcon = ImageView(context)
        pencilIcon.setImageDrawable(tinted(R.drawable.ic_pp_edit))
        pencilIcon.scaleType = ImageView.ScaleType.CENTER
        pencil.addView(pencilIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        pencil.setOnClickListener { feedback(it); showEdit() }
        val pencilParams = FrameLayout.LayoutParams(dp(88), dp(30), Gravity.END or Gravity.CENTER_VERTICAL)
        pencilParams.marginEnd = dp(12)
        bottom.addView(pencil, pencilParams)
        column.addView(bottom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)))
        column.addView(pager, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        pager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val position = manager.findFirstVisibleItemPosition()
                val view = manager.findViewByPosition(position)
                val offset = if (view != null && view.width > 0) -view.left.toFloat() / view.width else 0f
                dotsView.setPosition(position + offset)
            }
        })
        addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private inner class PageAdapter(private val pages: List<List<PanelItem>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = pages.size.coerceAtLeast(1)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val page = LinearLayout(context)
            page.orientation = LinearLayout.VERTICAL
            page.setPadding(dp(8), dp(4), dp(8), 0)
            page.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
            return object : RecyclerView.ViewHolder(page) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val page = holder.itemView as LinearLayout
            page.removeAllViews()
            val items = pages.getOrNull(position).orEmpty()
            for (r in 0 until ToolsLayout.PAGE_SIZE / 2) {
                val row = LinearLayout(context)
                row.orientation = LinearLayout.HORIZONTAL
                for (c in 0..1) {
                    val item = items.getOrNull(r * 2 + c)
                    val cell = FrameLayout(context)
                    cell.setPadding(dp(4), 0, dp(4), 0)
                    if (item != null) {
                        val view = when (item) {
                            is PanelItem.Key -> tile(item.key)
                            is PanelItem.Special -> specialTile(item.special)
                        }
                        view.setOnClickListener { onItemClick(it, item) }
                        cell.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                }
                page.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
    }

    private fun onItemClick(view: View, item: PanelItem) {
        feedback(view)
        when (item) {
            is PanelItem.Key -> host.onTool(item.key)
            is PanelItem.Special -> when (item.special) {
                ToolSpecial.TEXT -> showSub(Sub.TEXT)
                ToolSpecial.THEME -> showSub(Sub.THEME)
                ToolSpecial.RESIZE -> showSub(Sub.RESIZE)
                ToolSpecial.AI_MODEL -> showSub(Sub.AI_MODEL)
                ToolSpecial.SOUND -> {
                    prefs.edit { putBoolean(Settings.PREF_SOUND_ON, !soundOn()) }
                    showNormal()
                }
            }
        }
    }

    private fun showSub(sub: Sub) {
        resetState()
        editing = false
        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL
        val header = LinearLayout(context)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(8), dp(4), dp(8), dp(2))
        val back = FrameLayout(context)
        back.background = circle(pillColor)
        val backIcon = ImageView(context)
        backIcon.scaleType = ImageView.ScaleType.CENTER
        backIcon.setImageDrawable(tinted(R.drawable.ic_pp_back))
        back.addView(backIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        back.setOnClickListener { feedback(it); showNormal() }
        header.addView(back, LinearLayout.LayoutParams(dp(34), dp(34)))
        val title = TextView(context)
        title.setTextColor(textColor)
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        title.text = when (sub) {
            Sub.TEXT -> context.getString(R.string.pp_text_editing)
            Sub.THEME -> context.getString(R.string.tp_theme)
            Sub.RESIZE -> context.getString(R.string.tp_height)
            Sub.AI_MODEL -> context.getString(R.string.tp_ai_model)
        }
        val titleParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        titleParams.marginStart = dp(12)
        header.addView(title, titleParams)
        column.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))
        val body = when (sub) {
            Sub.TEXT -> textBody()
            Sub.THEME -> themeBody()
            Sub.RESIZE -> resizeBody()
            Sub.AI_MODEL -> aiBody()
        }
        column.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun textBody(): View {
        val scroll = ScrollView(context)
        scroll.overScrollMode = OVER_SCROLL_NEVER
        val list = LinearLayout(context)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(8), dp(2), dp(8), dp(8))
        for (rowKeys in ToolsLayout.textPanelKeys.chunked(6)) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            for (i in 0 until 6) {
                val key = rowKeys.getOrNull(i)
                val cell = FrameLayout(context)
                cell.setPadding(dp(3), dp(3), dp(3), dp(3))
                if (key != null) {
                    val button = FrameLayout(context)
                    button.background = round(pillColor, dp(14))
                    val icon = ImageView(context)
                    icon.scaleType = ImageView.ScaleType.CENTER
                    icon.setImageDrawable(iconFor(key))
                    icon.contentDescription = labelFor(key)
                    button.addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    button.setOnClickListener { feedback(it); host.onToolKeep(key) }
                    cell.addView(button, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(42)))
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        scroll.addView(list, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        return scroll
    }

    private fun themeBody(): View {
        val scroll = HorizontalScrollView(context)
        scroll.overScrollMode = OVER_SCROLL_NEVER
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(4), dp(8), dp(4))
        val current = prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
        for (theme in PocketThemes.all) {
            val card = LinearLayout(context)
            card.orientation = LinearLayout.VERTICAL
            card.gravity = Gravity.CENTER_HORIZONTAL
            card.addView(SwatchView(context, theme, theme.id == current, accent), LinearLayout.LayoutParams(dp(92), dp(58)))
            val label = TextView(context)
            label.text = context.getString(theme.label)
            label.setTextColor(textColor)
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            label.maxLines = 1
            label.gravity = Gravity.CENTER
            card.addView(label, LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT))
            card.setOnClickListener {
                feedback(it)
                applyTheme(theme)
            }
            val params = LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT)
            row.addView(card, params)
        }
        scroll.addView(row, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return scroll
    }

    private fun applyTheme(theme: PocketTheme) {
        PocketThemes.apply(prefs, theme)
        val auto = prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT)
        val night = if (auto && !theme.dark) "ink" else theme.id
        prefs.edit {
            putString(Settings.PREF_THEME_COLORS, theme.id)
            putString(Settings.PREF_THEME_COLORS_NIGHT, night)
        }
        KeyboardSwitcher.getInstance().setThemeNeedsReload()
    }

    private fun resizeBody(): View {
        val keys = (0 until 4).map { createPrefKeyForBooleanSettings(Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, it, 2) }
        val start = prefs.getFloat(keys[0], Defaults.PREF_KEYBOARD_HEIGHT_SCALE[0]).coerceIn(0.7f, 1.4f)
        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL
        column.gravity = Gravity.CENTER
        column.setPadding(dp(24), 0, dp(24), dp(8))
        val value = TextView(context)
        value.setTextColor(textColor)
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        value.gravity = Gravity.CENTER
        value.text = "${(start * 100).toInt()}%"
        column.addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val bar = SeekBar(context)
        bar.max = 70
        bar.progress = (start * 100).toInt() - 70
        bar.progressTintList = ColorStateList.valueOf(accent)
        bar.thumbTintList = ColorStateList.valueOf(accent)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val scale = (progress + 70) / 100f
                value.text = "${progress + 70}%"
                if (fromUser) prefs.edit { keys.forEach { putFloat(it, scale) } }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                KeyboardSwitcher.getInstance().setThemeNeedsReload()
            }
        })
        val barParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
        barParams.topMargin = dp(8)
        column.addView(bar, barParams)
        return column
    }

    private fun aiBody(): View {
        val scroll = ScrollView(context)
        scroll.overScrollMode = OVER_SCROLL_NEVER
        val list = LinearLayout(context)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(8), dp(2), dp(8), dp(8))
        val ready = AiProvider.entries.filter { AiKeyStore.hasKey(context, it) }
        if (ready.isEmpty()) {
            val message = TextView(context)
            message.text = context.getString(R.string.tp_no_ai)
            message.setTextColor(textColor)
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            message.setPadding(dp(8), dp(8), dp(8), dp(8))
            list.addView(message)
            val open = tile(tinted(R.drawable.ic_pp_back), context.getString(R.string.tp_open_settings))
            open.setOnClickListener { feedback(it); host.onTool(ToolbarKey.SETTINGS) }
            list.addView(open, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))
        } else {
            val current = AiSettings.provider(context)
            for (provider in ready) {
                val row = LinearLayout(context)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val selected = provider == current
                row.background = round(if (selected) ColorUtils.blendARGB(pillColor, accent, 0.3f) else pillColor, dp(20))
                row.setPadding(dp(10), 0, dp(12), 0)
                val badge = FrameLayout(context)
                badge.background = circle(provider.brandColor)
                val logo = ImageView(context)
                logo.scaleType = ImageView.ScaleType.CENTER
                logo.setImageDrawable(tinted(provider.logo, Color.WHITE))
                badge.addView(logo, FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
                row.addView(badge, LinearLayout.LayoutParams(dp(28), dp(28)))
                val name = TextView(context)
                name.text = provider.label + "  " + AiSettings.model(context, provider)
                name.setTextColor(textColor)
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                name.maxLines = 1
                name.ellipsize = TextUtils.TruncateAt.END
                val nameParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                nameParams.marginStart = dp(10)
                row.addView(name, nameParams)
                if (selected) {
                    val check = ImageView(context)
                    check.setImageDrawable(tinted(R.drawable.ic_pp_check, accent))
                    row.addView(check, LinearLayout.LayoutParams(dp(20), dp(20)))
                }
                row.setOnClickListener {
                    feedback(it)
                    AiSettings.setProvider(context, provider)
                    showSub(Sub.AI_MODEL)
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40))
                params.bottomMargin = dp(4)
                list.addView(row, params)
            }
            val chips = HorizontalScrollView(context)
            chips.isHorizontalScrollBarEnabled = false
            val chipRow = LinearLayout(context)
            chipRow.orientation = LinearLayout.HORIZONTAL
            val currentModel = AiSettings.model(context, current)
            for (model in current.suggestedModels) {
                val chip = TextView(context)
                chip.text = model
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                chip.setTextColor(if (model == currentModel) Color.WHITE else textColor)
                chip.background = round(if (model == currentModel) accent else pillColor, dp(16))
                chip.setPadding(dp(12), dp(6), dp(12), dp(6))
                chip.setOnClickListener {
                    feedback(it)
                    AiSettings.setModel(context, current, model)
                    showSub(Sub.AI_MODEL)
                }
                val chipParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                chipParams.marginEnd = dp(6)
                chipRow.addView(chip, chipParams)
            }
            chips.addView(chipRow)
            val chipsParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            chipsParams.topMargin = dp(6)
            list.addView(chips, chipsParams)
        }
        scroll.addView(list, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        return scroll
    }

    private fun slot(key: ToolbarKey?): View {
        val frame = FrameLayout(context)
        val icon = ImageView(context)
        icon.scaleType = ImageView.ScaleType.CENTER
        if (key != null) icon.setImageDrawable(iconFor(key))
        frame.addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun showEdit() {
        val scrollY = gridScroll?.scrollY ?: 0
        resetState()
        editing = true
        host.onEditMode(true)

        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL

        val header = LinearLayout(context)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(8), dp(4), dp(8), dp(4))

        val check = FrameLayout(context)
        check.background = circle(accent)
        val checkIcon = ImageView(context)
        checkIcon.scaleType = ImageView.ScaleType.CENTER
        checkIcon.setImageDrawable(tinted(R.drawable.ic_pp_check, Color.WHITE))
        check.addView(checkIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        check.setOnClickListener { feedback(it); exitEdit() }
        header.addView(check, LinearLayout.LayoutParams(dp(38), dp(38)))

        val pill = LinearLayout(context)
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.background = round(Color.TRANSPARENT, dp(22), dp(2), accent)
        pill.setPadding(dp(4), dp(2), dp(4), dp(2))
        for (key in state.toolbar) {
            val view = slot(key)
            view.background = round(pillColor, dp(16))
            val params = LinearLayout.LayoutParams(0, dp(30), 1f)
            params.setMargins(dp(2), 0, dp(2), 0)
            pill.addView(view, params)
            slotViews[key] = view
            attachDrag(view, key, true)
        }
        val pillParams = LinearLayout.LayoutParams(0, dp(38), 1f)
        pillParams.setMargins(dp(8), 0, dp(8), 0)
        header.addView(pill, pillParams)
        slotRow = pill

        val pinned = slot(state.pinned)
        pinned.background = circle(pillColor, dp(2), if (state.pinned == null) lineColor else accent)
        header.addView(pinned, LinearLayout.LayoutParams(dp(38), dp(38)))
        pinnedSlot = pinned
        state.pinned?.let { attachDrag(pinned, it, true) }
        column.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))

        val hint = TextView(context)
        hint.text = context.getString(R.string.pp_tools_hint)
        hint.setTextColor(textColor)
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        hint.gravity = Gravity.CENTER
        column.addView(hint, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26)))

        val scroll = ScrollView(context)
        scroll.overScrollMode = OVER_SCROLL_NEVER
        val grid = LinearLayout(context)
        grid.orientation = LinearLayout.VERTICAL
        grid.setPadding(dp(8), dp(2), dp(8), dp(8))
        for (rowKeys in state.panel.chunked(2)) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            for (i in 0..1) {
                val key = rowKeys.getOrNull(i)
                val cell = FrameLayout(context)
                cell.setPadding(dp(4), dp(3), dp(4), dp(3))
                if (key != null) {
                    val view = tile(key)
                    cell.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(42)))
                    tileViews[key] = view
                    attachDrag(view, key, false)
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        scroll.addView(grid, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        column.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        gridScroll = scroll
        scroll.post { scroll.scrollTo(0, scrollY) }

        addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val layer = FrameLayout(context)
        dragLayer = layer
        val line = View(context)
        line.setBackgroundColor(accent)
        line.visibility = GONE
        layer.addView(line, FrameLayout.LayoutParams(dp(3), dp(34)))
        indicator = line
        addView(layer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun exitEdit() {
        ToolsLayout.write(prefs, state)
        host.onToolbarChanged()
        host.onEditMode(false)
        showNormal()
    }

    private fun attachDrag(view: View, key: ToolbarKey, fromHeader: Boolean) {
        var downX = 0f
        var downY = 0f
        var started = false
        val longPress = Runnable {
            started = true
            view.parent?.requestDisallowInterceptTouchEvent(true)
            startDrag(view, key, fromHeader, downX, downY)
        }
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    started = false
                    view.postDelayed(longPress, 220)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (started) moveGhost(event.rawX, event.rawY)
                    else if (hypot(event.rawX - downX, event.rawY - downY) > touchSlop) view.removeCallbacks(longPress)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    if (started) endDrag(view, event.rawX, event.rawY, event.actionMasked == MotionEvent.ACTION_UP)
                    started = false
                }
            }
            true
        }
    }

    private fun screenOf(view: View): FloatArray {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return floatArrayOf(loc[0].toFloat(), loc[1].toFloat())
    }

    private fun startDrag(view: View, key: ToolbarKey, fromHeader: Boolean, rawX: Float, rawY: Float) {
        val layer = dragLayer ?: return
        feedback(view)
        dragKey = key
        val copy = if (fromHeader) {
            FrameLayout(context).apply {
                background = circle(accent)
                val icon = ImageView(context)
                icon.scaleType = ImageView.ScaleType.CENTER
                icon.setImageDrawable(KeyboardIconsSet.instance.getNewDrawable(key.name, context)?.mutate()?.apply { setTint(Color.WHITE) })
                addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        } else tile(key)
        val width = if (fromHeader) dp(42) else view.width
        val height = if (fromHeader) dp(42) else view.height
        copy.alpha = 0.95f
        copy.elevation = dp(8).toFloat()
        layer.addView(copy, FrameLayout.LayoutParams(width, height))
        ghost = copy
        view.alpha = 0.3f
        moveGhost(rawX, rawY)
    }

    private fun moveGhost(rawX: Float, rawY: Float) {
        val g = ghost ?: return
        val origin = screenOf(this)
        g.x = rawX - origin[0] - g.layoutParams.width / 2f
        g.y = rawY - origin[1] - g.layoutParams.height / 2f
        val scroll = gridScroll
        if (scroll != null) {
            val s = screenOf(scroll)
            val edge = dp(36)
            if (rawY > s[1] + scroll.height - edge) scroll.scrollBy(0, dp(10))
            else if (rawY < s[1] + edge && rawY > s[1] - dp(40)) scroll.scrollBy(0, -dp(10))
        }
        val key = dragKey ?: return
        updateHint(dropTarget(key, rawX, rawY), origin)
    }

    private fun clearHighlight() {
        val view = highlighted
        if (view != null) view.background = highlightedBackground
        highlighted = null
        highlightedBackground = null
    }

    private fun highlight(view: View?, drawable: Drawable) {
        if (view == null) return
        highlighted = view
        highlightedBackground = view.background
        view.background = drawable
    }

    private fun updateHint(target: Drop?, origin: FloatArray) {
        clearHighlight()
        val line = indicator
        line?.visibility = GONE
        when (target) {
            is Drop.Pinned -> highlight(pinnedSlot, circle(ColorUtils.blendARGB(pillColor, accent, 0.5f), dp(3), accent))
            is Drop.Toolbar -> if (line != null) {
                val row = slotRow
                val rowTop = if (row != null) screenOf(row)[1] - origin[1] else 0f
                line.x = target.lineX - origin[0] - line.layoutParams.width / 2f
                line.y = rowTop + ((row?.height ?: dp(38)) - line.layoutParams.height) / 2f
                line.visibility = VISIBLE
            }
            is Drop.Grid -> {
                val key = state.panel.getOrNull(target.target)
                highlight(key?.let { tileViews[it] }, round(ColorUtils.blendARGB(pillColor, accent, 0.35f), dp(22), dp(2), accent))
            }
            null -> {}
        }
    }

    private fun contains(view: View?, x: Float, y: Float, slack: Int = 0): Boolean {
        if (view == null || view.width == 0) return false
        val o = screenOf(view)
        return x >= o[0] - slack && x <= o[0] + view.width + slack && y >= o[1] - slack && y <= o[1] + view.height + slack
    }

    private fun endDrag(view: View, rawX: Float, rawY: Float, drop: Boolean) {
        val key = dragKey
        ghost?.let { (it.parent as? ViewGroup)?.removeView(it) }
        ghost = null
        dragKey = null
        view.alpha = 1f
        clearHighlight()
        indicator?.visibility = GONE
        if (key == null || !drop) return
        val newState = computeDrop(key, rawX, rawY) ?: return
        if (newState == state) return
        state = newState
        ToolsLayout.write(prefs, state)
        post { showEdit() }
    }

    private fun dropTarget(key: ToolbarKey, x: Float, y: Float): Drop? {
        if (contains(pinnedSlot, x, y, dp(6))) return Drop.Pinned
        if (contains(slotRow, x, y, dp(8))) {
            val remaining = slotViews.filterKeys { it != key }.values.toList()
            var index = 0
            for (v in remaining) {
                val o = screenOf(v)
                if (x > o[0] + v.width / 2f) index++
            }
            val row = slotRow
            val rowLeft = if (row != null) screenOf(row)[0] else 0f
            val rowRight = if (row != null) rowLeft + row.width else 0f
            val lineX = when {
                remaining.isEmpty() -> (rowLeft + rowRight) / 2f
                index == 0 -> screenOf(remaining.first())[0]
                index >= remaining.size -> screenOf(remaining.last())[0] + remaining.last().width
                else -> {
                    val before = remaining[index - 1]
                    val after = remaining[index]
                    (screenOf(before)[0] + before.width + screenOf(after)[0]) / 2f
                }
            }
            return Drop.Toolbar(index, lineX)
        }
        if (contains(gridScroll, x, y)) {
            var best = -1
            var bestDistance = Float.MAX_VALUE
            state.panel.forEachIndexed { i, k ->
                val v = tileViews[k] ?: return@forEachIndexed
                val o = screenOf(v)
                val d = hypot(x - (o[0] + v.width / 2f), y - (o[1] + v.height / 2f))
                if (d < bestDistance) {
                    bestDistance = d
                    best = i
                }
            }
            return Drop.Grid(best.coerceAtLeast(0))
        }
        return null
    }

    private fun computeDrop(key: ToolbarKey, x: Float, y: Float): ToolsState? {
        val target = dropTarget(key, x, y) ?: return null
        var toolbar = state.toolbar.filter { it != key }
        var pinned = state.pinned.takeIf { it != key }
        var panel = state.panel.filter { it != key }
        val wasInToolbar = key in state.toolbar
        when (target) {
            is Drop.Pinned -> {
                state.pinned?.takeIf { it != key }?.let { panel = listOf(it) + panel }
                pinned = key
            }
            is Drop.Toolbar -> {
                if (!wasInToolbar && toolbar.size >= ToolsLayout.MAX_TOOLBAR) {
                    host.onToolbarFull()
                    return null
                }
                val index = target.index.coerceIn(0, toolbar.size)
                toolbar = toolbar.toMutableList().apply { add(index, key) }
            }
            is Drop.Grid -> {
                val insertAt = target.target.coerceAtMost(panel.size)
                panel = panel.toMutableList().apply { add(insertAt, key) }
            }
        }
        return ToolsState(toolbar, pinned, panel)
    }

    private class SwatchView(context: Context, private val theme: PocketTheme, private val selected: Boolean, private val ring: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            paint.style = Paint.Style.FILL
            paint.color = theme.background.toInt()
            rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d)
            canvas.drawRoundRect(rect, 10 * d, 10 * d, paint)
            val padding = 8 * d
            val gap = 2.5f * d
            val rows = 3
            val cols = 8
            val keyW = (w - 2 * padding - (cols - 1) * gap) / cols
            val keyH = (h - 2 * padding - (rows - 1) * gap) / rows
            paint.color = theme.key.toInt()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val left = padding + c * (keyW + gap)
                    val top = padding + r * (keyH + gap)
                    rect.set(left, top, left + keyW, top + keyH)
                    if (r == rows - 1 && c >= cols - 2) paint.color = theme.accent.toInt() else paint.color = theme.key.toInt()
                    canvas.drawRoundRect(rect, 2 * d, 2 * d, paint)
                }
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (selected) 2.5f * d else 1f * d
            paint.color = if (selected) ring else ColorUtils.setAlphaComponent(Color.GRAY, 120)
            rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d)
            canvas.drawRoundRect(rect, 10 * d, 10 * d, paint)
        }
    }

    private class DotsView(context: Context, private val count: Int, private val active: Int, private val inactive: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var position = 0f

        fun setPosition(value: Float) {
            position = value
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val radius = resources.displayMetrics.density * 3f
            val gap = radius * 4f
            val total = (count - 1) * gap
            val startX = width / 2f - total / 2f
            for (i in 0 until count) {
                val nearness = (1f - kotlin.math.abs(position - i)).coerceIn(0f, 1f)
                paint.color = ColorUtils.blendARGB(inactive, active, nearness)
                canvas.drawCircle(startX + i * gap, height / 2f, radius, paint)
            }
        }
    }
}
