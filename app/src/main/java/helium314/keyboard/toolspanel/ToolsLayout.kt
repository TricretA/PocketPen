// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.toolspanel

import android.content.SharedPreferences
import android.content.Context
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.getStringResourceOrName
import helium314.keyboard.latin.common.Constants.Separators
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.ToolbarKey.*
import helium314.keyboard.latin.utils.getEnabledToolbarKeys
import helium314.keyboard.latin.utils.getPinnedToolbarKeys

data class ToolsState(val toolbar: List<ToolbarKey>, val pinned: ToolbarKey?, val panel: List<ToolbarKey>)

object ToolsLayout {
    const val MAX_TOOLBAR = 6
    const val PAGE_SIZE = 6
    const val PREF_ORDER = "pp_tools_order"
    const val PREF_MIGRATED = "pp_toolbar_v2"

    val defaultToolbar = listOf(EMOJI, AI, CLIPBOARD, UNDO, SETTINGS)
    val defaultPinned = VOICE
    val defaultPanel = listOf(
        ONE_HANDED, DPAD, FLOATING, HANDWRITING, NUMPAD, SPLIT, INCOGNITO, AUTOCORRECT,
        SELECT_ALL, COPY, CUT, PASTE, REDO
    )
    val catalog = defaultToolbar + defaultPinned + defaultPanel

    fun read(prefs: SharedPreferences): ToolsState {
        val toolbar = getEnabledToolbarKeys(prefs).filter { it in catalog }.distinct()
        val pinned = getPinnedToolbarKeys(prefs).firstOrNull { it in catalog && it !in toolbar }
        val saved = prefs.getString(PREF_ORDER, "").orEmpty().split(",").mapNotNull { name ->
            try { ToolbarKey.valueOf(name) } catch (_: IllegalArgumentException) { null }
        }
        val panel = (saved + catalog).filter { it in catalog && it !in toolbar && it != pinned }.distinct()
        return ToolsState(toolbar, pinned, panel)
    }

    fun write(prefs: SharedPreferences, state: ToolsState) {
        prefs.edit {
            putString(Settings.PREF_TOOLBAR_KEYS, encode(state.toolbar))
            putString(Settings.PREF_PINNED_TOOLBAR_KEYS, encode(listOfNotNull(state.pinned)))
            putString(PREF_ORDER, state.panel.joinToString(",") { it.name })
        }
    }

    fun label(context: Context, key: ToolbarKey): String =
        if (key == DPAD) context.getString(R.string.pp_text_editing)
        else key.name.lowercase().getStringResourceOrName("", context)

    fun reset(prefs: SharedPreferences) = write(prefs, ToolsState(defaultToolbar, defaultPinned, defaultPanel))

    private fun encode(enabled: List<ToolbarKey>): String {
        val rest = ToolbarKey.entries.filterNot { it in enabled }
        return (enabled.map { it.name + Separators.KV + true } + rest.map { it.name + Separators.KV + false })
            .joinToString(Separators.ENTRY)
    }
}
