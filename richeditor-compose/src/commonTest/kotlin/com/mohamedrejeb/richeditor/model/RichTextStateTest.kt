package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.*

@ExperimentalRichTextApi
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
    fun testClearSpanStyles() {
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

        val boldSpan = SpanStyle(fontWeight = FontWeight.Bold)
        val italicSpan = SpanStyle(fontStyle = FontStyle.Italic)
        val defaultSpan = SpanStyle()

        richTextState.addSpanStyle(
            spanStyle = boldSpan,
            // "Testing some" is bold.
            textRange = TextRange(0, 12),
        )
        richTextState.addSpanStyle(
            spanStyle = italicSpan,
            // "some text" is italic.
            textRange = TextRange(8, 17),
        )

        richTextState.selection = TextRange(8, 12)
        // Clear spans of "some".
        richTextState.clearSpanStyles()

        assertEquals(defaultSpan, richTextState.currentSpanStyle)
        richTextState.selection = TextRange(0, 8)
        // "Testing" is bold.
        assertEquals(boldSpan, richTextState.currentSpanStyle)
        richTextState.selection = TextRange(8, 12)
        // "some" is the default.
        assertEquals(defaultSpan, richTextState.currentSpanStyle)
        richTextState.selection = TextRange(12, 17)
        // "text" is italic.
        assertEquals(italicSpan, richTextState.currentSpanStyle)

        // Clear all spans.
        richTextState.clearSpanStyles(TextRange(0, 17))

        assertEquals(defaultSpan, richTextState.currentSpanStyle)
        richTextState.selection = TextRange(0, 17)
        assertEquals(defaultSpan, richTextState.currentSpanStyle)
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

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testClearRichSpanStyles() {
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

        val codeSpan = RichSpanStyle.Code()
        val linkSpan = RichSpanStyle.Link("https://example.com")
        val defaultSpan = RichSpanStyle.Default

        richTextState.addRichSpan(
            spanStyle = codeSpan,
            // "Testing some" is the code.
            textRange = TextRange(0, 12),
        )
        richTextState.addRichSpan(
            spanStyle = linkSpan,
            // "some text" is the link.
            textRange = TextRange(8, 17),
        )

        richTextState.selection = TextRange(8, 12)
        // Clear spans of "some".
        richTextState.clearRichSpans()

        assertEquals(defaultSpan, richTextState.currentRichSpanStyle)
        richTextState.selection = TextRange(0, 8)
        // "Testing" is the code.
        assertEquals(codeSpan, richTextState.currentRichSpanStyle)
        richTextState.selection = TextRange(8, 12)
        // "some" is the default.
        assertEquals(defaultSpan, richTextState.currentRichSpanStyle)
        richTextState.selection = TextRange(12, 17)
        // "text" is the link.
        assertEquals(linkSpan, richTextState.currentRichSpanStyle)

        // Clear all spans.
        richTextState.clearRichSpans(TextRange(0, 17))

        assertEquals(defaultSpan, richTextState.currentRichSpanStyle)
        richTextState.selection = TextRange(0, 17)
        assertEquals(defaultSpan, richTextState.currentRichSpanStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
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

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testToText() {
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

        assertEquals("Testing some text\nTesting some text", richTextState.toText())
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testTextCorrection() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hilo",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "b",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(2)
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello b",
                selection = TextRange(5),
            )
        )

        assertEquals("Hello\nb", richTextState.toText())
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testKeepStyleChangesOnLineBreak() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(5)
        richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        richTextState.toggleCodeSpan()
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello\n",
                selection = TextRange(6),
            )
        )

        assertEquals("Hello\n", richTextState.toText())
        assertEquals(SpanStyle(fontStyle = FontStyle.Italic), richTextState.currentSpanStyle)
        assertIs<RichSpanStyle.Code>(richTextState.currentRichSpanStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testKeepSpanStylesOnLineBreakOnTheMiddleOrParagraph() {
        val spanStyle = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)

        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                            spanStyle = spanStyle,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(3)
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hel\nlo",
                selection = TextRange(4),
            )
        )

        assertEquals("Hel\nlo", richTextState.toText())
        assertEquals(spanStyle, richTextState.currentSpanStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testResetRichSpanStylesOnLineBreakOnTheMiddleOrParagraph() {

        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                            richSpanStyle = RichSpanStyle.Code(),
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(3)
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hel\nlo",
                selection = TextRange(4),
            )
        )

        assertEquals("Hel\nlo", richTextState.toText())
        assertIs<RichSpanStyle.Default>(richTextState.currentRichSpanStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testUpdateSelectionOnAddOrderedListItem() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                    type = OrderedList(1),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(5)

        // Add new line which is going to add a new list item
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello\n",
                selection = TextRange(6),
            )
        )

        // Mimic undo adding new list item
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello",
                selection = TextRange(5),
            )
        )

