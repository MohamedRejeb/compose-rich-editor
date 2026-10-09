package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.plainText
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Selecting, copying, pasting and deleting around code blocks. What a selection exports is what
 * a copy writes to the clipboard, and a paste goes through the same pipeline the editor uses.
 *
 * A code block is kept whole when it is pasted into ordinary text, and whatever is pasted into
 * a code block becomes plain code lines of that block.
 */
@OptIn(ExperimentalRichTextApi::class)
class CodeBlockClipboardTest {

    private val markdown = "Before text\n\n```kotlin\nval a = 1\n    val b = 2\nval c = 3\n```\n\nAfter text"
    private val pythonBlockHtml = "<pre><code class=\"language-python\">p = 1\nq = 2</code></pre>"

    private fun state(source: String = markdown) = RichTextState().apply { setMarkdown(source) }

    private fun RichTextState.offsetOf(text: String): Int = annotatedString.text.indexOf(text)

    /** Each paragraph as `P:text` or `C:text`, with `C*` on the first line of a code block. */
    private fun RichTextState.shape(): List<String> = richParagraphList.map { paragraph ->
        val type = paragraph.type as? CodeBlock
        val mark = if (type == null) "P" else if (type.isBlockStart) "C*" else "C"
        "$mark:${paragraph.plainText()}"
    }

    private fun RichTextState.paste(at: TextRange, plain: String, html: String? = null) {
        selection = at
        pendingClipboardHtml = html
        pendingClipboardPlainText = plain
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.replace(at.min, at.max, plain)
        applyChangeList(buffer)
    }

