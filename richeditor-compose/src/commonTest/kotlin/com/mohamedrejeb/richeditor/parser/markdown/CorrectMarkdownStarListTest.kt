package com.mohamedrejeb.richeditor.parser.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for issue #637 — `correctMarkdownText` mangles
 * star-style unordered list markers (`* item`) by treating the `*` as
 * a possible emphasis marker and rewriting the surrounding spaces.
 */
class CorrectMarkdownStarListTest {

    @Test
    fun testStarBulletsAtTopLevelAreUntouched() {
        val input = "* a\n* b\n"
        val output = correctMarkdownText(input)

        assertEquals(input, output)
    }

    @Test
    fun testStarBulletsAfterBlankLineAreUntouched() {
        val input = "Intro\n\n* a\n* b\n"
        val output = correctMarkdownText(input)

        assertEquals(input, output)
    }

    @Test
    fun testNestedStarBullets() {
        val input = "* a\n* b\n  * c\n  * d\n"
        val expected = "* a\n* b\n    * c\n    * d\n"
        val output = correctMarkdownText(input)

        assertEquals(expected, output)
    }

    @Test
    fun testStarBulletsFollowingFencedCodeBlock() {
        val input = """
            ```
              var a = 10
            ```

            * a
            * b
              * c
              * d
        """.trimIndent() + "\n"
        val output = correctMarkdownText(input)

        // Bullets must retain their `* <space>item` form.
        assertEquals(
            true,
            output.contains("* a\n* b"),
            "Expected star bullets to survive, got: <$output>"
        )
    }

    @Test
    fun testMixedListMarkers() {
        val input = "* a\n- b\n+ c\n"
        val output = correctMarkdownText(input)

        assertEquals(input, output)
    }

}
