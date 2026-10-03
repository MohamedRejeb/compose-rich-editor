package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression test for the BTF2 selection propagation fix.
 *
 * Prior to the fix, user-gesture selections (mouse drag, shift+arrows) updated
 * [RichTextState.textFieldState] but not the legacy mirror field, causing `selection`
 * to return stale data. `toggleSpanStyle` would then operate on the wrong range.
 *
 * The fix makes `selection` read directly from [RichTextState.textFieldState], so it
 * always reflects the BTF2 canonical source. A snapshotFlow observer in
 * [BasicRichTextEditor] calls `handleSelectionChanged` after each gesture-driven change
 * so derived state (span style, paragraph style, trigger query) stays up to date.
 *
 * This test simulates the gesture path by calling `state.textFieldState.edit { selection = ... }`
 * directly, which is exactly what BTF2 does internally when the user drags or presses shift+arrow.
 */
@OptIn(ExperimentalTestApi::class)
class SelectionGestureToggleSpanStyleTest {

    @Test
    fun `toggleSpanStyle bolds gesture-selected text`() = runDesktopComposeUiTest {
        val state = RichTextState()

        setContent {
            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
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

        // Type "hello world" through the editor so the paragraph tree is populated.
        onNodeWithTag("editor").performTextInput("hello world")
        waitForIdle()

        // Simulate a user gesture selection of "hello" (indices 0..5) by editing
        // textFieldState directly. This is the same internal code path that BTF2
        // uses when the user drags or presses shift+arrows. It bypasses the
        // RichTextState.selection setter and updateTextFieldValue, replicating the
        // scenario that triggered the bug.
        state.textFieldState.edit {
            selection = TextRange(0, 5)
        }
        waitForIdle()

        // Toggle bold on the current selection. After the fix, this should apply to
        // the 5-character range set above. Before the fix, it would operate on a
        // stale (usually collapsed) selection and do nothing.
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        // Verify that the first 5 characters carry the bold span style.
        val annotated = state.annotatedString
        val boldSpan = annotated.spanStyles.firstOrNull { span ->
            span.item.fontWeight == FontWeight.Bold &&
                span.start <= 0 &&
                span.end >= 5
        }

        assertNotNull(
            boldSpan,
            "Expected a bold SpanStyle covering indices 0..5 but found none. " +
                "Spans found: ${annotated.spanStyles.map { "(${it.start}..${it.end} ${it.item.fontWeight})" }}",
        )

        // Also verify the text is still intact.
        assertTrue(
            annotated.text.startsWith("hello"),
            "Expected text to start with 'hello' but got: '${annotated.text}'",
        )
    }

    @Test
    fun `programmatic selection setter still applies bold correctly`() = runDesktopComposeUiTest {
        val state = RichTextState()

        setContent {
            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
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

        onNodeWithTag("editor").performTextInput("hello world")
        waitForIdle()

        // Use the programmatic setter (the path that existing tests use).
        // This must continue to work correctly after the fix.
        state.selection = TextRange(0, 5)
        waitForIdle()

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        val annotated = state.annotatedString
        val boldSpan = annotated.spanStyles.firstOrNull { span ->
            span.item.fontWeight == FontWeight.Bold &&
                span.start <= 0 &&
                span.end >= 5
        }

        assertNotNull(
            boldSpan,
            "Expected a bold SpanStyle covering indices 0..5 after programmatic selection. " +
                "Spans: ${annotated.spanStyles.map { "(${it.start}..${it.end} ${it.item.fontWeight})" }}",
        )
    }
}
