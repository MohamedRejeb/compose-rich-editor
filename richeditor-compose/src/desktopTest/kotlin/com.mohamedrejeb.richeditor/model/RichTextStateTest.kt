package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import org.junit.Test
import kotlin.test.assertEquals
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

}