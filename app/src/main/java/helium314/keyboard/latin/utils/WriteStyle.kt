package helium314.keyboard.latin.utils

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import helium314.keyboard.latin.settings.Settings

object WriteStyle {
    const val PREF = "pp_write_style"
    const val NORMAL = "normal"
    const val SAMPLE = "Hello 123"

    class Style(val id: String, val label: String, val transform: (String) -> String)

    private fun letters(upper: Int, lower: Int, digit: Int = -1, exceptions: Map<Int, Int> = emptyMap()): (String) -> String = { text ->
        val out = StringBuilder(text.length * 2)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            val mapped = when {
                exceptions.containsKey(cp) -> exceptions.getValue(cp)
                cp in 'A'.code..'Z'.code && upper >= 0 -> upper + (cp - 'A'.code)
                cp in 'a'.code..'z'.code && lower >= 0 -> lower + (cp - 'a'.code)
                cp in '0'.code..'9'.code && digit >= 0 -> digit + (cp - '0'.code)
                else -> cp
            }
            out.appendCodePoint(mapped)
        }
        out.toString()
    }

    private fun marked(mark: Char): (String) -> String = { text ->
        val out = StringBuilder(text.length * 2)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            out.appendCodePoint(cp)
            if (cp > ' '.code && cp != 0x200B && cp < 0x2E80 && cp != 0x0336 && cp != 0x0332) out.append(mark)
        }
        out.toString()
    }

    private val smallCaps = mapOf(
        'a' to 0x1D00, 'b' to 0x0299, 'c' to 0x1D04, 'd' to 0x1D05, 'e' to 0x1D07, 'f' to 0xA730, 'g' to 0x0262,
        'h' to 0x029C, 'i' to 0x026A, 'j' to 0x1D0A, 'k' to 0x1D0B, 'l' to 0x029F, 'm' to 0x1D0D, 'n' to 0x0274,
        'o' to 0x1D0F, 'p' to 0x1D18, 'q' to 0x01EB, 'r' to 0x0280, 's' to 0xA731, 't' to 0x1D1B, 'u' to 0x1D1C,
        'v' to 0x1D20, 'w' to 0x1D21, 'x' to 'x'.code, 'y' to 0x028F, 'z' to 0x1D22
    )

    private val wide: (String) -> String = { text ->
        val out = StringBuilder(text.length)
        text.forEach { c ->
            if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9') out.append((c.code + 0xFEE0).toChar()) else out.append(c)
        }
        out.toString()
    }

    private val small: (String) -> String = { text ->
        val out = StringBuilder(text.length)
        text.forEach { c -> out.appendCodePoint(smallCaps[c.lowercaseChar()]?.takeIf { c in 'a'..'z' || c in 'A'..'Z' } ?: c.code) }
        out.toString()
    }

    private val circled: (String) -> String = { text ->
        val out = StringBuilder(text.length)
        text.forEach { c ->
            when (c) {
                in 'A'..'Z' -> out.appendCodePoint(0x24B6 + (c - 'A'))
                in 'a'..'z' -> out.appendCodePoint(0x24D0 + (c - 'a'))
                in '1'..'9' -> out.appendCodePoint(0x2460 + (c - '1'))
                '0' -> out.appendCodePoint(0x24EA)
                else -> out.append(c)
            }
        }
        out.toString()
    }

    private val blackCircled: (String) -> String = { text ->
        val out = StringBuilder(text.length * 2)
        text.forEach { c ->
            when (c) {
                in 'A'..'Z' -> out.appendCodePoint(0x1F150 + (c - 'A'))
                in 'a'..'z' -> out.appendCodePoint(0x1F150 + (c - 'a'))
                in '1'..'9' -> out.appendCodePoint(0x2776 + (c - '1'))
                '0' -> out.appendCodePoint(0x24FF)
                else -> out.append(c)
            }
        }
        out.toString()
    }

    private val squared: (String) -> String = { text ->
        val out = StringBuilder(text.length * 2)
        text.forEach { c ->
            when (c) {
                in 'A'..'Z' -> out.appendCodePoint(0x1F130 + (c - 'A'))
                in 'a'..'z' -> out.appendCodePoint(0x1F130 + (c - 'a'))
                else -> out.append(c)
            }
        }
        out.toString()
    }

    private val blackSquared: (String) -> String = { text ->
        val out = StringBuilder(text.length * 2)
        text.forEach { c ->
            when (c) {
                in 'A'..'Z' -> out.appendCodePoint(0x1F170 + (c - 'A'))
                in 'a'..'z' -> out.appendCodePoint(0x1F170 + (c - 'a'))
                else -> out.append(c)
            }
        }
        out.toString()
    }

    private val italicH = mapOf('h'.code to 0x210E)
    private val scriptExceptions = mapOf(
        'B'.code to 0x212C, 'E'.code to 0x2130, 'F'.code to 0x2131, 'H'.code to 0x210B, 'I'.code to 0x2110,
        'L'.code to 0x2112, 'M'.code to 0x2133, 'R'.code to 0x211B, 'e'.code to 0x212F, 'g'.code to 0x210A, 'o'.code to 0x2134
    )
    private val frakturExceptions = mapOf(
        'C'.code to 0x212D, 'H'.code to 0x210C, 'I'.code to 0x2111, 'R'.code to 0x211C, 'Z'.code to 0x2128
    )
    private val doubleExceptions = mapOf(
        'C'.code to 0x2102, 'H'.code to 0x210D, 'N'.code to 0x2115, 'P'.code to 0x2119,
        'Q'.code to 0x211A, 'R'.code to 0x211D, 'Z'.code to 0x2124
    )

    val all = listOf(
        Style(NORMAL, "Normal") { it },
        Style("bold", "Bold", letters(0x1D400, 0x1D41A, 0x1D7CE)),
        Style("italic", "Italic", letters(0x1D434, 0x1D44E, -1, italicH)),
        Style("bold_italic", "Bold italic", letters(0x1D468, 0x1D482, 0x1D7CE)),
        Style("script", "Script", letters(0x1D49C, 0x1D4B6, -1, scriptExceptions)),
        Style("bold_script", "Bold script", letters(0x1D4D0, 0x1D4EA, 0x1D7CE)),
        Style("fraktur", "Fraktur", letters(0x1D504, 0x1D51E, -1, frakturExceptions)),
        Style("bold_fraktur", "Bold fraktur", letters(0x1D56C, 0x1D586, 0x1D7CE)),
        Style("double", "Double-struck", letters(0x1D538, 0x1D552, 0x1D7D8, doubleExceptions)),
        Style("sans", "Sans", letters(0x1D5A0, 0x1D5BA, 0x1D7E2)),
        Style("sans_bold", "Sans bold", letters(0x1D5D4, 0x1D5EE, 0x1D7EC)),
        Style("sans_italic", "Sans italic", letters(0x1D608, 0x1D622, 0x1D7E2)),
        Style("sans_bold_italic", "Sans bold italic", letters(0x1D63C, 0x1D656, 0x1D7EC)),
        Style("mono", "Monospace", letters(0x1D670, 0x1D68A, 0x1D7F6)),
        Style("circled", "Circled", circled),
        Style("black_circled", "Black circled", blackCircled),
        Style("squared", "Squared", squared),
        Style("black_squared", "Black squared", blackSquared),
        Style("small_caps", "Small caps", small),
        Style("wide", "Wide", wide),
        Style("strike", "Strikethrough", marked('̶')),
        Style("underline", "Underline", marked('̲')),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: all.first()

    fun current(context: Context): Style = byId(context.prefs().getString(PREF, NORMAL))

    private fun active(): Boolean {
        val attributes = Settings.getValues()?.mInputAttributes ?: return false
        return attributes.mIsGeneralTextInput && !attributes.mIsPasswordField
    }

    @JvmStatic
    fun style(context: Context, text: CharSequence): CharSequence {
        if (text.isEmpty()) return text
        val style = current(context)
        if (style.id == NORMAL || !active()) return text
        val source = text.toString()
        val styled = style.transform(source)
        if (styled == source) return text
        if (text !is Spanned) return styled
        val result = SpannableString(styled)
        for (span in text.getSpans(0, text.length, Any::class.java)) {
            val start = style.transform(source.substring(0, text.getSpanStart(span))).length
            val end = style.transform(source.substring(0, text.getSpanEnd(span))).length
            result.setSpan(span, start, end, text.getSpanFlags(span))
        }
        return result
    }
}
