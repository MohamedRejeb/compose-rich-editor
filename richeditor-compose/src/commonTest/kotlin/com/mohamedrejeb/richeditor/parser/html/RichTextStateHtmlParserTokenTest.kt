package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateHtmlParserTokenTest {

    @Test
    fun `token serializes to span with data attributes`() {
        val input = """<p>Hi <span data-trigger="mention" data-id="u1">@mohamed</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val html = RichTextStateHtmlParser.decode(state)

        assertTrue(
            html.contains("""<span data-trigger="mention" data-id="u1">""") &&
                (html.contains("@mohamed</span>") || html.contains("&commat;mohamed</span>")),
            "Expected data-trigger span with label in output, got: $html"
        )
    }

    @Test
    fun `token parses to Token rich span style`() {
        val input = """<p><span data-trigger="mention" data-id="u1">@mohamed</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)

        val span = state.getRichSpanByTextIndex(0)
        val style = span?.richSpanStyle
        assertTrue(style is RichSpanStyle.Token)
        assertEquals("mention", style.triggerId)
        assertEquals("u1", style.id)
        assertEquals("@mohamed", style.label)
        assertTrue(style.isAtomic)
    }

    @Test
    fun `multiple same-trigger tokens round-trip`() {
        val input = """<p>CC <span data-trigger="mention" data-id="u1">@alice</span> and <span data-trigger="mention" data-id="u2">@bob</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val html = RichTextStateHtmlParser.decode(state)

        assertTrue(html.contains("""data-trigger="mention" data-id="u1""""), "Missing first token: $html")
        assertTrue(html.contains("""data-trigger="mention" data-id="u2""""), "Missing second token: $html")
    }

    @Test
    fun `multiple different-trigger tokens round-trip`() {
        val input = """<p><span data-trigger="mention" data-id="u1">@alice</span> <span data-trigger="hashtag" data-id="release">#release</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val html = RichTextStateHtmlParser.decode(state)

        assertTrue(html.contains("""data-trigger="mention""""), "Missing mention: $html")
        assertTrue(html.contains("""data-trigger="hashtag""""), "Missing hashtag: $html")
    }

    @Test
    fun `unknown triggerId still parses and re-serializes`() {
        val input = """<p><span data-trigger="unknown" data-id="x1">@ghost</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val html = RichTextStateHtmlParser.decode(state)

        assertTrue(html.contains("""data-trigger="unknown""""), "Should preserve unknown trigger: $html")
    }

    @Test
    fun `span without data-trigger is plain span not token`() {
        val input = """<p><span class="foo">hi</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val span = state.getRichSpanByTextIndex(0)
        assertTrue(span?.richSpanStyle !is RichSpanStyle.Token)
    }

    @Test
    fun `malformed span with data-trigger but no data-id is plain`() {
        val input = """<p><span data-trigger="mention">@x</span></p>"""
        val state = RichTextStateHtmlParser.encode(input)
        val span = state.getRichSpanByTextIndex(0)
        assertTrue(span?.richSpanStyle !is RichSpanStyle.Token)
    }

    @Test
    fun `token id with quotes is escaped on output`() {
        // Direct construction exercises the escape path without relying on parser input shape.
        val state = com.mohamedrejeb.richeditor.model.RichTextState()
        state.setHtml("""<p><span data-trigger="mention" data-id='a"b'>@x</span></p>""")
        val html = state.toHtml()
        assertTrue(
            html.contains("""data-id="a&quot;b"""") || html.contains("""data-id='a"b'"""),
            "id quote must be escaped or preserved safely: $html"
        )
        assertTrue(!html.contains("""data-id="a"b""""), "raw unescaped quote broke attribute: $html")
    }
}
