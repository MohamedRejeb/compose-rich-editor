package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.FontRunStyle
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import com.mohamedrejeb.richeditor.model.RichSpanStyleDescriptor
import com.mohamedrejeb.richeditor.model.RichTextFormat
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.richSpanStyleDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextHtmlCustomStylesTest {

    private fun fontDescriptor(
        formats: Set<RichTextFormat> = setOf(RichTextFormat.Json, RichTextFormat.Html),
    ): RichSpanStyleDescriptor =
        richSpanStyleDescriptor<FontRunStyle>(
            kind = "app:font",
            formats = formats,
            encode = { style -> mapOf("slug" to style.slug) },
            decode = { attrs -> attrs["slug"]?.let { FontRunStyle(slug = it) } },
        )

    private fun stateWithFont(slug: String = "amiri"): RichTextState {
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = slug), TextRange(0, 5))
        return state
    }

    @Test
    fun `registered custom style round-trips through toHtml and setHtml`() {
        val state = stateWithFont()

        val html = state.toHtml()
        assertTrue("data-richeditor-kind" in html)

        val loaded = RichTextState()
        loaded.spanStyleRegistry.register(fontDescriptor())
        loaded.setHtml(html)

        assertEquals("Hello world", loaded.toText())
        val custom = loaded.toRichTextDocument().blocks.single().spans
            .filterIsInstance<RichTextSpanMark.Custom>()
            .single()
        assertEquals(0..4, custom.range)
        assertEquals(FontRunStyle(slug = "amiri"), custom.style)
    }

    @Test
    fun `custom style with html format opted out is not exported`() {
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor(formats = setOf(RichTextFormat.Json)))
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 5))

        assertTrue("data-richeditor-kind" !in state.toHtml())
    }

    @Test
    fun `html import without registration degrades to plain text`() {
        val html = stateWithFont().toHtml()

        val loaded = RichTextState()
        loaded.setHtml(html)

        assertEquals("Hello world", loaded.toText())
        assertTrue(
            loaded.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Custom },
        )
    }

    @Test
    fun `attrs payload escaping round-trips special characters`() {
        val trickySlug = "a&b=c%d \"quoted\" <tag>"
        val state = stateWithFont(slug = trickySlug)

        val loaded = RichTextState()
        loaded.spanStyleRegistry.register(fontDescriptor())
        loaded.setHtml(state.toHtml())

        val custom = loaded.toRichTextDocument().blocks.single().spans
            .filterIsInstance<RichTextSpanMark.Custom>()
            .single()
        assertEquals(FontRunStyle(slug = trickySlug), custom.style)
    }

    @Test
    fun `toHtml of a range carries registered custom spans`() {
        val state = stateWithFont()

        val html = state.toHtml(TextRange(0, 5))

        assertTrue("data-richeditor-kind" in html)

        val loaded = RichTextState()
        loaded.spanStyleRegistry.register(fontDescriptor())
        loaded.setHtml(html)
        assertTrue(
            loaded.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Custom && it.style == FontRunStyle(slug = "amiri") },
        )
    }

    @Test
    fun `setMarkdown resolves inline html custom spans through the state registry`() {
        val markdown = "before <span data-richeditor-kind=\"app:font\" data-richeditor-attrs=\"slug=amiri\">styled</span> after"

        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setMarkdown(markdown)

        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Custom && it.style == FontRunStyle(slug = "amiri") },
        )
    }

    @Test
    fun `pasting html with custom spans resolves through the target state registry`() {
        val html = stateWithFont().toHtml()

        val target = RichTextState()
        target.spanStyleRegistry.register(fontDescriptor())
        target.setText("Prefix ")
        target.insertHtml(html, position = 7)

        assertEquals("Prefix Hello world", target.toText())
        assertTrue(
            target.toRichTextDocument().blocks.single().spans
                .any { it is RichTextSpanMark.Custom && it.style == FontRunStyle(slug = "amiri") },
        )
    }
}
