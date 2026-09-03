package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.isUnspecified
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Enter inside a heading splits it the way Docs, Word and Notion do.
 *
 * A heading keeps its bold and its font size on the spans themselves, so splitting a paragraph
 * used to hand the tail those visuals (the new span is built from `fullSpanStyle`) without the
 * heading level that explains them: the new paragraph rendered like a heading, reported
 * `HeadingStyle.Normal` to the toolbar, and serialized as `<p><span style="font-size: 2.0em"><b>`.
 *
 * The behaviour pinned here:
 *  - inside the text, both halves are headings of the same level
 *  - at the end, the new paragraph is plain and typing in it is normal sized
 *  - at the start, the empty paragraph pushed above is plain and the heading keeps its content
 */
class HeadingParagraphSplitTest {

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

    /** An ordinary Enter at [at], driven through the real input pipeline. */
    private fun splitHeading(at: Int): RichTextState {
        val state = RichTextState()
        state.setHtml("<h1>title</h1>")
        state.selection = TextRange(at)
        state.imeBatch {
            replace(at, at, "\n")
            selection = TextRange(at + 1)
        }
        return state
    }

    private fun headingAt(state: RichTextState, offset: Int): HeadingStyle {
        state.selection = TextRange(offset)
        return state.currentHeadingStyle
    }

    private fun headingLevels(state: RichTextState): List<HeadingStyle> =
        state.richParagraphList.map { it.headingStyle }

    private fun reloaded(state: RichTextState): RichTextState {
        val fresh = RichTextState()
        fresh.setHtml(state.toHtml())
        return fresh
    }

    // Enter inside the heading text

    @Test
    fun `enter inside a heading leaves two headings of the same level`() {
        val state = splitHeading(3)

        assertEquals("tit\nle", state.toText())
        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.H1), headingLevels(state))
        assertEquals(HeadingStyle.H1, headingAt(state, 1), "the first half")
        assertEquals(HeadingStyle.H1, headingAt(state, 5), "the second half")
    }

    @Test
    fun `enter inside a heading serializes as two headings`() {
        val state = splitHeading(3)

        assertEquals("<h1>tit</h1><h1>le</h1>", state.toHtml())
    }

    @Test
    fun `enter inside a heading agrees with a reload`() {
        val state = splitHeading(3)
        val fresh = reloaded(state)

        assertEquals(state.toText(), fresh.toText())
        assertEquals(headingLevels(state), headingLevels(fresh))
        assertEquals(state.toHtml(), fresh.toHtml())
        assertEquals(
            headingAt(state, 5).let { state.currentSpanStyle },
            headingAt(fresh, 5).let { fresh.currentSpanStyle },
            "the second half must render the same after a reload",
        )
    }

    // Enter at the end of the heading

    @Test
    fun `enter at the end of a heading starts a plain paragraph`() {
        val state = splitHeading(5)

        assertEquals("title\n", state.toText())
        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.Normal), headingLevels(state))
        assertEquals(HeadingStyle.H1, headingAt(state, 1), "the heading keeps its level")
        assertEquals(HeadingStyle.Normal, headingAt(state, 6), "the new paragraph is plain")
    }

    @Test
    fun `enter at the end of a heading leaves no heading visuals behind`() {
        val state = splitHeading(5)

        state.selection = TextRange(6)
        assertNull(state.currentSpanStyle.fontWeight, "the new paragraph must not be bold")
        assertTrue(
            state.currentSpanStyle.fontSize.isUnspecified,
            "the new paragraph must not keep the heading's font size, was " +
                state.currentSpanStyle.fontSize,
        )
    }

    @Test
    fun `enter at the end of a heading serializes as a heading and an empty paragraph`() {
        val state = splitHeading(5)

        // A trailing empty paragraph is written as `<br>`, which reads back as one paragraph too
        // many (a known round trip defect, unrelated to headings), so the reload is compared on
        // the two paragraphs the split produced.
        assertEquals("<h1>title</h1><br>", state.toHtml())

        val fresh = reloaded(state)
        assertEquals(
            listOf(HeadingStyle.H1, HeadingStyle.Normal),
            headingLevels(fresh).take(2),
        )
        assertTrue(
            fresh.richParagraphList.size > state.richParagraphList.size,
            "the trailing blank line defect is expected here; drop this waiver once it is fixed",
        )
    }

    @Test
    fun `typing after a split at the end of a heading is normal sized`() {
        val state = splitHeading(5)
        state.imeBatch {
            replace(6, 6, "x")
            selection = TextRange(7)
        }

        assertEquals("title\nx", state.toText())
        assertEquals("<h1>title</h1><p>x</p>", state.toHtml())
        assertEquals(HeadingStyle.Normal, headingAt(state, 7))
        assertNull(state.currentSpanStyle.fontWeight)
        assertTrue(state.currentSpanStyle.fontSize.isUnspecified)

        val fresh = reloaded(state)
        assertEquals(state.toText(), fresh.toText())
        assertEquals(headingLevels(state), headingLevels(fresh))
        assertEquals(state.toHtml(), fresh.toHtml())
    }

    // Enter at the start of the heading

    @Test
    fun `enter at the start of a heading pushes a plain paragraph above it`() {
        val state = splitHeading(0)

        assertEquals("\ntitle", state.toText())
        assertEquals(listOf(HeadingStyle.Normal, HeadingStyle.H1), headingLevels(state))
        assertEquals(HeadingStyle.H1, headingAt(state, 2), "the heading keeps its level")
    }

    @Test
    fun `enter at the start of a heading serializes with an empty line above`() {
        val state = splitHeading(0)

        assertEquals("<br><h1>title</h1>", state.toHtml())

        val fresh = reloaded(state)
        assertEquals(state.toText(), fresh.toText())
        assertEquals(headingLevels(state), headingLevels(fresh))
        assertEquals(state.toHtml(), fresh.toHtml())
    }

    // undo

    @Test
    fun `undo restores the heading split inside the text`() {
        val state = splitHeading(3)
        state.history.undo()

        assertEquals("title", state.toText())
        assertEquals("<h1>title</h1>", state.toHtml())
        assertEquals(listOf(HeadingStyle.H1), headingLevels(state))
    }

    @Test
    fun `undo restores the heading split at the end`() {
        val state = splitHeading(5)
        state.history.undo()

        assertEquals("title", state.toText())
        assertEquals("<h1>title</h1>", state.toHtml())
        assertEquals(listOf(HeadingStyle.H1), headingLevels(state))
    }

    @Test
    fun `undo restores the heading split at the start`() {
        val state = splitHeading(0)
        state.history.undo()

        assertEquals("title", state.toText())
        assertEquals("<h1>title</h1>", state.toHtml())
        assertEquals(listOf(HeadingStyle.H1), headingLevels(state))
    }
}
