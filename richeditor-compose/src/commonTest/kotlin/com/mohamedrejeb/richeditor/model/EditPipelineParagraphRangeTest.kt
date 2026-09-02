package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit coverage for [adjustDegenerateTrailingParagraphRanges]. The builder puts each paragraph
 * separator inside the previous paragraph's range, so a trailing empty paragraph arrives here as
 * a zero-length range that BTF2 would drop.
 */
class EditPipelineParagraphRangeTest {

    private fun range(start: Int, end: Int, align: TextAlign = TextAlign.Unspecified) =
        AnnotatedString.Range(ParagraphStyle(textAlign = align), start, end)

    private fun bounds(ranges: List<AnnotatedString.Range<ParagraphStyle>>) =
        ranges.map { it.start to it.end }

    @Test
    fun `a list without degenerate ranges is returned unchanged`() {
        val ranges = listOf(range(0, 2), range(2, 4), range(4, 5))

        assertSame(ranges, adjustDegenerateTrailingParagraphRanges(ranges))
    }

    @Test
    fun `a single trailing empty paragraph re-owns the separator`() {
        // "a " + "" : the empty paragraph has no separator of its own.
        val ranges = listOf(range(0, 2), range(2, 2))

        assertEquals(
            listOf(0 to 1, 1 to 2),
            bounds(adjustDegenerateTrailingParagraphRanges(ranges)),
        )
    }

    @Test
    fun `two trailing empty paragraphs cascade the shrink`() {
        // "a " + " " + "" : only the last paragraph is degenerate to start with.
        val ranges = listOf(range(0, 2), range(2, 3), range(3, 3))

        assertEquals(
            listOf(0 to 1, 1 to 2, 2 to 3),
            bounds(adjustDegenerateTrailingParagraphRanges(ranges)),
        )
    }

    @Test
    fun `the single range of an empty document is dropped`() {
        val ranges = listOf(range(0, 0))

        assertEquals(emptyList(), adjustDegenerateTrailingParagraphRanges(ranges))
    }

    @Test
    fun `a middle empty paragraph is left alone`() {
        // "a " + " " + "b" : the middle paragraph already owns its own separator.
        val ranges = listOf(range(0, 2), range(2, 3), range(3, 4))

        assertSame(ranges, adjustDegenerateTrailingParagraphRanges(ranges))
    }

    @Test
    fun `the paragraph style of each range survives the shift`() {
        val ranges = listOf(
            range(0, 2, TextAlign.Center),
            range(2, 2, TextAlign.End),
        )

        val adjusted = adjustDegenerateTrailingParagraphRanges(ranges)

        assertEquals(TextAlign.Center, adjusted[0].item.textAlign)
        assertEquals(TextAlign.End, adjusted[1].item.textAlign)
    }
}
