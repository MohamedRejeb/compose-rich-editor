package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H1SpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class RichTextStateHtmlParserTest {
    @Test
    fun testRemoveHtmlTextExtraSpaces() {
        val html = """
            Hello       World!      Welcome to 
             
             Compose Rich Text Editor!
        """.trimIndent()

        assertEquals(
            "Hello World! Welcome to Compose Rich Text Editor!",
            removeHtmlTextExtraSpaces(html)
        )
    }

    @Test
    fun testParsingSimpleHtmlWithBrBackAndForth() {
        val html = "<br><p>Hello World&excl;</p>"

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(2, richTextState.richParagraphList.size)

        val parsedHtml = RichTextStateHtmlParser.decode(richTextState)

        assertEquals(html, parsedHtml)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testHtmlWithImage() {
        val html = """
            <!DOCTYPE html>
            <html>
            <body>

            <h1>The img element</h1>

            <img src="https://picsum.photos/200/300" alt="Girl in a jacket" width="500" height="600">

            </body>
            </html>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        val h1 = richTextState.richParagraphList[0].children.first()
        val image = richTextState.richParagraphList[1].children.first()

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals(1, richTextState.richParagraphList[0].children.size)
        // It's only 1, but we have the added rich span for each paragraph with index > 0
        assertEquals(2, richTextState.richParagraphList[1].children.size)
        assertEquals("The img element", h1.text)
        assertEquals(H1SpanStyle, h1.spanStyle)
        assertIs<RichSpanStyle.Image>(image.richSpanStyle)
    }

    @Test
    fun testBrInMiddleOrParagraph() {
        val html = """
            <h1>Hello<br>World!</h1>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals(1, richTextState.richParagraphList[0].children.size)
        assertEquals(1, richTextState.richParagraphList[1].children.size)

        val firstPart = richTextState.richParagraphList[0].children.first()
        val secondPart = richTextState.richParagraphList[1].children.first()

        assertEquals("Hello", firstPart.text)
        assertEquals("World!", secondPart.text)

        assertEquals(H1SpanStyle, firstPart.spanStyle)
        assertEquals(H1SpanStyle, secondPart.spanStyle)
    }
}