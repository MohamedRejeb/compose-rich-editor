package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentRoundTripTest {

    private val fixtures = listOf(
        "<p>Plain</p>",
        "<p></p>",
        "<p>Hello <b>bold</b> <i>italic</i> <u>under</u> <s>strike</s></p>",
        "<h1>Title</h1><p>Body</p>",
        "<p style=\"text-align:center\">Centered</p>",
        "<ol><li>One</li><li>Two</li></ol>",
        "<ol start=\"5\"><li>Five</li></ol>",
        "<ul><li>A</li><li>B</li></ul>",
        "<p><a href=\"https://example.com\">link</a> and <code>code</code></p>",
        "<p><span style=\"color:#112233;background:#445566\">styled</span></p>",
        "<p><span style=\"font-size:20px\">big</span> <span style=\"letter-spacing:2px\">spaced</span></p>",
        "<p><sub>sub</sub> and <sup>sup</sup></p>",
        "<p>a<img src=\"https://e.com/i.png\" width=\"10\" height=\"20\">b</p>",
        "<p>line<br>break</p>",
    )

    @Test
    fun `document round-trip is the identity for every fixture`() {
        fixtures.forEach { html ->
            val state = RichTextState().apply { setHtml(html) }
            val doc = state.toRichTextDocument()
            val rebuilt = RichTextState().setRichTextDocument(doc)
            assertEquals(doc, rebuilt.toRichTextDocument(), "Round-trip failed for: $html")
        }
    }

    @Test
    fun `round-trip preserves visible text`() {
        fixtures.forEach { html ->
            val state = RichTextState().apply { setHtml(html) }
            val rebuilt = RichTextState().setRichTextDocument(state.toRichTextDocument())
            assertEquals(state.toText(), rebuilt.toText(), "Text changed for: $html")
        }
    }

    @Test
    fun `block texts joined with newline match toText for prefix-free content`() {
        val state = RichTextState().apply {
            setHtml("<p>First <b>bold</b></p><h2>Heading</h2><p>Third paragraph</p>")
        }

        val document = state.toRichTextDocument()

        assertEquals(state.toText(), document.blocks.joinToString("\n") { it.text })
    }
}
