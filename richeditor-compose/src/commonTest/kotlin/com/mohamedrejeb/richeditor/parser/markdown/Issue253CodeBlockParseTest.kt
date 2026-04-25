package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Probe tests for #253 / #540: fenced code blocks (```) being parsed
 * to empty text by the markdown decoder.
 *
 * The reporter says input like:
 *
 *   ```
 *   fun foo() {}
 *   ```
 *
 * comes back as an empty paragraph after `setMarkdown` / `toMarkdown`.
 * These tests document the current behavior so we know whether the fix
 * is needed and what shape it should take.
 */
class Issue253CodeBlockParseTest {

    private val fenced =
        """
            |```
            |fun foo() {}
            |```
        """.trimMargin()

    private val fencedWithLang =
        """
            |```kotlin
            |fun foo() {}
            |```
        """.trimMargin()

    @Test
    fun `setMarkdown on a plain fenced code block keeps the inner text`() {
        val state = RichTextState()
        state.setMarkdown(fenced)

        val text = state.toText()
        assertTrue(
            "fun foo() {}" in text,
            "expected the code body to survive setMarkdown, got: '$text'"
        )
    }

    @Test
    fun `setMarkdown on a language-tagged fenced block keeps the inner text`() {
        val state = RichTextState()
        state.setMarkdown(fencedWithLang)

        val text = state.toText()
        assertTrue(
            "fun foo() {}" in text,
            "expected the code body to survive setMarkdown, got: '$text'"
        )
    }
}
