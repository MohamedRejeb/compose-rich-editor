package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A read-only editor is the read-only BasicTextField: it cannot be edited, but it can be
 * focused, text can be selected in it by pointer and keyboard, and the selection can be
 * copied. Nothing here must turn it back into static text.
 */
@OptIn(ExperimentalTestApi::class)
class ReadOnlyEditorTest {

    @Test
    fun `a click focuses a read-only editor`() = runReadOnlyEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(caretAt(state, 2)) }
        waitForIdle()

        assertTrue(state.isFocused, "a read-only editor must take focus on click")
    }

    @Test
    fun `a mouse drag selects text in a read-only editor`() = runReadOnlyEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput {
            moveTo(caretAt(state, 1))
            press()
        }
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performMouseInput { moveTo(caretAt(state, 5)) }
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performMouseInput { release() }
        waitForIdle()

        assertEquals(TextRange(1, 5), state.selection)
    }

    @Test
    fun `a keyboard extension selects text in a read-only editor`() = runReadOnlyEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(caretAt(state, 0)) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(Key.ShiftLeft)
            repeat(3) { pressKey(Key.DirectionRight) }
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()

        assertEquals(TextRange(0, 3), state.selection)
    }

    @Test
    fun `editing keys change nothing in a read-only editor`() = runReadOnlyEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(caretAt(state, 2)) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.ShiftLeft)
            pressKey(Key.Backspace)
            pressKey(Key.Delete)
            pressKey(Key.Enter)
        }
        waitForIdle()

        assertEquals(TEXT, state.annotatedString.text)
    }

    @Test
    fun `undo and redo shortcuts change nothing in a read-only editor`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            state.setText(TEXT)
            var readOnly by mutableStateOf(false)
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

            // An edit while editable, so there is an entry to undo.
            onNodeWithTag(EDITOR_TAG).performMouseInput { click(caretAt(state, TEXT.length)) }
            waitForIdle()
            onNodeWithTag(EDITOR_TAG).performTextInput("!")
            waitForIdle()
            assertEquals("$TEXT!", state.annotatedString.text)

            readOnly = true
            waitForIdle()
            pressShortcut(Key.Z, shift = false)
            assertEquals("$TEXT!", state.annotatedString.text, "undo must not run in a read-only editor")
            pressShortcut(Key.Z, shift = true)
            assertEquals("$TEXT!", state.annotatedString.text, "redo must not run in a read-only editor")

            // Back to editable: the entry was there all along.
            readOnly = false
            waitForIdle()
            pressShortcut(Key.Z, shift = false)
            assertEquals(TEXT, state.annotatedString.text, "the same shortcut undoes once editable again")
        }

    private fun DesktopComposeUiTest.pressShortcut(key: Key, shift: Boolean) {
        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(SHORTCUT_MODIFIER)
            if (shift) keyDown(Key.ShiftLeft)
            pressKey(key)
            if (shift) keyUp(Key.ShiftLeft)
            keyUp(SHORTCUT_MODIFIER)
        }
        waitForIdle()
    }

    private fun runReadOnlyEditor(block: DesktopComposeUiTest.(state: RichTextState) -> Unit) =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            state.setText(TEXT)
            setContent {
                BasicRichTextEditor(
                    state = state,
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EDITOR_TAG),
                )
            }
            waitForIdle()
            block(state)
        }

    private fun caretAt(state: RichTextState, offset: Int): Offset {
        val rect = checkNotNull(state.textLayoutResult).getCursorRect(offset)
        return Offset(rect.left, rect.center.y)
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        const val TEXT = "Hello World"
        val SHORTCUT_MODIFIER: Key =
            if (System.getProperty("os.name") == "Mac OS X") Key.MetaLeft else Key.CtrlLeft
    }
}
