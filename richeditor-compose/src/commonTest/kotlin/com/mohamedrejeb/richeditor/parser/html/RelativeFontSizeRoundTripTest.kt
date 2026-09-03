package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A relative font size must stay relative across a save and a load.
 *
 * CSS `em` and Compose [androidx.compose.ui.unit.TextUnit.Em] mean the same thing: a multiple of
 * the surrounding font size. The decoder used to resolve `em` against a hard coded 16 base, so
 * `font-size: 2em` came back as `32.sp` and was re-encoded as `font-size: 32.0px`: one save and
 * load turned every relative size in the document absolute, and the text stopped following the
 * font size the editor is rendered with.
 *
 * Headings make this visible without any authored CSS, because a level's visuals are `em` based
 * ([com.mohamedrejeb.richeditor.model.HeadingStyle.H1] is `2.em`) and reach the HTML as a span
 * style whenever the paragraph carrying them is not itself a heading tag.
 */
class RelativeFontSizeRoundTripTest {

    private fun fontSizeAt(state: RichTextState, offset: Int) =
        state.also { it.selection = TextRange(offset) }.currentSpanStyle.fontSize

    @Test
    fun `an em font size decodes as a relative size`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 2em;">big</span></p>""")

        val fontSize = fontSizeAt(state, 1)
        assertEquals(TextUnitType.Em, fontSize.type, "2em must stay relative")
        assertEquals(2.em, fontSize)
    }

    @Test
    fun `a percent font size decodes as a relative size`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 150%;">big</span></p>""")

        val fontSize = fontSizeAt(state, 1)
        assertEquals(TextUnitType.Em, fontSize.type, "150% must stay relative")
        assertEquals(1.5.em, fontSize)
    }

    @Test
    fun `a rem font size decodes as em and is written back as em`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 1.5rem;">big</span></p>""")

        assertEquals(1.5.em, fontSizeAt(state, 1), "rem is read as em, root relativity and all")
        assertEquals(
            """<p><span style="font-size: 1.5em;">big</span></p>""",
            state.toHtml(),
            "the rem unit is deliberately not preserved: Compose has nothing to write it back as",
        )
    }

    @Test
    fun `a px font size stays absolute`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 20px;">big</span></p>""")

        val fontSize = fontSizeAt(state, 1)
        assertEquals(TextUnitType.Sp, fontSize.type, "px is absolute and must stay absolute")
        assertEquals(20.sp, fontSize)
    }

    @Test
    fun `an em font size survives the html round trip`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 2em;">big</span></p>""")

        val html = state.toHtml()
        assertEquals("""<p><span style="font-size: 2.0em;">big</span></p>""", html)

        val fresh = RichTextState()
        fresh.setHtml(html)
        assertEquals(2.em, fontSizeAt(fresh, 1))
        assertEquals(html, fresh.toHtml(), "the second round trip must not change the document")
    }

    /**
     * `0.8em` is the size the encoder reserves for `<small>`, so a span authored at that size comes
     * back as a `<small>` tag on the first save. Accepted markup normalization, not data loss: the
     * size is identical either way and the document is stable from the second save on. `80%` decodes
     * to the same size and normalizes the same way.
     */
    @Test
    fun `a span sized like small is written back as a small tag`() {
        val state = RichTextState()
        state.setHtml("""<p><span style="font-size: 0.8em;">x</span></p>""")

        assertEquals(0.8f.em, fontSizeAt(state, 1))

        val html = state.toHtml()
        assertEquals("<p><small>x</small></p>", html)

        val fresh = RichTextState()
        fresh.setHtml(html)
        assertEquals(0.8f.em, fontSizeAt(fresh, 1))
        assertEquals(html, fresh.toHtml(), "stable from the second save on")

        val fromPercent = RichTextState()
        fromPercent.setHtml("""<p><span style="font-size: 80%;">x</span></p>""")
        assertEquals(html, fromPercent.toHtml())
    }

    @Test
    fun `the font size of a heading survives being written as a span`() {
        val state = RichTextState()
        state.setHtml("<h1>title</h1>")
        assertEquals(2.em, fontSizeAt(state, 1), "a heading keeps its em size on its spans")

        // The shape a paragraph carrying heading visuals serializes as.
        val asSpan = """<p><span style="font-size: 2.0em;"><b>title</b></span></p>"""
        val fresh = RichTextState()
        fresh.setHtml(asSpan)
        assertEquals(2.em, fontSizeAt(fresh, 1))
        assertEquals(asSpan, fresh.toHtml())
    }

    @Test
    fun `a heading document reports the same font size live and reloaded`() {
        val live = RichTextState()
        live.setHtml("""<h1>title</h1><p><span style="font-size: 1.5em;">big</span> normal</p>""")

        val fresh = RichTextState()
        fresh.setHtml(live.toHtml())
        assertEquals(live.annotatedString.text, fresh.annotatedString.text)

        for (offset in 0..live.annotatedString.text.length) {
            fresh.selection = TextRange(0)
            assertEquals(
                fontSizeAt(live, offset),
                fontSizeAt(fresh, offset),
                "font size at $offset",
            )
        }

        assertEquals(
            TextUnitType.Em,
            fontSizeAt(fresh, 8).type,
            "the relative size must still be relative after a reload",
        )
    }
}
