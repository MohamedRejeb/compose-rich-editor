package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Issue 584: the selection is cleared when the editor loses focus, so a focusable toolbar
 * button cannot style it.
 *
 * Cause: the Compose text field collapses its selection to the end when it loses focus
 * (CMP-2569). Fix: with `RichTextConfig.preserveSelectionOnFocusLoss` the editor puts the
 * range back as soon as it sees the focus loss. The default keeps the Compose behavior.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue584SelectionOnFocusLossTest {

    @Test
    fun `by default the selection collapses when focus moves away`() = runDesktopComposeUiTest {
        val state = editorState(preserve = false)
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, otherFocus = otherFocus) }
        focusEditorAndSelect(state)

        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(TextRange(SELECTED.max), state.selection)
    }

    @Test
    fun `the selection survives focus moving to another focusable`() = runDesktopComposeUiTest {
        val state = editorState()
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, otherFocus = otherFocus) }
        focusEditorAndSelect(state)

        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(SELECTED, state.selection)
    }

    @Test
    fun `a focusable toolbar button styles the selected range`() = runDesktopComposeUiTest {
        val state = editorState()
        setContent {
            Column {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG))
                Box(
                    Modifier
                        .size(40.dp)
                        .testTag(BUTTON_TAG)
                        .clickable { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
                )
            }
        }
        focusEditorAndSelect(state)

        onNodeWithTag(BUTTON_TAG).performMouseInput { click() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(SELECTED, state.selection)
        assertTrue("<b>Hello</b>" in state.toHtml(), state.toHtml())
    }

    @Test
    fun `the selection survives focus moving to another text field`() = runDesktopComposeUiTest {
        val state = editorState()
        setContent {
            Column {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG))
                var url by remember { mutableStateOf("") }
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth().testTag(FIELD_TAG),
                )
            }
        }
        focusEditorAndSelect(state)

        onNodeWithTag(FIELD_TAG).performMouseInput { click() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(SELECTED, state.selection)
    }

    @Test
    fun `the selection is still there when focus returns to the editor`() = runDesktopComposeUiTest {
        val state = editorState()
        val editorFocus = FocusRequester()
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, editorFocus = editorFocus, otherFocus = otherFocus) }
        focusEditorAndSelect(state)

        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()
        runOnIdle { editorFocus.requestFocus() }
        waitForIdle()

        assertTrue(state.isFocused)
        assertEquals(SELECTED, state.selection)
    }

    @Test
    fun `a read-only editor keeps its selection too`() = runDesktopComposeUiTest {
        val state = editorState()
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, otherFocus = otherFocus, readOnly = true) }
        focusEditorAndSelect(state)

        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(SELECTED, state.selection)
    }

    @Test
    fun `a selection the user collapsed stays collapsed after focus loss`() = runDesktopComposeUiTest {
        val state = editorState()
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, otherFocus = otherFocus) }
        focusEditorAndSelect(state)

        // Right arrow collapses to the selection end, the same shape as the focus loss collapse.
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals(TextRange(SELECTED.max), state.selection)
        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()

        assertFalse(state.isFocused)
        assertEquals(TextRange(SELECTED.max), state.selection)
    }

    @Test
    fun `clicking back into the editor places the caret`() = runDesktopComposeUiTest {
        val state = editorState()
        val otherFocus = FocusRequester()
        setContent { EditorWithFocusable(state, otherFocus = otherFocus) }
        focusEditorAndSelect(state)
        runOnIdle { otherFocus.requestFocus() }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(1f, 5f)) }
        waitForIdle()

        assertTrue(state.isFocused)
        assertEquals(TextRange(0), state.selection)
    }

    @Composable
    private fun EditorWithFocusable(
        state: RichTextState,
        otherFocus: FocusRequester,
        editorFocus: FocusRequester = remember { FocusRequester() },
        readOnly: Boolean = false,
    ) {
        Column {
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.fillMaxWidth().focusRequester(editorFocus).testTag(EDITOR_TAG),
                readOnly = readOnly,
            )
            Box(Modifier.size(40.dp).focusRequester(otherFocus).focusable())
        }
    }

    private fun editorState(preserve: Boolean = true): RichTextState =
        RichTextState().apply {
            config.preserveSelectionOnFocusLoss = preserve
            setText("Hello world")
        }

    private fun DesktopComposeUiTest.focusEditorAndSelect(state: RichTextState) {
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()
        assertTrue(state.isFocused)
        state.textFieldState.edit { selection = SELECTED }
        waitForIdle()
        assertEquals(SELECTED, state.selection)
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        const val BUTTON_TAG = "button"
        const val FIELD_TAG = "field"
        val SELECTED = TextRange(0, 5)
    }
}
