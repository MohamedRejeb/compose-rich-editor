package com.mohamedrejeb.richeditor.parser.html

import kotlin.test.Test
import kotlin.test.assertEquals

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
}