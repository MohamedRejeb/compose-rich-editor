package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Issue 809: pasting HTML whose CSS carried a number the platform could not parse crashed the
 * editor with a NumberFormatException.
 *
 * Cause: the CSS regexes used `\d`, which on Android also matches non-ASCII digits such as the
 * Arabic-Indic "١٥" (the other targets match ASCII digits only, so those cases pass there with
 * or without the fix), and the match was then handed to `toFloat()` or `toInt()`, which throw
 * on those digits, on values too large for an Int and on malformed decimals. Fix: the regexes
 * match ASCII digits only and a number that still fails to parse is ignored instead of thrown.
 * Reading those digits as numbers is covered by [CssNonAsciiDigitsTest].
 */
class Issue809CssNumberParsingTest {

    @Test
    fun `a size written with non ASCII digits is ignored`() {
        assertNull(CssEncoder.parseCssSize("١٥px"))
        assertEquals(TextUnit.Unspecified, CssEncoder.parseCssTextSize("١٥px"))
    }

    @Test
    fun `html with a non ASCII font size loads`() {
        val state = RichTextState()

        state.setHtml("""<p><span style="font-size: ١٥px; letter-spacing: ٢px">text</span></p>""")

        assertEquals("text", state.toText())
    }

    @Test
    fun `a color with non ASCII digits is ignored`() {
        assertNull(CssEncoder.parseCssColor("rgb(١٥, ١٥, ١٥)"))
        assertNull(CssEncoder.parseCssColor("rgba(١٥, ١٥, ١٥, ١)"))
    }

    @Test
    fun `a color channel too large for an Int is ignored`() {
        assertNull(CssEncoder.parseCssColor("rgb(99999999999, 0, 0)"))
        assertNull(CssEncoder.parseCssColor("rgba(99999999999, 0, 0, 1)"))
    }

    @Test
    fun `a malformed alpha is ignored`() {
        assertNull(CssEncoder.parseCssColor("rgba(0, 0, 0, 1.2.3)"))
        assertNull(CssEncoder.parseCssColor("rgba(0, 0, 0, .)"))
    }

    @Test
    fun `html with an unparseable color loads without the color`() {
        val state = RichTextState()

        state.setHtml("""<p><span style="color: rgb(99999999999, 0, 0)">text</span></p>""")

        assertEquals("text", state.toText())
    }
}
