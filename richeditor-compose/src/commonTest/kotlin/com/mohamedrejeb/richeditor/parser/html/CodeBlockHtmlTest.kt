package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.plainText
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class CodeBlockHtmlTest {

    private fun stateOf(html: String) = RichTextState().apply { setHtml(html) }

    private fun RichTextState.codeLines(): List<String> =
        richParagraphList.filter { it.type is CodeBlock }.map { it.plainText() }

    private fun RichTextState.lines(): List<String> =
        richParagraphList.map { it.plainText() }

    @Test
    fun `pre with code and a language class becomes a code block`() {
        val state = stateOf("<p>Before</p><pre><code class=\"language-kotlin\">fun a() {\n    val b = 1\n}</code></pre><p>After</p>")
        assertEquals(listOf("fun a() {", "    val b = 1", "}"), state.codeLines())
        assertEquals("kotlin", state.richParagraphList.map { it.type }.filterIsInstance<CodeBlock>().first().language)
        assertEquals(listOf("Before", "fun a() {", "    val b = 1", "}", "After"), state.richParagraphList.map { it.plainText() })
        assertTrue(state.richParagraphList.last().type is DefaultParagraph)
    }

    @Test
    fun `pre without code is a code block with no language`() {
        val state = stateOf("<pre>a\n  b</pre>")
        assertEquals(listOf("a", "  b"), state.codeLines())
        assertEquals(null, (state.richParagraphList.first().type as CodeBlock).language)
    }

    @Test
    fun `the newline after the opening tag and before the closing tag is not a line`() {
        assertEquals(listOf("a", "b"), stateOf("<pre><code>\na\nb\n</code></pre>").codeLines())
    }

    @Test
    fun `blank lines tabs and entities inside pre are kept`() {
        val state = stateOf("<pre><code>\ta &lt; b\n\nc &amp;&amp; d</code></pre>")
        assertEquals(listOf("\ta < b", "", "c && d"), state.codeLines())
    }

    @Test
    fun `markup inside pre is read as plain code`() {
        val state = stateOf("<pre><code><span class=\"k\">val</span> <b>a</b><br>b</code></pre>")
        assertEquals(listOf("val a", "b"), state.codeLines())
    }

    @Test
    fun `two pre elements stay two blocks`() {
        val state = stateOf("<pre><code>a</code></pre><pre><code>b</code></pre>")
        assertEquals(2, state.richParagraphList.toList().codeBlockGroups().size)
    }

    @Test
    fun `export writes pre and code with the language`() {
        val html = "<p>Before</p><pre><code class=\"language-kotlin\">fun a() {\n    val b = 1 &lt; 2\n}</code></pre><p>After</p>"
        assertEquals(html, stateOf(html).toHtml())
    }

    @Test
    fun `export without a language has no class`() {
        assertEquals("<pre><code>a\n\nb</code></pre>", stateOf("<pre>a\n\nb</pre>").toHtml())
    }

    @Test
    fun `a language with a quote is escaped in the class`() {
        val state = RichTextState().apply { setMarkdown("```a\"b\nx\n```") }
        val html = state.toHtml()
        assertTrue("a\"b" !in html.substringBefore(">x"), "the raw quote must not appear in the tag: $html")
        assertEquals(listOf("x"), stateOf(html).codeLines())
    }

    @Test
    fun `code inline outside pre is still a code span`() {
        val state = stateOf("<p>a <code>b</code></p>")
        assertTrue(state.richParagraphList.none { it.type is CodeBlock })
        assertEquals("<p>a <code>b</code></p>", state.toHtml())
    }

    @Test
    fun `a document that is only a pre round trips`() {
        val state = stateOf("<pre><code>a</code></pre>")
        assertEquals(listOf("a"), state.lines())
        assertEquals("<pre><code>a</code></pre>", state.toHtml())
    }

    @Test
    fun `an empty pre is one empty code line`() {
        val state = stateOf("<pre><code></code></pre>")
        assertEquals(listOf(""), state.codeLines())
        assertEquals("<pre><code></code></pre>", state.toHtml())
    }

    @Test
    fun `crlf inside pre splits lines`() {
        assertEquals(listOf("a", "b"), stateOf("<pre>a\r\nb</pre>").codeLines())
    }

    @Test
    fun `space runs inside pre are exported raw`() {
        val html = "<pre><code>a  b\n\tc  </code></pre>"
        assertEquals(html, stateOf(html).toHtml())
    }

    @Test
    fun `text after a pre starts its own paragraph`() {
        val state = stateOf("<pre>a</pre>b")
        assertEquals(listOf("a", "b"), state.lines())
        assertTrue(state.richParagraphList.last().type is DefaultParagraph)
    }

    @Test
    fun `an unclosed pre is closed at the end`() {
        assertEquals(listOf("a", "b"), stateOf("<pre>a\nb").codeLines())
    }

    @Test
    fun `pre inside a p ends the paragraph and the rest follows the block`() {
        val state = stateOf("<p>Before<pre>a</pre>After</p>")
        assertEquals(listOf("Before", "a", "After"), state.lines())
        assertEquals(listOf("a"), state.codeLines())
    }

    @Test
    fun `pre after a br keeps the br line`() {
        assertEquals(listOf("a", "b"), stateOf("<p>a<br></p><pre>b</pre>").lines())
        assertEquals(listOf("a", "b", "c"), stateOf("<p>a<br>b</p><pre>c</pre>").lines())
    }

    @Test
    fun `an explicit empty paragraph before a pre is kept`() {
        val state = stateOf("<p></p><pre>a</pre>")
        assertEquals(listOf("", "a"), state.lines())
        assertTrue(state.richParagraphList.first().type is DefaultParagraph)
    }

    @Test
    fun `pre between two lists round trips`() {
        val html = "<ul><li>x</li></ul><pre><code>a</code></pre><ul><li>y</li></ul>"
        val state = stateOf(html)
        assertEquals(listOf("x", "a", "y"), state.lines())
        assertEquals(html, state.toHtml())
    }

    @Test
    fun `pre inside a list item flattens to a top level block`() {
        val state = stateOf("<ul><li>x<pre>a</pre></li><li>y</li></ul>")
        assertEquals(listOf("x", "a", "y"), state.lines())
        assertTrue(state.richParagraphList[0].type is UnorderedList)
        assertTrue(state.richParagraphList[1].type is CodeBlock)
        assertTrue(state.richParagraphList[2].type is UnorderedList)
        assertEquals(listOf("x", "a", "y"), stateOf(state.toHtml()).lines())
    }

    @Test
    fun `pre as the only content of a list item keeps the code`() {
        val state = stateOf("<ul><li><pre>a</pre></li></ul>")
        assertEquals(listOf("a"), state.lines())
        assertTrue(state.richParagraphList.single().type is CodeBlock)
    }

    @Test
    fun `pre inside a blockquote keeps the code`() {
        val state = stateOf("<blockquote><pre>a\nb</pre></blockquote>")
        assertEquals(listOf("a", "b"), state.lines())
    }

    @Test
    fun `an ide clipboard pre with coloured spans and br becomes plain code lines`() {
        val html = "<div style=\"background-color:#1e1f22;\"><pre style=\"font-family:monospace;\">" +
            "<span style=\"font-style:italic;\">println</span>(<span style=\"color:#6aab73;\">\"a&#32;</span>b)" +
            "<br><br><span style=\"color:#cf8e6d;\">val&#32;</span>c&#32;=&#32;1</pre></div>"
        val state = stateOf(html)
        assertEquals(listOf("println(\"a b)", "", "val c = 1"), state.codeLines())
        assertEquals(state.codeLines(), state.lines())
    }
}
