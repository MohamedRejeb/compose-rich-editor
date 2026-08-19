package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.FontRunStyle
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The paste half of [RichTextConfig.richClipboardEnabled]: platform clipboard managers stash
 * incoming HTML in pendingClipboardHtml and the common text change handler consumes it, so
 * simulating both steps here covers the shared paste rule for every platform.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextClipboardConfigTest {

    @Test
    fun `paste imports html formatting when rich clipboard is enabled`() {
        val state = RichTextState()
        state.setText("Hello ")
        state.pendingClipboardHtml = "<b>bold</b>"
        state.onTextFieldValueChange(TextFieldValue("Hello bold", selection = TextRange(10)))

        assertEquals("Hello bold", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Bold && it.range == 6..9 },
        )
    }

    @Test
    fun `paste falls back to plain insertion when rich clipboard is disabled`() {
        val state = RichTextState()
        state.setText("Hello ")
        state.config.richClipboardEnabled = false
        state.pendingClipboardHtml = "<b>bold</b>"
        state.onTextFieldValueChange(TextFieldValue("Hello bold", selection = TextRange(10)))

        assertEquals("Hello bold", state.toText())
        assertTrue(state.toRichTextDocument().blocks.single().spans.none { it is RichTextSpanMark.Bold })
    }

    @Test
    fun `plain paste inherits the style at the caret like typed text`() {
        val state = RichTextState()
        state.setText("Hello")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 5))
        state.config.richClipboardEnabled = false
        state.selection = TextRange(5)
        state.pendingClipboardHtml = "<b>bold</b>"
        state.onTextFieldValueChange(TextFieldValue("Hellobold", selection = TextRange(9)))

        val customs = state.toRichTextDocument().blocks.single().spans
            .filterIsInstance<RichTextSpanMark.Custom>()
        assertEquals(listOf(0..8), customs.map { it.range })
    }
}
