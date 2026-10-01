package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class Issue810SelectionReplacementTest {
    private val markdown = "a\n***[b](https://example.com)c***"

    @Test
    fun markdownCollapseReparentsGrandchildren() {
        val state = RichTextState().apply { setMarkdown(markdown) }
        assertTreeOwnership(state)
    }

    @Test
    fun replacingParagraphBeforeNestedLinkPreservesTextStylesAndHistory() {
        for (selection in listOf(TextRange(0, 2), TextRange(2, 0))) {
            val state = RichTextState().apply { setMarkdown(markdown) }
            val before = state.toRichTextDocument()
            val expected = RichTextState().apply {
                setMarkdown("x\n***[b](https://example.com)c***")
            }.toRichTextDocument()
            state.selection = selection

            state.onTextFieldValueChange(TextFieldValue("x\nbc", TextRange(2)))

            assertEquals("x\nbc", state.toText())
            assertEquals(TextRange(2), state.selection)
            assertEquals(expected, state.toRichTextDocument())
            assertTreeOwnership(state)

            state.history.undo()
            assertEquals(before, state.toRichTextDocument())
            assertTreeOwnership(state)
            state.history.redo()
            assertEquals(expected, state.toRichTextDocument())
            assertTreeOwnership(state)
        }
    }

    @Test
    fun renderingCollapseReparentsGrandchildren() {
        val paragraph = RichParagraph()
        val bold = RichSpan(
            paragraph = paragraph,
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
        )
        val italic = RichSpan(
            paragraph = paragraph,
            parent = bold,
            spanStyle = SpanStyle(fontStyle = FontStyle.Italic),
        )
        italic.children.add(
            RichSpan(
                paragraph = paragraph,
                parent = italic,
                text = "b",
                richSpanStyle = RichSpanStyle.Link("https://example.com"),
            )
        )
        italic.children.add(RichSpan(paragraph = paragraph, parent = italic, text = "c"))
        bold.children.add(italic)
        paragraph.children.add(bold)
        val state = RichTextState(listOf(paragraph))
        val before = state.toRichTextDocument()

        state.updateAnnotatedString()

        assertEquals(before, state.toRichTextDocument())
        assertTreeOwnership(state)
    }

    private fun assertTreeOwnership(state: RichTextState) {
        val seen = mutableSetOf<RichSpan>()
        fun visit(span: RichSpan, parent: RichSpan?, paragraph: RichParagraph) {
            assertTrue(seen.add(span), "A span must occur exactly once in the tree")
            assertSame(parent, span.parent, "Span must point to its actual parent")
            assertSame(paragraph, span.paragraph, "Span must point to its owning paragraph")
            span.children.forEach { visit(it, span, paragraph) }
        }
        state.richParagraphList.forEach { paragraph ->
            paragraph.children.forEach { visit(it, null, paragraph) }
        }
    }
}
