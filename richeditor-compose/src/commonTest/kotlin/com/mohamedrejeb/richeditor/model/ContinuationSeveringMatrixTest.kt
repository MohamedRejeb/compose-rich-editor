package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every paragraph-level mutator applied to a `<br>` continuation must sever it, and the chain
 * behind it, into independent paragraphs. The html encoder can only write a continuation inside
 * the tag of the paragraph it continues, so a continuation that became a heading, a list item or
 * a centered paragraph has no faithful `<br>` form: its level, type or alignment would be dropped
 * on save, or smeared over the paragraph it continues.
 *
 * The document is `<p>a<br>b<br>c</p>` with the caret in `b`. Each row asserts the triple: the
 * model (the continuation flags of all three paragraphs plus the mutated property), the exact
 * `toHtml`, and reload agreement (a fresh `setHtml` of that html reports the same structure).
 */
class ContinuationSeveringMatrixTest {

    private data class Structure(
        val text: String,
        val continuations: List<Boolean>,
        val headings: List<HeadingStyle>,
        val types: List<String>,
    )

    private fun structure(state: RichTextState) = Structure(
        text = state.toText(),
        continuations = state.richParagraphList.map { it.isFromLineBreak },
        headings = state.richParagraphList.map { it.headingStyle },
        types = state.richParagraphList.map { paragraph ->
            val type = paragraph.type
            val level = (type as? ConfigurableListLevel)?.level ?: 0
            "${type::class.simpleName}:$level"
        },
    )

    private fun continuationDocument(html: String = "<p>a<br>b<br>c</p>"): RichTextState =
        RichTextState().apply { setHtml(html) }

    private fun assertReloadAgrees(state: RichTextState) {
        val fresh = RichTextState().apply { setHtml(state.toHtml()) }
        assertEquals(structure(state), structure(fresh), "a fresh decode must report the same structure")
    }

    // setHeadingStyle

    @Test
    fun `a heading on a middle continuation severs it and its chain`() {
        val state = continuationDocument()
        state.selection = TextRange(3)

        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals(listOf(false, false, false), structure(state).continuations)
        assertEquals(listOf(HeadingStyle.Normal, HeadingStyle.H1, HeadingStyle.Normal), structure(state).headings)
        assertEquals("<p>a</p><h1>b</h1><p>c</p>", state.toHtml())
        assertReloadAgrees(state)
    }

    @Test
    fun `a heading on the last continuation severs it`() {
        val state = continuationDocument("<p>a<br>b</p>")
        state.selection = TextRange(3)

        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals(listOf(false, false), structure(state).continuations)
        assertEquals(listOf(HeadingStyle.Normal, HeadingStyle.H1), structure(state).headings)
        assertEquals("<p>a</p><h1>b</h1>", state.toHtml())
        assertReloadAgrees(state)
    }

    @Test
    fun `a heading on the head severs the whole chain`() {
        // Same rule as addParagraphStyle on the head: the continuations were not touched by
        // the user, so they must not silently become part of the heading.
        val state = continuationDocument()
        state.selection = TextRange(1)

        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals(listOf(false, false, false), structure(state).continuations)
        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.Normal, HeadingStyle.Normal), structure(state).headings)
        assertEquals("<h1>a</h1><p>b</p><p>c</p>", state.toHtml())
        assertReloadAgrees(state)
    }

    @Test
    fun `a heading that changes nothing leaves the continuations alone`() {
        val state = continuationDocument()
        state.selection = TextRange(3)

        state.setHeadingStyle(HeadingStyle.Normal)

        assertEquals(listOf(false, true, true), structure(state).continuations)
        assertEquals("<p>a<br>b<br>c</p>", state.toHtml())
        assertReloadAgrees(state)
    }
}
