package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A batch carrying two deltas must shift the second one by how much the text actually moved.
 *
 * `applyChangeList` used to shift each delta by the arithmetic of the deltas alone
 * (`newText.length - originalRange.length`). That holds only while the model changes the text by
 * exactly what the platform asked for, and it does not: a delta landing on a list marker drops the
 * whole `"• "` prefix and takes the paragraph out of the list, so one character in, one character
 * out still shortens the text by two. The next delta was then shifted into thin air, and
 * `applyChange`'s own bounds check threw `IllegalArgumentException` out of the editor's
 * `InputTransformation`.
 *
 * Measuring the real length change after each delta covers the injecting cases too (a marker added,
 * a list renumbered), and the ranges are clamped so a rewrite that moves text the other way
 * degrades to a bounded edit instead of a crash. Found by `EditPipelineImeBatchFuzzTest`.
 */
class EditPipelineMultiDeltaShiftTest {

    private fun RichTextState.imeBatch(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        reconcileBufferWithModel(buffer)
        val text = buffer.asCharSequence().toString()
        val selection = buffer.selection
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(text, selection)
        // The editor's selection observer, which fires on every buffer commit.
        handleSelectionChanged(textFieldState.selection, fromGestureObserver = true)
    }

    @Test
    fun `a delta that unmarks a list shifts the delta after it by the real length change`() {
        val state = RichTextState()
        state.setHtml("<ul><li>abcd</li></ul>")
        assertEquals("• abcd", state.annotatedString.text)

        // Two disjoint replaces in one batch, applied high offset first so both carry the
        // offsets the platform measured against the pre-edit text.
        state.imeBatch {
            replace(5, 6, "t")
            replace(0, 1, "w")
        }

        assertEquals("wabct", state.toText())
        assertEquals(1, state.richParagraphList.size)
    }

    @Test
    fun `a delta that merges two list items shifts the delta after it`() {
        val state = RichTextState()
        state.setHtml("<ul><li>ab</li><li>cd</li></ul>")
        assertEquals("• ab • cd", state.annotatedString.text)

        state.imeBatch {
            replace(8, 9, "t")
            replace(4, 5, "w")
        }

        assertEquals("• abwct", state.toText())
        assertEquals(1, state.richParagraphList.size)
    }

    @Test
    fun `a delta that unmarks an ordered list shifts the delta after it`() {
        val state = RichTextState()
        state.setHtml("<ol><li>abcd</li></ol>")
        assertEquals("1. abcd", state.annotatedString.text)

        state.imeBatch {
            replace(6, 7, "t")
            replace(0, 1, "w")
        }

        assertEquals("wabct", state.toText())
    }

    @Test
    fun `a two delta batch with no model injection still applies both deltas`() {
        val state = RichTextState()
        state.setHtml("<p>abcdefgh</p>")

        state.imeBatch {
            replace(6, 7, "Y")
            replace(1, 2, "X")
        }

        assertEquals("aXcdefYh", state.toText())
    }

    @Test
    fun `a two delta batch of different lengths still applies both deltas`() {
        val state = RichTextState()
        state.setHtml("<p>abcdefgh</p>")

        state.imeBatch {
            replace(6, 7, "YYY")
            replace(1, 3, "")
        }

        assertEquals("adefYYYh", state.toText())
    }
}
