package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Hardware keyboard selection on a touch platform.
 *
 * On Android and iOS every selection change counts as a gesture, because the handles live
 * in popups and the press interaction is cancelled at long-press start. A hardware keyboard
 * reaches the same observer: shift+Right across a paragraph start would be pulled back by
 * the paragraph-start rule, and shift+ctrl+Right from a paragraph end would be held by the
 * sticky paragraph-end rule. The editor sees every physical key press before the framework,
 * so a selection change shortly after one is the keyboard's and is accepted as is. Once that
 * window lapses the gesture clamps apply again, and a change while a pointer is pressed is
 * the pointer's whatever the keyboard did, so it is clamped as before.
 */
@OptIn(ExperimentalTestApi::class)
class HardwareKeyboardGestureClampTest {

    /** One selection tick as BTF2 delivers it on a touch platform, with no pointer. */
    private fun RichTextState.gestureTick(newSelection: TextRange) {
        val previous = isApplyingProgrammaticSync
        isApplyingProgrammaticSync = true
        try {
            textFieldState.edit { selection = newSelection }
        } finally {
            isApplyingProgrammaticSync = previous
        }
        handleSelectionChanged(newSelection, fromGestureObserver = true)
    }

    /** "Line 4 Line 5 tail": paragraph 2 starts at 7, its first word ends at 11. */
    private fun touchState(): RichTextState {
        val state = RichTextState()
        state.setText("Line 4\nLine 5 tail")
        state.treatSelectionChangesAsGesture = true
        return state
    }

    @Test
    fun `shift plus Right from a paragraph end lands on the next paragraph start`() {
        val state = touchState()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        state.gestureTick(TextRange(0, 7))

        assertEquals(TextRange(0, 7), state.selection)
    }

    @Test
    fun `shift plus ctrl plus Right from a paragraph end selects the next paragraph's first word`() {
        val state = touchState()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        state.gestureTick(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `a tick onto the paragraph start after the key window lapsed is pulled back`() {
        val state = touchState()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        Thread.sleep(KEY_WINDOW_LAPSE_MS)
        state.gestureTick(TextRange(0, 7))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `a tick into the first word after the key window lapsed is held on the paragraph end`() {
        val state = touchState()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        Thread.sleep(KEY_WINDOW_LAPSE_MS)
        state.gestureTick(TextRange(0, 11))

        assertEquals(TextRange(0, 6), state.selection)
    }

    /**
     * Desktop inside the one second gesture grace after a click: the gesture counts as live,
     * but the extension is the keyboard's.
     */
    @Test
    fun `a keyboard extension on desktop inside the gesture grace lands on the next paragraph start`() {
        val state = RichTextState()
        state.setText("Line 4\nLine 5 tail")
        state.treatSelectionChangesAsGesture = false
        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        state.gestureTick(TextRange(0, 7))

        assertEquals(TextRange(0, 7), state.selection)
    }

    /**
     * A held Shift auto-repeats its key down on some platforms, so a shift+drag with the mouse
     * keeps the key window open. The pointer is pressed, so the change is the pointer's.
     */
    @Test
    fun `a tick onto the paragraph start while the pointer is pressed is pulled back inside the key window`() {
        val state = RichTextState()
        state.setText("Line 4\nLine 5 tail")
        state.treatSelectionChangesAsGesture = false
        state.onSelectionGestureStart()
        state.gestureTick(TextRange(0, 6))

        state.notePhysicalKeyEvent()
        state.gestureTick(TextRange(0, 7))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `shift plus Right through the editor lands on the next paragraph start`() =
        runTouchEditor(readOnly = false) { state ->
            onNodeWithTag(EDITOR_TAG).performKeyInput {
                keyDown(Key.ShiftLeft)
                pressKey(Key.DirectionRight)
                keyUp(Key.ShiftLeft)
            }
            waitForIdle()

            assertEquals(TextRange(6, 7), state.selection)
        }

    @Test
    fun `shift plus Right through a read-only editor lands on the next paragraph start`() =
        runTouchEditor(readOnly = true) { state ->
            onNodeWithTag(EDITOR_TAG).performKeyInput {
                keyDown(Key.ShiftLeft)
                pressKey(Key.DirectionRight)
                keyUp(Key.ShiftLeft)
            }
            waitForIdle()

            assertEquals(TextRange(6, 7), state.selection)
        }

    /** A focused editor in touch mode with the caret on the end of "Line 4". */
    private fun runTouchEditor(
        readOnly: Boolean,
        block: androidx.compose.ui.test.DesktopComposeUiTest.(state: RichTextState) -> Unit,
    ) = runDesktopComposeUiTest(width = 480, height = 360) {
        val state = RichTextState()
        state.setText("Line 4\nLine 5 tail")
        setContent {
            BasicRichTextEditor(
                state = state,
                readOnly = readOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(EDITOR_TAG),
            )
        }
        waitForIdle()
        val rect = checkNotNull(state.textLayoutResult).getCursorRect(6)
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(rect.left, rect.center.y)) }
        waitForIdle()
        assertEquals(TextRange(6), state.selection)
        state.treatSelectionChangesAsGesture = true
        block(state)
    }

    private companion object {
        const val EDITOR_TAG = "editor"

        /** Past the 300 ms physical key window. */
        const val KEY_WINDOW_LAPSE_MS = 400L
    }
}
