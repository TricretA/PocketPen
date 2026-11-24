// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.toolspanel

import android.view.Gravity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup
import android.widget.FrameLayout
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.getCodeForToolbarKey

object ToolsPanelController {
    private var panel: ToolsPanelView? = null

    @JvmStatic
    fun isOpen() = panel != null

    @JvmStatic
    fun toggle(ime: LatinIME) {
        if (panel != null) dismiss() else show(ime)
    }

    @JvmStatic
    fun reset() {
        panel = null
    }

    @JvmStatic
    fun dismiss() {
        val current = panel ?: return
        panel = null
        (current.parent as? ViewGroup)?.removeView(current)
        KeyboardSwitcher.getInstance().suggestionStrip?.setToolsOpen(false)
    }

    private fun place(params: FrameLayout.LayoutParams, host: ViewGroup, editing: Boolean): Boolean {
        val switcher = KeyboardSwitcher.getInstance()
        val wrapper = switcher.wrapperView ?: return false
        if (wrapper.height <= 0) return false
        val strip = if (editing) (switcher.suggestionStrip?.height ?: 0) else 0
        val hostLocation = IntArray(2)
        val wrapperLocation = IntArray(2)
        host.getLocationOnScreen(hostLocation)
        wrapper.getLocationOnScreen(wrapperLocation)
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = wrapperLocation[0] - hostLocation[0]
        params.topMargin = wrapperLocation[1] - hostLocation[1] - strip
        params.width = wrapper.width
        params.height = wrapper.height + strip
        return true
    }

    private fun reserve(host: ViewGroup): Int {
        val inset = ViewCompat.getRootWindowInsets(host)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val minimum = (40 * host.resources.displayMetrics.density).toInt()
        return if (inset > 0) inset else minimum
    }

    private fun show(ime: LatinIME) {
        val switcher = KeyboardSwitcher.getInstance()
        val host = switcher.toolsHost ?: return
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        if (!place(params, host, false)) return
        val view = ToolsPanelView(host.context, object : ToolsPanelView.Host {
            override fun onTool(key: ToolbarKey) {
                dismiss()
                send(ime, key)
            }

            override fun onToolKeep(key: ToolbarKey) {
                send(ime, key)
            }

            override fun onEditMode(editing: Boolean) {
                val current = panel ?: return
                val layoutParams = current.layoutParams as? FrameLayout.LayoutParams ?: return
                if (place(layoutParams, host, editing)) current.layoutParams = layoutParams
                current.setBottomReserve(reserve(host))
            }

            override fun onToolbarChanged() {
                switcher.suggestionStrip?.rebuildToolbarKeys()
            }

            override fun onToolbarFull() {
                switcher.showToast(ime.getString(R.string.pp_toolbar_full), true)
            }
        })
        panel = view
        view.setBottomReserve(reserve(host))
        host.addView(view, params)
        switcher.suggestionStrip?.setToolsOpen(true)
    }

    private fun send(ime: LatinIME, key: ToolbarKey) {
        val code = getCodeForToolbarKey(key)
        if (code != KeyCode.UNSPECIFIED)
            ime.onCodeInput(code, Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE, false)
    }
}
