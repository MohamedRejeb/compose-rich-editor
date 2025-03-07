package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalRichTextApi
class RichTextStateMarkdownParserDecodeTest {

    /**
     * Decode tests
     */

    @Test
    fun testDecodeBold() {
        val expectedText = "Hello World!"
        val state = RichTextState()

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.onTextFieldValueChange(
            TextFieldValue(
                text = expectedText,
                selection = TextRange(expectedText.length)
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = "**$expectedText**",
            actual = markdown
        )
    }

    @Test
    fun testDecodeItalic() {
        val expectedText = "Hello World!"
        val state = RichTextState()

        state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        state.onTextFieldValueChange(
            TextFieldValue(
                text = expectedText,
                selection = TextRange(expectedText.length)
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = "*$expectedText*",
            actual = markdown
        )
    }

    @Test
    fun testDecodeLineThrough() {
        val expectedText = "Hello World!"
        val state = RichTextState()

        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        state.onTextFieldValueChange(
            TextFieldValue(
                text = expectedText,
                selection = TextRange(expectedText.length)
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = "~~$expectedText~~",
            actual = markdown
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testDecodeUnderline() {
        val expectedText = "Hello World!"
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph().also {
                    it.children.add(
                        RichSpan(
                            text = expectedText,
                            paragraph = it,
                            spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                        )
                    )
                }
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = "<u>$expectedText</u>",
            actual = markdown
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testDecodeLineBreak() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph().also {
                    it.children.add(
                        RichSpan(
                            text = "Hello",
                            paragraph = it
                        )
                    )
                },
                RichParagraph(),
                RichParagraph(),
                RichParagraph(),
                RichParagraph().also {
                    it.children.add(
                        RichSpan(
                            text = "World!",
                            paragraph = it
                        )
                    )
                }
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected =
                """
                    Hello

                    <br>
                    <br>
                    World!
                """.trimIndent(),
            actual = markdown,
        )
    }

    @Test
    fun testDecodeOneEmptyLine() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(),
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected = "",
            actual = markdown,
        )
    }

    @Test
    fun testDecodeTwoEmptyLines() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(),
                RichParagraph(),
            )
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected = """

                <br>
            """.trimIndent(),
            actual = markdown,
        )
    }

    @Test
    fun testDecodeWithEnterLineBreakInTheMiddle() {
        val state = RichTextState()
        state.setMarkdown(
            """
                Hello

                World!
            """.trimIndent(),
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected = """
                Hello

                World!
            """.trimIndent(),
            actual = markdown,
        )
    }

    @Test
    fun testDecodeWithTwoHtmlLineBreaks() {
        val state = RichTextState()
        state.setMarkdown(
            """
                Hello

                <br>

                <br>

                World!
            """.trimIndent(),
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected = """
                Hello

                <br>
                <br>
                World!
            """.trimIndent(),
            actual = markdown,
        )
    }

    @Test
    fun testDecodeWithTwoHtmlLineBreaksAndTextInBetween() {
        val state = RichTextState()
        state.setMarkdown(
            """
                Hello

                <br>
                q

                <br>

                World!
            """.trimIndent(),
        )

        val markdown = RichTextStateMarkdownParser.decode(state)

        assertEquals(
            expected = """
                Hello

                <br>
                q

                <br>
                World!
            """.trimIndent(),
            actual = markdown,
        )
    }

    @Test
    fun testDecodeStyledTextWithSpacesInStyleEdges1() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph().also {
                    it.children.add(
                        RichSpan(
                            text = "  Hello  ",
                            paragraph = it,
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                        ),
                    )

                    it.children.add(
                        RichSpan(
                            text = "World!",
                            paragraph = it,
                        ),
                    )
                },
            )
        )

        assertEquals(
            expected = "  **Hello**  World!",
            actual = state.toMarkdown()
        )
    }

    @Test
    fun testDecodeStyledTextWithSpacesInStyleEdges2() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph().also {
                    it.children.add(
                        RichSpan(
                            text = "  Hello  ",
                            paragraph = it,
                            spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                        ).also {
                            it.children.add(
                                RichSpan(
                                    text = "  World!  ",
                                    paragraph = it.paragraph,
                                    parent = it,
                                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                                ),
                            )
                        },
                    )
                },
            )
        )

        assertEquals(
            expected = "  **Hello    *World!***  ",
            actual = state.toMarkdown()
        )
    }

    @Test
    fun testDecodeTitles() {
        val markdown = """
            # Prompt
            ## Emphasis
        """.trimIndent()

        val state = RichTextState()

        state.setMarkdown(markdown)

        assertEquals(
            """
                # Prompt
                ## Emphasis
            """.trimIndent(),
            state.toMarkdown()
        )
    }

    @Test
    fun testDecodeTextRange() {
        val state = RichTextState()
        state.setMarkdown("""
            Hello **bold** text
            with *italic* style
            and ~~strikethrough~~ formatting
        """.trimIndent())

        // Test middle range
        assertEquals(
            "**bold** text\nwith",
            state.toMarkdown(TextRange(6, 20))
        )

        // Test start range
        assertEquals(
            "Hello **bold**",
            state.toMarkdown(TextRange(0, 10))
        )

        // Test end range
        assertEquals(
            "~~strikethrough~~ formatting",
            state.toMarkdown(TextRange(38, 62))
        )

        // Test empty range
        assertEquals(
            "",
            state.toMarkdown(TextRange(5, 5))
        )

        // Test invalid range
        assertEquals(
            "",
            state.toMarkdown(TextRange(100, 200))
        )
    }

    @Test
    fun testDecodeTextRangeWithPartialFormatting() {
        val state = RichTextState()
        state.setMarkdown("""
            This is **bold and *italic* text** here
            with ~~strike through~~ formatting
        """.trimIndent())

        // Test range that starts in middle of bold and ends after italic
        assertEquals(
            "**bold and *italic* text**",
            state.toMarkdown(TextRange(8, 28))
        )

        // Test range that starts before bold and ends in middle of italic
        assertEquals(
            "This is **bold and *ita***",
            state.toMarkdown(TextRange(0, 20))
        )

        // Test range that starts in middle of one format and ends in middle of another
        assertEquals(
            "**and *italic* text** here\nwith ~~strike~~",
            state.toMarkdown(TextRange(13, 45))
        )
    }

    @Test
    fun testDecodeTextRangeWithLists() {
        val state = RichTextState()
        state.setMarkdown("""
            1. First item
            2. Second **bold** item
               - Subitem one
               - Subitem two
            3. Third item
        """.trimIndent())

        // Test range within a list
        assertEquals(
            "Second **bold** item\n   - Subitem one",
            state.toMarkdown(TextRange(14, 47))
        )

        // Test range across multiple list items
        assertEquals(
            "2. Second **bold** item\n   - Subitem one\n   - Subitem two\n3. Third",
            state.toMarkdown(TextRange(13, 82))
        )
    }

    @Test
    fun testDecodeListsWithDifferentLevels() {
        val expectedMarkdown = """
            1. F
                1. FFO
                2. FSO
                - FFU
                - FSU
                    - FSU3
            - FFU
                1. FFO
            Last
        """.trimIndent()

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "F",
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
                            text = "FFO",
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
                            text = "FSO",
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
                            text = "FFU",
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
                            text = "FSU",
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
                            text = "FSU3",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FFU",
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
                            text = "FFO",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Last",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedMarkdown, richTextState.toMarkdown())
    }

}
