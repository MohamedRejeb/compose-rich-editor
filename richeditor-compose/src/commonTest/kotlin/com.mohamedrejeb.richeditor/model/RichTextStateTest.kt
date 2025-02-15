package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
    fun testRemoveCharactersWithNestedLevel() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 1,
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
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.nestedLevel)
        assertEquals(2, thirdParagraphType.nestedLevel)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(2, fourthParagraphType.number)
        assertEquals(1, fourthParagraphType.nestedLevel)
    }

    @Test
    fun testAddOrderedListWithNestedLevel1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 1,
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
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.nestedLevel)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(1, fourthParagraphType.number)
        assertEquals(1, fourthParagraphType.nestedLevel)
    }

    @Test
    fun testAddOrderedListWithNestedLevel2() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.nestedLevel)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(3, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.nestedLevel)
    }

    @Test
    fun testAddUnorderedListWithNestedLevel1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
        state.toggleUnorderedList()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)

        assertIs<UnorderedList>(thirdParagraphType)
        assertEquals(2, thirdParagraphType.nestedLevel)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(1, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.nestedLevel)
    }

    @Test
    fun testIncreaseListNestedLevelSimple1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 3,
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
                        initialNestedLevel = 1,
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

        state.increaseListNestedLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]
        val fourthParagraph = state.richParagraphList[3]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type
        val fourthParagraphType = fourthParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)

        assertIs<UnorderedList>(thirdParagraphType)
        assertEquals(3, thirdParagraphType.nestedLevel)

        assertIs<OrderedList>(fourthParagraphType)
        assertEquals(2, fourthParagraphType.number)
        assertEquals(2, fourthParagraphType.nestedLevel)
    }

    @Test
    fun testIncreaseListNestedLevelSimple2() {
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

        state.increaseListNestedLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.nestedLevel)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)
    }

    @Test
    fun testIncreaseListNestedLevelComplex() {
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
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 3,
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
                        initialNestedLevel = 1,
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
        state.increaseListNestedLevel()

        val pOne = state.richParagraphList[0].type
        val pTwo = state.richParagraphList[1].type
        val pThree = state.richParagraphList[2].type
        val pFour = state.richParagraphList[3].type
        val pFive = state.richParagraphList[4].type

        assertIs<OrderedList>(pOne)
        assertEquals(1, pOne.number)
        assertEquals(1, pOne.nestedLevel)

        assertIs<OrderedList>(pTwo)
        assertEquals(1, pTwo.number)
        assertEquals(2, pTwo.nestedLevel)

        assertIs<OrderedList>(pThree)
        assertEquals(1, pThree.number)
        assertEquals(3, pThree.nestedLevel)

        assertIs<OrderedList>(pFour)
        assertEquals(1, pFour.number)
        assertEquals(4, pFour.nestedLevel)

        assertIs<OrderedList>(pFive)
        assertEquals(2, pFive.number)
        assertEquals(1, pFive.nestedLevel)
    }

    @Test
    fun testCanIncreaseListNestedLevelCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
        assertFalse(state.canIncreaseListNestedLevel(selectedParagraphs1))

        state.selection = TextRange(9)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertFalse(state.canIncreaseListNestedLevel(selectedParagraphs2))
        assertFalse(state.canIncreaseListNestedLevel)

        state.selection = TextRange(20)
        val selectedParagraphs3 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canIncreaseListNestedLevel(selectedParagraphs3))
        assertTrue(state.canIncreaseListNestedLevel)
    }

    @Test
    fun testCanIncreaseListNestedLevelNonCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 3,
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
        assertFalse(state.canIncreaseListNestedLevel(selectedParagraphs1))
        assertFalse(state.canIncreaseListNestedLevel)

        state.selection = TextRange(18, 23)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canIncreaseListNestedLevel(selectedParagraphs2))
        assertTrue(state.canIncreaseListNestedLevel)
    }

    @Test
    fun testDecreaseListNestedLevelSimple1() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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

        state.decreaseListNestedLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type

        assertIs<OrderedList>(firstParagraphType)
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.nestedLevel)
        assertEquals(2, secondParagraphType.number)
        assertEquals(1, secondParagraphType.nestedLevel)
    }

    @Test
    fun testDecreaseListNestedLevelSimple2() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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

        state.selection = TextRange(state.textFieldValue.text.length - 5)

        state.decreaseListNestedLevel()

        val firstParagraph = state.richParagraphList[0]
        val secondParagraph = state.richParagraphList[1]
        val thirdParagraph = state.richParagraphList[2]

        val firstParagraphType = firstParagraph.type
        val secondParagraphType = secondParagraph.type
        val thirdParagraphType = thirdParagraph.type

        assertIs<UnorderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.nestedLevel)

        assertIs<OrderedList>(secondParagraphType)
        assertEquals(1, secondParagraphType.number)
        assertEquals(1, secondParagraphType.nestedLevel)

        assertIs<OrderedList>(thirdParagraphType)
        assertEquals(1, thirdParagraphType.number)
        assertEquals(2, thirdParagraphType.nestedLevel)
    }

    @Test
    fun testDecreaseListNestedLevelComplex() {
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
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 3,
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
                        initialNestedLevel = 4,
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
                        initialNestedLevel = 1,
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
        state.decreaseListNestedLevel()

        val pOne = state.richParagraphList[0].type
        val pTwo = state.richParagraphList[1].type
        val pThree = state.richParagraphList[2].type
        val pFour = state.richParagraphList[3].type
        val pFive = state.richParagraphList[4].type

        assertIs<OrderedList>(pOne)
        assertEquals(1, pOne.number)
        assertEquals(1, pOne.nestedLevel)

        assertIs<OrderedList>(pTwo)
        assertEquals(2, pTwo.number)
        assertEquals(1, pTwo.nestedLevel)

        assertIs<OrderedList>(pThree)
        assertEquals(1, pThree.number)
        assertEquals(2, pThree.nestedLevel)

        assertIs<OrderedList>(pFour)
        assertEquals(1, pFour.number)
        assertEquals(3, pFour.nestedLevel)

        assertIs<OrderedList>(pFive)
        assertEquals(3, pFive.number)
        assertEquals(1, pFive.nestedLevel)
    }

    @Test
    fun testCanDecreaseListNestedLevelCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
        assertFalse(state.canDecreaseListNestedLevel(selectedParagraphs1))
        assertFalse(state.canDecreaseListNestedLevel)

        state.selection = TextRange(9)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canDecreaseListNestedLevel(selectedParagraphs2))
        assertTrue(state.canDecreaseListNestedLevel)
    }

    @Test
    fun testCanDecreaseListNestedLevelNonCollapsed() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 2,
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
                        initialNestedLevel = 3,
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
        assertFalse(state.canDecreaseListNestedLevel(selectedParagraphs1))
        assertFalse(state.canDecreaseListNestedLevel)

        state.selection = TextRange(9, 16)
        val selectedParagraphs2 = state.getRichParagraphListByTextRange(state.selection)
        assertTrue(state.canDecreaseListNestedLevel(selectedParagraphs2))
        assertTrue(state.canDecreaseListNestedLevel)
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
    fun testKeepNestedLevelOnChangingUnorderedListItemToOrdered() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1
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
                        initialNestedLevel = 2
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
        assertEquals(1, firstParagraphType.nestedLevel)
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.nestedLevel)
    }

    @Test
    fun testKeepNestedLevelOnChangingOrderedListItemToUnordered() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
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
                        initialNestedLevel = 2
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
        assertEquals(1, firstParagraphType.nestedLevel)
        assertEquals(2, secondParagraphType.nestedLevel)
    }

    @Test
    fun testRemoveSelectionFromEndEdges() {
        // This was causing a crash when trying to remove text from the end edges of the two paragraphs with lists.
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
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
                        initialNestedLevel = 2
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
                        initialNestedLevel = 2
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
                        initialNestedLevel = 1
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

        richTextState.selection = TextRange(4, 14)
        richTextState.removeSelectedText()

        assertEquals(2, richTextState.richParagraphList.size)
    }

}