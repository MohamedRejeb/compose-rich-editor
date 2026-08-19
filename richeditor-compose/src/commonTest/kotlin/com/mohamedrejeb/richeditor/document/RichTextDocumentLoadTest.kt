package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalRichTextApi::class)
class RichTextDocumentLoadTest {

    @Test
    fun `set rich text document applies the given selection`() {
        val document = RichTextDocument(blocks = listOf(RichTextBlock(text = "Hello world")))

        val state = RichTextState().setRichTextDocument(document, selection = TextRange(2, 5))

        assertEquals(TextRange(2, 5), state.selection)
    }

    @Test
    fun `set rich text document without selection moves the caret to the end`() {
        val document = RichTextDocument(blocks = listOf(RichTextBlock(text = "Hello world")))

        val state = RichTextState().setRichTextDocument(document)

        assertEquals(TextRange(11), state.selection)
    }

    @Test
    fun `set rich text document without selection moves the caret to the end on a used state`() {
        val state = RichTextState()
        state.setText("Hi")
        state.selection = TextRange(0)

        state.setRichTextDocument(
            RichTextDocument(blocks = listOf(RichTextBlock(text = "Hello world"))),
        )

        assertEquals(TextRange(11), state.selection)
    }

    @Test
    fun `set rich text document clears undo history`() {
        val state = RichTextState()
        state.setText("Before")
        state.selection = TextRange(0, 6)
        state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))

        state.setRichTextDocument(
            RichTextDocument(blocks = listOf(RichTextBlock(text = "After"))),
        )

        assertFalse(state.history.canUndo)
        assertEquals("After", state.toText())
    }

    @Test
    fun `hand-authored document ranges come back unchanged from the read api`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello world",
                    spans = listOf(
                        RichTextSpanMark.Bold(range = 0..4),
                        RichTextSpanMark.Custom(range = 2..8, style = FontRunStyle(slug = "amiri")),
                        RichTextSpanMark.TextColor(range = 6..10, argb = 0xFF112233),
                    ),
                ),
                RichTextBlock(
                    text = "Second line",
                    spans = listOf(
                        RichTextSpanMark.Italic(range = 0..5),
                        RichTextSpanMark.Link(range = 7..10, url = "https://example.com"),
                    ),
                ),
            ),
        )

        val state = RichTextState().setRichTextDocument(document)

        assertEquals(document, state.toRichTextDocument())
    }
}
