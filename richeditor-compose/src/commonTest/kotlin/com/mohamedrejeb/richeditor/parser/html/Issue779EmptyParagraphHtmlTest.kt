package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression pins for the HTML-import half of #779: `setHtml` silently dropped
 * explicitly empty `<p></p>` (and heading) elements.
 *
 * The parser preemptively adds a spare blank paragraph after closing a
 * non-blank block element, and a following block open recycles any blank
 * current paragraph. An explicitly closed empty `<p>` left its paragraph
 * blank, so the next block open recycled it and the blank line vanished.
 * Fixed by preserving the paragraph of an empty `<p>`/heading whose open and
 * close tags are both explicit; Ksoup's `isImplied` flags distinguish real
 * `<p></p>` from browser-style normalization of invalid nesting (`<p><p>`,
 * stray `</p>`), which keeps collapsing as before.
 */
class Issue779EmptyParagraphHtmlTest {

    @Test
    fun setHtmlPreservesEmptyParagraphBetweenParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>This</p><p></p><p>Signature</p>")
        assertEquals(3, state.richParagraphList.size)
        assertEquals("This\n\nSignature", state.toText())
    }

    @Test
    fun setHtmlPreservesLeadingEmptyParagraph() {
        val state = RichTextState()
        state.setHtml("<p></p><p>Text</p>")
        assertEquals(2, state.richParagraphList.size)
        assertEquals("\nText", state.toText())
    }

    @Test
    fun setHtmlPreservesTrailingEmptyParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Text</p><p></p>")
        assertEquals(2, state.richParagraphList.size)
        assertEquals("Text\n", state.toText())
    }

    @Test
    fun setHtmlPreservesEmptyHeading() {
        val state = RichTextState()
        state.setHtml("<p>Text</p><h1></h1><p>More</p>")
        assertEquals(3, state.richParagraphList.size)
        assertEquals("Text\n\nMore", state.toText())
    }

    @Test
    fun setHtmlSingleEmptyParagraphStaysEmptyDocument() {
        val state = RichTextState()
        state.setHtml("<p></p>")
        assertEquals("", state.toText())
    }

    @Test
    fun setHtmlEmptyParagraphWithBrStillProducesSingleBlankLine() {
        // The <p><br></p> form worked before the fix; pin that it still does.
        val state = RichTextState()
        state.setHtml("<p>This</p><p><br></p><p>Signature</p>")
        assertEquals(3, state.richParagraphList.size)
        assertEquals("This\n\nSignature", state.toText())
    }
}
