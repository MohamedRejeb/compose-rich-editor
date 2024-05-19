package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RichTextStateTest {

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testApplyStyleToLink() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Before Link After",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(6, 9)
        richTextState.addLinkToSelection("https://www.google.com")

        richTextState.selection = TextRange(1, 12)
        richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        richTextState.selection = TextRange(7)
        assertTrue(richTextState.isLink)
    }

    @Test
    fun testPreserveStyleOnRemoveAllCharacters() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Add some styling
        richTextState.selection = TextRange(0, 4)
        richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        richTextState.addCodeSpan()

        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))
        assertTrue(richTextState.isCodeSpan)

        // Delete All text
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "",
                selection = TextRange.Zero,
            )
        )

        // Check that the style is preserved
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))
        assertTrue(richTextState.isCodeSpan)

        // Add some text
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "New text",
                selection = TextRange(8),
            )
        )

        // Check that the style is preserved
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))
        assertTrue(richTextState.isCodeSpan)
    }

    @Test
    fun testAddSpanStyleByTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Add some styling by text range
        richTextState.addSpanStyle(
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
            textRange = TextRange(0, 4),
        )

        // In the middle
        richTextState.selection = TextRange(2)
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        // In the edges
        richTextState.selection = TextRange(0)
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        richTextState.selection = TextRange(4)
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        // Outside the range
        richTextState.selection = TextRange(5)
        assertNotEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))
    }

    @Test
    fun testRemoveSpanStyleByTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
                        ),
                    )
                }
            )
        )

        // Remove some styling by text range
        richTextState.removeSpanStyle(
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
            textRange = TextRange(0, 4),
        )

        // In the middle
        richTextState.selection = TextRange(2)
        assertNotEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        // In the edges
        richTextState.selection = TextRange(0)
        assertNotEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        richTextState.selection = TextRange(4)
        assertNotEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))

        // Outside the range
        richTextState.selection = TextRange(5)
        assertEquals(richTextState.currentSpanStyle, SpanStyle(fontWeight = FontWeight.Bold))
    }

    @Test
    fun testAddRichSpanStyleByTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Add some styling by text range
        richTextState.addRichSpan(
            spanStyle = RichSpanStyle.Code(),
            textRange = TextRange(0, 4),
        )

        // In the middle
        richTextState.selection = TextRange(2)
        assertEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        // In the edges
        richTextState.selection = TextRange(0)
        assertEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        richTextState.selection = TextRange(4)
        assertEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        // Outside the range
        richTextState.selection = TextRange(5)
        assertNotEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)
    }

    @Test
    fun testRemoveRichSpanStyleByTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                            richSpansStyle = RichSpanStyle.Code(),
                        ),
                    )
                }
            )
        )

        // Remove some styling by text range
        richTextState.removeRichSpan(
            spanStyle = RichSpanStyle.Code(),
            textRange = TextRange(0, 4),
        )

        // In the middle
        richTextState.selection = TextRange(2)
        assertNotEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        // In the edges
        richTextState.selection = TextRange(0)
        assertNotEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        richTextState.selection = TextRange(4)
        assertNotEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)

        // Outside the range
        richTextState.selection = TextRange(5)
        assertEquals(richTextState.currentRichSpanStyle::class, RichSpanStyle.Code::class)
    }

}