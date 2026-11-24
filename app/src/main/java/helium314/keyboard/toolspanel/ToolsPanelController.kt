// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.toolspanel

import android.view.Gravity
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

    private fun show(ime: LatinIME) {
        val switcher = KeyboardSwitcher.getInstance()
        val host = switcher.toolsHost ?: return
        val height = switcher.wrapperView?.height ?: 0
        if (height <= 0) return
        val view = ToolsPanelView(host.context, object : ToolsPanelView.Host {
            override fun onTool(key: ToolbarKey) {
                dismiss()
                val code = getCodeForToolbarKey(key)
                if (code != KeyCode.UNSPECIFIED)
                    ime.onCodeInput(code, Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE, false)
            }

            override fun onEditMode(editing: Boolean) {
                val current = panel ?: return
                val strip = switcher.suggestionStrip?.height ?: 0
                val params = current.layoutParams as? FrameLayout.LayoutParams ?: return
                params.height = height + if (editing) strip else 0
                current.layoutParams = params
            }

            override fun onToolbarChanged() {
                switcher.suggestionStrip?.rebuildToolbarKeys()
            }

            override fun onToolbarFull() {
                switcher.showToast(ime.getString(R.string.pp_toolbar_full), true)
            }
        })
        panel = view
        host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM))
        switcher.suggestionStrip?.setToolsOpen(true)
    }
}
