package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit coverage for the trailing empty paragraph. The builder puts each paragraph separator
 * inside the previous paragraph's range, so a trailing empty paragraph arrives with a zero-length
 * range that BTF2 drops; the output buffer appends the [EmptyLineAnchor] for it instead.
 */
@OptIn(ExperimentalFoundationApi::class)
class EditPipelineTrailingParagraphTest {

    private fun range(start: Int, end: Int) =
        AnnotatedString.Range(ParagraphStyle(), start, end)

    private fun outputOf(state: RichTextState): String {
        val buffer = TextFieldState(state.annotatedString.text).toTextFieldBuffer()
        state.applyRichTextStyles(buffer)
        return buffer.asCharSequence().toString()
    }

    @Test
    fun `a collapsed last range at the end of the text is the trailing empty paragraph`() {
        val ranges = listOf(range(0, 2), range(2, 2))

        assertSame(ranges.last(), trailingEmptyParagraphRange(ranges, textLength = 2))
    }

    @Test
    fun `an empty document is a trailing empty paragraph`() {
        val ranges = listOf(range(0, 0))

        assertSame(ranges.last(), trailingEmptyParagraphRange(ranges, textLength = 0))
    }

    @Test
    fun `a last range that holds text is not one`() {
        assertNull(trailingEmptyParagraphRange(listOf(range(0, 2), range(2, 3)), textLength = 3))
    }

    @Test
    fun `a collapsed range before the last one is never mistaken for the trailing one`() {
        assertNull(
            trailingEmptyParagraphRange(listOf(range(0, 2), range(2, 2), range(2, 5)), textLength = 5)
        )
    }

    @Test
    fun `a collapsed last range that is not at the end of the text is not one`() {
        assertNull(trailingEmptyParagraphRange(listOf(range(0, 2), range(2, 2)), textLength = 3))
    }

    @Test
    fun `the output appends the anchor after the model text and keeps the separator`() {
        val state = RichTextState().setText("a\n")

        assertEquals(state.annotatedString.text + EmptyLineAnchor, outputOf(state))
    }

    @Test
    fun `two empty paragraphs own a single separator and the anchor`() {
        val state = RichTextState().setText("\n")

        assertEquals(" $EmptyLineAnchor", outputOf(state))
    }

    @Test
    fun `an empty document renders the anchor alone`() {
        val state = RichTextState()

        assertEquals(EmptyLineAnchor, outputOf(state))
    }

    @Test
    fun `a document without a trailing empty paragraph is left untouched`() {
        val state = RichTextState().setText("a\nb")

        assertEquals(state.annotatedString.text, outputOf(state))
    }
}
