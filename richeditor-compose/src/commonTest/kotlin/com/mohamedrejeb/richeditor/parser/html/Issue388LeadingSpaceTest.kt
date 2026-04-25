package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for #388 (leading whitespace inside paragraph content).
 *
 * The HTML spec says runs of ASCII whitespace inside block content collapse
 * to a single space, but the non-breaking space (U+00A0, `&nbsp;`) is not
 * part of the collapsible set. The parser must therefore:
 *  - collapse spaces, tabs, newlines, etc. inside a paragraph;
 *  - NOT collapse or trim non-breaking spaces, so authors can use them to
 *    preserve leading or repeated visual whitespace (e.g. indentation).
 */
class Issue388LeadingSpaceTest {

    @Test
    fun `setHtml collapses ASCII leading whitespace inside a paragraph`() {
        val state = RichTextState()
        state.setHtml("<p>   hello</p>")

        // Browser-equivalent behavior: ASCII spaces collapse to nothing at the
        // start of a block.
        assertEquals("hello", state.toText())
    }

    @Test
    fun `setHtml preserves nbsp at the start of a paragraph`() {
        val state = RichTextState()
        state.setHtml("<p>&nbsp;hello</p>")

        assertEquals(" hello", state.toText())
    }

    @Test
    fun `setHtml preserves multiple consecutive nbsp inside a paragraph`() {
        val state = RichTextState()
        state.setHtml("<p>&nbsp;&nbsp;&nbsp;hello</p>")

        assertEquals("   hello", state.toText())
    }

    @Test
    fun `setHtml preserves nbsp interleaved with regular spaces`() {
        val state = RichTextState()
        // Authors typically pad with leading nbsp then a real space before the word.
        state.setHtml("<p>&nbsp;&nbsp; hello</p>")

        assertEquals("   hello", state.toText())
    }

    @Test
    fun `toHtml round-trips nbsp inside a paragraph`() {
        val state = RichTextState()
        state.setHtml("<p>&nbsp;&nbsp;hello</p>")

        val html = state.toHtml()
        // Either &nbsp; or the raw U+00A0 char is acceptable per HTML spec.
        // Reject the obvious lossy case (no nbsp at all) and re-decode to
        // confirm the leading whitespace round-trips through the encoder.
        assertTrue(
            "&nbsp;" in html || " " in html,
            "expected toHtml to retain nbsp, got: $html"
        )
        val roundTrip = RichTextState().also { it.setHtml(html) }
        assertEquals("  hello", roundTrip.toText())
    }
}
