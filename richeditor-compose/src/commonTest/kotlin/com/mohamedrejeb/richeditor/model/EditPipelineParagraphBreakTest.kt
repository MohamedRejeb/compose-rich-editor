package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A paragraph break must split a paragraph wherever it lands, not only at or after the caret the
 * state happened to be holding.
 *
 * `checkForParagraphs` used to start its scan at the pre-edit caret and stop as soon as it walked
 * behind it, on the theory that anything earlier had already been processed. Nothing guarantees
 * that under the BTF2 pipeline: `applyChangeList` replays a delta at the offset the platform
 * reports, which can be anywhere, while `textFieldValue.selection` still holds the caret from
 * before the batch. A newline landing behind that caret was then never turned into a paragraph, and
 * since `updateAnnotatedString` builds its text with `replace('\n', ' ')`, it surfaced as a literal
 * space: the user pressed Enter and got a space.
 *
 * The single-paragraph case worked by accident, through the #640 escape hatch that dropped the
 * threshold to zero whenever a lone paragraph held newlines. These pin the multi-paragraph case
 * that hatch did not cover. Found by `EditPipelineImeBatchFuzzTest`.
 */
class EditPipelineParagraphBreakTest {

    /** One IME batch, replayed the way the editor's `InputTransformation` replays it. */
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
    fun `an enter behind the caret splits the paragraph it lands in`() {
        val state = RichTextState()
        state.setHtml("<p>abc</p><p>def</p>")
        // The caret sits at the end of the document; the platform reports an edit behind it.
        state.selection = TextRange(7)

        state.imeBatch { replace(5, 5, "\n") }

        assertEquals("abc\nd\nef", state.toText())
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun `an enter in the first of two paragraphs splits it`() {
        val state = RichTextState()
        state.setHtml("<p>abc</p><p>def</p>")
        state.selection = TextRange(7)

        state.imeBatch { replace(2, 2, "\n") }

        assertEquals("ab\nc\ndef", state.toText())
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun `an enter after a leading empty paragraph splits the paragraph below`() {
        val state = RichTextState()
        state.setHtml("<p></p><p>abcdef</p>")
        state.selection = TextRange(7)

        state.imeBatch { replace(4, 4, "\n") }

        assertEquals("\nabc\ndef", state.toText())
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun `an enter behind the caret keeps the buffer and the model text in step`() {
        val state = RichTextState()
        state.setHtml("<p>abc</p><p>def</p>")
        state.selection = TextRange(7)

        state.imeBatch { replace(5, 5, "\n") }

        assertEquals(
            state.annotatedString.text,
            state.toText().replace('\n', ' '),
            "a break that split the model must be a separator in the buffer text",
        )
        assertEquals(state.annotatedString.text, state.textFieldState.text.toString())
    }
}
