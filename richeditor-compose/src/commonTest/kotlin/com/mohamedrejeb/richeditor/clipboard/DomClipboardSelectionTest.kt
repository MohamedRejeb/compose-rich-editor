package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The range the web copy and cut handlers act on. The browser fires those events for a
 * collapsed caret too, and the general [RichTextState.copySelection] falls back to the last
 * non-collapsed selection there, which on web means copying content the user no longer has
 * selected, possibly from text that has changed since.
 */
class DomClipboardSelectionTest {

    @Test
    fun `a non-collapsed selection is the range to copy`() {
        val state = RichTextState()
        state.setText("Hello World")
        state.selection = TextRange(0, 5)

        assertEquals(TextRange(0, 5), state.domClipboardSelection())
    }

    @Test
    fun `a collapsed caret copies nothing even after an earlier selection`() {
        val state = RichTextState()
        state.setText("Hello World")
        state.selection = TextRange(0, 5)
        state.selection = TextRange(7)

        assertNotNull(state.copySelection, "the general fallback still remembers the earlier range")
        assertNull(state.domClipboardSelection(), "a DOM copy with a caret must copy nothing")
    }
}
