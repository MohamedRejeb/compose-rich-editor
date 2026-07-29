package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentEncoderBlockTest {

    @Test
    fun `plain paragraphs map to paragraph blocks with flattened text`() {
        val state = RichTextState().apply { setHtml("<p>Hello</p><p>World</p>") }
        val doc = RichTextDocumentEncoder.encode(state)
        assertEquals(listOf("Hello", "World"), doc.blocks.map { it.text })
        assertEquals(
            listOf<RichTextBlockType>(RichTextBlockType.Paragraph, RichTextBlockType.Paragraph),
            doc.blocks.map { it.type },
        )
    }

    @Test
    fun `heading paragraph maps to headingLevel and no style marks`() {
        val state = RichTextState().apply { setHtml("<h2>Title</h2>") }
        val block = RichTextDocumentEncoder.encode(state).blocks.single()
        assertEquals(2, block.headingLevel)
        assertEquals("Title", block.text)
        assertEquals(emptyList(), block.spans)
    }

    @Test
    fun `list paragraphs map to list item types with 0-based indent, prefix excluded from text`() {
        val state = RichTextState().apply { setHtml("<ol start=\"3\"><li>One</li></ol><ul><li>Two</li></ul>") }
        val doc = RichTextDocumentEncoder.encode(state)
        assertEquals(RichTextBlockType.ListItem(ordered = true, indent = 0, startNumber = 3), doc.blocks[0].type)
        assertEquals("One", doc.blocks[0].text)
        assertEquals(RichTextBlockType.ListItem(ordered = false, indent = 0), doc.blocks[1].type)
        assertEquals("Two", doc.blocks[1].text)
    }

    @Test
    fun `text align set through toggleParagraphStyle is captured without composition`() {
        val state = RichTextState()
        state.setText("Hello")
        state.toggleParagraphStyle(state.currentParagraphStyle.copy(textAlign = TextAlign.Center))
        val block = RichTextDocumentEncoder.encode(state).blocks.single()
        assertEquals(TextAlign.Center, block.textAlign)
    }

    @Test
    fun `empty state maps to the empty document`() {
        assertEquals(RichTextDocument.empty(), RichTextDocumentEncoder.encode(RichTextState()))
    }
}
