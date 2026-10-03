package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The state the toolbar reads must follow a mouse drag tick by tick, through the real editor
 * and its selection observer, not only once the button is released.
 */
@OptIn(ExperimentalTestApi::class)
class DragSelectionToolbarStateTest {

    @Test
    fun `dragging past a bold word drops bold before the mouse is released`() =
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
            state.setText("hello world")
            state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 5))
            waitForIdle()

            val layout = checkNotNull(state.textLayoutResult)
            fun caretAt(offset: Int): Offset {
                val rect = layout.getCursorRect(offset)
                return Offset(rect.left, (rect.top + rect.bottom) / 2f)
            }

            onNodeWithTag("editor").performMouseInput {
                moveTo(caretAt(1))
                press()
            }
            waitForIdle()

            onNodeWithTag("editor").performMouseInput { moveTo(caretAt(3)) }
            waitForIdle()
            assertEquals(TextRange(1, 3), state.selection)
            assertEquals(
                FontWeight.Bold,
                state.currentSpanStyle.fontWeight,
                "the first extension stays inside the bold word",
            )

            onNodeWithTag("editor").performMouseInput { moveTo(caretAt(9)) }
            waitForIdle()
            assertEquals(TextRange(1, 9), state.selection)
            assertNull(
                state.currentSpanStyle.fontWeight,
                "the selection now covers plain text, so bold must be off while the button is down",
            )

            onNodeWithTag("editor").performMouseInput { release() }
            waitForIdle()
            assertNull(state.currentSpanStyle.fontWeight)
        }
}
