package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextStatePrimitivesTest {

    @Test
    fun `insertText at start`() {
        val state = RichTextState()
        state.setText("World")
        state.insertText(at = 0, text = "Hello ")
        assertEquals("Hello World", state.toText())
    }

    @Test
    fun `insertText in the middle inherits the surrounding span`() {
        val state = RichTextState()
        state.setHtml("<p><b>Bold</b></p>")
        state.insertText(at = 2, text = "XY")
        assertEquals("BoXYld", state.toText())
        state.selection = TextRange(2, 4)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, state.currentSpanStyle.fontWeight)
    }

    @Test
    fun `insertText with newline splits the paragraph`() {
        val state = RichTextState()
        state.setText("ab")
        state.insertText(at = 1, text = "\n")
        assertEquals("a\nb", state.toText())
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `deleteRange inside one paragraph`() {
        val state = RichTextState()
        state.setText("Hello World")
        state.deleteRange(TextRange(5, 11))
        assertEquals("Hello", state.toText())
    }

    @Test
    fun `deleteRange across a paragraph boundary merges paragraphs`() {
        val state = RichTextState()
        state.setText("ab\ncd")
        state.deleteRange(TextRange(2, 4))
        assertEquals("abd", state.toText())
        assertEquals(1, state.richParagraphList.size)
    }
}
