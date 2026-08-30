package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
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
}
