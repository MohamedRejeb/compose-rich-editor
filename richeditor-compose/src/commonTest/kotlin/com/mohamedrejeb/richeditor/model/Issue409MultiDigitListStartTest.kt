package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Probe + regression tests for #409: typing a multi-digit ordered-list
 * marker (e.g. "57. ") should convert to an OrderedList starting at 57,
 * not collapse to 5.
 *
 * The auto-detect path lives in `RichTextState.checkListStart`, which is
 * called from `onTextFieldValueChange` once the buffer's first run
 * matches the marker pattern.
 */
class Issue409MultiDigitListStartTest {

    @Test
    fun `typing a single digit list start creates ordered list at that number`() {
        val state = RichTextState()
        // Simulate the user typing "1. " in an empty paragraph.
        state.onTextFieldValueChange(TextFieldValue(text = "1. ", selection = androidx.compose.ui.text.TextRange(3)))

        val firstParagraph = state.richParagraphList.firstOrNull()
        assertNotNull(firstParagraph)

        val type = firstParagraph.type
        assertTrue(type is OrderedList, "expected OrderedList, got ${type::class.simpleName}")
        assertEquals(1, type.number)
    }

    @Test
    fun `typing a two digit list start preserves the full number`() {
        val state = RichTextState()
        state.onTextFieldValueChange(TextFieldValue(text = "57. ", selection = androidx.compose.ui.text.TextRange(4)))

        val firstParagraph = state.richParagraphList.firstOrNull()
        assertNotNull(firstParagraph)

        val type = firstParagraph.type
        assertTrue(type is OrderedList, "expected OrderedList, got ${type::class.simpleName}")
        assertEquals(57, type.number)
    }

    @Test
    fun `typing a three digit list start preserves the full number`() {
        val state = RichTextState()
        state.onTextFieldValueChange(TextFieldValue(text = "123. ", selection = androidx.compose.ui.text.TextRange(5)))

        val firstParagraph = state.richParagraphList.firstOrNull()
        assertNotNull(firstParagraph)

        val type = firstParagraph.type
        assertTrue(type is OrderedList, "expected OrderedList, got ${type::class.simpleName}")
        assertEquals(123, type.number)
    }

    // Note: a follow-up scenario "user types 57. Something in one chunk" does
    // NOT auto-convert and is intentionally left out. Auto-detect only runs
    // when the buffer's first run exactly matches the marker pattern, which
    // happens naturally during live keystroke-by-keystroke typing (where the
    // user hits space after "57."). The same input pasted whole is left as
    // content, by design.
}
