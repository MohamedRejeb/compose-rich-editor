package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The paste branch of `applyChangeList` runs under `skipTextFieldStateSync`, so the selection it
 * writes only reaches [RichTextState.pendingSelectionDuringSync] and the buffer keeps its pre-edit
 * value. The replay must therefore read the pending-aware selection: whenever the platform replaces
 * a range wider than the caret (a composition-replacing paste), the raw getter points somewhere
 * else entirely and the pasted content lands at the wrong offset with nothing removed.
 */
class EditPipelinePasteSelectionTest {

    @Test
    fun `a paste replaces the delta original range and not the stale buffer selection`() {
        val state = RichTextState()
        state.setText("abcdefgh", TextRange(5))
        state.pendingClipboardHtml = "<p>XY</p>"
        state.pendingClipboardPlainText = "XY"

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 5, "XY")
        state.applyChangeList(buffer)

        assertEquals("abXYfgh", state.toText())
    }

    @Test
    fun `a paste over the buffer selection still replaces exactly that range`() {
        val state = RichTextState()
        state.setText("abcdefgh", TextRange(2, 5))
        state.pendingClipboardHtml = "<p>XY</p>"
        state.pendingClipboardPlainText = "XY"

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 5, "XY")
        state.applyChangeList(buffer)

        assertEquals("abXYfgh", state.toText())
    }
}
