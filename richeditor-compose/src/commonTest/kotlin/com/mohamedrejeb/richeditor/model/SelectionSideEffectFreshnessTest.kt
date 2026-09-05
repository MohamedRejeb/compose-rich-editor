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
 * indicators built on it, the staged style bags and the #635 mask) against the two shapes
 * that the mid-drag gate in [RichTextState.handleSelectionChanged] used to leave stale:
 *
 * 1. A pointer drag whose ticks are all gated and which comes to rest inside a paragraph,
 *    so the clamp in [RichTextState.onSelectionGestureEnd] is a no-op and the gesture end
 *    used to return without running any catch-up.
 * 2. A keyboard extension (shift+arrow on desktop). The selection observer reports those
 *    with `fromGestureObserver = true` exactly like a drag, but no pointer gesture is live,
 *    so nothing ever arrives to catch the derived state up.
 *
 * Boundary, deliberately pinned by `a keyboard extension inside the gesture grace window
 * is still gated` below: liveness is a one second grace window after the last gesture
 * activity, so keyboard extensions issued within a second of a click are still treated as
 * gesture ticks. The staleness there is bounded (the first tick past the grace runs the
 * full pass), unlike the unbounded staleness these tests pin.
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
    fun `a drag resting inside a paragraph refreshes the derived styles when the gesture ends`() {
        val state = mixedStyleState()
        state.selection = TextRange(0)

        state.onSelectionGestureStart()

        // First extension: collapsed to non-collapsed runs the full pass.
        state.observerTick(TextRange(0, 4))
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the drag's first extension must run the full pass",
        )

        // Every later tick is non-collapsed on both sides and is gated while the pointer
        // is down, so the derived state stays on the first extension's range.
        state.observerTick(TextRange(0, 7))
        state.observerTick(TextRange(0, 10))

        state.onSelectionGestureEnd()

        assertEquals(
            TextRange(0, 10),
            state.selection,
            "the resting selection is inside the paragraph so the clamp is a no-op",
        )
        assertNull(
            state.currentSpanStyle.fontWeight,
            "the gesture end must run the deferred side effects against the resting range " +
                "even when the clamp changes nothing",
        )
        assertNull(
            state.currentSpanStyle.fontStyle,
            "the resting range spans both styled regions so neither attribute is common",
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
     * The accepted boundary of the liveness window. A click ends its gesture, which arms a
     * one second grace, so a keyboard extension issued inside that window still looks like a
     * gesture tick and is gated. The staleness lasts only until the grace lapses.
     */
    @Test
    fun `a keyboard extension inside the gesture grace window is still gated`() {
        val state = mixedStyleState()
        state.selection = TextRange(0)

        state.onSelectionGestureStart()
        state.onSelectionGestureEnd()

        state.observerTick(TextRange(0, 4))
        state.observerTick(TextRange(0, 10))

        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "inside the grace window the extension is treated as a drag tick and gated",
        )
    }
}
