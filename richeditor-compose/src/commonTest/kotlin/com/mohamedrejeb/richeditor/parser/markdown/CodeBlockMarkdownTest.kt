package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.plainText
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class CodeBlockMarkdownTest {

    private fun stateOf(markdown: String) = RichTextState().apply { setMarkdown(markdown) }

    private fun RichTextState.codeLines(): List<String> =
        richParagraphList.filter { it.type is CodeBlock }.map { it.plainText() }

    @Test
    fun `a fenced block becomes code lines with its language`() {
        val state = stateOf("Before\n\n```kotlin\nfun a() {\n    val b = 1\n}\n```\n\nAfter")
        assertEquals(listOf("fun a() {", "    val b = 1", "}"), state.codeLines())
        val types = state.richParagraphList.map { it.type }
        assertEquals("kotlin", types.filterIsInstance<CodeBlock>().first().language)
        assertTrue(types.first() is DefaultParagraph)
        assertTrue(types.last() is DefaultParagraph)
        assertEquals("Before", state.richParagraphList.first().plainText())
        assertEquals("After", state.richParagraphList.last().plainText())
    }

    @Test
    fun `a block without a language has a null language`() {
        val state = stateOf("```\na\n```")
        assertEquals(null, (state.richParagraphList.first().type as CodeBlock).language)
        assertEquals(listOf("a"), state.codeLines())
    }

    @Test
    fun `blank lines tabs and inline markdown inside a block are kept as written`() {
        val state = stateOf("```\n\ta **b**\n\n  c * d\n```")
        assertEquals(listOf("\ta **b**", "", "  c * d"), state.codeLines())
    }

    @Test
    fun `windows line endings leave no carriage return in code lines`() {
        val state = stateOf("```kotlin\r\nval a\r\nval b\r\n```\r\n")
        assertEquals(listOf("val a", "val b"), state.codeLines())
    }

    @Test
    fun `an unclosed fence runs to the end`() {
        val state = stateOf("text\n\n```kotlin\nval a\nval b")
        assertEquals(listOf("val a", "val b"), state.codeLines())
    }

    @Test
    fun `a document that ends with a block has no extra empty paragraph`() {
        val state = stateOf("a\n\n```\nb\n```")
        assertTrue(state.richParagraphList.last().type is CodeBlock)
    }

    @Test
    fun `blank lines around a block add no empty paragraph`() {
        val state = stateOf("Before\n\n\n```\nb\n```\n\n\nAfter")
        assertEquals(listOf("Before", "b", "After"), state.richParagraphList.map { it.plainText() })
    }

    @Test
    fun `two blocks in a row stay two blocks`() {
        val state = stateOf("```kotlin\na\n```\n```python\nb\n```")
        assertEquals(2, state.richParagraphList.toList().codeBlockGroups().size)
    }

    @Test
    fun `an odd language tag is kept as written`() {
        val state = stateOf("```c++ title=\"x\"\na\n```")
        assertEquals("c++ title=\"x\"", (state.richParagraphList.first().type as CodeBlock).language)
    }

    @Test
    fun `export writes a fence with the language`() {
        val markdown = "Before\n\n```kotlin\nfun a() {\n    val b = 1 * 2\n}\n```\n\nAfter"
        assertEquals(markdown, stateOf(markdown).toMarkdown())
    }

    @Test
    fun `export uses a longer fence when the code holds backticks`() {
        val markdown = "````\na ``` b\n````"
        val state = stateOf(markdown)
        assertEquals(listOf("a ``` b"), state.codeLines())
        assertEquals(markdown, state.toMarkdown())
    }

    @Test
    fun `a round trip keeps the code lines`() {
        val markdown = "```python\ndef a():\n\n    return \"*x*\"\n```"
        val again = stateOf(stateOf(markdown).toMarkdown())
        assertEquals(listOf("def a():", "", "    return \"*x*\""), again.codeLines())
    }
}
