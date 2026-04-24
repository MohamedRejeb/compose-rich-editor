package com.mohamedrejeb.richeditor.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reproduces https://github.com/MohamedRejeb/compose-rich-editor/issues/579
 *
 * `setText` should preserve the literal characters passed in, even when they
 * look like markdown (`*`, `_`, `` ` ``, `#`, ...). These characters have no
 * special meaning to `setText`, only to `setMarkdown`.
 */
class RichTextStateSetTextTest {

    @Test
    fun setText_preservesAsterisks() {
        val state = RichTextState()

        state.setText("*some text*")

        assertEquals("*some text*", state.toText())
        assertEquals("*some text*", state.annotatedString.text)
    }

    @Test
    fun setText_preservesDoubleAsterisks() {
        val state = RichTextState()

        state.setText("**bold**")

        assertEquals("**bold**", state.toText())
    }

    @Test
    fun setText_preservesUnderscores() {
        val state = RichTextState()

        state.setText("_italic_")

        assertEquals("_italic_", state.toText())
    }

    @Test
    fun setText_preservesBackticks() {
        val state = RichTextState()

        state.setText("`code`")

        assertEquals("`code`", state.toText())
    }

    @Test
    fun setText_preservesHashHeading() {
        val state = RichTextState()

        state.setText("# heading")

        assertEquals("# heading", state.toText())
    }

    @Test
    fun setText_preservesMixedMarkdownLikeChars() {
        val state = RichTextState()

        val raw = "*a* **b** _c_ `d` ~e~"
        state.setText(raw)

        assertEquals(raw, state.toText())
    }
}
