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

}