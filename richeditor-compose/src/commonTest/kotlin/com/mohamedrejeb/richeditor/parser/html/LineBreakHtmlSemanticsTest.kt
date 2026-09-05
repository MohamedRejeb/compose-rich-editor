package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `<br>` follows HTML semantics on both sides of the parser.
 *
 * Decode side: a `<br>` ends the line it sits on and that line is rendered even when it is empty,
 * while the line it opens is rendered only when something fills it (a browser collapses an empty
 * last line). So a bare `<br>` between blocks is one empty paragraph, `<p><br></p>` is one empty
 * paragraph, `<p>a<br></p>` is just `a`, and `<p>a<br>b</p>` is one paragraph continued over two
 * lines, which is the [com.mohamedrejeb.richeditor.paragraph.RichParagraph.isFromLineBreak]
 * continuation.
 *
 * Encode side: an empty block keeps its own tag with a `<br>` child (`<p><br></p>`,
 * `<h1><br></h1>`, `<li><br></li>`), so `setHtml(toHtml())` is a fixed point for any number of
 * empty paragraphs anywhere in the document and an emptied heading keeps its level.
 *
 * Old documents written with bare `<br>` for empty paragraphs decode to the layout a browser
 * renders for them, so nothing already saved changes shape on reload.
 */
class LineBreakHtmlSemanticsTest {

    private fun decoded(html: String): RichTextState = RichTextState().apply { setHtml(html) }

    private fun flatten(span: RichSpan): String =
        span.text + span.children.joinToString("") { flatten(it) }

    /** Paragraph texts without list prefixes. */
    private fun texts(state: RichTextState): List<String> =
        state.richParagraphList.map { paragraph -> paragraph.children.joinToString("") { flatten(it) } }

    private fun continuationFlags(state: RichTextState): List<Boolean> =
        state.richParagraphList.map { it.isFromLineBreak }

    private fun headingLevels(state: RichTextState): List<HeadingStyle> =
        state.richParagraphList.map { it.headingStyle }

    // Decode: bare line breaks between blocks (the old empty paragraph format)

    @Test
    fun `a bare line break after a paragraph is one empty paragraph`() {
        val state = decoded("<p>a</p><br>")

        assertEquals(listOf("a", ""), texts(state))
    }

    @Test
    fun `bare line breaks decode to the lines a browser renders`() {
        val state = decoded("<br><p>First</p><br><br><p>Second</p><br>")

        assertEquals(listOf("", "First", "", "", "Second", ""), texts(state))
    }

    @Test
    fun `a bare line break after a list is one empty paragraph`() {
        val state = decoded("<ul><li>x</li></ul><br>")

        assertEquals(listOf("x", ""), texts(state))
    }

    @Test
    fun `a document of only line breaks has one paragraph per line break`() {
        assertEquals(listOf(""), texts(decoded("<br>")))
        assertEquals(listOf("", ""), texts(decoded("<br><br>")))
    }

    @Test
    fun `a top level line break after text ends the line`() {
        assertEquals(listOf("text"), texts(decoded("text<br>")))

        val continued = decoded("text<br>more")
        assertEquals(listOf("text", "more"), texts(continued))
        assertEquals(listOf(false, true), continuationFlags(continued))
    }

    // Decode: line breaks inside a block

    @Test
    fun `a line break inside a paragraph continues it`() {
        val state = decoded("<p>a<br>b</p>")

        assertEquals(listOf("a", "b"), texts(state))
        assertEquals(listOf(false, true), continuationFlags(state))
        assertEquals("<p>a<br>b</p>", state.toHtml())
    }

    @Test
    fun `a line break inside a heading continues the heading`() {
        val state = decoded("<h1>a<br>b</h1>")

        assertEquals(listOf("a", "b"), texts(state))
        assertEquals(listOf(false, true), continuationFlags(state))
        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.H1), headingLevels(state))
        assertEquals("<h1>a<br>b</h1>", state.toHtml())
    }

    @Test
    fun `a trailing line break inside a paragraph adds nothing`() {
        assertEquals(listOf("a"), texts(decoded("<p>a<br></p>")))
        assertEquals(listOf("a", "b"), texts(decoded("<p>a<br></p><p>b</p>")))
        assertEquals(listOf("a"), texts(decoded("<p><b>a<br></b></p>")))
    }

    @Test
    fun `a paragraph holding only a line break is one empty paragraph`() {
        assertEquals(listOf(""), texts(decoded("<p><br></p>")))
        assertEquals(listOf("a", ""), texts(decoded("<p>a</p><p><br></p>")))
        assertEquals(listOf("a", "", "b"), texts(decoded("<p>a</p><p><br></p><p>b</p>")))
        assertEquals(listOf("", "b"), texts(decoded("<p><br></p><p>b</p>")))
    }

    @Test
    fun `a heading holding only a line break is one empty heading`() {
        val state = decoded("<h1><br></h1>")

        assertEquals(listOf(""), texts(state))
        assertEquals(listOf(HeadingStyle.H1), headingLevels(state))
    }

    @Test
    fun `a block opening after a heading line break is not a heading`() {
        val state = decoded("<h1>a<br></h1><p>b</p>")

        assertEquals(listOf("a", "b"), texts(state))
        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.Normal), headingLevels(state))
    }

    // Decode: line breaks inside list items

    @Test
    fun `a list item holding only a line break is one empty item`() {
        val state = decoded("<ul><li><br></li></ul>")

        assertEquals(1, state.richParagraphList.size)
        assertTrue(state.richParagraphList.single().type is UnorderedList)
    }

    @Test
    fun `a trailing line break inside a list item does not merge the next item`() {
        val state = decoded("<ul><li>a<br></li><li>b</li></ul>")

        assertEquals(listOf("a", "b"), state.richParagraphList.map { it.children.single().text })
        assertTrue(state.richParagraphList.all { it.type is UnorderedList })
        assertFalse(state.richParagraphList.any { it.isFromLineBreak })
        assertEquals("<ul><li>a</li><li>b</li></ul>", state.toHtml())
    }

    @Test
    fun `a line break in an empty host item keeps the nested list nested`() {
        val state = decoded("<ul><li><br><ul><li>c</li></ul></li></ul>")

        assertEquals(2, state.richParagraphList.size)
        val (host, nested) = state.richParagraphList
        assertTrue(host.isEmpty())
        assertEquals(1, (host.type as UnorderedList).level)
        assertEquals("c", nested.children.single().text)
        assertEquals(2, (nested.type as UnorderedList).level)
        assertFalse(nested.isFromLineBreak)
    }

    @Test
    fun `a line break continuation inside a list item still round trips`() {
        val html = "<ul><li>a<br>b</li></ul>"

        assertEquals(html, decoded(html).toHtml())
    }
}