//        assertEquals("1. Hello", richTextState.toText())
//        assertEquals(TextRange(5), richTextState.selection)
    }

    @Test
    fun testMergeTwoListItemsByRemovingLineBreak() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 1,
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(6)

        // Remove line break
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "• aaa• bbb",
                selection = TextRange(5),
            )
        )

        assertEquals("• aaabbb", richTextState.toText())
        assertEquals(TextRange(5), richTextState.selection)
    }

    @Test
    fun testUndoAddingOrderedListItem() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                    type = OrderedList(1),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(5)

        // Add new line which is going to add a new list item
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello\n",
                selection = TextRange(9),
            )
        )

        // Mimic undo adding new list item
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello",
                selection = TextRange(8),
            )
        )

        assertEquals("1. Hello", richTextState.toText())
        assertEquals(TextRange(8), richTextState.selection)
    }

    @Test
    fun testRemoveTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Remove the text range
        richTextState.removeTextRange(TextRange(0, 5))

        assertEquals("", richTextState.toText())
        assertEquals(TextRange(0), richTextState.selection)
    }

    @Test
    fun testRemoveTextRange2() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello World!",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Rich Editor",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(richTextState.textFieldValue.text.length)

        // Remove the text range
        richTextState.removeTextRange(TextRange(0, 5))

        assertEquals(" World!\nRich Editor", richTextState.toText())
        assertEquals(TextRange(0), richTextState.selection)
    }

    @Test
    fun testRemoveSelectedText() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Select the text
        richTextState.selection = TextRange(0, 5)

        // Remove the selected text
        richTextState.removeSelectedText()

        assertEquals("", richTextState.toText())
        assertEquals(TextRange(0), richTextState.selection)
    }

    @Test
    fun testAddTextAtIndex() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Add text at index
        richTextState.addTextAtIndex(5, " World")

        assertEquals("Hello World", richTextState.toText())
        assertEquals(TextRange(11), richTextState.selection)
    }

    @Test
    fun testAddTextAfterSelection() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Select the text
        richTextState.selection = TextRange(5)

        // Add text after selection
        richTextState.addTextAfterSelection(" World")

        assertEquals("Hello World", richTextState.toText())
        assertEquals(TextRange(11), richTextState.selection)
    }

    @Test
    fun testReplaceTextRange() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Replace the text range
        richTextState.replaceTextRange(TextRange(0, 5), "Hi")

        assertEquals("Hi", richTextState.toText())
        assertEquals(TextRange(2), richTextState.selection)
    }

    @Test
    fun testReplaceTextRange2() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello World!",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Rich Editor",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(richTextState.textFieldValue.text.length)

        // Replace the text range
        richTextState.replaceTextRange(TextRange(0, 5), "Hi")

        assertEquals("Hi World!\nRich Editor", richTextState.toText())
        assertEquals(TextRange(2), richTextState.selection)
    }

    @Test
    fun testReplaceSelectedText() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Select the text
        richTextState.selection = TextRange(0, 5)

        // Replace the selected text
        richTextState.replaceSelectedText("Hi")

        assertEquals("Hi", richTextState.toText())
        assertEquals(TextRange(2), richTextState.selection)
    }

    @Test
    fun testDeletingMultipleEmptyParagraphs() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    key = 2,
                ),
                RichParagraph(
                    key = 3,
                ),
                RichParagraph(
                    key = 4,
                ),
                RichParagraph(
                    key = 5,
                ),
            )
        )

        // Select the text
        richTextState.selection = TextRange(9, 6)

        // Remove the selected text
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello ",
                selection = TextRange(6),
            )
        )

        assertEquals(2, richTextState.richParagraphList.size)
    }

    fun testAutoRecognizeOrderedListUtil(number: Int) {
        val state = RichTextState()
        val text = "$number. "

        state.onTextFieldValueChange(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length),
            )
        )

        val orderedList = state.richParagraphList.first().type

        assertIs<OrderedList>(orderedList)
        assertEquals(number, orderedList.number)
        assertTrue(state.isOrderedList)
    }

    @Test
    fun testAutoRecognizeOrderedList() {
        testAutoRecognizeOrderedListUtil(1)
        testAutoRecognizeOrderedListUtil(28)
    }

    @Test
    fun testAutoRecognizeUnorderedList() {
        val state = RichTextState()

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "- ",
                selection = TextRange(2),
            )
        )

        val orderedList = state.richParagraphList.first().type

        assertIs<UnorderedList>(orderedList)
        assertTrue(state.isUnorderedList)
    }

    @Test
    fun testRemoveCharactersWithLevel() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "CD",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(state.textFieldValue.text.length - 5)
        val before = state.textFieldValue.text.substring(0, state.textFieldValue.text.length - 6)
        val after = state.textFieldValue.text.substring(state.textFieldValue.text.length - 5)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = before + after,
                selection = TextRange(state.textFieldValue.text.length - 6),
            )
        )

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.level)
        assertEquals(2, thirdParagraphType.level)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(2, fourthParagraphType.number)
        assertEquals(1, fourthParagraphType.level)
    }

    @Test
    fun testAddOrderedListWithLevel1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(state.textFieldValue.text.length - 5)
        state.toggleOrderedList()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.level)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(1, fourthParagraphType.number)
        assertEquals(1, fourthParagraphType.level)
    }

    @Test
    fun testAddOrderedListWithLevel2() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(state.textFieldValue.text.length - 5)
        state.toggleOrderedList()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.level)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(3, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.level)
    }

    @Test
    fun testAddUnorderedListWithLevel1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 3,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        state.selection = TextRange(thirdParagraph.getFirstNonEmptyChild()!!.fullTextRange.min)
        state.toggleUnorderedList()

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)

        assertIs<UnorderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.level)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(1, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.level)
    }

    @Test
    fun testIncreaseListLevelSimple1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.increaseListLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)

        assertIs<UnorderedList>(thirdParagraphType)
        assertEquals(3, thirdParagraphType.level)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(2, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.level)
    }

    @Test
    fun testIncreaseListLevelSimple2() {
        val state = RichTextState()

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1.",
                selection = TextRange(2),
            )
        )

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. ",
                selection = TextRange(3),
            )
        )

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello",
                selection = TextRange(8),
            )
        )

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello \n",
                selection = TextRange(10),
            )
        )

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hello 2. World",
                selection = TextRange(17),
            )
        )

        state.increaseListLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.level)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)
    }

    @Test
    fun testIncreaseListLevelComplex() {
        /**
         * Initial:
         * 1. A
         * 2. A
         *     1. A
         *         1. A
         * 1. A
         *
         * Expected:
         * 1. A
         *    1. A
         *      1. A
         *          1. A
         * 2. A
         */
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 3,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
            )
        )

        state.selection = TextRange(6, 12)
        state.increaseListLevel()

        val pOne = state.richParagraphList[0].type
        val pTwo = state.richParagraphList[1].type
        val pThree = state.richParagraphList[2].type
        val pFour = state.richParagraphList[3].type
        val pFive = state.richParagraphList[4].type

        assertIs<OrderedList>(pOne)
        assertEquals(1, pOne.number)
        assertEquals(1, pOne.level)

        assertIs<OrderedList>(pTwo)
        assertEquals(1, pTwo.number)
        assertEquals(2, pTwo.level)

        assertIs<OrderedList>(pThree)
        assertEquals(1, pThree.number)
        assertEquals(3, pThree.level)

        assertIs<OrderedList>(pFour)
        assertEquals(1, pFour.number)
        assertEquals(4, pFour.level)

        assertIs<OrderedList>(pFive)
        assertEquals(2, pFive.number)
        assertEquals(1, pFive.level)
    }

    @Test
    fun testCanIncreaseListLevelCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "World",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(6)
        val selectedParagraphs1 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canIncreaseListLevel(selectedParagraphs1))

        state.selection = TextRange(9)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canIncreaseListLevel(selectedParagraphs2))
        assertFalse(state.canIncreaseListLevel)

        state.selection = TextRange(20)
        val selectedParagraphs3 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canIncreaseListLevel(selectedParagraphs3))
        assertTrue(state.canIncreaseListLevel)
    }

    @Test
    fun testCanIncreaseListLevelNonCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "World",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(6, 15)
        val selectedParagraphs1 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canIncreaseListLevel(selectedParagraphs1))
        assertFalse(state.canIncreaseListLevel)

        state.selection = TextRange(18, 23)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canIncreaseListLevel(selectedParagraphs2))
        assertTrue(state.canIncreaseListLevel)
    }

    @Test
    fun testDecreaseListLevelSimple1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "World",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(9)

        state.decreaseListLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.level)
        assertEquals(2, secondParagraphType.number)
        assertEquals(1, secondParagraphType.level)
    }

    @Test
    fun testDecreaseListLevelSimple2() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]

        state.selection = TextRange(secondParagraph.getFirstNonEmptyChild()!!.fullTextRange.min)

        state.decreaseListLevel()

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.level)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(1, secondParagraphType.level)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(1, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.level)
    }

    @Test
    fun testDecreaseListLevelComplex() {
        /**
         * Initial:
         * 1. A
         *    1. A
         *      1. A
         *          1. A
         *  2. A
         *
         * Expected:
         * 1. A
         * 2. A
         *    1. A
         *      1. A
         *  3. A
         */
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 4,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
            )
        )

        state.selection = TextRange(5, 12)
        state.decreaseListLevel()

        val pOne = state.richParagraphList[0].type
        val pTwo = state.richParagraphList[1].type
        val pThree = state.richParagraphList[2].type
        val pFour = state.richParagraphList[3].type
        val pFive = state.richParagraphList[4].type

        assertIs<OrderedList>(pOne)
        assertEquals(1, pOne.number)
        assertEquals(1, pOne.level)

        assertIs<OrderedList>(pTwo)
        assertEquals(2, pTwo.number)
        assertEquals(1, pTwo.level)

        assertIs<OrderedList>(pThree)
        assertEquals(1, pThree.number)
        assertEquals(2, pThree.level)

        assertIs<OrderedList>(pFour)
        assertEquals(1, pFour.number)
        assertEquals(3, pFour.level)

        assertIs<OrderedList>(pFive)
        assertEquals(3, pFive.number)
        assertEquals(1, pFive.level)
    }

    @Test
    fun testCanDecreaseListLevelCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "World",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        state.selection = TextRange(6)
        val selectedParagraphs1 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canDecreaseListLevel(selectedParagraphs1))
        assertFalse(state.canDecreaseListLevel)

        state.selection = TextRange(9)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canDecreaseListLevel(selectedParagraphs2))
        assertTrue(state.canDecreaseListLevel)
    }

    @Test
    fun testCanDecreaseListLevelNonCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "World",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        )
                    )
                },
            )
        )

        state.selection = TextRange(9, 6)
        val selectedParagraphs1 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canDecreaseListLevel(selectedParagraphs1))
        assertFalse(state.canDecreaseListLevel)

        state.selection = TextRange(9, 16)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canDecreaseListLevel(selectedParagraphs2))
        assertTrue(state.canDecreaseListLevel)
    }

    @Test
    fun testAddingTwoConsecutiveLineBreaks() {
        val state = RichTextState()

        state.setText("Hello")

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello\n",
                selection = TextRange(6),
            )
        )

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello \n",
                selection = TextRange(7),
            )
        )

        assertEquals(3, state.richParagraphList.size)
        assertEquals("Hello\n\n", state.toText())
    }

    /**
     * Test to mimic the behavior of the Android suggestion.
     * Can only reproduced on real device.
     *
     * [420](https://github.com/MohamedRejeb/compose-rich-editor/issues/420)
     */
    @Test
    fun testMimicAndroidSuggestion() {
        val richTextState = RichTextState()

        richTextState.setHtml(
            """
                <p>Hi </p>
                <p>World! </p>
            """.trimIndent()
        )

        // Select the text
        richTextState.selection = TextRange(3)

        // Add text after selection
        // What's happening is that the space added after "Kotlin" from the suggestion is being removed.
        // It's been considered as the trailing space for the paragraph.
        // Which will lead to the selection being at the start of the next paragraph.
        // To fix this we need to add a space after the selection.
        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "Hi Kotlin World! ",
                selection = TextRange(10)
            )
        )

        assertEquals(TextRange(10), richTextState.selection)
        assertEquals("Hi Kotlin  World! ", richTextState.annotatedString.text)
    }

    @Test
    fun testIsUnorderedListStateWithSingleParagraph() {
        val richTextState = RichTextState()

        assertFalse(richTextState.isUnorderedList)

        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "- ",
                selection = TextRange(2),
            )
        )

        assertTrue(richTextState.isUnorderedList)

        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            )
        )

        assertFalse(richTextState.isUnorderedList)
    }

    @Test
    fun testIsUnorderedListStateWithMultipleParagraphs() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = DefaultParagraph(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "ccc",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Selecting single unordered list paragraph
        richTextState.selection = TextRange(6)

        assertTrue(richTextState.isUnorderedList)

        // Selecting single default paragraph
        richTextState.selection = TextRange(12)

        assertFalse(richTextState.isUnorderedList)

        // Selecting multiple unordered list paragraphs
        richTextState.selection = TextRange(2, 8)

        assertTrue(richTextState.isUnorderedList)
    }

    @Test
    fun testIsOrderedListStateWithSingleParagraph() {
        val richTextState = RichTextState()

        assertFalse(richTextState.isOrderedList)

        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "1. ",
                selection = TextRange(3),
            )
        )

        assertTrue(richTextState.isOrderedList)

        richTextState.onTextFieldValueChange(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            )
        )

        assertFalse(richTextState.isOrderedList)
    }

    @Test
    fun testIsOrderedListStateWithMultipleParagraphs() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(1),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(2),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = DefaultParagraph(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "ccc",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Selecting single ordered list paragraph
        richTextState.selection = TextRange(6)

        assertTrue(richTextState.isOrderedList)

        // Selecting single default paragraph
        richTextState.selection = TextRange(14)

        assertFalse(richTextState.isOrderedList)

        // Selecting multiple ordered list paragraphs
        richTextState.selection = TextRange(2, 10)

        assertTrue(richTextState.isOrderedList)
    }

    @Test
    fun testIsListStateWithMultipleParagraphs() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(1),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = DefaultParagraph(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "ccc",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Selecting single ordered list paragraph
        richTextState.selection = TextRange(5)

        assertTrue(richTextState.isList)

        // Selecting single unordered list paragraph
        richTextState.selection = TextRange(10)

        assertTrue(richTextState.isList)

        // Selecting single default paragraph
        richTextState.selection = TextRange(14)

        assertFalse(richTextState.isList)

        // Selecting multiple unordered list paragraphs
        richTextState.selection = TextRange(2, 10)

        assertTrue(richTextState.isList)
    }

    @Test
    fun testKeepLevelOnChangingUnorderedListItemToOrdered() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(6)

        richTextState.toggleOrderedList()

        val firstParagraph = richTextState.richParagraphList[0]
        val secondParagraph = richTextState.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.level)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)
    }

    @Test
    fun testKeepLevelOnChangingOrderedListItemToUnordered() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "aaa",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "bbb",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(9)

        richTextState.toggleUnorderedList()

        val firstParagraph = richTextState.richParagraphList[0]
        val secondParagraph = richTextState.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertIs<UnorderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.level)
        assertEquals(2, secondParagraphType.level)
    }

    @Test
    fun testRemoveSelectionFromEndEdges() {
        // This was causing a crash when trying to remove text from the end edges of the two paragraphs with lists.
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "A",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "B",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "C",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "D",
                            paragraph = it,
                        ),
                    )
                },
            )
        )

        richTextState.selection = TextRange(4, 15)
        richTextState.removeSelectedText()

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals("A", richTextState.richParagraphList[0].children.first().text)
        assertEquals("D", richTextState.richParagraphList[1].children.first().text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertHtml("<b>Inserted</b>", 0)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        val firstSpan = paragraph.children[0]
        assertEquals("Inserted", firstSpan.text)
        assertEquals(FontWeight.Bold, firstSpan.spanStyle.fontWeight)

        val secondSpan = paragraph.children[1]
        assertEquals("Initial content", secondSpan.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before content After</p>")

        richTextState.insertHtml("<i>Inserted</i>", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(3, paragraph.children.size)

        assertEquals("Before ", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontStyle.Italic, insertedSpan.spanStyle.fontStyle)

        assertEquals("content After", paragraph.children[2].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertHtml("<u>Inserted</u>", 15)

        richTextState.printParagraphs()

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(TextDecoration.Underline, insertedSpan.spanStyle.textDecoration)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 6)
        richTextState.printParagraphs()

        assertEquals(3, richTextState.richParagraphList.size)
        assertEquals("First", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New1", richTextState.richParagraphList[1].children[0].text)
        assertEquals("New2Last", richTextState.richParagraphList[2].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>FirstLast</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 5)

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals("FirstNew1", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New2Last", richTextState.richParagraphList[1].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 5)

        assertEquals(3, richTextState.richParagraphList.size)
        assertEquals("FirstNew1", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New2", richTextState.richParagraphList[1].children[0].text)
        assertEquals("Last", richTextState.richParagraphList[2].children[0].text)
    }


    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsWithBr() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<br><p>New1</p><p>New2</p>", 5)

        assertEquals(4, richTextState.richParagraphList.size)
        assertEquals("First", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New1", richTextState.richParagraphList[1].children[0].text)
        assertEquals("New2", richTextState.richParagraphList[2].children[0].text)
        assertEquals("Last", richTextState.richParagraphList[3].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertEmptyHtml() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Content</p>")

        richTextState.insertHtml("", 3)

        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals("Content", richTextState.richParagraphList[0].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("**Inserted**", 0)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        val firstSpan = paragraph.children[0]
        assertEquals("Inserted", firstSpan.text)
        assertEquals(FontWeight.Bold, firstSpan.spanStyle.fontWeight)

        val secondSpan = paragraph.children[1]
        assertEquals("Initial content", secondSpan.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before content After</p>")

        richTextState.insertMarkdown("*Inserted*", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(3, paragraph.children.size)

        assertEquals("Before ", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontStyle.Italic, insertedSpan.spanStyle.fontStyle)

        assertEquals("content After", paragraph.children[2].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("__Inserted__", 15)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontWeight.Bold, insertedSpan.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertEmptyMarkdown() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(1, paragraph.children.size)

        val span = paragraph.children[0]
        assertEquals("Initial content", span.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertComplexMarkdown() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("**Bold** and *italic*\nNew paragraph with __bold__", 15)

        assertEquals(2, richTextState.richParagraphList.size)

        // First paragraph
        val firstParagraph = richTextState.richParagraphList[0]
        assertEquals(4, firstParagraph.children.size)

        assertEquals("Initial content", firstParagraph.children[0].text)

        val boldSpan = firstParagraph.children[1]
        assertEquals("Bold", boldSpan.text)
        assertEquals(FontWeight.Bold, boldSpan.spanStyle.fontWeight)

        assertEquals(" and ", firstParagraph.children[2].text)

        val italicSpan = firstParagraph.children[3]
        assertEquals("italic", italicSpan.text)
        assertEquals(FontStyle.Italic, italicSpan.spanStyle.fontStyle)

        // Second paragraph
        val secondParagraph = richTextState.richParagraphList[1]
        assertEquals(2, secondParagraph.children.size)

        assertEquals("New paragraph with ", secondParagraph.children[0].text)

        val boldSpan2 = secondParagraph.children[1]
        assertEquals("bold", boldSpan2.text)
        assertEquals(FontWeight.Bold, boldSpan2.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertSingleParagraph() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        val newParagraph = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Inserted",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        richTextState.insertParagraphs(listOf(newParagraph), 15)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontWeight.Bold, insertedSpan.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMultipleParagraphs() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before Middle After</p>")

        val paragraph1 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "First",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        val paragraph2 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Second",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                )
            )
        }

        richTextState.insertParagraphs(listOf(paragraph1, paragraph2), 7)

        assertEquals(2, richTextState.richParagraphList.size)

        // First paragraph
        val firstParagraph = richTextState.richParagraphList[0]
        assertEquals(2, firstParagraph.children.size)
        assertEquals("Before ", firstParagraph.children[0].text)

        val firstInserted = firstParagraph.children[1]
        assertEquals("First", firstInserted.text)
        assertEquals(FontWeight.Bold, firstInserted.spanStyle.fontWeight)

        // Second paragraph
        val secondParagraph = richTextState.richParagraphList[1]
        assertEquals(2, secondParagraph.children.size)

        val secondInserted = secondParagraph.children[0]
        assertEquals("Second", secondInserted.text)
        assertEquals(FontStyle.Italic, secondInserted.spanStyle.fontStyle)

        assertEquals("Middle After", secondParagraph.children[1].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertParagraphsEdgeCases() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Original</p>")

        // Create test paragraphs
        val paragraph1 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Start",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        val paragraph2 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "End",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                )
            )
        }

        // Test inserting at position 0
        richTextState.insertParagraphs(listOf(paragraph1), 0)
        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals(2, richTextState.richParagraphList[0].children.size)
        assertEquals("Start", richTextState.richParagraphList[0].children[0].text)
        assertEquals(FontWeight.Bold, richTextState.richParagraphList[0].children[0].spanStyle.fontWeight)
        assertEquals("Original", richTextState.richParagraphList[0].children[1].text)

        // Test inserting at the end
        richTextState.insertParagraphs(listOf(paragraph2), richTextState.annotatedString.text.length)
        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals(3, richTextState.richParagraphList[0].children.size)
        assertEquals("End", richTextState.richParagraphList[0].children[2].text)
        assertEquals(FontStyle.Italic, richTextState.richParagraphList[0].children[2].spanStyle.fontStyle)

        // Test inserting empty paragraph list
        val textBefore = richTextState.annotatedString.text
        richTextState.insertParagraphs(emptyList(), 5)
        assertEquals(textBefore, richTextState.annotatedString.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertParagraphsStylePreservation() {
        val richTextState = RichTextState()

        // Setup initial content with styled paragraph and spans
        val initialParagraph = RichParagraph(
            key = 1,
            paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        ).also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Styled ",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
            paragraph.children.add(
                RichSpan(
                    text = "content",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                )
            )
        }
        richTextState.insertParagraphs(listOf(initialParagraph), 0)

        // Create new paragraph with its own styles
        val newParagraph = RichParagraph(
            key = 2,
            paragraphStyle = ParagraphStyle(textAlign = TextAlign.End)
        ).also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "New",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                )
            )
        }

        // Insert in the middle of styled content
        richTextState.insertParagraphs(listOf(newParagraph), 7)

        // Verify results
        assertEquals(1, richTextState.richParagraphList.size)
        val resultParagraph = richTextState.richParagraphList[0]

        richTextState.printParagraphs()
        // Check paragraph style preservation
        assertEquals(TextAlign.Center, resultParagraph.paragraphStyle.textAlign)

        // Check spans and their styles
        assertEquals(3, resultParagraph.children.size)

        val firstSpan = resultParagraph.children[0]
        assertEquals("Styled ", firstSpan.text)
        assertEquals(FontWeight.Bold, firstSpan.spanStyle.fontWeight)

        val insertedSpan = resultParagraph.children[1]
        assertEquals("New", insertedSpan.text)
        assertEquals(TextDecoration.Underline, insertedSpan.spanStyle.textDecoration)

        val lastSpan = resultParagraph.children[2]
        assertEquals("content", lastSpan.text)
        assertEquals(FontStyle.Italic, lastSpan.spanStyle.fontStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testLooseLinksAfterChangingConfig() {
        val html = """
            <a href="https://www.google.com"><b>Google</b></a>
        """.trimIndent()

        val richTextState = RichTextState()
        richTextState.setHtml(html)
        richTextState.config.linkTextDecoration = TextDecoration.None

        val link = richTextState.richParagraphList[0].children.first()

        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals(0, link.children.size)
        assertIs<RichSpanStyle.Link>(link.richSpanStyle)
        assertEquals("Google", link.text)
        assertEquals(FontWeight.Bold, link.spanStyle.fontWeight)
    }
}