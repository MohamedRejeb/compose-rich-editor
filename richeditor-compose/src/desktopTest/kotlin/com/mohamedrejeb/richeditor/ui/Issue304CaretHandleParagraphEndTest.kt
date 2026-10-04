package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.holdCaretHandleOnParagraphEnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Issue 304: dragging the caret handle into the empty space after a paragraph's last line
 * made the caret jump to the start of the next paragraph.
 *
 * Cause: the hit test reports the next paragraph's start for that empty space. A tap there is
 * corrected from the press position and a selection handle by its own rule, but a caret
 * handle lives in a popup, delivers no pointer, and its one step move is the same shape a
 * keyboard cursor step produces, so it was left alone.
 * Fix: the text field fires a "handle moved" haptic after every handle step and never for a
 * keyboard step. On that signal a caret that left the last line of a paragraph for the next
 * paragraph's start is put back on the paragraph's end.
 */
@OptIn(ExperimentalTestApi::class)
class Issue304CaretHandleParagraphEndTest {

    /** "Hi" ends at 2, "Next line here" starts at 3. */
    private fun runEditor(
        text: String = "Hi\nNext line here",
        block: DesktopComposeUiTest.(RichTextState) -> Unit,
    ) = runDesktopComposeUiTest {
        val state = RichTextState().apply { setText(text) }
        setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth()) }
        waitForIdle()
        block(state)
    }

    /**
     * One caret handle step as the text field delivers it: the caret is written as a user
     * selection change, then the handle haptic fires. Returns whether the step was undone.
     */
    private fun DesktopComposeUiTest.caretHandleStep(state: RichTextState, to: TextRange): Boolean {
        state.selectionBeforeUserSelectionChange = state.textFieldState.selection
        state.writeSelection(to)
        val held = state.holdCaretHandleOnParagraphEnd()
        waitForIdle()
        return held
    }

    private fun RichTextState.writeSelection(newSelection: TextRange) {
        val previous = isApplyingProgrammaticSync
        isApplyingProgrammaticSync = true
        try {
            textFieldState.edit { selection = newSelection }
        } finally {
            isApplyingProgrammaticSync = previous
        }
    }

    private fun DesktopComposeUiTest.placeCaret(state: RichTextState, at: Int) {
        state.writeSelection(TextRange(at))
        waitForIdle()
    }

    @Test
    fun `a caret handle dragged past a paragraph end stays on the paragraph end`() = runEditor { state ->
        placeCaret(state, 1)

        assertFalse(caretHandleStep(state, TextRange(2)))
        assertTrue(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(2), state.selection)
    }

    @Test
    fun `the caret stays on the paragraph end while the handle keeps reporting the next paragraph`() = runEditor { state ->
        placeCaret(state, 2)

        repeat(3) { assertTrue(caretHandleStep(state, TextRange(3))) }

        assertEquals(TextRange(2), state.selection)
    }

    @Test
    fun `a fast drag that skips the paragraph end is held too`() = runEditor { state ->
        placeCaret(state, 1)

        assertTrue(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(2), state.selection)
    }

    @Test
    fun `a handle moved down along the left edge reaches the next paragraph start`() = runEditor { state ->
        placeCaret(state, 0)

        assertFalse(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a handle moved down from an empty paragraph reaches the next paragraph`() = runEditor(text = "\nNext") { state ->
        placeCaret(state, 0)

        assertFalse(caretHandleStep(state, TextRange(1)))

        assertEquals(TextRange(1), state.selection)
    }

    @Test
    fun `a handle moving inside the next paragraph reaches its start`() = runEditor { state ->
        placeCaret(state, 6)

        assertFalse(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a handle landing past the next paragraph start is accepted`() = runEditor { state ->
        placeCaret(state, 2)

        assertFalse(caretHandleStep(state, TextRange(5)))

        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun `a selection handle step is left to its own rule`() = runEditor { state ->
        state.writeSelection(TextRange(0, 2))
        waitForIdle()

        assertFalse(caretHandleStep(state, TextRange(0, 3)))
    }

    @Test
    fun `the next paragraph being a list item is held on the paragraph end as well`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setHtml("<p>Hi</p><ul><li>item</li></ul>") }
        setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth()) }
        waitForIdle()
        placeCaret(state, 2)

        assertTrue(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(2), state.selection)
    }

    @Test
    fun `a caret step without the handle signal moves to the next paragraph`() = runEditor { state ->
        placeCaret(state, 2)

        placeCaret(state, 3)

        assertEquals(TextRange(3), state.selection)
    }
}
