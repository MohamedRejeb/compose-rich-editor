package com.mohamedrejeb.richeditor.parser.html

import kotlin.test.Test
import kotlin.test.assertEquals

internal class RichTextHtmlParserTest {
    @Test
    fun testRemoveHtmlTextExtraSpaces() {
        val html = """
            Hello       World!      Welcome to 
             
             Compose Rich Text Editor!
        """.trimIndent()

        assertEquals(
            "Hello World! Welcome to Compose Rich Text Editor!",
            RichTextHtmlParser.removeHtmlTextExtraSpaces(html)
        )
    }
}