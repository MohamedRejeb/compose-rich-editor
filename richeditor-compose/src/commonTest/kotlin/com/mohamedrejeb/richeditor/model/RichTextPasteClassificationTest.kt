package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Classifying the text change that follows a clipboard read. The platform inserts the
 * clipboard's plain text; when the managers stashed it, a paste is recognized structurally
 * (old selection replaced by exactly that text) even when the result is shorter than the
 * replaced selection. Without the plain text, the legacy grow-only heuristic applies.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextPasteClassificationTest {

    @Test
    fun `paste over a longer selection keeps rich formatting`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 11)
        state.pendingClipboardHtml = "<b>Hi</b>"
        state.pendingClipboardPlainText = "Hi"

        state.onTextFieldValueChange(TextFieldValue("Hi", selection = TextRange(2)))

        assertEquals("Hi", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Bold && it.range == 0..1 },
        )
    }

    @Test
    fun `paste over an equal length selection keeps rich formatting`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 2)
        state.pendingClipboardHtml = "<b>Hi</b>"
        state.pendingClipboardPlainText = "Hi"

        state.onTextFieldValueChange(TextFieldValue("Hillo world", selection = TextRange(2)))

        assertEquals("Hillo world", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Bold && it.range == 0..1 },
        )
    }

    @Test
    fun `typing over a selection after a clipboard read stays plain`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 5)
        state.pendingClipboardHtml = "<b>clipboard content</b>"
        state.pendingClipboardPlainText = "clipboard content"

        state.onTextFieldValueChange(TextFieldValue("X world", selection = TextRange(1)))

        assertEquals("X world", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Bold },
        )
    }

    @Test
    fun `deleting the selection after a clipboard read is not a paste`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 6)
        state.pendingClipboardHtml = "<b>Hi</b>"
        state.pendingClipboardPlainText = "Hi"

        state.onTextFieldValueChange(TextFieldValue("world", selection = TextRange(0)))

        assertEquals("world", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Bold },
        )
    }

    @Test
    fun `windows line endings in the stashed plain text still match`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 11)
        state.pendingClipboardHtml = "<p><b>a</b></p><p>b</p>"
        state.pendingClipboardPlainText = "a\r\nb"

        state.onTextFieldValueChange(TextFieldValue("a\nb", selection = TextRange(3)))

        assertEquals("a\nb", state.toText())
        val document = state.toRichTextDocument()
        assertEquals(2, document.blocks.size)
        assertTrue(document.blocks.first().spans.any { it is RichTextSpanMark.Bold })
    }

    @Test
    fun `growth heuristic still applies when no plain text was stashed`() {
        val state = RichTextState()
        state.setText("Hello ")
        state.pendingClipboardHtml = "<b>bold</b>"

        state.onTextFieldValueChange(TextFieldValue("Hello bold", selection = TextRange(10)))

        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Bold && it.range == 6..9 },
        )
    }
}
