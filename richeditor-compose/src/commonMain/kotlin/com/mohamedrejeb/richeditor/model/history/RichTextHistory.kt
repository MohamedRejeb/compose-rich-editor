package com.mohamedrejeb.richeditor.model.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
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

    /** Re-entrancy depth of [group]; > 0 while a grouping block runs. */
    private var groupingDepth: Int = 0

    /** Pre-block snapshot of the active [group]; null when inactive or invalidated. */
    private var groupingBefore: RichTextSnapshot? = null

    /** True once the active [group] has pushed its undo entry. */
    private var groupingHasEntry: Boolean = false

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
        invalidateActiveGrouping()
        refreshDerivedState()
        return true
    }

    public fun redo(): Boolean {
        val group = redoStack.removeLastOrNull() ?: return false
        host.restoreState(group.after)
        undoStack.addLast(group)
        coalescer.reset()
        invalidateActiveGrouping()
        refreshDerivedState()
        return true
    }

    public fun clear() {
        undoStack.clear()
        redoStack.clear()
        coalescer.reset()
        invalidateActiveGrouping()
        refreshDerivedState()
    }

    /**
     * Runs [block] and records every commit made inside it as a single undo entry,
     * regardless of the commit kinds involved (typing, formatting, structural, paste).
     *
     * Entering the group seals any pending coalesced typing/deletion group, and the
     * group itself is sealed on exit, so surrounding commits never merge into it.
     * Undo restores the state captured just before [block] ran; redo restores the
     * state after its last commit. Nested calls join the outermost group. A block
     * that commits nothing adds no entry. If [block] throws, whatever it already
     * committed is kept as one undo entry and the exception is rethrown.
     */
    @ExperimentalRichTextApi
    public fun <R> group(block: () -> R): R {
        if (groupingDepth > 0) {
            groupingDepth++
            try {
                return block()
            } finally {
                groupingDepth--
            }
        }
        coalescer.reset()
        groupingDepth = 1
        groupingBefore = host.captureState(clock())
        groupingHasEntry = false
        try {
            return block()
        } finally {
            groupingDepth = 0
            groupingBefore = null
            groupingHasEntry = false
            refreshDerivedState()
        }
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
        if (groupingDepth > 0) {
            if (!groupingHasEntry) {
                val before = groupingBefore ?: beforeSnapshot
                undoStack.addLast(UndoGroup(before = before, after = before))
                trimUndoStackToLimit()
                groupingHasEntry = true
            }
            return
        }
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
        // Commits inside a group never feed the coalescer: the group was sealed on
        // entry and must stay sealed so commits after it start fresh entries.
        if (groupingDepth == 0) {
            coalescer.noteCommit(trigger, now)
        }
        refreshDerivedState()
    }

    /**
     * Undo, redo, and clear invalidate the active group's pushed entry (it may have
     * been popped, moved, or wiped). The next commit inside the group opens a fresh
     * entry from its own pre-commit snapshot.
     */
    private fun invalidateActiveGrouping() {
        if (groupingDepth == 0) return
        groupingBefore = null
        groupingHasEntry = false
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
