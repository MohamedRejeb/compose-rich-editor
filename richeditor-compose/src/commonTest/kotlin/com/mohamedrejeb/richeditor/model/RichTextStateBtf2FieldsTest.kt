package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextStateBtf2FieldsTest {

    @Test
    fun `scrollState starts at zero`() {
        val state = RichTextState()
        assertEquals(0, state.scrollState.value)
    }

    @Test
    fun `textFieldState mirrors text after setText`() {
        val state = RichTextState()
        state.setText("Hello World")
        assertEquals("Hello World", state.textFieldState.text.toString())
    }

    @Test
    fun `textFieldState mirrors text after setHtml`() {
        val state = RichTextState()
        state.setHtml("<p>Hello</p><p>World</p>")
        assertEquals(state.annotatedString.text, state.textFieldState.text.toString())
    }

    @Test
    fun `setTextFieldStateFromValue defers while skipTextFieldStateSync is set`() {
        val state = RichTextState()
        state.setText("abc")
        state.skipTextFieldStateSync = true
        try {
            state.setTextFieldStateFromValue("abcd", TextRange(4))
            assertEquals("abc", state.textFieldState.text.toString())
            assertEquals("abcd", state.pendingTextDuringSync)
            assertEquals(TextRange(4), state.pendingSelectionDuringSync)
        } finally {
            state.skipTextFieldStateSync = false
        }
    }
}
