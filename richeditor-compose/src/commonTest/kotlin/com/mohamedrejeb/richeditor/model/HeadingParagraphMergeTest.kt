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

    // Typing over the paragraph separator: the merge and the insert happen in one edit, and
    // the style the inserted text inherits must be the heading's, not the plain paragraph's
    // that the selection started in front of.

    @Test
    fun `text replacing the separator into a heading takes the heading visuals`() {
        val state = RichTextState()
        state.setHtml("<h1>ab</h1><p>cd</p>")
        state.selection = TextRange(3)
        state.imeBatch {
            replace(2, 3, "X")
            selection = TextRange(3)
        }

        assertEquals("abXcd", state.toText())
        assertEquals((0 until 5).toSet(), boldIndices(state))
        assertEquals("<h1>abXcd</h1>", state.toHtml())

        val fresh = RichTextState()
        fresh.setHtml(state.toHtml())
        assertEquals(boldIndices(fresh), boldIndices(state))
    }

    @Test
    fun `an emptied heading keeps its visuals through enter and a replace across the separator`() {
        val state = RichTextState()
        state.setHtml("<h1>title</h1>")
        for (index in 4 downTo 0) {
            state.selection = TextRange(index + 1)
            state.imeBatch {
                replace(index, index + 1, "")
                selection = TextRange(index)
            }
        }
        assertEquals("", state.toText())
        assertParityWithFreshDecode(state, "the emptied heading")

        state.imeBatch {
            replace(0, 0, "\n")
            selection = TextRange(1)
        }
        assertParityWithFreshDecode(state, "enter in the empty heading")

        state.imeBatch {
            replace(0, 1, "xb")
            selection = TextRange(2)
        }
        assertEquals("xb", state.toText())
        assertEquals(listOf(HeadingStyle.H1), state.richParagraphList.map { it.headingStyle })
        assertParityWithFreshDecode(state, "the replace across the separator")
    }

    private fun assertParityWithFreshDecode(state: RichTextState, label: String) {
        val fresh = RichTextState()
        fresh.setHtml(state.toHtml())
        assertEquals(state.toText(), fresh.toText(), "$label: text")
        assertEquals(
            state.richParagraphList.map { it.headingStyle },
            fresh.richParagraphList.map { it.headingStyle },
            "$label: heading levels",
        )

        val selection = state.selection
        fresh.selection = TextRange(0)
        fresh.selection = selection
        assertEquals(
            fresh.currentSpanStyle,
            state.currentSpanStyle,
            "$label: currentSpanStyle at $selection",
        )
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
