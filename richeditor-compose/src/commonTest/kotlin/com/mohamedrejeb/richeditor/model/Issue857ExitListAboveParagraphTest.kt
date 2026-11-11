package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Issue 857: Enter on an empty list item leaves the list. With a paragraph below the list, that
 * paragraph lost its first character and an extra empty paragraph appeared.
 *
 * Cause: the exit turned the empty item into a plain paragraph and skipped the new paragraph, but
 * left the typed newline in the pending text. Dropping the list prefix moved that newline back, so
 * the paragraph scan met it a second time and split the paragraph below, removing its first
 * character as if it were the newline. With the list at the end of the document nothing was there
 * to split, which hid the bug.
 *
 * Fix: the exit removes the typed newline from the pending text and steps the caret back with it.
 * Numbered items below the exit used to keep their old numbers; they restart at 1, the way they
 * are exported.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue857ExitListAboveParagraphTest {

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

    private fun RichTextState.type(text: String) {
        val at = selection.min
        imeBatch {
            replace(at, at, text)
            selection = TextRange(at + text.length)
        }
    }

    /** Types through the [RichTextState.onTextFieldValueChange] bridge instead of a change list. */
    private fun RichTextState.typeThroughBridge(text: String) {
        val at = selection.min
        val current = annotatedString.text
        onTextFieldValueChange(
            TextFieldValue(
                text = current.substring(0, at) + text + current.substring(at),
                selection = TextRange(at + text.length),
            )
        )
    }

    /** A list with one item and an empty second item holding the caret, above [below]. */
    private fun listAbove(
        below: String,
        ordered: Boolean = false,
        typing: RichTextState.(String) -> Unit = { type(it) },
    ): RichTextState =
        RichTextState().apply {
            setHtml("<p></p>$below")
            selection = TextRange(0)
            if (ordered) toggleOrderedList() else toggleUnorderedList()
            typing("Item")
            typing("\n")
        }

    @Test
    fun `leaving a bullet list keeps the paragraph below intact`() {
        val state = listAbove("<p>Next line</p>")

        state.type("\n")

        assertEquals(3, state.richParagraphList.size)
        assertIs<UnorderedList>(state.richParagraphList[0].type)
        assertIs<DefaultParagraph>(state.richParagraphList[1].type)
        assertEquals("• Item  Next line", state.annotatedString.text)
        assertEquals(TextRange(7), state.selection)

        state.type("Typed")

        assertEquals(
            "<ul><li>Item</li></ul><p>Typed</p><p>Next line</p>",
            state.toHtml(),
        )
    }

    @Test
    fun `leaving a numbered list keeps the paragraph below intact`() {
        val state = listAbove("<p>Next line</p>", ordered = true)

        state.type("\n")

        assertEquals(3, state.richParagraphList.size)
        assertIs<OrderedList>(state.richParagraphList[0].type)
        assertIs<DefaultParagraph>(state.richParagraphList[1].type)

        state.type("Typed")

        assertEquals(
            "<ol><li>Item</li></ol><p>Typed</p><p>Next line</p>",
            state.toHtml(),
        )
    }

    @Test
    fun `leaving a list keeps every paragraph below intact`() {
        val state = listAbove("<p>First</p><p>Second</p>")

        state.type("\n")
        state.type("Typed")

        assertEquals(
            "<ul><li>Item</li></ul><p>Typed</p><p>First</p><p>Second</p>",
            state.toHtml(),
        )
    }

    @Test
    fun `leaving a list above another list keeps that list intact`() {
        val state = listAbove("<ol><li>One</li><li>Two</li></ol>")

        state.type("\n")
        state.type("Typed")

        assertEquals(
            "<ul><li>Item</li></ul><p>Typed</p><ol><li>One</li><li>Two</li></ol>",
            state.toHtml(),
        )
    }

    @Test
    fun `leaving a numbered list in the middle restarts the numbers below`() {
        val state = RichTextState().apply {
            setHtml("<ol><li>A</li><li>C</li></ol>")
            selection = TextRange(annotatedString.text.indexOf("A") + 1)
        }

        state.type("\n")
        state.type("\n")

        assertEquals("1. A  1. C", state.annotatedString.text)
        assertEquals("<ol><li>A</li></ol><p><br></p><ol><li>C</li></ol>", state.toHtml())
    }

    @Test
    fun `leaving a numbered list keeps the numbers of a list further below`() {
        val state = listAbove("<p>Between</p><ol><li>One</li><li>Two</li></ol>", ordered = true)

        state.type("\n")

        assertEquals("1. Item  Between 1. One 2. Two", state.annotatedString.text)
    }

    @Test
    fun `leaving a list through the text field value bridge keeps the paragraph below intact`() {
        val state = listAbove("<p>Next line</p>", typing = { typeThroughBridge(it) })

        state.typeThroughBridge("\n")
        state.typeThroughBridge("Typed")

        assertEquals(
            "<ul><li>Item</li></ul><p>Typed</p><p>Next line</p>",
            state.toHtml(),
        )
    }

    @Test
    fun `leaving a list at the end of the document still works`() {
        val state = listAbove("")

        state.type("\n")

        assertEquals(2, state.richParagraphList.size)
        assertIs<DefaultParagraph>(state.richParagraphList[1].type)

        state.type("Typed")

        assertEquals("<ul><li>Item</li></ul><p>Typed</p>", state.toHtml())
    }

    @Test
    fun `undo after leaving a list brings the empty item back`() {
        val state = listAbove("<p>Next line</p>")
        val beforeExit = state.toHtml()

        state.type("\n")
        state.history.undo()

        assertEquals(beforeExit, state.toHtml())
    }
}
