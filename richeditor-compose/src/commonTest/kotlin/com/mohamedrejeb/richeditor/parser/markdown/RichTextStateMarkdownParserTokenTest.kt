package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateMarkdownParserTokenTest {

    @Test
    fun `token decodes to trigger link syntax`() {
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown("Hi [@mohamed](trigger:mention:u1)")

        val span = state.getRichSpanByTextIndex(3) // position of '@'
        val style = span?.richSpanStyle
        assertTrue(style is RichSpanStyle.Token, "Got: $style")
        assertEquals("mention", style.triggerId)
        assertEquals("u1", style.id)
        assertEquals("@mohamed", style.label)
    }

    @Test
    fun `token round-trips through markdown`() {
        val input = "Hi [@mohamed](trigger:mention:u1)"
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown(input)
        val out = state.toMarkdown()

        assertTrue(
            out.contains("[@mohamed](trigger:mention:u1)"),
            "Expected trigger link in output: $out"
        )
    }

    @Test
    fun `regular link is not confused with token`() {
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown("[click](https://example.com)")

        val span = state.getRichSpanByTextIndex(0)
        val style = span?.richSpanStyle
        assertTrue(style is RichSpanStyle.Link)
        assertEquals("https://example.com", style.url)
    }

    @Test
    fun `hashtag trigger round-trips`() {
        val input = "See [#release](trigger:hashtag:release-1)"
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown(input)
        val out = state.toMarkdown()

        assertTrue(out.contains("[#release](trigger:hashtag:release-1)"), "Got: $out")
    }

    @Test
    fun `malformed trigger destination is not treated as token`() {
        // Missing id portion - invariant: we must not fabricate a Token with an empty id.
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown("[x](trigger:mention:)")

        val span = state.getRichSpanByTextIndex(0)
        val style = span?.richSpanStyle
        assertTrue(style !is RichSpanStyle.Token, "Malformed payload must not become a Token, got $style")
    }

    @Test
    fun `id with underscores and hyphens parses correctly`() {
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setMarkdown("[@user_name](trigger:mention:user-id_1)")

        val span = state.getRichSpanByTextIndex(0)
        val style = span?.richSpanStyle
        assertTrue(style is RichSpanStyle.Token)
        assertEquals("user-id_1", style.id)
        assertEquals("@user_name", style.label)
    }
}
