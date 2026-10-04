package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Issue 715: on web, a paste into a text field inside a dialog was taken by the editor behind
 * the dialog.
 *
 * Cause: a dialog is a separate layer with its own focus, so the editor underneath stays
 * focused, and the web clipboard handlers listen on the document and only checked that focus.
 * Fix: Compose routes key events to the top layer only, so the editor records the clipboard
 * shortcuts it receives and the web handlers ignore a keyboard clipboard event whose shortcut
 * the editor never saw. This pins the routing that fix relies on.
 */
@OptIn(ExperimentalTestApi::class)
class Issue715ClipboardShortcutRoutingTest {

    @Test
    fun `the editor records a clipboard shortcut pressed in it`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setText("Hello") }
        setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG)) }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.V)
            keyUp(Key.CtrlLeft)
        }
        waitForIdle()

        assertTrue(state.sawClipboardShortcutKey)
    }

    @Test
    fun `typing is not a clipboard shortcut`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setText("Hello") }
        setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG)) }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.V) }
        waitForIdle()

        assertFalse(state.sawClipboardShortcutKey)
    }

    @Test
    fun `a clipboard shortcut pressed in a dialog does not reach the editor behind it`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setText("Hello") }
        var showDialog by mutableStateOf(false)
        setContent {
            Column {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG))
                if (showDialog) {
                    Dialog(onDismissRequest = { showDialog = false }) {
                        var url by remember { mutableStateOf("") }
                        BasicTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth().testTag(DIALOG_FIELD_TAG),
                        )
                    }
                }
            }
        }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()
        showDialog = true
        waitForIdle()
        onNodeWithTag(DIALOG_FIELD_TAG).performMouseInput { click() }
        waitForIdle()

        onNodeWithTag(DIALOG_FIELD_TAG).performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.V)
            keyUp(Key.CtrlLeft)
        }
        waitForIdle()

        assertTrue(state.isFocused, "the editor behind a dialog keeps its focus, which is why focus alone cannot decide")
        assertFalse(state.sawClipboardShortcutKey)
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        const val DIALOG_FIELD_TAG = "dialogField"
    }
}
