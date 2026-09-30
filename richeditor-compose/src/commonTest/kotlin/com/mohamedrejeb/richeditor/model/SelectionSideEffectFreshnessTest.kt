package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the freshness of the derived selection state (currentSpanStyle, the toolbar
 * indicators built on it, the staged style bags and the #635 mask) on every path that
 * delivers a selection through the observer:
 *
 * 1. A pointer drag, tick by tick while the pointer is down, and at the gesture end.
 * 2. A selection handle drag on a touch platform. The handles live in popups, so the
 *    editor never sees a press or a release; the observer ticks are all it gets.
 * 3. A keyboard extension (shift+arrow on desktop), with or without a recent click.
 *
 * Every tick must run the full pass. A skipped tick leaves the toolbar describing an older
 * range, and a style toggle then acts on the wrong text.
 */
class SelectionSideEffectFreshnessTest {

    /**
     * One observer tick as BTF2 delivers it: the platform writes the selection into the
     * buffer, then the editor's snapshotFlow collector calls the handler.
     */
    private fun RichTextState.observerTick(newSelection: TextRange) {
        val previous = isApplyingProgrammaticSync
        isApplyingProgrammaticSync = true
        try {
            textFieldState.edit { selection = newSelection }
        } finally {
            isApplyingProgrammaticSync = previous
        }
        handleSelectionChanged(newSelection, fromGestureObserver = true)
    }

    /**
     * "bold plain": bold over `bold`, italic over `plain`, so every selection that spans
     * both regions has a common style with neither attribute set.
     *
     * [RichTextState.treatSelectionChangesAsGesture] defaults to true on the touch
     * platforms; clearing it makes these desktop-shaped tests read the same everywhere.
     */
    private fun mixedStyleState(): RichTextState {
        val state = RichTextState()
        state.setText("bold plain")
        state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 4))
        state.addSpanStyle(SpanStyle(fontStyle = FontStyle.Italic), TextRange(5, 10))
        state.treatSelectionChangesAsGesture = false
        return state
    }

    @Test
    fun `a pointer drag refreshes the derived styles on every tick while the pointer is down`() {
        val state = mixedStyleState()
        state.selection = TextRange(0)

        state.onSelectionGestureStart()

        state.observerTick(TextRange(0, 4))
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the drag's first extension must report bold",
        )

        state.observerTick(TextRange(0, 7))
        assertNull(
            state.currentSpanStyle.fontWeight,
            "a tick that extends past the bold region must drop bold while the pointer is down",
        )

        state.observerTick(TextRange(0, 10))
        assertNull(state.currentSpanStyle.fontStyle)

        state.onSelectionGestureEnd()

        assertEquals(
            TextRange(0, 10),
            state.selection,
            "the resting selection is inside the paragraph so the clamp is a no-op",
        )
        assertNull(state.currentSpanStyle.fontWeight)
        assertNull(state.currentSpanStyle.fontStyle)
    }

    /**
     * The shape of a selection handle drag on Android and iOS: every tick is treated as a
     * gesture, both sides are non-collapsed, and no gesture end ever arrives.
     */
    @Test
    fun `a handle drag on a touch platform refreshes the derived styles on every tick`() {
        val state = mixedStyleState()
        state.treatSelectionChangesAsGesture = true
        state.selection = TextRange(0)

        state.observerTick(TextRange(0, 4))
        assertEquals(FontWeight.Bold, state.currentSpanStyle.fontWeight)

        state.observerTick(TextRange(0, 10))
        assertNull(
            state.currentSpanStyle.fontWeight,
            "extending the handle past the bold region must drop bold without a gesture end",
        )

        state.observerTick(TextRange(0, 4))
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "dragging the handle back onto the bold region must report bold again",
        )
    }

    @Test
    fun `a keyboard extension refreshes the derived styles on every tick`() {
        val state = mixedStyleState()
        state.selection = TextRange(0)

        // No onSelectionGestureStart: shift+arrow on desktop reaches the observer with
        // fromGestureObserver = true but without any pointer gesture behind it.
        state.observerTick(TextRange(0, 4))
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "extending over the bold region must report bold",
        )

        state.observerTick(TextRange(0, 10))
        assertNull(
            state.currentSpanStyle.fontWeight,
            "extending past the bold region must drop bold instead of keeping the previous " +
                "tick's answer",
        )

        state.observerTick(TextRange(0, 4))
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "shrinking back onto the bold region must report bold again",
        )
    }

    /**
     * A click ends its gesture, which arms a one second grace, so a keyboard extension issued
     * inside that window is treated as a gesture tick. It must be as fresh as any other tick.
     */
    @Test
    fun `a keyboard extension inside the gesture grace window refreshes the derived styles`() {
        val state = mixedStyleState()
        state.selection = TextRange(0)

        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()

        state.observerTick(TextRange(0, 4))
        state.observerTick(TextRange(0, 10))

        assertNull(
            state.currentSpanStyle.fontWeight,
            "a tick inside the grace window must refresh the derived styles like any other",
        )
    }
}
