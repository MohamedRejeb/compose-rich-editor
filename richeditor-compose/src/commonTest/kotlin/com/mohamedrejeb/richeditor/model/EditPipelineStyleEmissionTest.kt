package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the paragraph-range shapes [applyRichTextStyles] emits from. The builder appends each
 * separator inside the previous paragraph's block, so only the final range can be degenerate; the
 * emission drops collapsed ranges and appends the [EmptyLineAnchor] to render the line the
 * trailing one stands for.
 */
@OptIn(ExperimentalFoundationApi::class)
class EditPipelineStyleEmissionTest {

    private fun bufferOf(text: String) = TextFieldState(text).toTextFieldBuffer()

    @Test
    fun `an empty middle paragraph owns its separator so its range is never collapsed`() {
        val state = RichTextState().setText("a\n\nb")

        val ranges = state.annotatedString.paragraphStyles
        assertEquals(3, ranges.size)
        ranges.forEach { assertTrue(it.start != it.end, "range $it should own its separator") }
    }

    @Test
    fun `only the trailing paragraph range is collapsed`() {
        val state = RichTextState().setText("a\n")

        val ranges = state.annotatedString.paragraphStyles
        assertEquals(2, ranges.size)
        assertTrue(ranges[0].start != ranges[0].end)
        assertEquals(ranges[1].start, ranges[1].end)
    }

    @Test
    fun `emission leaves the buffer text alone when no paragraph range is collapsed`() {
        val state = RichTextState().setText("a\n\nb")
        val buffer = bufferOf(state.annotatedString.text)

        state.applyRichTextStyles(buffer)

        assertEquals(state.annotatedString.text, buffer.asCharSequence().toString())
    }

    @Test
    fun `emission appends the anchor for a trailing empty paragraph`() {
        val state = RichTextState().setText("a\n")
        val buffer = bufferOf(state.annotatedString.text)

        state.applyRichTextStyles(buffer)

        assertEquals("a $EmptyLineAnchor", buffer.asCharSequence().toString())
    }
}
