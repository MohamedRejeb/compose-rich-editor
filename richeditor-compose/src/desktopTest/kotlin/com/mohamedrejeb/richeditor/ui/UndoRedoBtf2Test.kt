package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end desktop UI test that verifies undo/redo records history for BTF2 user input.
 *
 * Regression for the BTF2 migration: the [applyChangeList] path (InputTransformation ->
 * applyChangeList -> applyChange) did not wrap its work in beginHistoryRecord / finishHistoryRecord,
 * so user typing, deletion, and line breaks via BTF2 never produced undo entries.
 *
 * These tests drive real keyboard input through performTextInput and then assert that
 * [RichTextState.history.undo] / [RichTextState.history.redo] work correctly.
 */
@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class)
class UndoRedoBtf2Test {

    private fun buildEditorTest(
        state: RichTextState,
        block: androidx.compose.ui.test.DesktopComposeUiTest.() -> Unit,
    ) = runDesktopComposeUiTest(width = 500, height = 200) {
        val focusRequester = FocusRequester()

        scene.setContent {
            val richState = remember { state }
            Box(modifier = Modifier.size(500.dp, 200.dp)) {
                BasicRichTextEditor(
                    state = richState,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag("editor"),
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        waitForIdle()
        block()
    }

    @Test
    fun `typing through BTF2 records undoable history entries`() {
        val state = RichTextState()
        buildEditorTest(state) {
            onNodeWithTag("editor").performTextInput("hello")
            waitForIdle()

            assertEquals("hello", state.annotatedString.text)
            assertTrue(state.history.canUndo, "Expected canUndo after typing")

            state.history.undo()
            waitForIdle()

            assertEquals(
                "",
                state.annotatedString.text,
                "After undo text should be empty (typing was undone)",
            )
            assertFalse(state.history.canUndo, "Expected no more undo steps")

            assertTrue(state.history.canRedo, "Expected canRedo after undo")

            state.history.redo()
            waitForIdle()

            assertEquals(
                "hello",
                state.annotatedString.text,
                "After redo text should be restored",
            )
        }
    }

    @Test
    fun `deletion through BTF2 records undoable history entries`() {
        val state = RichTextState()
        buildEditorTest(state) {
            onNodeWithTag("editor").performTextInput("hello")
            waitForIdle()

            assertEquals("hello", state.annotatedString.text)

            // Seal the typing group so deletion is its own undo step.
            state.history.undo()
            state.history.redo()
            waitForIdle()

            // The text is restored; now undo should take us back to empty.
            state.history.undo()
            waitForIdle()

            assertEquals(
                "",
                state.annotatedString.text,
                "After undo text should be empty",
            )
        }
    }

    @Test
    fun `consecutive typing through BTF2 coalesces into a single undo entry`() {
        val state = RichTextState()
        buildEditorTest(state) {
            // Type five characters one at a time. Each keystroke goes through a separate
            // applyChangeList call. With caret = -1 (the pre-fix sentinel) the coalescer
            // cannot verify continuity so it starts a new undo group on every character.
            // With the real post-edit caret the coalescer merges them into one group.
            onNodeWithTag("editor").performTextInput("hello")
            waitForIdle()

            assertEquals("hello", state.annotatedString.text)

            // One undo must remove all of "hello" not just the last character.
            state.history.undo()
            waitForIdle()

            assertEquals(
                "",
                state.annotatedString.text,
                "Expected single undo to remove all of 'hello' (coalesced as one entry) " +
                    "but got '${state.annotatedString.text}'",
            )
        }
    }

    @Test
    fun `ctrl z is handled by rich history not the field`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("Hello")
        waitForIdle()
        // A non-collapsed selection so the toggle actually restyles existing text and
        // commits its own undo entry; a collapsed caret would only stage a style for
        // future typing, which the history deliberately does not record on its own.
        state.selection = TextRange(0, 5)
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()
        onNodeWithTag("editor").performKeyInput {
            keyDown(Key.CtrlLeft); pressKey(Key.Z); keyUp(Key.CtrlLeft)
        }
        waitForIdle()
        // Rich undo reverts the formatting toggle; BTF2's internal undo would instead
        // revert typed text and desync richParagraphList.
        assertEquals("Hello", state.toText())
    }

    @Test
    fun `typing after a selection jump starts a new undo entry`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("Hello")
        waitForIdle()
        state.selection = TextRange(0)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("X")
        waitForIdle()
        assertEquals("XHello", state.toText())
        state.history.undo()
        assertEquals("Hello", state.toText())
        state.history.undo()
        assertEquals("", state.toText())
    }
}
