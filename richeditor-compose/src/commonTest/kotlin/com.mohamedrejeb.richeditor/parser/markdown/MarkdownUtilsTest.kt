package com.mohamedrejeb.richeditor.parser.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownUtilsTest {

    @Test
    fun testCorrectMarkdown1() {
        val markdownInput = "**Bold **Normal"
        val expectedOutput = "**Bold** Normal"

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }

    @Test
    fun testCorrectMarkdown2() {
        val markdownInput = "**Bold ***Normal*"
        val expectedOutput = "**Bold** *Normal*"

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }


    @Test
    fun testCorrectMarkdown3() {
        val markdownInput = "**Bold ***Normal  **~Test ~*  "
        val expectedOutput = "**Bold** *Normal*  *~Test~*   "

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }

    @Test
    fun testCorrectMarkdown4() {
        val markdownInput = "*Hey All * **HHH**"
        val expectedOutput = "*Hey All*  **HHH**"

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }

    @Test
    fun testCorrectMarkdown5() {
        val markdownInput = "***Bold-Italic ***normal"
        val expectedOutput = "***Bold-Italic*** normal"

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }

}