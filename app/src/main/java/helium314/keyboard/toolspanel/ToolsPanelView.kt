// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.toolspanel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
import kotlin.math.hypot

@SuppressLint("ClickableViewAccessibility")
class ToolsPanelView(context: Context, private val host: Host) : FrameLayout(context) {

    interface Host {
        fun onTool(key: ToolbarKey)
        fun onEditMode(editing: Boolean)
        fun onToolbarChanged()
        fun onToolbarFull()
    }

    private val colors = Settings.getValues().mColors
    private val prefs = context.prefs()
    private val density = resources.displayMetrics.density
    private val background = ColorUtils.setAlphaComponent(colors.get(ColorType.MAIN_BACKGROUND), 255)
    private val textColor = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 255)
    private val pillColor = ColorUtils.blendARGB(background, textColor, 0.14f)
    private val lineColor = ColorUtils.blendARGB(background, textColor, 0.35f)
    private val accent = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 255)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var state = ToolsLayout.read(prefs)
    var editing = false
        private set

    private val tileViews = LinkedHashMap<ToolbarKey, View>()
    private val slotViews = LinkedHashMap<ToolbarKey, View>()
    private var pinnedSlot: View? = null
    private var gridScroll: ScrollView? = null
    private var dragLayer: FrameLayout? = null
    private var ghost: View? = null
    private var dragKey: ToolbarKey? = null
    private var dots: DotsView? = null
    private var slotRow: View? = null

    init {
        isClickable = true
        showNormal()
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

    private fun iconFor(key: ToolbarKey) = KeyboardIconsSet.instance.getNewDrawable(key.name, context)?.mutate()?.apply { setTint(textColor) }

    private fun labelFor(key: ToolbarKey) = ToolsLayout.label(context, key)

    private fun feedback(view: View) =
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS)

    private fun tile(key: ToolbarKey): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = round(pillColor, dp(28))
        row.setPadding(dp(18), 0, dp(14), 0)
        val icon = ImageView(context)
        icon.setImageDrawable(iconFor(key))
        row.addView(icon, LinearLayout.LayoutParams(dp(24), dp(24)))
        val label = TextView(context)
        label.text = labelFor(key)
        label.setTextColor(textColor)
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = dp(14)
        row.addView(label, lp)
        return row
    }

    private fun slot(key: ToolbarKey?): View {
        val frame = FrameLayout(context)
        val icon = ImageView(context)
        icon.scaleType = ImageView.ScaleType.CENTER
        if (key != null) icon.setImageDrawable(iconFor(key))
        frame.addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun showNormal() {
        removeAllViews()
        editing = false
        tileViews.clear(); slotViews.clear(); pinnedSlot = null; gridScroll = null; dragLayer = null; ghost = null
        setBackgroundColor(background)

        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL
        val pages = state.panel.chunked(ToolsLayout.PAGE_SIZE)

        val pager = RecyclerView(context)
        val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        pager.layoutManager = manager
        pager.adapter = PageAdapter(pages)
        pager.overScrollMode = OVER_SCROLL_NEVER
        PagerSnapHelper().attachToRecyclerView(pager)
        column.addView(pager, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = FrameLayout(context)
        val dotsView = DotsView(context, pages.size.coerceAtLeast(1), accent, lineColor)
        dots = dotsView
        bottom.addView(dotsView, FrameLayout.LayoutParams(dp(80), dp(20), Gravity.CENTER))
        val pencil = FrameLayout(context)
        pencil.background = round(Color.TRANSPARENT, dp(20), dp(1), lineColor)
        val pencilIcon = ImageView(context)
        pencilIcon.setImageDrawable(context.getDrawable(R.drawable.ic_pp_edit)?.mutate()?.apply { setTint(textColor) })
        pencilIcon.scaleType = ImageView.ScaleType.CENTER
        pencil.addView(pencilIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        pencil.setOnClickListener { feedback(it); showEdit() }
        val pencilParams = FrameLayout.LayoutParams(dp(110), dp(38), Gravity.END or Gravity.CENTER_VERTICAL)
        pencilParams.marginEnd = dp(14)
        bottom.addView(pencil, pencilParams)
        column.addView(bottom, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(46)))

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

    private inner class PageAdapter(private val pages: List<List<ToolbarKey>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = pages.size.coerceAtLeast(1)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val page = LinearLayout(context)
            page.orientation = LinearLayout.VERTICAL
            page.setPadding(dp(10), dp(6), dp(10), dp(2))
            page.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
            return object : RecyclerView.ViewHolder(page) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val page = holder.itemView as LinearLayout
            page.removeAllViews()
            val keys = pages.getOrNull(position).orEmpty()
            for (r in 0 until ToolsLayout.PAGE_SIZE / 2) {
                val row = LinearLayout(context)
                row.orientation = LinearLayout.HORIZONTAL
                for (c in 0..1) {
                    val key = keys.getOrNull(r * 2 + c)
                    val cell = FrameLayout(context)
                    cell.setPadding(dp(4), dp(4), dp(4), dp(4))
                    if (key != null) {
                        val view = tile(key)
                        view.setOnClickListener { feedback(it); host.onTool(key) }
                        cell.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                }
                page.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
    }

    private fun showEdit() {
        val scrollY = gridScroll?.scrollY ?: 0
        removeAllViews()
        editing = true
        host.onEditMode(true)
        tileViews.clear(); slotViews.clear()
        setBackgroundColor(background)

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
        checkIcon.setImageDrawable(context.getDrawable(R.drawable.ic_pp_check)?.mutate()?.apply { setTint(Color.WHITE) })
        check.addView(checkIcon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        check.setOnClickListener { feedback(it); exitEdit() }
        header.addView(check, LinearLayout.LayoutParams(dp(40), dp(40)))

        val pill = LinearLayout(context)
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.background = round(Color.TRANSPARENT, dp(24), dp(2), accent)
        pill.setPadding(dp(4), dp(2), dp(4), dp(2))
        for (key in state.toolbar) {
            val view = slot(key)
            view.background = round(pillColor, dp(18))
            val params = LinearLayout.LayoutParams(0, dp(32), 1f)
            params.setMargins(dp(2), 0, dp(2), 0)
            pill.addView(view, params)
            slotViews[key] = view
            attachDrag(view, key, true)
        }
        val pillParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        pillParams.setMargins(dp(8), 0, dp(8), 0)
        header.addView(pill, pillParams)
        slotRow = pill

        val pinned = slot(state.pinned)
        pinned.background = circle(pillColor, dp(2), if (state.pinned == null) lineColor else accent)
        header.addView(pinned, LinearLayout.LayoutParams(dp(40), dp(40)))
        pinnedSlot = pinned
        state.pinned?.let { attachDrag(pinned, it, true) }
        column.addView(header, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52)))

        val hint = TextView(context)
        hint.text = context.getString(R.string.pp_tools_hint)
        hint.setTextColor(textColor)
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        hint.gravity = Gravity.CENTER
        column.addView(hint, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(32)))

        val scroll = ScrollView(context)
        scroll.overScrollMode = OVER_SCROLL_NEVER
        val grid = LinearLayout(context)
        grid.orientation = LinearLayout.VERTICAL
        grid.setPadding(dp(10), dp(4), dp(10), dp(10))
        for (rowKeys in state.panel.chunked(2)) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            for (i in 0..1) {
                val key = rowKeys.getOrNull(i)
                val cell = FrameLayout(context)
                cell.setPadding(dp(4), dp(4), dp(4), dp(4))
                if (key != null) {
                    val view = tile(key)
                    cell.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52)))
                    tileViews[key] = view
                    attachDrag(view, key, false)
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        scroll.addView(grid, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        column.addView(scroll, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        gridScroll = scroll
        scroll.post { scroll.scrollTo(0, scrollY) }

        addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val layer = FrameLayout(context)
        dragLayer = layer
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
        val width = if (fromHeader) dp(44) else view.width
        val height = if (fromHeader) dp(44) else view.height
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
        val scroll = gridScroll ?: return
        val s = screenOf(scroll)
        val edge = dp(36)
        if (rawY > s[1] + scroll.height - edge) scroll.scrollBy(0, dp(10))
        else if (rawY < s[1] + edge && rawY > s[1] - dp(40)) scroll.scrollBy(0, -dp(10))
    }

    private fun contains(view: View?, x: Float, y: Float, slack: Int = 0): Boolean {
        if (view == null || view.width == 0) return false
        val o = screenOf(view)
        return x >= o[0] - slack && x <= o[0] + view.width + slack && y >= o[1] - slack && y <= o[1] + view.height + slack
    }

    private fun endDrag(view: View, rawX: Float, rawY: Float, drop: Boolean) {
        val key = dragKey
        ghost?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        ghost = null
        dragKey = null
        view.alpha = 1f
        if (key == null || !drop) return
        val newState = computeDrop(key, rawX, rawY) ?: return
        if (newState == state) return
        state = newState
        ToolsLayout.write(prefs, state)
        post { showEdit() }
    }

    private fun computeDrop(key: ToolbarKey, x: Float, y: Float): ToolsState? {
        var toolbar = state.toolbar.filter { it != key }
        var pinned = state.pinned.takeIf { it != key }
        var panel = state.panel.filter { it != key }
        val wasInToolbar = key in state.toolbar
        when {
            contains(pinnedSlot, x, y, dp(6)) -> {
                state.pinned?.takeIf { it != key }?.let { panel = listOf(it) + panel }
                pinned = key
            }
            contains(slotRow, x, y, dp(8)) -> {
                if (!wasInToolbar && toolbar.size >= ToolsLayout.MAX_TOOLBAR) {
                    host.onToolbarFull()
                    return null
                }
                var index = 0
                for ((k, v) in slotViews) {
                    if (k == key) continue
                    val o = screenOf(v)
                    if (x > o[0] + v.width / 2f) index++
                }
                index = index.coerceIn(0, toolbar.size)
                toolbar = toolbar.toMutableList().apply { add(index, key) }
            }
            contains(gridScroll, x, y) -> {
                var best = -1
                var bestDistance = Float.MAX_VALUE
                state.panel.forEachIndexed { i, k ->
                    val v = tileViews[k] ?: return@forEachIndexed
                    val o = screenOf(v)
                    val d = hypot(x - (o[0] + v.width / 2f), y - (o[1] + v.height / 2f))
                    if (d < bestDistance) { bestDistance = d; best = i }
                }
                val target = best.coerceAtLeast(0)
                val insertAt = if (best < 0) panel.size else target.coerceAtMost(panel.size)
                panel = panel.toMutableList().apply { add(insertAt, key) }
            }
            else -> return null
        }
        return ToolsState(toolbar, pinned, panel)
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
