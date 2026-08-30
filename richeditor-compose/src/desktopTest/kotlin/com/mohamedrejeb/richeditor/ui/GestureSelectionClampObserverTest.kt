package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pins the paragraph-edge clamp on the path the editor actually uses for gesture selections.
 *
 * Since the selection observer replaced the legacy onTextFieldValueChange bridge,
 * `adjustGestureSelection` is reached only from `handleSelectionChanged`. The model-level
 * suites (`DragSelectionParagraphEdgeTest`, `Issue730LongPressSelectionTest`) drive
 * `onTextFieldValueChange` directly and therefore no longer cover the live path at all.
 */
@OptIn(ExperimentalTestApi::class)
class GestureSelectionClampObserverTest {

    @Test
    fun `mouse drag onto the next paragraph start is pulled back onto the dragged line`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            // "alpha beta gamma delta": paragraph 2 starts at offset 11
            state.setText("alpha beta\ngamma delta")
            waitForIdle()

            val layout = checkNotNull(state.textLayoutResult)
            fun caretAt(offset: Int): Offset {
                val rect = layout.getCursorRect(offset)
                return Offset(rect.left, (rect.top + rect.bottom) / 2f)
            }

            // Press inside "beta" so the anchor is fixed, then let the observer settle so the
            // first extension is a genuine collapsed to non-collapsed tick.
            onNodeWithTag("editor").performMouseInput {
                moveTo(caretAt(6))
                press()
            }
            waitForIdle()

            // Drag onto paragraph 2's start offset: the platform reports (6, 11), which selects
            // the virtual separator and lights up the next line.
            onNodeWithTag("editor").performMouseInput { moveTo(caretAt(11)) }
            waitForIdle()

            onNodeWithTag("editor").performMouseInput { release() }
            waitForIdle()

            assertEquals(TextRange(6, 10), state.selection)
        }

    /**
     * The same correction expressed directly against the observer entry point, which is what
     * `BasicRichTextEditor`'s snapshotFlow collector calls. Keeps the pin meaningful even if
     * the platform's drag-to-offset mapping shifts under a Compose upgrade.
     */
    @Test
    fun `gesture tick landing on a paragraph start is clamped before the side effects run`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setText("alpha beta\ngamma delta")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            // Caret inside "beta", then one gesture extension landing on paragraph 2's start.
            state.handleSelectionChanged(TextRange(6), fromGestureObserver = true)
            state.handleSelectionChanged(TextRange(6, 11), fromGestureObserver = true)

            assertEquals(TextRange(6, 10), state.selection)
        }

    @Test
    fun `a gesture tick that does not reach a paragraph start is left alone`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setText("alpha beta\ngamma delta")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            state.handleSelectionChanged(TextRange(6), fromGestureObserver = true)
            state.handleSelectionChanged(TextRange(6, 13), fromGestureObserver = true)

            assertEquals(TextRange(6, 13), state.selection)
        }

    @Test
    fun `a programmatic selection landing on a paragraph start is never clamped`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setText("alpha beta\ngamma delta")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            state.selection = TextRange(6, 11)

            assertEquals(TextRange(6, 11), state.selection)
        }
}
