package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * During a drag selection the moving edge may not land on a paragraph's start offset
 * (that selects the virtual separator and highlights the next line) nor extend below
 * the pointer's line (word acceleration otherwise swallows the next line's first
 * word when dragging into the empty area after a short line). The anchor edge,
 * keyboard, and programmatic selections are untouched.
 */
@OptIn(ExperimentalTestApi::class)
class DragSelectionParagraphEdgeTest {

    // "alpha beta gamma delta": paragraph 2 starts at offset 11
    private fun twoParagraphState(): RichTextState {
        val state = RichTextState()
        state.setText("alpha beta\ngamma delta")
        return state
    }

    private fun RichTextState.platformSelection(selection: TextRange) {
        onTextFieldValueChange(TextFieldValue(textFieldValue.text, selection))
    }

    @Test
    fun `drag selection ending on a paragraph edge is pulled back onto the paragraph`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()

        // Word selection "beta", then a drag extension landing on paragraph 2's start
        state.platformSelection(TextRange(6, 10))
        state.platformSelection(TextRange(6, 11))

        assertEquals(TextRange(6, 10), state.selection)
    }

    @Test
    fun `drag selection ending inside the next paragraph is honored`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()

        state.platformSelection(TextRange(6, 13))

        assertEquals(TextRange(6, 13), state.selection)
    }

    @Test
    fun `reversed drag selection ending on a paragraph edge keeps its direction`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()

        // The reversed max is the moving edge (start-handle dragged forward)
        state.platformSelection(TextRange(8, 6))
        state.platformSelection(TextRange(11, 6))

        assertEquals(TextRange(10, 6), state.selection)
    }

    @Test
    fun `collapsed caret on a paragraph edge is not affected by the gesture`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()

        state.platformSelection(TextRange(11))

        assertEquals(TextRange(11), state.selection)
    }

    @Test
    fun `select all is honored even with a trailing empty paragraph`() {
        val state = RichTextState()
        state.setText("abc\n")
        state.onSelectionGestureStart()

        // Trailing empty paragraph starts at the document end; select-all must survive
        val length = state.textFieldValue.text.length
        state.platformSelection(TextRange(0, length))

        assertEquals(TextRange(0, length), state.selection)
    }

    @Test
    fun `drag selection ending on a list item edge is pulled back`() {
        val state = RichTextState()
        state.setMarkdown("1. aa\n2. bb")
        state.onSelectionGestureStart()

        // "1. aa 2. bb": item 2 starts at offset 6 (including its "2. " prefix)
        state.platformSelection(TextRange(0, 4))
        state.platformSelection(TextRange(0, 6))

        assertEquals(TextRange(0, 5), state.selection)
    }

    @Test
    fun `keyboard selection without a gesture is honored`() {
        val state = twoParagraphState()

        // Shift+Down whole-line selection legitimately ends at a paragraph start
        state.platformSelection(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `keyboard selection after the gesture grace period is honored`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()

        Thread.sleep(1200)
        state.platformSelection(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `press cancel at long press start does not end the gesture mid drag`() {
        val state = twoParagraphState()

        // Android cancels the press interaction when the long-press drag takes over
        // the pointer; the selection changes that keep flowing sustain the gesture
        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()

        state.platformSelection(TextRange(6, 10))
        Thread.sleep(800)
        state.platformSelection(TextRange(6, 11))

        assertEquals(TextRange(6, 10), state.selection)
    }

    @Test
    fun `on touch platforms a handle drag long after the press is still pulled back`() {
        val state = twoParagraphState()
        // Android/iOS: the selection handles never press the field, so no gesture
        // signal exists; every platform selection change counts as gesture-driven
        state.treatSelectionChangesAsGesture = true

        state.platformSelection(TextRange(6, 10))
        state.platformSelection(TextRange(6, 11))

        assertEquals(TextRange(6, 10), state.selection)
    }

    @Test
    fun `on touch platforms select all is still honored`() {
        val state = twoParagraphState()
        state.treatSelectionChangesAsGesture = true

        val length = state.textFieldValue.text.length
        state.platformSelection(TextRange(0, length))

        assertEquals(TextRange(0, length), state.selection)
    }

    @Test
    fun `drag into the empty area after a short line clamps the selection to that line`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.setText("ab\ncdef ghij")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            // Pointer in the empty area to the right of the short first line
            val layout = checkNotNull(state.textLayoutResult)
            val pointer = Offset(
                layout.getLineRight(0) + 100f,
                (layout.getLineTop(0) + layout.getLineBottom(0)) / 2,
            )
            state.onSelectionGesturePointerMove(pointer)

            // "ab cdef ghij": the platform maps the drag past the separator and word
            // acceleration extends to the end of "cdef" (offset 7)
            state.platformSelection(TextRange(0, 2))
            state.platformSelection(TextRange(0, 7))

            assertEquals(TextRange(0, 2), state.selection)
        }

    @Test
    fun `a stale pointer position does not clamp`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.setText("ab\ncdef ghij")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            val layout = checkNotNull(state.textLayoutResult)
            val pointer = Offset(
                layout.getLineRight(0) + 100f,
                (layout.getLineTop(0) + layout.getLineBottom(0)) / 2,
            )
            state.onSelectionGesturePointerMove(pointer)
            state.platformSelection(TextRange(0, 2))
            // Handle drags never cross the editor node, so their selection changes
            // arrive with an old pointer position; those must pass through
            Thread.sleep(600)

            state.platformSelection(TextRange(0, 7))

            assertEquals(TextRange(0, 7), state.selection)
        }

    @Test
    fun `a pointer on the target line does not clamp`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.setText("ab\ncdef ghij")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            // Pointer genuinely on the second line: selecting its first word is intent
            val layout = checkNotNull(state.textLayoutResult)
            val pointer = Offset(
                8f,
                (layout.getLineTop(1) + layout.getLineBottom(1)) / 2,
            )
            state.onSelectionGesturePointerMove(pointer)

            state.platformSelection(TextRange(0, 2))
            state.platformSelection(TextRange(0, 7))

            assertEquals(TextRange(0, 7), state.selection)
        }

    @Test
    fun `backward drag does not clamp the anchor to the pointer line`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.setText("ab\ncdef ghij")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            // Anchor inside "ghij" on line 1, dragging up to line 0: the anchor
            // legitimately stays below the pointer's line
            state.platformSelection(TextRange(11, 8))

            val layout = checkNotNull(state.textLayoutResult)
            state.onSelectionGesturePointerMove(
                Offset(4f, (layout.getLineTop(0) + layout.getLineBottom(0)) / 2),
            )
            state.platformSelection(TextRange(11, 1))

            assertEquals(TextRange(11, 1), state.selection)
        }

    @Test
    fun `backward drag with the anchor on a paragraph edge keeps the anchor`() {
        val state = twoParagraphState()
        state.onSelectionGestureStart()

        // The anchor sits exactly on paragraph 2's start; dragging the other edge
        // backward must not move the anchor
        state.platformSelection(TextRange(11, 8))
        state.platformSelection(TextRange(11, 6))

        assertEquals(TextRange(11, 6), state.selection)
    }

    @Test
    fun `pull back applies inside the press window too`() = runBlocking {
        val state = twoParagraphState()

        val pressJob = launch(Dispatchers.Default) {
            state.adjustSelectionAndRegisterPressPosition(Offset(8f, 8f))
        }
        delay(80)
        state.onSelectionGestureStart()

        state.platformSelection(TextRange(6, 10))
        state.platformSelection(TextRange(6, 11))
        pressJob.cancel()

        assertEquals(TextRange(6, 10), state.selection)
    }
}
