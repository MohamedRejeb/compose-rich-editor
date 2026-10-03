package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The buffer-change form of the #779 "trailing space refresh". A suggestion pick at a
 * paragraph end commits the picked word and then puts a space after it the way the keyboard
 * puts a space after any word: Gboard selects the next character and commits " " over it,
 * Samsung deletes it and commits " ". At a paragraph end that character is the paragraph
 * separator, so the buffer reports a delta that replaces the separator with a space. Replaying
 * it verbatim removes the separator and merges the next paragraph into the current one
 * ("Hi how This second paragraph..."). The IME meant "a space after my word", so the space
 * belongs inside the paragraph and the separator stays.
 *
 * A pick can also leave the separator alone and step over it instead: a token rewritten by a
 * shorter suggestion arrives as a minimal diff with the caret number untouched, so the caret
 * ends one past the paragraph's new end with no selection change to observe. That form is
 * folded into the same batch and gets its space materialized inside the paragraph.
 */
class Issue779SeparatorReplacedBySpaceTest {

    private companion object {
        /** Past the 300 ms IME follow-up window. */
        const val WINDOW_LAPSE_MS = 400L

        /** Past the typing coalescing window, so the pick is its own undo entry. */
        const val COALESCE_LAPSE_MS = 600L
    }

    /** "Hi" / "This second" / "End" with the caret at the end of "Hi" (index 2). */
    private fun doc(): RichTextState {
        val state = RichTextState()
        state.setHtml("<p>Hi</p><p>This second</p><p>End</p>")
        state.selection = TextRange(2)
        return state
    }

    private fun RichTextState.imeEdit(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        reconcileBufferWithModel(buffer)
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(buffer.asCharSequence().toString(), buffer.selection)
        handleSelectionChanged(textFieldState.selection, fromGestureObserver = true)
    }

