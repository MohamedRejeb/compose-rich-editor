package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Model-level invariant tests for [RichTextState.handleSelectionChanged].
 *
 * The Compose UI test harness batches pointer events synchronously, so the
 * recomposition-mid-drag race that the desktop drag bug depends on cannot be
 * reproduced through performMouseInput. These tests assert the structural
 * invariant directly at the model layer.
 *
 * Invariant: when [RichTextState.handleSelectionChanged] is called with
 * fromGestureObserver = true and both the old and new selections are
 * non-collapsed (i.e. mid-drag), [RichTextState.annotatedString] must NOT be
 * reassigned. A reassignment triggers Compose recomposition, which interrupts
 * BTF2's internal pointer tracking and freezes selection after the first drag
 * tick.
 *
 * Detection mechanism: [RichTextState.updateAnnotatedString] always reassigns
 * [RichTextState.annotatedString] to the result of a fresh buildAnnotatedString
 * call. AnnotatedString is a value type, but each buildAnnotatedString creates
 * a new object, so reference equality (assertSame / assertNotSame) proves
 * whether updateAnnotatedString ran.
 */
class HandleSelectionChangedDragInvariantTest {

    /**
     * Helper to simulate BTF2 updating textFieldState.selection before the snapshotFlow fires.
     * In production, BTF2 always updates textFieldState BEFORE handleSelectionChanged is called
     * (via either the selection setter's setTextFieldStateFromValue, or BTF2 directly).
     */
    private fun RichTextState.setSelectionAndHandle(
        newSelection: TextRange,
        fromGestureObserver: Boolean = false,
    ) {
        val previous = isApplyingProgrammaticSync
        isApplyingProgrammaticSync = true
        try {
            textFieldState.edit { selection = newSelection }
        } finally {
            isApplyingProgrammaticSync = previous
        }
        handleSelectionChanged(newSelection, fromGestureObserver = fromGestureObserver)
    }

    @Test
    fun `gesture observer skips annotatedString rebuild when both old and new selections are non-collapsed`() {
        val state = RichTextState()
        state.setText("hello world this is a longer line for dragging")

        // Establish initial non-collapsed selection (drag has started).
        state.setSelectionAndHandle(TextRange(5, 7), fromGestureObserver = true)
        val annotatedAfterDragStart = state.annotatedString

        // Subsequent drag tick: still non-collapsed, just a different range.
        state.setSelectionAndHandle(TextRange(5, 12), fromGestureObserver = true)
        val annotatedAfterMidDragTick = state.annotatedString

        // Invariant: mid-drag ticks must NOT rebuild annotatedString.
        // updateAnnotatedString reassigns the field to a new buildAnnotatedString result,
        // so reference equality proves no rebuild happened.
        assertSame(
            annotatedAfterDragStart,
            annotatedAfterMidDragTick,
            "annotatedString must not be rebuilt during a non-collapsed to non-collapsed selection " +
                "transition from the gesture observer; rebuilding triggers recomposition which " +
                "freezes the drag in BTF2",
        )
    }

    @Test
    fun `gesture observer rebuilds annotatedString on collapsed to non-collapsed drag start`() {
        val state = RichTextState()
        state.setText("hello world")

        // Drag starts: collapsed to non-collapsed.
        val annotatedBefore = state.annotatedString
        state.setSelectionAndHandle(TextRange(0, 5), fromGestureObserver = true)
        val annotatedAfter = state.annotatedString

        // The mask appears for the first time, so annotatedString must be rebuilt.
        assertNotSame(
            annotatedBefore,
            annotatedAfter,
            "annotatedString must be rebuilt when the selection transitions from collapsed to " +
                "non-collapsed at drag start, so the background mask appears",
        )
    }

