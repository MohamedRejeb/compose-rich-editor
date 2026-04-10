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
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
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

    @Test
    fun testEncodeWithLeadingSpaces() {
        val markdown = """First line
            
    indented line"""

        val state = RichTextStateMarkdownParser.encode(markdown)

        val parsedString = state.toText()

        assertEquals(
            expected = """
                First line
                
                indented line
            """.trimIndent(),
            actual = parsedString
        )
    }

    @Test
    fun testEncodeInlineCodeInDifferentLines() {
        val markdown =
            """
                Hello `World!
                Kotlin` MP
            """.trimIndent()

        val state = RichTextStateMarkdownParser.encode(markdown)

        val parsedString = state.toText()

        assertEquals(
            expected =
                """
                    Hello World! Kotlin MP
                """.trimIndent(),
            actual = parsedString,
        )

        assertEquals(
            expected = 1,
            actual = state.richParagraphList.size,
        )
    }

    @Test
    fun testEncodeOrderedListWithNestedListWithTwoSpacesIndent() {
        val markdown = """
            1. Item1
            2. Item2
              1. Item2.1
              2. Item2.2
            3. Item3
            Item4
        """.trimIndent()

        testEncodeOrderedListWithNestedListHelper(markdown)
    }

    @Test
    fun testEncodeOrderedListWithNestedListWithFourSpacesIndent() {
        val markdown = """
            1. Item1
            2. Item2
                1. Item2.1
                2. Item2.2
            3. Item3
            Item4
        """.trimIndent()

        testEncodeOrderedListWithNestedListHelper(markdown)
    }

    fun testEncodeOrderedListWithNestedListHelper(markdown: String) {
        val richTextState = RichTextStateMarkdownParser.encode(markdown)

        assertEquals(6, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]
        val fourthItem = richTextState.richParagraphList[3].children[0]
        val fifthItem = richTextState.richParagraphList[4].children[0]
        val sixthItem = richTextState.richParagraphList[5].children[0]

        richTextState.richParagraphList.forEachIndexed { i, p ->
            val type = p.type

            if (i == 5) {
                assertIs<DefaultParagraph>(type)
                return@forEachIndexed
            }

            assertIs<OrderedList>(type)

            if (
                i == 0 ||
                i == 1 ||
                i == 4
            )
                assertEquals(1, type.level)
            else
                assertEquals(2, type.level)
        }

        assertEquals("Item1", firstItem.text)
        assertEquals("Item2", secondItem.text)
        assertEquals("Item2.1", thirdItem.text)
        assertEquals("Item2.2", fourthItem.text)
        assertEquals("Item3", fifthItem .text)
        assertEquals("Item4", sixthItem .text)
    }

    @Test
    fun testEncodeOrderedListWithNestedUList() {
        val markdown = """
            1. Item1
            2. Item2
              - Item2.1
              - Item2.2
            3. Item3
            Item4
        """.trimIndent()

        val richTextState = RichTextStateMarkdownParser.encode(markdown)

        richTextState.printParagraphs()

        assertEquals(6, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]
        val fourthItem = richTextState.richParagraphList[3].children[0]
        val fifthItem = richTextState.richParagraphList[4].children[0]
        val sixthItem = richTextState.richParagraphList[5].children[0]

        richTextState.richParagraphList.forEachIndexed { i, p ->
            val type = p.type

            if (i == 5) {
                assertIs<DefaultParagraph>(type)
                return@forEachIndexed
            }

            if (i == 2 || i == 3)
                assertIs<UnorderedList>(type)
            else
                assertIs<OrderedList>(type)

            if (
                i == 0 ||
                i == 1 ||
                i == 4
            )
                assertEquals(1, type.level)
            else
                assertEquals(2, type.level)
        }

        assertEquals("Item1", firstItem.text)
        assertEquals("Item2", secondItem.text)
        assertEquals("Item2.1", thirdItem.text)
        assertEquals("Item2.2", fourthItem.text)
        assertEquals("Item3", fifthItem .text)
        assertEquals("Item4", sixthItem .text)
    }

    @Test
    fun testHtmlEncodeSetRichSpanParentCorrectly() {
        val html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"background-color:#1e1f22;color:#bcbec4\"><pre style=\"font-family:'JetBrains Mono',monospace;font-size:9.8pt;\"><span style=\"font-style:italic;\">println</span>(<span style=\"color:#6aab73;\">\"insertHtml:&#32;</span><span style=\"color:#cf8e6d;\">\$</span>position<span style=\"color:#6aab73;\">\"</span>)</pre></div></body></html>"
        val richTextState = RichTextStateHtmlParser.encode(html)

        richTextState.selection = TextRange(0)

        assertEquals(1, richTextState.richParagraphList[0].children.size)
        assertEquals(13, richTextState.richParagraphList[0].children[0].spanStyle.fontSize.value.toInt())
        assertEquals(7, richTextState.richParagraphList[0].children[0].children.size)
        richTextState.richParagraphList[0].children[0].children.forEach {
            assertEquals(richTextState.richParagraphList[0].children[0], it.parent)
        }

        richTextState.onTextFieldValueChange(
            newTextFieldValue = TextFieldValue(
                text = "\n${richTextState.textFieldValue.text}",
                selection = TextRange(1)
            )
        )
    }

    @Test
    fun testHtmlEncodeInheritSpanStyleCorrectly() {
        val html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"background-color:#1e1f22;color:#bcbec4\"><pre style=\"font-family:'JetBrains Mono',monospace;font-size:9.8pt;\"><span style=\"font-style:italic;\">println</span>(<span style=\"color:#6aab73;\">\"selection&#32;html:&#32;</span><span style=\"color:#cf8e6d;\">\$</span>html<span style=\"color:#6aab73;\">\"</span>)<br><span style=\"font-style:italic;\">println</span>(<span style=\"color:#6aab73;\">\"selection&#32;text:&#32;</span><span style=\"color:#cf8e6d;\">\$</span>text<span style=\"color:#6aab73;\">\"</span>)<br><br><span style=\"color:#cf8e6d;\">val&#32;</span>htmlSelection&#32;=&#32;<span style=\"color:#cf8e6d;\">object&#32;</span>:&#32;StringSelection(html),&#32;Transferable&#32;{</pre></div></body></html>"
        val richTextState = RichTextStateHtmlParser.encode(html)

        val fontSize = richTextState.richParagraphList[0].getFirstNonEmptyChild()!!.fullSpanStyle.fontSize

        richTextState.richParagraphList.forEachIndexed { index, paragraph ->
            val paragraphFontSize = paragraph.getFirstNonEmptyChild()!!.fullSpanStyle.fontSize
            assertEquals(fontSize, paragraphFontSize)
        }
    }

    @Test
    fun testHtmlEncodeInheritSpanStyleCorrectly2() {
        val html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body><div style=\"background-color:#1e1f22;color:#bcbec4\"><pre style=\"font-family:'JetBrains Mono',monospace;font-size:9.8pt;\"><span style=\"color:#cf8e6d;\">val&#32;</span>html&#32;=&#32;<span style=\"color:#6aab73;\">\"hello\"<br></span><span style=\"color:#cf8e6d;\">val&#32;</span>richTextState&#32;=&#32;RichTextStateHtmlParser.encode(html)</pre></div></body></html>"
        val richTextState = RichTextStateHtmlParser.encode(html)

        val fontSize = richTextState.richParagraphList[0].getFirstNonEmptyChild()!!.fullSpanStyle.fontSize

        richTextState.richParagraphList.forEachIndexed { index, paragraph ->
            val paragraphFontSize = paragraph.getFirstNonEmptyChild()!!.fullSpanStyle.fontSize
            assertEquals(fontSize, paragraphFontSize)
        }
    }

}