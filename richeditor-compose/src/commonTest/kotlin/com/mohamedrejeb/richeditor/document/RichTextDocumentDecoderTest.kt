package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentDecoderTest {

    private fun stateOf(doc: RichTextDocument): RichTextState =
        RichTextState().apply { setRichTextDocument(doc) }

    @Test
    fun `decoded paragraphs render the block text`() {
        val doc = RichTextDocument(
            blocks = listOf(RichTextBlock(text = "Hello"), RichTextBlock(text = "World")),
        )
        assertEquals("Hello\nWorld", stateOf(doc).toText())
    }

    @Test
    fun `bold mark round-trips through html export`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello bold",
                    spans = listOf(RichTextSpanMark.Bold(range = 6..9)),
                ),
            ),
        )
        assertEquals("<p>Hello <b>bold</b></p>", stateOf(doc).toHtml())
    }

    @Test
    fun `ordered numbering is derived from document order and start numbers`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true, startNumber = 3)),
                RichTextBlock(text = "b", type = RichTextBlockType.ListItem(ordered = true)),
            ),
        )
        assertEquals("<ol start=\"3\"><li>a</li><li>b</li></ol>", stateOf(doc).toHtml())
    }

    @Test
    fun `decode then encode is the identity for library-shaped documents`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "Title", headingLevel = 1),
                RichTextBlock(
                    text = "Hello bold link",
                    spans = listOf(
                        RichTextSpanMark.Bold(range = 6..9),
                        RichTextSpanMark.Link(range = 11..14, url = "https://example.com"),
                    ),
                ),
                RichTextBlock(text = "One", type = RichTextBlockType.ListItem(ordered = true)),
            ),
        )
        assertEquals(doc, stateOf(doc).toRichTextDocument())
    }
}
