package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentEncoderMarksTest {

    private fun docOf(html: String): RichTextDocument =
        RichTextDocumentEncoder.encode(RichTextState().apply { setHtml(html) })

    @Test
    fun `bold and link produce marks at inclusive ranges`() {
        val block = docOf("<p>Hello <b>bold</b> <a href=\"https://example.com\">link</a></p>").blocks.single()
        assertEquals("Hello bold link", block.text)
        assertEquals(
            listOf(
                RichTextSpanMark.Bold(range = 6..9),
                RichTextSpanMark.Link(range = 11..14, url = "https://example.com"),
            ),
            block.spans,
        )
    }

    @Test
    fun `adjacent equal-styled spans merge into one mark regardless of tree shape`() {
        val split = docOf("<p><b>ab</b><b>cd</b></p>")
        val joined = docOf("<p><b>abcd</b></p>")
        assertEquals(joined, split)
        assertEquals(listOf(RichTextSpanMark.Bold(range = 0..3)), joined.blocks.single().spans)
    }

    @Test
    fun `combined underline and strikethrough decompose into two marks`() {
        val block = docOf("<p><u><s>x</s></u></p>").blocks.single()
        assertEquals(
            listOf(
                RichTextSpanMark.Underline(range = 0..0),
                RichTextSpanMark.Strikethrough(range = 0..0),
            ),
            block.spans,
        )
    }

    @Test
    fun `color and background map to color and highlight marks`() {
        val block = docOf("<p><span style=\"color:#ff0000;background:#00ff00\">x</span></p>").blocks.single()
        assertEquals(
            listOf(
                RichTextSpanMark.TextColor(range = 0..0, argb = 0xFFFF0000),
                RichTextSpanMark.Highlight(range = 0..0, argb = 0xFF00FF00),
            ),
            block.spans,
        )
    }

    @Test
    fun `inline image becomes one placeholder char with an image mark`() {
        val block = docOf("<p>a<img src=\"https://e.com/i.png\" width=\"10\" height=\"20\">b</p>").blocks.single()
        assertEquals("a${InlineImagePlaceholder}b", block.text)
        val image = block.spans.single() as RichTextSpanMark.Image
        assertEquals(1..1, image.range)
        assertEquals("https://e.com/i.png", image.url)
    }

    @Test
    fun `marks are sorted by start position then kind order`() {
        val block = docOf("<p><i><b>ab</b></i></p>").blocks.single()
        assertEquals(
            listOf(RichTextSpanMark.Bold(range = 0..1), RichTextSpanMark.Italic(range = 0..1)),
            block.spans,
        )
    }
}
