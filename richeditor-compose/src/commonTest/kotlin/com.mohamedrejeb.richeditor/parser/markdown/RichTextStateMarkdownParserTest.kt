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
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextStateMarkdownParserTest {

    /**
     * Encode tests
     */

    @Test
    fun testBold() {
        val markdown = "**Hello World!**"
        val expectedText = "Hello World!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = SpanStyle(fontWeight = FontWeight.Bold),
            actual = state.richParagraphList.first().children.first().spanStyle
        )
    }

    @Test
    fun testBoldWithNestedItalic() {
        val markdown = "**Hello *World!***"
        val expectedText = "Hello World!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        val firstChild = state.richParagraphList.first().children.first()
        val secondChild = firstChild.children.first()

        assertEquals(
            expected = SpanStyle(fontWeight = FontWeight.Bold),
            actual = firstChild.spanStyle
        )

        assertEquals(
            expected = SpanStyle(fontStyle = FontStyle.Italic),
            actual = secondChild.spanStyle
        )
    }

    @Test
    fun testUnderline() {
        val markdown = "<u>Hello World!</u>"
        val expectedText = "Hello World!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = SpanStyle(textDecoration = TextDecoration.Underline),
            actual = state.richParagraphList.first().children.first().spanStyle
        )
    }

    @Test
    fun testLineThrough() {
        val markdown = "~~Hello World!~~"
        val expectedText = "Hello World!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertEquals(
            expected = SpanStyle(textDecoration = TextDecoration.LineThrough),
            actual = state.richParagraphList.first().children.first().spanStyle
        )
    }

    @Test
    fun testEncodeLineBreak() {
        val markdown = """
            Hello
            <br>
            
            
            
            World!
        """.trimIndent()

        val state = RichTextStateMarkdownParser.encode(markdown)

        assertEquals(
            expected = 4,
            actual = state.richParagraphList.size,
        )

        state.setMarkdown(
            """
            Hello
            
            
            
            World!
        """.trimIndent()
        )

        assertEquals(
            expected = 3,
            actual = state.richParagraphList.size,
        )

        state.setMarkdown(
            """
            Hello
            
            <br>
            <br>
            
            World!
        """.trimIndent()
        )

        assertEquals(
            expected = 5,
            actual = state.richParagraphList.size,
        )
    }

    @Test
    fun testEncodeMarkdownWithSingleDollar() {
        val markdown = "Hello World $100!"
        val expectedText = "Hello World $100!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )
    }

    @Test
    fun testEncodeMarkdownWithDoubleDollar() {
        val markdown = "Hello World $$100!"
        val expectedText = "Hello World $$100!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )
    }

    @Test
    fun testEncodeMarkdownWithInlineMath() {
        val markdown = "Hello World \$100$!"
        val expectedText = "Hello World 100!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )
    }

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

}