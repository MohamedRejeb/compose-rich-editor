package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit coverage for [substituteTrailingSeparatorWithNewline]. The builder puts each paragraph
 * separator inside the previous paragraph's range, so a trailing empty paragraph arrives with a
 * zero-length range that BTF2 drops; the newline makes MultiParagraph render that line instead.
 */
@OptIn(ExperimentalFoundationApi::class)
class EditPipelineTrailingParagraphTest {

    private fun range(start: Int, end: Int) =
        AnnotatedString.Range(ParagraphStyle(), start, end)

    private fun bufferOf(text: String) = TextFieldState(text).toTextFieldBuffer()

    @Test
    fun `a trailing empty paragraph turns its separator into a newline`() {
        // "a " + "" : the empty paragraph has no separator of its own.
        val buffer = bufferOf("a ")

        assertTrue(substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 2), range(2, 2))))
        assertEquals("a\n", buffer.asCharSequence().toString())
    }

    @Test
    fun `two empty paragraphs own a single separator and still render a newline`() {
        // The all-empty document: two paragraphs share one separator space.
        val buffer = bufferOf(" ")

        assertTrue(substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 1), range(1, 1))))
        assertEquals("\n", buffer.asCharSequence().toString())
    }

    @Test
    fun `the substitution keeps the buffer length so style offsets stay valid`() {
        val buffer = bufferOf("ab ")

        substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 3), range(3, 3)))

        assertEquals(3, buffer.length)
    }

    @Test
    fun `a document without a trailing empty paragraph is left untouched`() {
        val buffer = bufferOf("a b")

        assertFalse(substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 2), range(2, 3))))
        assertEquals("a b", buffer.asCharSequence().toString())
        assertEquals(0, buffer.changes.changeCount)
    }

    @Test
    fun `an empty document is left untouched`() {
        val buffer = bufferOf("")

        assertFalse(substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 0))))
        assertEquals("", buffer.asCharSequence().toString())
    }

    @Test
    fun `a preceding character that is not a separator is never consumed`() {
        val buffer = bufferOf("ab")

        assertFalse(substituteTrailingSeparatorWithNewline(buffer, listOf(range(0, 2), range(2, 2))))
        assertEquals("ab", buffer.asCharSequence().toString())
    }
}
