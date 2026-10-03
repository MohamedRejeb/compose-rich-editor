package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The pure document filter behind [RichTextConfig.features]: every mark and block attribute
 * maps to one [RichTextFeature], a disallowed feature strips the styling and keeps the text,
 * except images, which have no text form and are removed with their placeholder character.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextFeatureFilterTest {

    private object Marker : RichSpanStyle {
        override fun getSpanStyle(config: RichTextConfig): SpanStyle = SpanStyle()
    }

    private fun doc(vararg blocks: RichTextBlock) = RichTextDocument(blocks.toList())

    private fun block(text: String, vararg spans: RichTextSpanMark) = RichTextBlock(text, spans = spans.toList())

    private val spanMarks: List<Pair<RichTextSpanMark, RichTextFeature>> = listOf(
        RichTextSpanMark.Bold(0..4) to RichTextFeature.Bold,
        RichTextSpanMark.Italic(0..4) to RichTextFeature.Italic,
        RichTextSpanMark.Underline(0..4) to RichTextFeature.Underline,
        RichTextSpanMark.Strikethrough(0..4) to RichTextFeature.Strikethrough,
        RichTextSpanMark.CodeSpan(0..4) to RichTextFeature.CodeSpan,
        RichTextSpanMark.Link(0..4, url = "https://example.com") to RichTextFeature.Link,
        RichTextSpanMark.TextColor(0..4, argb = 0xFFFF0000) to RichTextFeature.TextColor,
        RichTextSpanMark.Highlight(0..4, argb = 0xFFFFFF00) to RichTextFeature.Highlight,
        RichTextSpanMark.FontSize(0..4, size = 20.sp) to RichTextFeature.FontSize,
        RichTextSpanMark.FontWeight(0..4, weight = 300) to RichTextFeature.FontWeight,
        RichTextSpanMark.LetterSpacing(0..4, size = 2.sp) to RichTextFeature.LetterSpacing,
        RichTextSpanMark.BaselineShift(0..4, multiplier = 0.5f) to RichTextFeature.BaselineShift,
        RichTextSpanMark.Shadow(0..4, argb = 0xFF000000, offsetX = 1f, offsetY = 1f, blurRadius = 1f) to RichTextFeature.Shadow,
        RichTextSpanMark.Token(0..4, trigger = "@", id = "1", label = "@user") to RichTextFeature.Token,
        RichTextSpanMark.Custom(0..4, style = Marker) to RichTextFeature.CustomSpanStyle,
    )

    @Test
    fun `All returns the same document instance`() {
        val document = doc(block("Hello", RichTextSpanMark.Bold(0..4)), RichTextBlock("Two", headingLevel = 2))

        assertSame(document, document.restrictedTo(RichTextFeature.All))
    }

    @Test
    fun `each span mark is removed when its feature is disallowed`() {
        spanMarks.forEach { (mark, feature) ->
            val filtered = doc(block("Hello", mark)).restrictedTo(RichTextFeature.All - feature)

            assertEquals(doc(block("Hello")), filtered, "disallowing $feature should remove $mark")
        }
    }

    @Test
    fun `each span mark is kept when only its feature is allowed`() {
        spanMarks.forEach { (mark, feature) ->
            val document = doc(block("Hello", mark))

            assertEquals(document, document.restrictedTo(setOf(feature)), "allowing $feature should keep $mark")
        }
    }

    @Test
    fun `an empty feature set keeps the text and the paragraph breaks only`() {
        val document = doc(
            RichTextBlock("Title", headingLevel = 1, spans = listOf(RichTextSpanMark.Italic(0..4))),
            RichTextBlock("item", type = RichTextBlockType.ListItem(ordered = true), textAlign = TextAlign.Center),
            RichTextBlock("tail", isLineBreak = true),
        )

        assertEquals(
            doc(RichTextBlock("Title"), RichTextBlock("item"), RichTextBlock("tail", isLineBreak = true)),
            document.restrictedTo(emptySet()),
        )
    }

    @Test
    fun `unknown marks are always removed`() {
        val document = doc(block("Hello", RichTextSpanMark.Unknown(0..4, kind = "x", rawJson = "{}")))

        assertEquals(doc(block("Hello")), document.restrictedTo(RichTextFeature.All - RichTextFeature.Bold))
    }

    @Test
    fun `disallowed lists flatten to paragraphs and keep their text`() {
        val document = doc(
            RichTextBlock("a", type = RichTextBlockType.ListItem(ordered = true, indent = 1, startNumber = 3)),
            RichTextBlock("b", type = RichTextBlockType.ListItem(ordered = false)),
        )

        assertEquals(
            doc(RichTextBlock("a"), RichTextBlock("b", type = RichTextBlockType.ListItem(ordered = false))),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.OrderedList),
        )
        assertEquals(
            doc(RichTextBlock("a", type = RichTextBlockType.ListItem(ordered = true, indent = 1, startNumber = 3)), RichTextBlock("b")),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.UnorderedList),
        )
    }

    @Test
    fun `a disallowed heading flattens and keeps the marks the author wrote`() {
        val document = doc(RichTextBlock("Title", headingLevel = 2, spans = listOf(RichTextSpanMark.Bold(0..1))))

        assertEquals(
            doc(block("Title", RichTextSpanMark.Bold(0..1))),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.Heading),
        )
        assertEquals(
            doc(RichTextBlock("Title")),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.Heading - RichTextFeature.Bold),
        )
    }

    @Test
    fun `disallowed paragraph style resets alignment direction indent and line height`() {
        val document = doc(
            RichTextBlock(
                "x",
                textAlign = TextAlign.End,
                textDirection = TextDirection.Rtl,
                textIndent = TextIndent(firstLine = 4.sp),
                lineHeight = 30.sp,
                isLineBreak = true,
            ),
        )

        assertEquals(
            doc(RichTextBlock("x", isLineBreak = true)),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.ParagraphStyle),
        )
    }

    @Test
    fun `a disallowed image is removed with its placeholder and later marks shift`() {
        val document = doc(
            block(
                "ab￼cd",
                RichTextSpanMark.Bold(0..1),
                RichTextSpanMark.Image(2..2, url = "https://example.com/i.png"),
                RichTextSpanMark.Italic(3..4),
                RichTextSpanMark.Underline(1..3),
            ),
        )

        assertEquals(
            doc(
                block(
                    "abcd",
                    RichTextSpanMark.Bold(0..1),
                    RichTextSpanMark.Italic(2..3),
                    RichTextSpanMark.Underline(1..2),
                ),
            ),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.Image),
        )
    }

    @Test
    fun `an explicit full set short-circuits like All`() {
        val document = doc(block("Hello", RichTextSpanMark.Bold(0..4)))

        assertSame(document, document.restrictedTo(RichTextFeature.entries.toSet()))
    }

    @Test
    fun `a blank paragraph survives a restrictive filter`() {
        val document = doc(block("a"), block(""), block("b"))

        assertEquals(document, document.restrictedTo(emptySet()))
    }

    @Test
    fun `images at the block edges and in pairs are removed and marks on them dropped`() {
        val document = doc(
            block(
                "\uFFFCa\uFFFC\uFFFCb\uFFFC",
                RichTextSpanMark.Image(0..0, url = "u"),
                RichTextSpanMark.Image(2..2, url = "u"),
                RichTextSpanMark.Image(3..3, url = "u"),
                RichTextSpanMark.Image(5..5, url = "u"),
                RichTextSpanMark.Bold(0..5),
                RichTextSpanMark.Italic(2..3),
                RichTextSpanMark.Underline(3..4),
            ),
        )

        assertEquals(
            doc(block("ab", RichTextSpanMark.Bold(0..1), RichTextSpanMark.Underline(1..1))),
            document.restrictedTo(RichTextFeature.All - RichTextFeature.Image),
        )
    }

    @Test
    fun `a block emptied by image removal is dropped`() {
        val document = doc(
            block("￼", RichTextSpanMark.Image(0..0, url = "https://example.com/i.png")),
            block("text"),
        )

        assertEquals(doc(block("text")), document.restrictedTo(RichTextFeature.All - RichTextFeature.Image))
    }

    @Test
    fun `a document reduced to nothing becomes the empty document`() {
        val document = doc(block("￼", RichTextSpanMark.Image(0..0, url = "https://example.com/i.png")))

        assertEquals(RichTextDocument.empty(), document.restrictedTo(emptySet()))
    }

    @Test
    fun `allowed images stay in place`() {
        val document = doc(block("a￼b", RichTextSpanMark.Image(1..1, url = "https://example.com/i.png")))

        assertEquals(document, document.restrictedTo(setOf(RichTextFeature.Image)))
    }
}
