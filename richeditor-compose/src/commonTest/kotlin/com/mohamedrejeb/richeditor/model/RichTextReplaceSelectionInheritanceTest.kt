package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.FontRunStyle
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Typing or pasting over a non-collapsed selection styles the inserted text from the
 * replaced range's start (the platform typing-attributes convention), not from the
 * character before the caret, and the restyle is part of the same edit so undo/redo
 * see a single entry.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextReplaceSelectionInheritanceTest {

    private val redArgb = 0xFFFF0000L

    private fun stateWithRedWorld(): RichTextState {
        val state = RichTextState()
        state.setText("Hello world")
        state.addSpanStyle(SpanStyle(color = Color(redArgb.toInt())), TextRange(6, 11))
        return state
    }

    @Test
    fun `typing over a colored selection inherits the replaced color`() {
        val state = stateWithRedWorld()
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello X", selection = TextRange(7)))

        assertEquals("Hello X", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.TextColor && it.range == 6..6 && it.argb == redArgb },
        )
    }

    @Test
    fun `typing over a selection does not inherit the style before the caret`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 6))
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello X", selection = TextRange(7)))

        assertEquals("Hello X", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Bold && 6 in it.range },
        )
    }

    @Test
    fun `replacement styling is a single undo entry`() {
        val state = stateWithRedWorld()
        val before = state.toRichTextDocument()
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello X", selection = TextRange(7)))

        state.history.undo()
        assertEquals(before, state.toRichTextDocument())
    }

    @Test
    fun `custom rich span style is inherited when replacing its text`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(6, 11))
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello X", selection = TextRange(7)))

        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Custom && it.range == 6..6 && it.style == FontRunStyle(slug = "amiri") },
        )
    }

    @Test
    fun `link is not inherited when replacing the whole link`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(RichSpanStyle.Link(url = "https://example.com"), TextRange(6, 11))
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello X", selection = TextRange(7)))

        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Link },
        )
    }

    @Test
    fun `plain paste over a styled selection inherits the replaced style`() {
        val state = stateWithRedWorld()
        state.selection = TextRange(6, 11)

        state.onTextFieldValueChange(TextFieldValue("Hello pasted", selection = TextRange(12)))

        assertEquals("Hello pasted", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.TextColor && it.range == 6..11 && it.argb == redArgb },
        )
    }
}
