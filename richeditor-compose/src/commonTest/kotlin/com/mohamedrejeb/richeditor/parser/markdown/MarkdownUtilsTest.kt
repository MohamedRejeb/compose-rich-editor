package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

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

    @Test
    fun testCorrectMarkdownListIndentation() {
        val markdownInput = """
            - *Hey All * **HHH**
              - Item 2
                - ***Bold-Italic ***normal
            Hey
        """.trimIndent()
        val expectedOutput = """
            - *Hey All*  **HHH**
                - Item 2
                    - ***Bold-Italic*** normal
            Hey
        """.trimIndent()

        assertEquals(
            expectedOutput,
            correctMarkdownText(markdownInput)
        )
    }

    @Test
    fun testDeepNestedList20Levels() {
        val input = buildString {
            repeat(20) { level ->
                append("  ".repeat(level))  // 2-space indent per level
                appendLine("- Item ${level + 1}")
            }
        }.trimEnd()

        val output = correctMarkdownText(input)

        assertTrue(
            output.length >= input.length,
            "Lost ${input.length - output.length} characters"
        )
    }

    @Test
    fun testDeepNestedListWithFormatting() {
        val input = buildString {
            repeat(20) { level ->
                append("  ".repeat(level))
                appendLine("- Item ${level + 1} **bold** *italic*")
            }
        }.trimEnd()

        val output = correctMarkdownText(input)
        assertTrue(output.length >= input.length)
    }


    @Test
    fun testParseDeepNestedListDoesNotCrash() {
        // This should not throw StringIndexOutOfBoundsException
        val markdown = buildString {
            repeat(20) { level ->
                append("  ".repeat(level))
                appendLine("- Item ${level + 1} **bold** [link](url)")
            }
        }.trimEnd()

        // This calls correctMarkdownText(), builds AST, then calls getTextInNode()
        val encoded = RichTextStateMarkdownParser.encode(markdown)
        assertTrue(encoded.richParagraphList.isNotEmpty())
    }

    @Test
    fun testNestedListWithoutCrash() {
        // From the production crash report
        val message = buildString {
            repeat(20) { i ->
                append("- Item $i\n")
                append("  - Nested $i\n")
            }
            append("**Bold at end** and [link](http://test.com)")
        }

        // Should not throw StringIndexOutOfBoundsException
        val state = RichTextState()
        state.setMarkdown(message)
        assertNotNull(state.annotatedString.text)
    }

}