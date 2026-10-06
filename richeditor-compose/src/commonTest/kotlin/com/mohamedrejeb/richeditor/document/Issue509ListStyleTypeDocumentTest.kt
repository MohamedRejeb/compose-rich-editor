package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression pins for #509: a list item block carries the CSS `list-style-type` keyword
 * of its list, so a document keeps a per-list marker style the way HTML does.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue509ListStyleTypeDocumentTest {

    private fun stateOf(doc: RichTextDocument): RichTextState =
        RichTextState().apply { setRichTextDocument(doc) }

    @Test
    fun `the keyword of each item is encoded`() {
        val state = RichTextState().apply {
            setHtml(
                """<ol style="list-style-type: lower-alpha"><li>a</li></ol><ul style="list-style-type: circle"><li>b</li></ul><ol><li>c</li></ol>"""
            )
        }

        val doc = RichTextDocumentEncoder.encode(state)

        assertEquals(RichTextBlockType.ListItem(ordered = true, listStyleType = "lower-alpha"), doc.blocks[0].type)
        assertEquals(RichTextBlockType.ListItem(ordered = false, listStyleType = "circle"), doc.blocks[1].type)
        assertEquals(RichTextBlockType.ListItem(ordered = true), doc.blocks[2].type)
    }

    @Test
    fun `a style type without a keyword is encoded as unset`() {
        val state = RichTextState().apply { setHtml("<ul><li>a</li></ul>") }
        state.selection = TextRange(state.annotatedString.text.indexOf("a"))
        state.setUnorderedListStyleType(UnorderedListStyleType.from("-"))

        val doc = RichTextDocumentEncoder.encode(state)

        assertEquals(RichTextBlockType.ListItem(ordered = false), doc.blocks[0].type)
    }

    @Test
    fun `a keyword is decoded into the item's style type`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true, listStyleType = "upper-roman")),
                RichTextBlock(text = "b", type = RichTextBlockType.ListItem(ordered = false, indent = 1, listStyleType = "square")),
            ),
        )

        val state = stateOf(doc)

        assertEquals(OrderedListStyleType.UpperRoman, (state.richParagraphList[0].type as OrderedList).styleTypeOverride)
        assertEquals(UnorderedListStyleType.Square, (state.richParagraphList[1].type as UnorderedList).styleTypeOverride)
        assertEquals(
            """<ol style="list-style-type: upper-roman;"><li>a<ul style="list-style-type: square;"><li>b</li></ul></li></ol>""",
            state.toHtml(),
        )
    }

    @Test
    fun `an unknown keyword is decoded as unset`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true, listStyleType = "cjk-ideographic")),
            ),
        )

        assertNull((stateOf(doc).richParagraphList[0].type as OrderedList).styleTypeOverride)
    }

    @Test
    fun `decode then encode keeps the keyword`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true, listStyleType = "lower-alpha")),
                RichTextBlock(text = "b", type = RichTextBlockType.ListItem(ordered = true, listStyleType = "lower-alpha")),
                RichTextBlock(text = "c", type = RichTextBlockType.ListItem(ordered = false, listStyleType = "disc")),
            ),
        )

        assertEquals(doc, stateOf(doc).toRichTextDocument())
    }
}
