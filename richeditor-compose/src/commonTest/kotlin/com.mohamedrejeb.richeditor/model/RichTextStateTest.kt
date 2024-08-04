package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.*

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

    @OptIn(ExperimentalRichTextApi::class)
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

        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), richTextState.currentSpanStyle)
        assertTrue(richTextState.isCodeSpan)

        // Delete All text
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "",
                selection = TextRange.Zero,
            )
        )

        // Check that the style is preserved
        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), richTextState.currentSpanStyle)
        assertTrue(richTextState.isCodeSpan)

        // Add some text
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "New text",
                selection = TextRange(8),
            )
        )

        // Check that the style is preserved
        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), richTextState.currentSpanStyle)
        assertTrue(richTextState.isCodeSpan)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testResetStylingOnMultipleNewLine() {
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
        richTextState.selection = TextRange(0, richTextState.annotatedString.text.length)
        richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        richTextState.addCodeSpan()

        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), richTextState.currentSpanStyle)
        assertTrue(richTextState.isCodeSpan)

        // Add new line
        val newText = "${richTextState.annotatedString.text}\n"
        richTextState.selection = TextRange(richTextState.annotatedString.text.length)
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newText.length),
            )
        )

        // Check that the style is preserved
        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), richTextState.currentSpanStyle)
        assertTrue(richTextState.isCodeSpan)

        // Add new line
        val newText2 = "${richTextState.annotatedString.text}\n"
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = newText2,
                selection = TextRange(newText2.length),
            )
        )

        // Check that the style is being reset
        assertEquals(SpanStyle(), richTextState.currentSpanStyle)
        assertFalse(richTextState.isCodeSpan)
    }

    @OptIn(ExperimentalRichTextApi::class)
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

    @OptIn(ExperimentalRichTextApi::class)
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

    @OptIn(ExperimentalRichTextApi::class)
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

    @OptIn(ExperimentalRichTextApi::class)
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
                            richSpanStyle = RichSpanStyle.Code(),
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

    @Test
    fun testGetSpanStyle() {
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

                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Get the style by text range
        assertEquals(
            SpanStyle(fontWeight = FontWeight.Bold),
            richTextState.getSpanStyle(TextRange(0, 4)),
        )

        assertEquals(
            SpanStyle(),
            richTextState.getSpanStyle(TextRange(9, 19)),
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testGetRichSpanStyle() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                            richSpanStyle = RichSpanStyle.Code(),
                        ),
                    )

                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Get the style by text range
        assertEquals(
            RichSpanStyle.Code(),
            richTextState.getRichSpanStyle(TextRange(0, 4)),
        )

        assertEquals(
            RichSpanStyle.Default,
            richTextState.getRichSpanStyle(TextRange(9, 19)),
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testGetParagraphStyle() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                    paragraphStyle = ParagraphStyle(
                        textAlign = TextAlign.Center,
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
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

        // Get the style by text range
        assertEquals(
            ParagraphStyle(
                textAlign = TextAlign.Center,
            ),
            richTextState.getParagraphStyle(TextRange(0, 4)),
        )

        assertEquals(
            ParagraphStyle(),
            richTextState.getParagraphStyle(TextRange(19, 21)),
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testGetParagraphType() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Testing some text",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
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

        // Get the style by text range
        assertEquals(
            UnorderedList::class,
            richTextState.getParagraphType(TextRange(0, 4))::class,
        )

        assertEquals(
            DefaultParagraph::class,
            richTextState.getParagraphType(TextRange(19, 21))::class,
        )
    }

}