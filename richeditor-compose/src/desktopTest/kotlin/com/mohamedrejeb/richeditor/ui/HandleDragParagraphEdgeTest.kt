package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Selection handle drags at a paragraph edge.
 *
 * The handles live in popups, so the editor never sees their pointer and the pointer-line
 * clamp cannot help. Dragging the end handle into the empty space right of a short line
 * hits the next paragraph's start, and the platform's word acceleration then extends the
 * selection over that paragraph's first word. From offsets alone that is indistinguishable
 * from a handle placed on the first word itself, so the rule is deliberately narrow: an end
 * handle that rests exactly on a paragraph's end and then lands inside the next paragraph's
 * first word stays on the paragraph end. Every other move is accepted as is.
 */
@OptIn(ExperimentalTestApi::class)
class HandleDragParagraphEdgeTest {

    /**
     * One handle drag tick as BTF2 delivers it on a touch platform: the buffer is written,
     * the observer calls the handler, and no pointer position ever reaches the editor.
     */
    private fun RichTextState.handleTick(newSelection: TextRange) {
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
    fun `an end handle resting on a paragraph end stays there when the next tick lands in the first word of the next paragraph`() {
        val state = touchState()

        state.handleTick(TextRange(0, 4))
        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 11))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `the end handle stays on the paragraph end while the platform keeps reporting the first word`() {
        val state = touchState()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 11))
        state.handleTick(TextRange(0, 11))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `a tick exactly on the next paragraph start is still pulled back onto the paragraph end`() {
        val state = touchState()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 7))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `a tick one past the first word of the next paragraph is accepted`() {
        val state = touchState()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 12))

        assertEquals(TextRange(0, 12), state.selection)
    }

    /**
     * Desktop has no selection handles. A keyboard word extension (ctrl+shift+Right) reaches
     * the observer without a pointer too, and inside the one second grace after a click the
     * gesture still counts as live, so the handle rule must not run there.
     */
    @Test
    fun `a keyboard word extension on desktop inside the gesture grace is not held on the paragraph end`() {
        val state = RichTextState()
        state.setText("Line 4\nLine 5 tail")
        state.treatSelectionChangesAsGesture = false
        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `a diagonal end handle drag that never rests on the paragraph end selects the first word`() {
        val state = touchState()

        state.handleTick(TextRange(0, 4))
        state.handleTick(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `an end handle dragged past the first word of the next paragraph is accepted`() {
        val state = touchState()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 13))

        assertEquals(TextRange(0, 13), state.selection)
    }

    @Test
    fun `an end handle dragged back onto the first word from further in the paragraph is accepted`() {
        val state = touchState()

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, 13))
        state.handleTick(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)
    }

    @Test
    fun `a reversed selection whose moving max rests on a paragraph end stays there`() {
        val state = touchState()

        // Anchor at 2, the moving edge is the end and the range is reversed.
        state.handleTick(TextRange(6, 2))
        state.handleTick(TextRange(11, 2))

        assertEquals(TextRange(6, 2), state.selection)
    }

    @Test
    fun `an end handle resting on a paragraph end stays there when the next paragraph is a list item`() {
        val state = RichTextState()
        state.setHtml("<p>Before</p><ul><li>First item</li></ul>")
        state.treatSelectionChangesAsGesture = true
        val text = state.annotatedString.text
        val firstWordEnd = text.indexOf("First") + "First".length

        state.handleTick(TextRange(0, 6))
        state.handleTick(TextRange(0, firstWordEnd))

        assertEquals(TextRange(0, 6), state.selection)
    }

    @Test
    fun `a finger drag with a fresh pointer on the next paragraph's first word is not held on the paragraph end`() =
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
            state.setText("Line 4\nLine 5 tail")
            waitForIdle()
            state.treatSelectionChangesAsGesture = true

            val layout = checkNotNull(state.textLayoutResult)
            val onFirstWordOfLine5 = layout.getCursorRect(9).let { Offset(it.left, it.center.y) }

            state.handleTick(TextRange(0, 6))
            state.onSelectionGesturePointerMove(onFirstWordOfLine5)
            state.handleTick(TextRange(0, 11))

            assertEquals(TextRange(0, 11), state.selection)
        }
}
