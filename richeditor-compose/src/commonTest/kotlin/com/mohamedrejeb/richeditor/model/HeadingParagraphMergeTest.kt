package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Text merged into a heading paragraph must take the heading's visuals.
 *
 * A heading keeps its bold and its font size on the spans themselves ([RichParagraph.applyHeadingStyle]
 * writes them there), not as something resolved at render time from the paragraph's level. Merging
 * moved the spans of the paragraph below straight into the heading without that step, so
 * backspacing a plain paragraph into a heading left the heading half styled on screen: `title`
 * large and bold, `body` normal, inside one `<h1>`.
 *
 * `toHtml` writes the whole merged paragraph as a heading either way, so reloading the document
 * rendered it differently from what the user had just been looking at. Found by
 * `EditPipelineImeBatchFuzzTest`, whose derived state oracle compares the live editor against a
 * fresh decode of its own document.
 */
class HeadingParagraphMergeTest {

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

    /** Every character index the state renders bold. */
    private fun boldIndices(state: RichTextState): Set<Int> =
        state.annotatedString.spanStyles
            .filter { it.item.fontWeight == FontWeight.Bold }
            .flatMap { (it.start until it.end).toList() }
            .toSet()

    private fun mergedHeading(): RichTextState {
        val state = RichTextState()
        state.setHtml("<h1>title</h1><p>body</p>")
        state.selection = TextRange(6)
        // Backspace on the paragraph separator: the plain paragraph merges into the heading.
        state.imeBatch {
            replace(5, 6, "")
            selection = TextRange(5)
        }
        return state
    }

    @Test
    fun `merging a paragraph into a heading renders the whole heading`() {
        val state = mergedHeading()

        assertEquals(1, state.richParagraphList.size)
        assertEquals("titlebody", state.toText())
        assertEquals(
            (0 until 9).toSet(),
            boldIndices(state),
            "the merged text should render with the heading's weight",
        )
    }

    @Test
    fun `merging a paragraph into a heading survives a round trip unchanged`() {
        val state = mergedHeading()

        val fresh = RichTextState()
        fresh.setHtml(state.toHtml())

        assertEquals("<h1>titlebody</h1>", state.toHtml())
        assertEquals(state.annotatedString.text, fresh.annotatedString.text)
        assertEquals(
            boldIndices(fresh),
            boldIndices(state),
            "reloading the document must not change how the heading renders",
        )
    }

    @Test
    fun `the merged text reports the heading style to the toolbar`() {
        val state = mergedHeading()

        state.selection = TextRange(9)
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the caret in the merged tail is inside a heading",
        )
        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)
    }

    @Test
    fun `merging into a plain paragraph adds no heading visuals`() {
        val state = RichTextState()
        state.setHtml("<p>title</p><p>body</p>")
        state.selection = TextRange(6)
        state.imeBatch {
            replace(5, 6, "")
            selection = TextRange(5)
        }

        assertEquals("titlebody", state.toText())
        assertEquals(emptySet(), boldIndices(state))
        assertEquals(SpanStyle(), state.getSpanStyle(TextRange(0, 9)))
    }
}
