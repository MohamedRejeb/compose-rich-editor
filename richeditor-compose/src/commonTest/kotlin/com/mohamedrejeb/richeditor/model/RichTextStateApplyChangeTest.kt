package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RichTextStateApplyChangeTest {

    @Test
    fun `applyChange with collapsed range inserts`() {
        val state = RichTextState()
        state.setText("ac")
        state.applyChange(originalRange = TextRange(1, 1), newText = "b")
        assertEquals("abc", state.toText())
    }

    @Test
    fun `applyChange with empty newText deletes`() {
        val state = RichTextState()
        state.setText("abc")
        state.applyChange(originalRange = TextRange(1, 2), newText = "")
        assertEquals("ac", state.toText())
    }

    @Test
    fun `applyChange replaces a range`() {
        val state = RichTextState()
        state.setText("Hello World")
        state.applyChange(originalRange = TextRange(6, 11), newText = "There")
        assertEquals("Hello There", state.toText())
    }

    @Test
    fun `applyChange with reversed range behaves like forward range`() {
        val state = RichTextState()
        state.setText("abc")
        state.applyChange(originalRange = TextRange(2, 1), newText = "X")
        assertEquals("aXc", state.toText())
    }

    @Test
    fun `applyChange rejects out of bounds range`() {
        val state = RichTextState()
        state.setText("abc")
        assertFailsWith<IllegalArgumentException> {
            state.applyChange(originalRange = TextRange(0, 10), newText = "x")
        }
    }

    @Test
    fun `applyChange replacement whose deletion demotes a list still inserts at the recomputed position`() {
        val state = RichTextState()
        state.setText("Hello")
        state.selection = TextRange(0, 5)
        state.toggleOrderedList()
        assertEquals("1. Hello", state.toText())

        // The range spans from inside the list prefix into the item text, so
        // deleting it demotes the paragraph and drops the whole "1. " prefix,
        // not just the two characters the range covers. The insertion must land
        // at the position the deletion actually left, not at the range's stale
        // start index (which would land one character into "Hello").
        state.applyChange(originalRange = TextRange(1, 3), newText = "X")

        assertEquals("XHello", state.toText())
    }

    @Test
    fun `applyChange after a list Enter clears justInsertedListParagraph so a real replacement is not absorbed as the IME echo`() {
        val state = RichTextState()
        state.onTextFieldValueChange(TextFieldValue(text = "1.", selection = TextRange(2)))
        state.onTextFieldValueChange(TextFieldValue(text = "1. ", selection = TextRange(3)))
        state.onTextFieldValueChange(TextFieldValue(text = "1. Hello", selection = TextRange(8)))
        // Enter creates a new list item and injects its "2. " startText,
        // arming the IME startText echo guard (justInsertedListParagraph).
        state.onTextFieldValueChange(TextFieldValue(text = "1. Hello\n", selection = TextRange(9)))
        assertEquals("1. Hello\n2. ", state.toText())

        // A real replacement that happens to remove exactly the new item's
        // startText length must not be absorbed as the harmless IME echo: if it
        // were, the "2. " prefix would survive and "X" would land after it
        // instead of replacing it.
        state.applyChange(originalRange = TextRange(9, 12), newText = "X")

        assertEquals("1. Hello\nX", state.toText())
    }
}
