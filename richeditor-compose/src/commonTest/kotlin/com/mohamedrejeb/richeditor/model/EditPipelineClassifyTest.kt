package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditPipelineClassifyTest {

    @Test
    fun `single insertion records one typing history entry`() {
        val state = RichTextState()
        state.setText("ab")
        state.history.clear()
        state.applyUserEditForTest(originalRange = TextRange(2, 2), newText = "c")
        assertEquals("abc", state.toText())
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("ab", state.toText())
    }

    @Test
    fun `deletion records a delete history entry`() {
        val state = RichTextState()
        state.setText("abc")
        state.history.clear()
        state.applyUserEditForTest(originalRange = TextRange(2, 3), newText = "")
        assertEquals("ab", state.toText())
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("abc", state.toText())
    }

    @Test
    fun `two consecutive typing edits coalesce into one undo entry`() {
        val state = RichTextState()
        state.setText("")
        state.history.clear()
        state.applyUserEditForTest(originalRange = TextRange(0, 0), newText = "a")
        state.applyUserEditForTest(originalRange = TextRange(1, 1), newText = "b")
        assertEquals("ab", state.toText())
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("", state.toText())
        assertTrue(!state.history.canUndo)
    }

    @Test
    fun `same-length replacement after typing does not coalesce with the typing burst`() {
        val state = RichTextState()
        state.setText("a")
        state.history.clear()

        val typingBuffer = state.textFieldState.toTextFieldBuffer()
        typingBuffer.replace(1, 1, "b")
        state.applyChangeList(typingBuffer)
        assertEquals("ab", state.toText())
        // applyChangeList suppresses the textFieldState mirror mid-replay (Task 4's
        // InputTransformation reconciles it in production); bridge it here so the next
        // toTextFieldBuffer() snapshot starts from the post-edit text.
        state.setTextFieldStateFromValue(text = state.toText(), selection = state.selection)

        val structuralBuffer = state.textFieldState.toTextFieldBuffer()
        structuralBuffer.replace(0, 2, "xy")
        state.applyChangeList(structuralBuffer)
        assertEquals("xy", state.toText())

        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("ab", state.toText())
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("a", state.toText())
    }

    @Test
    fun `applyChangeList replays a buffer edit through the real pipeline`() {
        val state = RichTextState()
        state.setText("ab")
        state.history.clear()

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 2, "c")
        state.applyChangeList(buffer)

        assertEquals("abc", state.toText())
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("ab", state.toText())
    }

    @Test
    fun `applyChangeList clears a stale clipboard stash after a non-paste edit`() {
        val state = RichTextState()
        state.setText("ab")
        state.history.clear()
        state.pendingClipboardHtml = "<b>not a match</b>"
        state.pendingClipboardPlainText = "not a match"

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 2, "c")
        state.applyChangeList(buffer)

        assertNull(state.pendingClipboardHtml)
        assertNull(state.pendingClipboardPlainText)
    }
}
