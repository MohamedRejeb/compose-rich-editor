package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.style.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RichTextDocumentModelTest {

    @Test
    fun `document requires at least one block`() {
        assertFailsWith<IllegalArgumentException> { RichTextDocument(blocks = emptyList()) }
    }

    @Test
    fun `empty document has a single empty paragraph block`() {
        val doc = RichTextDocument.empty()
        assertEquals(1, doc.blocks.size)
        assertEquals("", doc.blocks.first().text)
        assertEquals(RichTextBlockType.Paragraph, doc.blocks.first().type)
    }

    @Test
    fun `block rejects mark outside text bounds`() {
        assertFailsWith<IllegalArgumentException> {
            RichTextBlock(text = "ab", spans = listOf(RichTextSpanMark.Bold(range = 0..2)))
        }
    }

    @Test
    fun `block rejects empty mark range`() {
        // A mark must cover at least one character.
        assertFailsWith<IllegalArgumentException> {
            RichTextBlock(text = "ab", spans = listOf(RichTextSpanMark.Bold(range = IntRange.EMPTY)))
        }
    }

    @Test
    fun `block rejects heading level outside 0 to 6`() {
        assertFailsWith<IllegalArgumentException> { RichTextBlock(text = "a", headingLevel = 7) }
    }

    @Test
    fun `list item start number requires ordered list`() {
        assertFailsWith<IllegalArgumentException> {
            RichTextBlockType.ListItem(ordered = false, startNumber = 3)
        }
    }

    @Test
    fun `structurally identical documents are equal`() {
        fun build() = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello bold",
                    spans = listOf(RichTextSpanMark.Bold(range = 6..9)),
                    textAlign = TextAlign.Center,
                ),
                RichTextBlock(
                    text = "One",
                    type = RichTextBlockType.ListItem(ordered = true, indent = 0),
                ),
            )
        )
        assertEquals(build(), build())
        assertEquals(build().hashCode(), build().hashCode())
    }
}
