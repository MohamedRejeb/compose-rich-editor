package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
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
 * keyboard step. On that signal a caret that landed on the next paragraph's start is put back
 * on the paragraph's end when the handle is in that empty space, judged from where the caret
 * came from. A tap there while the caret already rests on the next paragraph's start changes
 * nothing, so it is corrected when the tap is over.
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
    fun `a handle moving back along the next paragraph reaches its start`() = runEditor { state ->
        placeCaret(state, 6)

        for (caret in listOf(5, 4, 3)) assertFalse(caretHandleStep(state, TextRange(caret)))

        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a handle landing past the next paragraph start is accepted`() = runEditor { state ->
        placeCaret(state, 2)

        assertFalse(caretHandleStep(state, TextRange(5)))

        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun `a handle coming back up from the line below into the empty space is put on the paragraph end`() = runEditor { state ->
        // The end of "Next line here": the finger column is far right of where "Hi" ends.
        placeCaret(state, 17)

        assertTrue(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(2), state.selection)
    }

    /** "Hi" 0..2, "Next line here" 3..17, "End" 18..21: the last paragraph is the short one. */
    private val threeParagraphs = "Hi\nNext line here\nEnd"

    @Test
    fun `a handle coming back up from the end of a short paragraph is put on the end of the longer one above`() =
        runEditor(text = threeParagraphs) { state ->
            placeCaret(state, 16)

            assertFalse(caretHandleStep(state, TextRange(17)))
            assertFalse(caretHandleStep(state, TextRange(21)))
            assertTrue(caretHandleStep(state, TextRange(18)))

            assertEquals(TextRange(17), state.selection)
        }

    @Test
    fun `a fast drag back along a line reaches the paragraph start it was heading for`() =
        runEditor {
            // Each step skips several characters and the last one starts further right than
            // "Hi" ends, so only the direction of travel says this is not the empty space.
            placeCaret(it, 17)

            assertFalse(caretHandleStep(it, TextRange(13)))
            assertFalse(caretHandleStep(it, TextRange(8)))
            assertFalse(caretHandleStep(it, TextRange(3)))

            assertEquals(TextRange(3), it.selection)
        }

    @Test
    fun `a first step from inside a line to its paragraph start is accepted when the line above is longer`() =
        runEditor(text = threeParagraphs) { state ->
            placeCaret(state, 20)

            assertFalse(caretHandleStep(state, TextRange(18)))

            assertEquals(TextRange(18), state.selection)
        }

    @Test
    fun `a handle moving left along the next paragraph's first line reaches its start`() = runEditor { state ->
        placeCaret(state, 4)

        assertFalse(caretHandleStep(state, TextRange(3)))

        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `a handle moving left to a line start that follows an empty paragraph reaches it`() = runEditor(text = "\nNext") { state ->
        placeCaret(state, 2)

        assertFalse(caretHandleStep(state, TextRange(1)))

        assertEquals(TextRange(1), state.selection)
    }

    @Test
    fun `a handle coming down a wrapped paragraph into the empty space of its last line stays in it`() =
        runDesktopComposeUiTest(width = 200, height = 400) {
            val state = RichTextState().apply { setText("aaaa bbbb cccc dddd eeee ffff gggg hhhh ii\nNext") }
            setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth()) }
            waitForIdle()
            val layout = checkNotNull(state.textLayoutResult)
            val paragraphEnd = state.annotatedString.text.indexOf("Next") - 1
            val lastLine = layout.getLineForOffset(paragraphEnd)
            assertTrue(lastLine > 0, "the first paragraph must wrap")
            // The visible end of the line above, which reaches further right than the last line.
            val above = layout.getLineEnd(lastLine - 1, visibleEnd = true)
            assertTrue(layout.getCursorRect(above).left > layout.getCursorRect(paragraphEnd).left)
            placeCaret(state, above)

            assertTrue(caretHandleStep(state, TextRange(paragraphEnd + 1)))

            assertEquals(TextRange(paragraphEnd), state.selection)
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

    /** The middle of the empty space right of "Hi", and the left edge of the line below. */
    private fun RichTextState.emptySpaceAfterFirstParagraph(): Offset {
        val layout = checkNotNull(textLayoutResult)
        return Offset(x = layout.getLineRight(0) + 120f, y = (layout.getLineTop(0) + layout.getLineBottom(0)) / 2f)
    }

    private fun RichTextState.startOfSecondParagraph(): Offset {
        val layout = checkNotNull(textLayoutResult)
        return Offset(x = 1f, y = (layout.getLineTop(1) + layout.getLineBottom(1)) / 2f)
    }

    @Test
    fun `a tap in the empty space of a paragraph moves a caret resting on the next paragraph's start`() =
        tapBackIntoEmptySpace(touch = true)

    @Test
    fun `a click in the empty space of a paragraph moves a caret resting on the next paragraph's start`() =
        tapBackIntoEmptySpace(touch = false)

    private fun tapBackIntoEmptySpace(touch: Boolean) = runTaggedEditor { state ->
        tap(touch, state.emptySpaceAfterFirstParagraph())
        assertEquals(TextRange(2), state.selection, "first tap")

        tap(touch, state.startOfSecondParagraph())
        assertEquals(TextRange(3), state.selection, "tap on the next paragraph's start")

        tap(touch, state.emptySpaceAfterFirstParagraph())
        assertEquals(TextRange(2), state.selection, "second tap in the empty space")
    }

    @Test
    fun `a press that moves like a scroll leaves a caret on the next paragraph's start alone`() =
        runTaggedEditor { state ->
            tap(touch = true, state.startOfSecondParagraph())
            assertEquals(TextRange(3), state.selection)
            val start = state.emptySpaceAfterFirstParagraph()

            onNodeWithTag(EDITOR_TAG).performTouchInput {
                down(start)
                moveBy(Offset(0f, 60f))
                up()
            }
            waitForIdle()

            assertEquals(TextRange(3), state.selection)
        }

    private fun runTaggedEditor(block: DesktopComposeUiTest.(RichTextState) -> Unit) =
        runDesktopComposeUiTest(width = 480, height = 300) {
            val state = RichTextState().apply { setText("Hi\nNext line here") }
            setContent {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG))
            }
            waitForIdle()
            block(state)
        }

    private fun DesktopComposeUiTest.tap(touch: Boolean, position: Offset) {
        if (touch) onNodeWithTag(EDITOR_TAG).performTouchInput { click(position) }
        else onNodeWithTag(EDITOR_TAG).performMouseInput { click(position) }
        waitForIdle()
    }

    @Test
    fun `a caret step without the handle signal moves to the next paragraph`() = runEditor { state ->
        placeCaret(state, 2)

        placeCaret(state, 3)

        assertEquals(TextRange(3), state.selection)
    }

    private companion object {
        const val EDITOR_TAG = "editor"
    }
}
