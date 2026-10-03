package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Splitting a paragraph must not reorder the spans that move to the new paragraph.
 *
 * `RichParagraph.slice` hands the split span's children over to the tail span by walking them from
 * the last index down (so `removeAt` stays valid) while appending each one to the end of the new
 * list, which reverses them. The split span only has children when a styled run opened inside it,
 * so the shape that breaks is the ordinary one: a paragraph like `abc<b>def</b>ghi`, where the
 * parser nests the styled run and the text after it under the leading run. Pressing Enter anywhere
 * inside that leading run rewrote `bcdefghi` as `bcghidef`.
 *
 * Found by `EditPipelineImeBatchFuzzTest`; the split offsets that land in a later run were always
 * fine, which is why every existing single run test passed.
 */
class ParagraphSplitSpanOrderTest {

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

    private fun splitAt(html: String, at: Int): RichTextState {
        val state = RichTextState()
        state.setHtml(html)
        state.selection = TextRange(at)
        state.imeBatch {
            replace(at, at, "\n")
            selection = TextRange(at + 1)
        }
        return state
    }

    @Test
    fun `a split inside the leading run keeps the following runs in order`() {
        val html = "<p>abc<b>def</b>ghi</p>"
        for (at in 0..3) {
            val state = splitAt(html, at)
            assertEquals(
                "abcdefghi".substring(0, at) + "\n" + "abcdefghi".substring(at),
                state.toText(),
                "splitting at $at must not reorder the runs",
            )
        }
    }

    @Test
    fun `a split inside a later run keeps the following runs in order`() {
        val html = "<p>abc<b>def</b>ghi</p>"
        for (at in 4..9) {
            val state = splitAt(html, at)
            assertEquals(
                "abcdefghi".substring(0, at) + "\n" + "abcdefghi".substring(at),
                state.toText(),
                "splitting at $at must not reorder the runs",
            )
        }
    }

    @Test
    fun `a split inside the leading run keeps the styled run on its own text`() {
        val state = splitAt("<p>abc<b>def</b>ghi</p>", 1)

        assertEquals("a\nbcdefghi", state.toText())
        assertEquals("<p>a</p><p>bc<b>def</b>ghi</p>", state.toHtml())
    }

    @Test
    fun `a split inside the leading run of three styled runs keeps them in order`() {
        val html = "<p>one<b>two</b>three<i>four</i>five</p>"
        val plain = "onetwothreefourfive"
        for (at in 0..3) {
            val state = splitAt(html, at)
            assertEquals(
                plain.substring(0, at) + "\n" + plain.substring(at),
                state.toText(),
                "splitting at $at must not reorder the runs",
            )
        }
    }
}