    @Test
    fun `a pick that rewrites the word and commits a space over the separator in one batch keeps the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "h"); selection = TextRange(3) }
        state.imeEdit { replace(3, 3, "o"); selection = TextRange(4) }
        state.handleCompositionChanged(TextRange(0, 4))
        assertEquals("Hiho\nThis second\nEnd", state.toText())

        // Gboard's "Hi how" pick: the composing word is replaced, then the space refresh
        // selects the separator and commits " " over it, all inside one batch edit.
        state.imeEdit {
            replace(0, 4, "Hi how")
            replace(6, 7, " ")
            selection = TextRange(7)
        }

        assertEquals("Hi how \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(7), state.selection)
    }

    @Test
    fun `a space committed over the separator on its own keeps the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "s"); selection = TextRange(3) }
        state.handleCompositionChanged(TextRange(0, 3))

        // The refresh arrives as its own batch after the word commit.
        state.imeEdit {
            replace(3, 4, " ")
            selection = TextRange(4)
        }

        assertEquals("His \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun `a pick whose replacement swallows the separator keeps the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "h"); selection = TextRange(3) }
        state.imeEdit { replace(3, 3, "o"); selection = TextRange(4) }
        state.handleCompositionChanged(TextRange(0, 4))

        // The same pick with the two steps merged into one delta by the change tracker.
        state.imeEdit {
            replace(0, 5, "Hi how ")
            selection = TextRange(7)
        }

        assertEquals("Hi how \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(7), state.selection)
    }

    @Test
    fun `a delete pass followed by a space pass keeps the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "s"); selection = TextRange(3) }
        state.handleCompositionChanged(TextRange(0, 3))
        state.handleCompositionChanged(null)

        // Samsung's refresh as two InputConnection passes: deleteSurroundingText(0, 1), then
        // commitText(" ").
        state.imeEdit { replace(3, 4, ""); selection = TextRange(3) }
        assertEquals("His\nThis second\nEnd", state.toText())
        state.imeEdit { replace(3, 3, " "); selection = TextRange(4) }

        assertEquals("His \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun `a lone deletion of the separator long after typing still joins the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "s"); selection = TextRange(3) }
        Thread.sleep(WINDOW_LAPSE_MS)

        state.imeEdit { replace(3, 4, ""); selection = TextRange(3) }

        assertEquals("HisThis second\nEnd", state.toText())
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `two separate deltas in one batch keep their offsets with the separator kept`() {
        val state = RichTextState()
        state.setHtml("<p>ab cd</p><p>X</p>")
        state.selection = TextRange(5)

        // Buffer coordinates: after the first replace the separator sits at 6.
        state.imeEdit {
            replace(0, 2, "abc")
            replace(6, 7, " ")
            selection = TextRange(7)
        }

        assertEquals("abc cd \nX", state.toText())
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `a replacement ending in a space inside a paragraph is untouched`() {
        val state = doc()
        state.selection = TextRange(7)

        state.imeEdit { replace(3, 8, "That "); selection = TextRange(8) }

        assertEquals("Hi\nThat second\nEnd", state.toText())
    }

    @Test
    fun `a selection across the separator replaced by a letter still joins the paragraphs`() {
        val state = doc()
        state.selection = TextRange(1, 4)

        state.imeEdit { replace(1, 4, "x"); selection = TextRange(2) }

        assertEquals("Hxhis second\nEnd", state.toText())
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `the pick is one undo entry that restores the typed word and the paragraphs`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "h"); selection = TextRange(3) }
        state.imeEdit { replace(3, 3, "o"); selection = TextRange(4) }
        state.handleCompositionChanged(TextRange(0, 4))
        Thread.sleep(COALESCE_LAPSE_MS)
        state.imeEdit {
            replace(0, 4, "Hi how")
            replace(6, 7, " ")
            selection = TextRange(7)
        }

        assertTrue(state.history.undo())

        assertEquals("Hiho\nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun `a commit whose caret the IME placed past the separator gets its space inside the paragraph`() {
        val state = doc()
        for ((i, c) in "hithere".withIndex()) {
            state.imeEdit { replace(2 + i, 2 + i, c.toString()); selection = TextRange(3 + i) }
        }
        assertEquals("Hihithere\nThis second\nEnd", state.toText())

        // Gboard's "Hi there" pick on the emulator: the token is rewritten and the caret is
        // set one past it in the same batch, over the separator it takes for its space.
        state.imeEdit {
            replace(0, 9, "Hi there")
            selection = TextRange(9)
        }

        assertEquals("Hi there \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(9), state.selection)
    }

    @Test
    fun `a commit whose caret stays at its end gets no space`() {
        val state = doc()
        state.imeEdit { replace(2, 2, "s"); selection = TextRange(3) }

        state.imeEdit {
            replace(0, 3, "Has")
            selection = TextRange(3)
        }

        assertEquals("Has\nThis second\nEnd", state.toText())
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a space typed over a selected separator on a physical keyboard still joins the paragraphs`() {
        val state = doc()
        state.selection = TextRange(2, 3)
        state.notePhysicalKeyEvent()

        state.imeEdit { replace(2, 3, " "); selection = TextRange(3) }

        assertEquals("Hi This second\nEnd", state.toText())
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `Enter at a paragraph end adds no stray space`() {
        val state = doc()

        state.imeEdit { replace(2, 2, "\n"); selection = TextRange(3) }

        assertEquals("Hi\n\nThis second\nEnd", state.toText())
        assertEquals(4, state.richParagraphList.size)
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a caret step right after the in-batch refresh is plain navigation`() {
        val state = doc()
        for ((i, c) in "hithere".withIndex()) {
            state.imeEdit { replace(2 + i, 2 + i, c.toString()); selection = TextRange(3 + i) }
        }
        state.imeEdit {
            replace(0, 9, "Hi there")
            selection = TextRange(9)
        }
        assertEquals("Hi there \nThis second\nEnd", state.toText())

        state.setTextFieldStateFromValue(state.textFieldState.text.toString(), TextRange(10))
        state.handleSelectionChanged(TextRange(10), fromGestureObserver = true)

        assertEquals("Hi there \nThis second\nEnd", state.toText())
        assertEquals(TextRange(10), state.selection)
    }

    @Test
    fun `a shorter suggestion that arrives as a minimal diff with the caret untouched gets its space`() {
        val state = doc()
        for ((i, c) in "hithere".withIndex()) {
            state.imeEdit { replace(2 + i, 2 + i, c.toString()); selection = TextRange(3 + i) }
        }

        // Gboard on the emulator, "Hihithere" picked as "Hi there": the change tracker reports
        // the minimal diff ("hi" at 2..4 becomes " ") and the caret number stays 9, which is
        // now one past the separator.
        state.imeEdit {
            replace(2, 4, " ")
            selection = TextRange(9)
        }

        assertEquals("Hi there \nThis second\nEnd", state.toText())
        assertEquals(3, state.richParagraphList.size)
        assertEquals(TextRange(9), state.selection)
    }

    @Test
    fun `a composition ending after the in-batch refresh does not re-arm the step refresh`() {
        val state = doc()
        for ((i, c) in "hithere".withIndex()) {
            state.imeEdit { replace(2 + i, 2 + i, c.toString()); selection = TextRange(3 + i) }
        }
        state.handleCompositionChanged(TextRange(2, 9))
        state.imeEdit {
            replace(2, 4, " ")
            selection = TextRange(9)
        }
        // The composition observer reports the pick's composition ending after the batch.
        state.handleCompositionChanged(null)

        state.setTextFieldStateFromValue(state.textFieldState.text.toString(), TextRange(10))
        state.handleSelectionChanged(TextRange(10), fromGestureObserver = true)

        assertEquals("Hi there \nThis second\nEnd", state.toText())
        assertEquals(TextRange(10), state.selection)
    }
}
