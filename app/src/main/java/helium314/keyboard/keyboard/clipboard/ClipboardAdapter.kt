// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Settings
import java.io.File
import java.util.concurrent.Executors

class SquareFrame(context: Context) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}

class ClipboardAdapter(
    val clipboardLayoutParams: ClipboardLayoutParams,
    val keyEventListener: OnKeyEventListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var clipboardHistoryManager: ClipboardHistoryManager? = null
        set(value) {
            field = value
            refresh()
        }

    var pinnedIconResId = 0
    var itemBackgroundId = 0
    var itemTypeFace: Typeface? = null
    var itemTextColor = 0
    var itemTextSize = 0f

    private sealed interface Row {
        class Header(val title: Int, val count: Int) : Row
        class Item(val entry: ClipboardHistoryEntry) : Row
    }

    private var rows: List<Row> = emptyList()
    private val thumbnails = LruCache<Long, Bitmap>(48)
    private val loader = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        val manager = clipboardHistoryManager
        val entries = if (manager == null) emptyList()
            else (0 until manager.getHistorySize()).mapNotNull { manager.getHistoryEntry(it) }
        val recent = entries.filter { !it.isPinned }
        val pinned = entries.filter { it.isPinned }
        val list = mutableListOf<Row>()
        if (recent.isNotEmpty()) {
            list.add(Row.Header(R.string.clipboard_section_recent, recent.size))
            recent.forEach { list.add(Row.Item(it)) }
        }
        if (pinned.isNotEmpty()) {
            list.add(Row.Header(R.string.clipboard_section_pinned, pinned.size))
            pinned.forEach { list.add(Row.Item(it)) }
        }
        rows = list
        notifyDataSetChanged()
    }

    fun spanSize(position: Int): Int {
        val row = rows.getOrNull(position) ?: return SPAN_COUNT
        return if (row is Row.Item && row.entry.filename != null) IMAGE_SPAN else SPAN_COUNT
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int): Int = when (val row = rows[position]) {
        is Row.Header -> TYPE_HEADER
        is Row.Item -> if (row.entry.filename != null) TYPE_IMAGE else TYPE_TEXT
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val colors = Settings.getValues().mColors
        val margin = dp(context, 4)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = TextView(context)
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize * 0.62f)
                view.typeface = Typeface.create(itemTypeFace ?: Typeface.DEFAULT, Typeface.BOLD)
                view.setTextColor(colors.get(ColorType.KEY_HINT_TEXT))
                view.letterSpacing = 0.08f
                view.setPadding(dp(context, 10), dp(context, 12), dp(context, 8), dp(context, 4))
                view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                HeaderHolder(view)
            }
            TYPE_IMAGE -> {
                val frame = SquareFrame(context)
                frame.background = rounded(colors.get(ColorType.KEY_BACKGROUND), dp(context, 14).toFloat())
                val image = ImageView(context)
                image.scaleType = ImageView.ScaleType.CENTER_CROP
                val radius = dp(context, 14).toFloat()
                image.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                image.clipToOutline = true
                frame.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                val pin = ImageView(context)
                pin.setImageResource(pinnedIconResId)
                colors.setColor(pin, ColorType.CLIPBOARD_PIN)
                val pinParams = FrameLayout.LayoutParams(dp(context, 18), dp(context, 18))
                pinParams.gravity = Gravity.TOP or Gravity.END
                pinParams.setMargins(0, dp(context, 6), dp(context, 6), 0)
                frame.addView(pin, pinParams)
                val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                frame.layoutParams = params
                EntryHolder(frame, null, image, pin)
            }
            else -> {
                val row = LinearLayout(context)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.minimumHeight = dp(context, 46)
                row.background = rounded(colors.get(ColorType.KEY_BACKGROUND), dp(context, 16).toFloat())
                row.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
                val pin = ImageView(context)
                pin.setImageResource(pinnedIconResId)
                colors.setColor(pin, ColorType.CLIPBOARD_PIN)
                val pinParams = LinearLayout.LayoutParams(dp(context, 16), dp(context, 16))
                pinParams.rightMargin = dp(context, 10)
                row.addView(pin, pinParams)
                val text = TextView(context)
                text.typeface = itemTypeFace
                text.setTextColor(colors.get(ColorType.KEY_TEXT))
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize * 0.82f)
                text.maxLines = 3
                text.ellipsize = android.text.TextUtils.TruncateAt.END
                row.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                row.layoutParams = params
                EntryHolder(row, text, null, pin)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderHolder).bind(row)
            is Row.Item -> (holder as EntryHolder).bind(row.entry)
        }
    }

    private inner class HeaderHolder(val view: TextView) : RecyclerView.ViewHolder(view) {
        fun bind(row: Row.Header) {
            view.text = view.context.getString(row.title).uppercase() + "  " + row.count
        }
    }

    private inner class EntryHolder(
        view: View,
        val text: TextView?,
        val image: ImageView?,
        val pin: ImageView,
    ) : RecyclerView.ViewHolder(view) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(entry: ClipboardHistoryEntry) {
            itemView.tag = entry.id
            pin.visibility = if (entry.isPinned) View.VISIBLE else View.GONE
            text?.text = entry.text?.take(1000)
            if (image != null) loadThumbnail(entry, image)
            itemView.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) keyEventListener.onKeyDown(entry.id)
                false
            }
            itemView.setOnClickListener { keyEventListener.onKeyUp(entry.id) }
            itemView.setOnLongClickListener {
                showMenu(itemView, entry.id)
                true
            }
        }
    }

    private fun loadThumbnail(entry: ClipboardHistoryEntry, imageView: ImageView) {
        imageView.tag = entry.id
        val cached = thumbnails.get(entry.id)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }
        imageView.setImageDrawable(null)
        val name = entry.filename ?: return
        val file = File(ClipboardDao.clipFilesDir, name)
        loader.execute {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val options = BitmapFactory.Options()
            options.inSampleSize = (bounds.outWidth / THUMBNAIL_PX).coerceAtLeast(1)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bitmap != null) thumbnails.put(entry.id, bitmap)
            mainHandler.post {
                if (imageView.tag == entry.id) {
                    if (bitmap != null) imageView.setImageBitmap(bitmap)
                    else imageView.setImageResource(R.drawable.ic_pp_snippet)
                }
            }
        }
    }

    private fun showMenu(anchor: View, id: Long) {
        val manager = clipboardHistoryManager ?: return
        val entry = manager.findEntry(id) ?: return
        val context = anchor.context
        val bar = LinearLayout(context)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.background = rounded(Color.rgb(18, 20, 27), dp(context, 30).toFloat())
        bar.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6))
        bar.elevation = dp(context, 8).toFloat()
        val popup = PopupWindow(bar, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.isOutsideTouchable = true
        fun action(label: Int, icon: Int, tint: Int, onClick: () -> Unit) {
            val item = LinearLayout(context)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
            val glyph = ImageView(context)
            glyph.setImageResource(icon)
            glyph.setColorFilter(tint)
            item.addView(glyph, LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)))
            val caption = TextView(context)
            caption.setText(label)
            caption.setTextColor(tint)
            caption.textSize = 11f
            caption.gravity = Gravity.CENTER
            item.addView(caption, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            item.setOnClickListener {
                popup.dismiss()
                onClick()
            }
            bar.addView(item)
        }
        val paper = Color.rgb(247, 244, 237)
        if (entry.isPinned) action(R.string.clipboard_unpin, R.drawable.ic_pp_pin, paper) { manager.setPinned(id, false) }
        else action(R.string.clipboard_pin, R.drawable.ic_pp_pin, paper) { manager.setPinned(id, true) }
        action(R.string.clipboard_paste, R.drawable.ic_pp_paste, paper) { manager.pasteEntry(entry) }
        action(R.string.clipboard_delete, R.drawable.ic_pp_delete, Color.rgb(255, 120, 80)) { manager.removeById(id) }
        bar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(anchor, (anchor.width - bar.measuredWidth) / 2, -(anchor.height + bar.measuredHeight) / 2)
    }

    companion object {
        const val SPAN_COUNT = 6
        private const val IMAGE_SPAN = 2
        private const val TYPE_HEADER = 0
        private const val TYPE_TEXT = 1
        private const val TYPE_IMAGE = 2
        private const val THUMBNAIL_PX = 280
    }
}