    private fun RichTextState.delete(from: Int, to: Int) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.replace(from, to, "")
        applyChangeList(buffer)
    }

    @Test
    fun `a whole block copies as code with its lines and indentation`() {
        val state = state()
        val range = TextRange(state.offsetOf("val a"), state.offsetOf("val c = 3") + 9)
        assertEquals("val a = 1\n    val b = 2\nval c = 3", state.toText(range))
        assertEquals(
            "<pre><code class=\"language-kotlin\">val a = 1\n    val b = 2\nval c = 3</code></pre>",
            state.toHtml(range),
        )
        assertEquals("```kotlin\nval a = 1\n    val b = 2\nval c = 3\n```", state.toMarkdown(range))
    }

    @Test
    fun `part of a block copies as a block of the selected text`() {
        val state = state()
        val range = TextRange(state.offsetOf("val a") + 4, state.offsetOf("val c") + 3)
        assertEquals("a = 1\n    val b = 2\nval", state.toText(range))
        assertEquals("<pre><code class=\"language-kotlin\">a = 1\n    val b = 2\nval</code></pre>", state.toHtml(range))
    }

    @Test
    fun `a selection across text and code copies both`() {
        val state = state()
        val range = TextRange(state.offsetOf("text"), state.offsetOf("val b") + 5)
        assertEquals("text\nval a = 1\n    val b", state.toText(range))
        assertEquals(
            "<p>text</p><pre><code class=\"language-kotlin\">val a = 1\n    val b</code></pre>",
            state.toHtml(range),
        )
        assertEquals("text\n\n```kotlin\nval a = 1\n    val b\n```", state.toMarkdown(range))
    }

    @Test
    fun `plain lines pasted into a block become lines of that block`() {
        val state = state()
        state.paste(TextRange(state.offsetOf("val b")), "x = 1\ny = 2")
        assertEquals(
            listOf("P:Before text", "C*:val a = 1", "C:    x = 1", "C:y = 2val b = 2", "C:val c = 3", "P:After text"),
            state.shape(),
        )
    }

    @Test
    fun `a block pasted in the middle of text splits the text around it`() {
        val state = state("Hello world")
        state.paste(TextRange(5), "p = 1\nq = 2", pythonBlockHtml)
        assertEquals(listOf("P:Hello", "C*:p = 1", "C:q = 2", "P: world"), state.shape())
        assertEquals("Hello\n\n```python\np = 1\nq = 2\n```\n\n world", state.toMarkdown())
    }

    @Test
    fun `a block pasted at the end of text follows it`() {
        val state = state("Hello")
        state.paste(TextRange(5), "p = 1\nq = 2", pythonBlockHtml)
        assertEquals(listOf("P:Hello", "C*:p = 1", "C:q = 2"), state.shape())
    }

    @Test
    fun `a block pasted at the start of text comes before it`() {
        val state = state("Hello")
        state.paste(TextRange(0), "p = 1\nq = 2", pythonBlockHtml)
        assertEquals(listOf("C*:p = 1", "C:q = 2", "P:Hello"), state.shape())
    }

    @Test
    fun `a block pasted next to another block stays a block of its own`() {
        val state = state()
        state.paste(TextRange(state.offsetOf("fore")), "p = 1\nq = 2", pythonBlockHtml)
        assertEquals(
            listOf("P:Be", "C*:p = 1", "C:q = 2", "P:fore text", "C*:val a = 1", "C:    val b = 2", "C:val c = 3", "P:After text"),
            state.shape(),
        )
        assertEquals(2, state.richParagraphList.toList().codeBlockGroups().size)
    }

    @Test
    fun `one line of code pasted into text joins the text`() {
        val state = state("Hello world")
        state.paste(TextRange(5), "a = 1", "<pre><code class=\"language-kotlin\">a = 1</code></pre>")
        assertEquals(listOf("P:Helloa = 1 world"), state.shape())
    }

    @Test
    fun `part of a block copied from one editor pastes as a block into another`() {
        val source = state()
        val range = TextRange(source.offsetOf("val a") + 4, source.offsetOf("val c") + 3)
        val target = state("Hello world")
        target.paste(TextRange(5), source.toText(range), source.toHtml(range))
        assertEquals(listOf("P:Hello", "C*:a = 1", "C:    val b = 2", "C:val", "P: world"), target.shape())
    }

    @Test
    fun `formatted text pasted into a block becomes plain code`() {
        val state = state()
        state.paste(TextRange(state.offsetOf("val b")), "bold", "<b>bold</b>")
        assertEquals(
            listOf("P:Before text", "C*:val a = 1", "C:    boldval b = 2", "C:val c = 3", "P:After text"),
            state.shape(),
        )
        assertTrue(state.toRichTextDocument().blocks[2].spans.isEmpty(), "no formatting inside the code line")
    }

    @Test
    fun `paragraphs pasted into a block become lines of that block`() {
        val state = state()
        state.paste(TextRange(state.offsetOf("val b")), "x\ny\nz", "<p><b>x</b></p><ul><li>y</li></ul><p>z</p>")
        assertEquals(
            listOf("P:Before text", "C*:val a = 1", "C:    x", "C:y", "C:zval b = 2", "C:val c = 3", "P:After text"),
            state.shape(),
        )
        assertEquals(1, state.richParagraphList.toList().codeBlockGroups().size)
    }

    @Test
    fun `a block pasted into a block joins it`() {
        val state = state()
        state.paste(TextRange(state.offsetOf("val b")), "p = 1\nq = 2", pythonBlockHtml)
        assertEquals(
            "Before text\n\n```kotlin\nval a = 1\n    p = 1\nq = 2val b = 2\nval c = 3\n```\n\nAfter text",
            state.toMarkdown(),
        )
        assertEquals(1, state.richParagraphList.toList().codeBlockGroups().size)
    }

    @Test
    fun `deleting from text into a block keeps the rest of the block`() {
        val state = state()
        state.delete(state.offsetOf("text"), state.offsetOf("val b") + 5)
        assertEquals(listOf("P:Before  = 2", "C:val c = 3", "P:After text"), state.shape())
        assertEquals("Before  = 2\n\n```kotlin\nval c = 3\n```\n\nAfter text", state.toMarkdown())
        assertEquals(1, state.renderedCodeBlocks.size)
    }

    @Test
    fun `deleting the first line of a block keeps the block and its language`() {
        val state = state()
        state.delete(state.offsetOf("val a"), state.offsetOf("    val b"))
        assertEquals("Before text\n\n```kotlin\n    val b = 2\nval c = 3\n```\n\nAfter text", state.toMarkdown())
    }

    @Test
    fun `deleting everything leaves an empty paragraph and no block`() {
        val state = state()
        state.delete(0, state.annotatedString.text.length)
        assertEquals(listOf("P:"), state.shape())
        assertEquals(emptyList(), state.renderedCodeBlocks)
    }
}