    @Test
    fun `gesture observer rebuilds annotatedString on non-collapsed to collapsed drag release`() {
        val state = RichTextState()
        state.setText("hello world")

        // Establish a non-collapsed selection (mid-drag state).
        state.setSelectionAndHandle(TextRange(0, 5), fromGestureObserver = true)
        val annotatedDuringDrag = state.annotatedString

        // User releases / clicks: non-collapsed to collapsed.
        state.setSelectionAndHandle(TextRange(5, 5), fromGestureObserver = true)
        val annotatedAfterRelease = state.annotatedString

        // The mask must be removed, so annotatedString must be rebuilt.
        assertNotSame(
            annotatedDuringDrag,
            annotatedAfterRelease,
            "annotatedString must be rebuilt when the selection transitions from non-collapsed to " +
                "collapsed at drag release, so the background mask is removed",
        )
    }

    @Test
    fun `gesture observer skips all side effects during mid-drag tick`() {
        val state = RichTextState()
        state.setText("hello world this is a longer line for dragging")

        // Establish drag start: collapsed to non-collapsed (fires all side effects).
        state.setSelectionAndHandle(TextRange(5, 7), fromGestureObserver = true)
        val annotatedAfterDragStart = state.annotatedString

        // Mid-drag tick: non-collapsed to non-collapsed.
        state.setSelectionAndHandle(TextRange(5, 12), fromGestureObserver = true)

        // state.selection reads from textFieldState, which was updated above.
        assertEquals(TextRange(5, 12), state.selection)

        // annotatedString must NOT be rebuilt (no per-tick recomposition trigger).
        // updateAnnotatedString always produces a new buildAnnotatedString object, so reference
        // equality proves no rebuild happened. This covers the most expensive side effect.
        assertSame(
            annotatedAfterDragStart,
            state.annotatedString,
            "annotatedString must not be rebuilt during mid-drag",
        )
    }

    @Test
    fun `gesture observer fires all side effects on drag-release tick`() {
        val state = RichTextState()
        state.setText("hello world this is text")

        // Drag start.
        state.setSelectionAndHandle(TextRange(5, 7), fromGestureObserver = true)
        // Mid-drag tick (no side effects).
        state.setSelectionAndHandle(TextRange(5, 12), fromGestureObserver = true)
        val annotatedMidDrag = state.annotatedString

        // Release: non-collapsed to collapsed.
        state.setSelectionAndHandle(TextRange(12, 12), fromGestureObserver = true)

        // annotatedString MUST be rebuilt (mask is removed on release).
        assertNotSame(
            annotatedMidDrag,
            state.annotatedString,
            "annotatedString must rebuild on drag release so the mask is removed",
        )
        // state.selection reads from textFieldState, which was updated above.
        assertEquals(
            TextRange(12, 12),
            state.selection,
            "state.selection must reflect the final position",
        )
    }

    @Test
    fun `programmatic caller rebuilds annotatedString on every non-collapsed change for issue 635 mask correctness`() {
        val state = RichTextState()
        state.setText("hello world this is text")

        // Programmatic setter path (default fromGestureObserver = false). The mask logic must
        // continue to fire for every non-collapsed change to maintain Issue 635 behavior
        // when callers use state.selection = ... directly.
        state.setSelectionAndHandle(TextRange(0, 5), fromGestureObserver = false)
        val annotatedAfterFirst = state.annotatedString

        state.setSelectionAndHandle(TextRange(0, 10), fromGestureObserver = false)
        val annotatedAfterSecond = state.annotatedString

        // For the programmatic path, the rebuild fires on every non-collapsed transition
        // because the background mask in the prebuilt annotatedString depends on the exact
        // selection range. (The gesture path skips this for performance during drag because
        // the mask presence flag, not its exact bounds, is what users perceive at drag speed.)
        assertNotSame(
            annotatedAfterFirst,
            annotatedAfterSecond,
            "annotatedString must be rebuilt on every non-collapsed change in the programmatic " +
                "path with fromGestureObserver false, to keep the Issue 635 background mask in " +
                "sync with the exact selection range",
        )
    }
}
