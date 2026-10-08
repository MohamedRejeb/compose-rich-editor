package com.mohamedrejeb.richeditor.parser.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownFencesTest {

    @Test
    fun `text inside a fence is left exactly as written`() {
        val markdown = "a\n```kotlin\n  val a = b * c\n      d ~ e\n```\nz"
        val corrected = correctMarkdownOutsideFences(markdown)
        assertEquals("  val a = b * c\n      d ~ e", corrected.substringAfter("```kotlin\n").substringBefore("\n```"))
    }

    @Test
    fun `text without a fence is corrected as before`() {
        val markdown = "**bold **text\n- item"
        assertEquals(correctMarkdownText(markdown), correctMarkdownOutsideFences(markdown))
    }

    @Test
    fun `a tilde fence and an unclosed fence are code to the end`() {
        val markdown = "~~~\na * b\n~~~\nx\n```\nc * d"
        val corrected = correctMarkdownOutsideFences(markdown)
        assertEquals(true, "a * b" in corrected)
        assertEquals(true, corrected.endsWith("c * d"))
    }

    @Test
    fun `a fence is longer than any backtick run in the code`() {
        assertEquals("```", fenceFor("val a"))
        assertEquals("````", fenceFor("a ``` b"))
        assertEquals("``````", fenceFor("`````"))
    }
}
