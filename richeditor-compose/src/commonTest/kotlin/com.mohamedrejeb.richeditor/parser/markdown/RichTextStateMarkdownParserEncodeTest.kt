package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.parser.utils.H1SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H2SpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RichTextStateMarkdownParserEncodeTest {

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
    fun testBoldWithItalic() {
        val markdown = "***Hello World!***"
        val expectedText = "Hello World!"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        val richSpan = state.richParagraphList.first().children[0]

        assertEquals(
            expected = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
            actual = richSpan.spanStyle
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
    fun testEncodeWithOneHtmlLineBreakInTheMiddle() {
        val markdownList = listOf(
            """
                Hello
                <br>
                World!
            """.trimIndent(),

            """
                Hello
                <br>
                
                World!
            """.trimIndent(),


            """
                Hello
                
                <br>
                
                World!
            """.trimIndent(),
        )

        markdownList.forEach { markdown ->
            val state = RichTextStateMarkdownParser.encode(markdown)

            assertEquals(
                expected = 4,
                actual = state.richParagraphList.size,
            )
        }

    }

    @Test
    fun testEncodeWithEnterLineBreakInTheMiddle() {
        val markdownList = listOf(
            """
                Hello
                
                World!
            """.trimIndent(),

            """
                Hello
                
    
                
                World!
            """.trimIndent(),
        )

        markdownList.forEach { markdown ->
            val state = RichTextStateMarkdownParser.encode(markdown)

            assertEquals(
                expected = 3,
                actual = state.richParagraphList.size,
            )
        }
    }

    @Test
    fun testEncodeWithTwoHtmlLineBreaks() {
        val markdown = """
            Hello
            
            <br>
            <br>
            
            World!
        """.trimIndent()

        val state = RichTextStateMarkdownParser.encode(markdown)

        assertEquals(
            expected = 5,
            actual = state.richParagraphList.size,
        )
    }

    @Test
    fun testEncodeWithTwoHtmlLineBreaksAndTextInBetween() {
        val markdown1 = """
            Hello
            
            <br>
            q
            <br>
            
            World!
        """.trimIndent()

        assertEquals(
            expected = 5,
            actual = RichTextStateMarkdownParser.encode(markdown1).richParagraphList.size,
        )

        val markdown2 = """
            Hello
            
            <br>
            q
            
            <br>
            
            World!
        """.trimIndent()

        assertEquals(
            expected = 7,
            actual = RichTextStateMarkdownParser.encode(markdown2).richParagraphList.size,
        )

        val markdown3 = """
            Hello
            
            <br>
            
            q
            <br>
            
            World!
        """.trimIndent()

        assertEquals(
            expected = 7,
            actual = RichTextStateMarkdownParser.encode(markdown3).richParagraphList.size,
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

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testEncodeMarkdownWithLink() {
        val markdown = "Before [Google](https://www.google.com) after"
        val expectedText = "Before Google after"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        assertIs<RichSpanStyle.Link>(
            state.richParagraphList.first().children[1].richSpanStyle
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testEncodeMarkdownWithTextUrl() {
        val markdown = "Before https://www.google.com after"
        val expectedText = "Before https://www.google.com after"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        val linkRichSpan = state.richParagraphList.first().children[1]
        val linkRichSpanStyle = linkRichSpan.richSpanStyle

        assertIs<RichSpanStyle.Link>(linkRichSpanStyle)
        assertEquals(linkRichSpan.text, "https://www.google.com")
        assertEquals(linkRichSpanStyle.url, "https://www.google.com")
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testEncodeMarkdownWithImage() {
        val imageUrl = "https://www.imageurl.com"
        val imageAlt = "image-alt"

        val markdown = "Image: ![$imageAlt]($imageUrl)"
        val expectedText = "Image: "
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText.dropLast(1),
        )

        val imageRichSpan = state.richParagraphList.first().children[1]
        val imageRichSpanStyle = imageRichSpan.richSpanStyle

        assertIs<RichSpanStyle.Image>(imageRichSpanStyle)
        assertEquals(imageUrl, imageRichSpanStyle.model)
        assertEquals(imageAlt, imageRichSpanStyle.contentDescription)
    }


    @Test
    fun testEncodeMarkdownWithSpacesInsideStyling() {
        val markdown = "**Bold **Normal"
        val expectedText = "Bold Normal"
        val state = RichTextStateMarkdownParser.encode(markdown)
        val actualText = state.annotatedString.text

        assertEquals(
            expected = expectedText,
            actual = actualText,
        )

        val paragraph = state.richParagraphList.first()
        val boldRichSpan = paragraph.children[0]
        val normalRichSpan = paragraph.children[1]

        assertEquals(
            expected = SpanStyle(fontWeight = FontWeight.Bold),
            actual = boldRichSpan.spanStyle,
        )

        assertEquals(
            expected = SpanStyle(),
            actual = normalRichSpan.spanStyle,
        )
    }

    // https://github.com/MohamedRejeb/compose-rich-editor/issues/385
    @Test
    fun testInitMarkdownWithEmptyLinesAndTypeText() {
        val state = RichTextState()

        state.setMarkdown("<br>")

        assertEquals(state.richParagraphList.size, 2)

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "  d",
                selection = TextRange(3)
            )
        )

        assertEquals(state.richParagraphList.size, 2)
    }

    @Test
    fun testEncodeTitles() {
        val markdown = """
            # Prompt
            ## Emphasis
        """.trimIndent()

        val state = RichTextState()

        state.setMarkdown(markdown)

        assertEquals(2, state.richParagraphList.size)

        val firstParagraph = state.richParagraphList[0]

        assertEquals(H1SpanStyle, firstParagraph.getFirstNonEmptyChild()!!.spanStyle)

        val secondParagraph = state.richParagraphList[1]
        assertEquals(H2SpanStyle, secondParagraph.getFirstNonEmptyChild()!!.spanStyle)

        assertEquals("Prompt\nEmphasis", state.toText())
    }

}