package com.mohamedrejeb.richeditor.model.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.TimeSource

/**
 * Abstraction over `RichTextState` consumed by [RichTextHistory]. Exposes only the
 * minimum surface the controller needs so it can be unit-tested with a fake host.
 */
internal interface RichTextHistoryHost {
    fun captureState(timestampMs: Long): RichTextSnapshot
    fun restoreState(snapshot: RichTextSnapshot)
}

/**
 * Undo/redo controller for `RichTextState`.
 *
 * The controller stores a bounded stack of groups. Each group holds a `before`
 * snapshot (state prior to the group's first commit) and an `after` snapshot
 * (state after the most recent commit in the group). Undo restores `before` and
 * transfers the group to the redo stack; redo restores `after` and transfers it
 * back.
 *
 * Coalescing is delegated to [RichTextHistoryCoalescer].
 */
public class RichTextHistory internal constructor(
    private val host: RichTextHistoryHost,
    limit: Int,
    coalesceWindowMs: Long,
    private val clock: () -> Long = DefaultClock,
) {
    private val undoStack: ArrayDeque<UndoGroup> = ArrayDeque()
    private val redoStack: ArrayDeque<UndoGroup> = ArrayDeque()
    private val coalescer = RichTextHistoryCoalescer(windowMs = coalesceWindowMs)

    private var canUndoState by mutableStateOf(false)
    private var canRedoState by mutableStateOf(false)

    /** Maximum groups retained on the undo stack. `0` disables history. */
    public var limit: Int = limit
        set(value) {
            require(value >= 0) { "limit must be >= 0" }
            field = value
            trimUndoStackToLimit()
            refreshDerivedState()
        }

    /** Idle window for coalescing consecutive typing / deletion into one undo group. */
    public var coalesceWindowMs: Long
        get() = coalescer.windowMs
        set(value) {
            require(value >= 0) { "coalesceWindowMs must be >= 0" }
            coalescer.windowMs = value
        }

    public val canUndo: Boolean get() = canUndoState
    public val canRedo: Boolean get() = canRedoState

    public fun undo(): Boolean {
        val group = undoStack.removeLastOrNull() ?: return false
        host.restoreState(group.before)
        redoStack.addLast(group)
        coalescer.reset()
        refreshDerivedState()
        return true
    }

    public fun redo(): Boolean {
        val group = redoStack.removeLastOrNull() ?: return false
        host.restoreState(group.after)
        undoStack.addLast(group)
        coalescer.reset()
        refreshDerivedState()
        return true
    }

    public fun clear() {
        undoStack.clear()
        redoStack.clear()
        coalescer.reset()
        refreshDerivedState()
    }

    internal fun onProgrammaticReplace() {
        clear()
    }

    /**
     * Seals the pending coalesced group without pushing a snapshot. Called when an
     * operation logically ends the current typing burst (e.g. a collapsed-selection
     * style toggle that stages styles for future typing) but has no tree change worth
     * recording. The next committed edit starts a fresh group.
     */
    internal fun sealPendingGroup() {
        coalescer.noteSelectionJump()
    }

    /** Capture a snapshot via the host without mutating the stacks. */
    internal fun captureForCommit(timestampMs: Long): RichTextSnapshot =
        host.captureState(timestampMs)

    /**
     * Called BEFORE a mutation is applied. [beforeSnapshot] must reflect the state
     * prior to the mutation. The controller decides, based on [trigger] and the
     * coalescer's state, whether to open a new group or extend the pending one.
     */
    internal fun onCommit(trigger: CommitTrigger, beforeSnapshot: RichTextSnapshot) {
        if (trigger == CommitTrigger.SelectionJump) {
            coalescer.noteSelectionJump()
            return
        }
        redoStack.clear()
        val now = clock()
        if (coalescer.shouldStartNewGroup(trigger, now)) {
            undoStack.addLast(UndoGroup(before = beforeSnapshot, after = beforeSnapshot))
            trimUndoStackToLimit()
        }
    }

    /**
     * Called AFTER a mutation has been applied. Updates the tail group's `after`
     * snapshot so redo rolls forward to the latest state.
     */
    internal fun onAfterCommit(trigger: CommitTrigger) {
        if (trigger == CommitTrigger.SelectionJump || trigger == CommitTrigger.Programmatic) return
        val now = clock()
        val tail = undoStack.lastOrNull() ?: return
        val after = host.captureState(now)
        undoStack[undoStack.lastIndex] = tail.copy(after = after)
        coalescer.noteCommit(trigger, now)
        refreshDerivedState()
    }

    private fun trimUndoStackToLimit() {
        while (undoStack.size > limit) undoStack.removeFirst()
    }

    private fun refreshDerivedState() {
        canUndoState = undoStack.isNotEmpty()
        canRedoState = redoStack.isNotEmpty()
    }

    private data class UndoGroup(
        val before: RichTextSnapshot,
        val after: RichTextSnapshot,
    )

    internal companion object {
        private val Start = TimeSource.Monotonic.markNow()
        val DefaultClock: () -> Long = { Start.elapsedNow().inWholeMilliseconds }
    }
}
