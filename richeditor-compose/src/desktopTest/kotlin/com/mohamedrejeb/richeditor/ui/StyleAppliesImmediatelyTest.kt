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
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression test for the style-not-rendering-until-selection-moves bug.
 *
 * Root cause: BTF2 only re-runs OutputTransformation (and thus refreshes the visible
 * styled text) when the text field state changes. Style-only operations like
 * [RichTextState.toggleSpanStyle] mutate the paragraph tree but leave the buffer text
 * and selection unchanged, and `setTextFieldStateFromValue` early-returns when text and
 * selection are already in sync, so BTF2's layout cache was never invalidated and the
 * styled output kept displaying the pre-toggle appearance until the next selection change.
 *
 * The fix invalidates the OutputTransformation after every `updateAnnotatedString`
 * rebuild that is a style-only change, forcing BTF2 to re-invoke it and re-render the
 * new styles without waiting for a selection change.
 */
@OptIn(ExperimentalTestApi::class)
class StyleAppliesImmediatelyTest {

    /**
     * Assert that toggling bold on a gesture-selected range is reflected in the state's
     * text layout result immediately, without requiring any further user action (e.g.
     * moving the caret).
     *
     * The model state (`annotatedString`) is always correct after the toggle; the
     * observable that reveals the rendering bug is the text layout result, which is the
     * output of BTF2's layout pass fed by OutputTransformation. If OT was not re-run,
     * its layout input will not carry the bold span.
     */
    @Test
    fun `bold applied to gesture-selected range renders without further user action`() =
        runDesktopComposeUiTest {
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
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
            waitForIdle()

            // Type initial content so the paragraph tree is populated.
            onNodeWithTag("editor").performTextInput("hello world")
            waitForIdle()

            // Simulate a user gesture selection of "hello" (indices 0..5) by editing
            // textFieldState directly. This is exactly what BTF2 does when the user
            // drags or presses shift+arrows; it bypasses the RichTextState.selection setter.
            state.textFieldState.edit { selection = TextRange(0, 5) }
            waitForIdle()

            // Apply bold via a toolbar action (no secondary user interaction).
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            waitForIdle()

            // The model state is always correct; verify first as a sanity check.
            val boldInAnnotatedString = state.annotatedString.spanStyles.any { range ->
                range.item.fontWeight == FontWeight.Bold &&
                    range.start <= 0 &&
                    range.end > 0
            }
            assertTrue(boldInAnnotatedString, "Expected bold span in annotatedString covering position 0")

            // The critical assertion: BTF2's layout pass must have consumed the new styles.
            // textLayoutResult is set by onTextLayout which fires only when BTF2 re-lays
            // out the text. If OT was not re-run, its layout input will not carry any bold span.
            val layoutResult = state.textLayoutResult
            assertNotNull(layoutResult, "Expected textLayoutResult to be set after toggle")
            val boldInLayout = layoutResult.layoutInput.text.spanStyles.any { range ->
                range.item.fontWeight == FontWeight.Bold &&
                    range.start <= 0 &&
                    range.end > 0
            }
            assertTrue(
                boldInLayout,
                "Expected bold span in BTF2 text layout result covering position 0. " +
                    "Spans found: ${layoutResult.layoutInput.text.spanStyles.map { "(${it.start}..${it.end} w=${it.item.fontWeight})" }}",
            )
        }

    /**
     * Verify that link insertion also renders immediately without selection change.
     */
    @Test
    fun `addLink renders immediately without further user action`() =
        runDesktopComposeUiTest {
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
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
            waitForIdle()

            onNodeWithTag("editor").performTextInput("click here")
            waitForIdle()

            state.textFieldState.edit { selection = TextRange(0, 5) }
            waitForIdle()

            state.addLink(text = "click", url = "https://example.com")
            waitForIdle()

            // The link renders as an underlined, link-colored span; assert it reached both the
            // model and BTF2's layout input rather than merely that a layout happened.
            val linkedInModel = state.annotatedString.spanStyles.any { range ->
                range.item.textDecoration == TextDecoration.Underline && range.start == 0 && range.end == 5
            }
            assertTrue(
                linkedInModel,
                "Expected an underlined link span over 0..5 in annotatedString. " +
                    "Spans: ${state.annotatedString.spanStyles.map { "(${it.start}..${it.end} ${it.item.textDecoration})" }}",
            )

            val layoutResult = state.textLayoutResult
            assertNotNull(layoutResult, "Expected textLayoutResult after addLink")
            val linkedInLayout = layoutResult.layoutInput.text.spanStyles.any { range ->
                range.item.textDecoration == TextDecoration.Underline && range.start == 0 && range.end == 5
            }
            assertTrue(
                linkedInLayout,
                "Expected the underlined link span over 0..5 in BTF2's layout input. " +
                    "Spans: ${layoutResult.layoutInput.text.spanStyles.map { "(${it.start}..${it.end} ${it.item.textDecoration})" }}",
            )
        }
}
