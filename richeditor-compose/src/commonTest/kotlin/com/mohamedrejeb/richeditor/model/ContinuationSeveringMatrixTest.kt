package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
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
@OptIn(ExperimentalRichTextApi::class)
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

    // The "- " auto conversion, typed through the ime pipeline. Typed in front of existing
    // text it converts nothing on any paragraph (the marker has to be the whole line), so the
    // reachable continuation shape is an empty middle line.

    private fun RichTextState.imeBatch(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        reconcileBufferWithModel(buffer)
        val text = buffer.asCharSequence().toString()
        val selection = buffer.selection
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(text, selection)
        handleSelectionChanged(textFieldState.selection, fromGestureObserver = true)
    }

    @Test
    fun `a list marker typed on an empty continuation severs it and its chain`() {
        val state = continuationDocument("<p>a<br><br>c</p>")
        state.selection = TextRange(2)

        state.imeBatch {
            replace(2, 2, "-")
            selection = TextRange(3)
        }
        state.imeBatch {
            replace(3, 3, " ")
            selection = TextRange(4)
        }

        assertEquals(listOf(false, false, false), structure(state).continuations)
        assertEquals(listOf("DefaultParagraph:0", "UnorderedList:1", "DefaultParagraph:0"), structure(state).types)
        assertEquals("<p>a</p><ul><li><br></li></ul><p>c</p>", state.toHtml())
        assertReloadAgrees(state)
    }

    // List level changes. The html decoder no longer flags a list item, so an item carrying
    // the flag only comes from a document import (isLineBreak on a list item block).

    private fun flaggedListItems(headIndent: Int, tailIndent: Int): RichTextState {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = false, indent = headIndent)),
                RichTextBlock(
                    text = "b",
                    type = RichTextBlockType.ListItem(ordered = false, indent = tailIndent),
                    isLineBreak = true,
                ),
            ),
        )
        return RichTextState().setRichTextDocument(document, selection = TextRange(6))
    }

    @Test
    fun `a level increase on a flagged list item severs it`() {
        val state = flaggedListItems(headIndent = 0, tailIndent = 0)

        state.increaseListLevel()

        assertEquals(listOf(false, false), structure(state).continuations)
        assertEquals(listOf("UnorderedList:1", "UnorderedList:2"), structure(state).types)
        assertEquals("<ul><li>a<ul><li>b</li></ul></li></ul>", state.toHtml())
        assertReloadAgrees(state)
    }

    @Test
    fun `a level decrease on a flagged list item severs it`() {
        val state = flaggedListItems(headIndent = 0, tailIndent = 1)

        state.decreaseListLevel()

        assertEquals(listOf(false, false), structure(state).continuations)
        assertEquals(listOf("UnorderedList:1", "UnorderedList:1"), structure(state).types)
        assertEquals("<ul><li>a</li><li>b</li></ul>", state.toHtml())
        assertReloadAgrees(state)
    }
}
