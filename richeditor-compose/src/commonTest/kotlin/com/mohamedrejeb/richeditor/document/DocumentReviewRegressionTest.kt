package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression pins for the findings of the pre-merge code review of the document model:
 * parser-reachable states must never crash the encoder, range snapshots must not leak
 * out-of-range content, and heading or image styling must survive a full round-trip.
 */
class DocumentReviewRegressionTest {

    private fun roundTrip(state: RichTextState): RichTextDocument =
        RichTextState().setRichTextDocument(state.toRichTextDocument()).toRichTextDocument()

    @Test
    fun `negative ordered list start values are representable and round-trip`() {
        val state = RichTextState().apply { setHtml("<ol start=\"-5\"><li>a</li></ol>") }
        val doc = state.toRichTextDocument()
        assertEquals(
            RichTextBlockType.ListItem(ordered = true, indent = 0, startNumber = -5),
            doc.blocks.single().type,
        )
        assertEquals(doc, roundTrip(state))
    }

    @Test
    fun `range snapshot excludes images outside the range`() {
        val state = RichTextState().apply {
            setHtml("<p>abc<img src=\"https://secret.example/x.png\" width=\"10\" height=\"20\">def</p>")
        }
        val block = state.toRichTextDocument(TextRange(0, 3)).blocks.single()
        assertEquals("abc", block.text)
        assertEquals(emptyList(), block.spans)
    }

    @Test
    fun `range snapshot of a mid-list item preserves its visible number`() {
        val state = RichTextState().apply {
            setHtml("<ol start=\"5\"><li>a</li><li>b</li><li>c</li></ol>")
        }
        // TextRange coordinates include list prefixes, so index into the annotated text.
        val cIndex = state.annotatedString.text.indexOf("c")
        val block = state.toRichTextDocument(TextRange(cIndex, cIndex + 1)).blocks.single()
        assertEquals(RichTextBlockType.ListItem(ordered = true, indent = 0, startNumber = 7), block.type)
    }

    @Test
    fun `custom font size inside a heading survives a round-trip`() {
        val state = RichTextState().apply {
            setHtml("<h1><span style=\"font-size: 30px;\">Title</span></h1>")
        }
        val doc = state.toRichTextDocument()
        assertEquals(
            listOf<RichTextSpanMark>(
                RichTextSpanMark.FontSize(
                    range = 0..4,
                    size = TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
            ),
            doc.blocks.single().spans,
        )
        assertEquals(doc, roundTrip(state))
    }

    @Test
    fun `explicit normal weight inside a heading survives a round-trip`() {
        val state = RichTextState().apply {
            setHtml("<h1><span style=\"font-weight: 400;\">Title</span></h1>")
        }
        val doc = state.toRichTextDocument()
        assertEquals(
            listOf<RichTextSpanMark>(RichTextSpanMark.FontWeight(range = 0..4, weight = 400)),
            doc.blocks.single().spans,
        )
        assertEquals(doc, roundTrip(state))
    }

    @Test
    fun `image without dimension attributes round-trips without gaining dimensions`() {
        val state = RichTextState().apply {
            setHtml("<p><img src=\"https://e.com/i.png\">a</p>")
        }
        val doc = state.toRichTextDocument()
        val image = doc.blocks.single().spans.single() as RichTextSpanMark.Image
        assertEquals(TextUnit.Unspecified, image.width)
        assertEquals(TextUnit.Unspecified, image.height)
        assertEquals(doc, roundTrip(state))
    }
}
