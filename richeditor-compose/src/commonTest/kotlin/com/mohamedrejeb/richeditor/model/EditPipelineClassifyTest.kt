package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditPipelineClassifyTest {

    @Test
    fun `single insertion records one typing history entry`() {
        val state = RichTextState()
        state.setText("ab")
        state.history.clear()
        state.applyUserEditForTest(originalRange = TextRange(2, 2), newText = "c")
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
        assertTrue(state.history.canUndo)
        state.history.undo()
        assertEquals("abc", state.toText())
    }
}
