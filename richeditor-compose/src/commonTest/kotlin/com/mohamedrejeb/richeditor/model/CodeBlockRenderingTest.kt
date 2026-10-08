package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Code blocks as located in the rendered text: their range and the token styles drawn over
 * them. The styles are presentation only and must never reach the document or an export.
 */
@OptIn(ExperimentalRichTextApi::class)
class CodeBlockRenderingTest {

    private val colors = CodeBlockColors(
        keyword = SpanStyle(color = Color.Red),
        string = SpanStyle(color = Color.Green),
        number = SpanStyle(color = Color.Blue),
        comment = SpanStyle(color = Color.Gray),
        annotation = SpanStyle(color = Color.Yellow),
    )

    private fun stateOf(markdown: String) = RichTextState().apply {
        config.codeBlockColors = colors
        setMarkdown(markdown)
    }

    @Test
    fun `tokens are placed at their offsets in the rendered text`() {
        val state = stateOf("ab\n\n```kotlin\nval a = 1\n```")
        assertEquals("ab val a = 1", state.annotatedString.text)
        val block = state.renderedCodeBlocks.single()
        assertEquals(TextRange(3, 12), block.range)
        assertEquals(
            listOf(AnnotatedString.Range(colors.keyword, 3, 6), AnnotatedString.Range(colors.number, 11, 12)),
            block.styles,
        )
    }

    @Test
    fun `a comment that spans lines is one style across them`() {
        val state = stateOf("```kotlin\n/* a\nb */ val\n```")
        assertEquals(
            listOf(AnnotatedString.Range(colors.comment, 0, 9), AnnotatedString.Range(colors.keyword, 10, 13)),
            state.renderedCodeBlocks.single().styles,
        )
    }

    @Test
    fun `an unknown language has a block and no styles`() {
        val block = stateOf("```whatever\nval a\n```").renderedCodeBlocks.single()
        assertEquals(TextRange(0, 5), block.range)
        assertEquals(emptyList(), block.styles)
    }

    @Test
    fun `two blocks are rendered separately`() {
        val state = stateOf("```kotlin\nval a\n```\n```python\ndef b\n```")
        assertEquals(2, state.renderedCodeBlocks.size)
        assertEquals(listOf(colors.keyword, colors.keyword), state.renderedCodeBlocks.flatMap { it.styles }.map { it.item })
    }

    @Test
    fun `text without a code block renders none`() {
        assertEquals(emptyList(), stateOf("just **text**").renderedCodeBlocks)
    }

    @Test
    fun `token colours never reach an export`() {
        val state = stateOf("```kotlin\nval a = 1\n```")
        assertTrue(state.renderedCodeBlocks.single().styles.isNotEmpty())
        assertFalse("color" in state.toHtml(), state.toHtml())
        assertEquals("```kotlin\nval a = 1\n```", state.toMarkdown())
        assertTrue(state.annotatedString.spanStyles.none { it.item.color == Color.Red })
        assertTrue(state.toRichTextDocument().blocks.all { it.spans.isEmpty() })
    }

    @Test
    fun `changing the colours restyles the block`() {
        val state = stateOf("```kotlin\nval a\n```")
        val other = CodeBlockColors(SpanStyle(color = Color.Cyan), colors.string, colors.number, colors.comment, colors.annotation)
        state.config.codeBlockColors = other
        assertEquals(Color.Cyan, state.renderedCodeBlocks.single().styles.single().item.color)
    }

    @Test
    fun `editing the code recomputes the tokens`() {
        val state = stateOf("```kotlin\nval a\n```")
        state.setMarkdown("```kotlin\nfoo val\n```")
        assertEquals(
            listOf(AnnotatedString.Range(colors.keyword, 4, 7)),
            state.renderedCodeBlocks.single().styles,
        )
        state.setMarkdown("plain")
        assertEquals(emptyList(), state.renderedCodeBlocks)
    }

    @Test
    fun `withCodeBlockStyles adds the styles and keeps the text`() {
        val state = stateOf("```kotlin\nval a\n```")
        val styled = state.annotatedString.withCodeBlockStyles(state.renderedCodeBlocks)
        assertEquals(state.annotatedString.text, styled.text)
        assertTrue(styled.spanStyles.any { it.item == colors.keyword && it.start == 0 && it.end == 3 })
        assertSame(state.annotatedString, state.annotatedString.withCodeBlockStyles(emptyList()))
    }

    @Test
    fun `the cache tokenizes a block once while its text is unchanged`() {
        val cache = CodeBlockTokenCache()
        val first = cache.tokens("val a", "kotlin")
        assertSame(first, cache.tokens("val a", "kotlin"))
        cache.keepOnlyUsed()
        assertSame(first, cache.tokens("val a", "kotlin"))
        cache.keepOnlyUsed()
        cache.keepOnlyUsed()
        assertFalse(first === cache.tokens("val a", "kotlin"))
    }
}
