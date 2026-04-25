package com.mohamedrejeb.richeditor.model.history

/**
 * Decides how consecutive commits merge into undo groups.
 *
 * Rules:
 * - `Typing` + `Typing` coalesce if within [windowMs] and the caret moved
 *   continuously (new caret == previous caret + added text length).
 * - `Delete` + `Delete` coalesce if within [windowMs].
 * - `Typing` <-> `Delete` always breaks.
 * - `LineBreak`, `Formatting`, `Structural`, `Paste`, `Programmatic` always start a
 *   new group and seal the pending one.
 * - `SelectionJump` does not push a snapshot; it only seals the pending group.
 */
internal class RichTextHistoryCoalescer(
    var windowMs: Long,
) {
    private enum class Kind { Typing, Delete }

    private var pendingKind: Kind? = null
    private var pendingLastTsMs: Long = 0L
    private var pendingCaret: Int = -1
    private var sealed: Boolean = true

    fun shouldStartNewGroup(trigger: CommitTrigger, nowMs: Long): Boolean {
        if (sealed) return true
        return when (trigger) {
            is CommitTrigger.Typing -> {
                if (pendingKind != Kind.Typing) return true
                if (nowMs - pendingLastTsMs > windowMs) return true
                val expectedCaret = pendingCaret + trigger.addedText.length
                if (trigger.caret != expectedCaret) return true
                false
            }
            is CommitTrigger.Delete -> {
                if (pendingKind != Kind.Delete) return true
                if (nowMs - pendingLastTsMs > windowMs) return true
                false
            }
            CommitTrigger.LineBreak,
            CommitTrigger.Formatting,
            CommitTrigger.Structural,
            CommitTrigger.Paste,
            CommitTrigger.SelectionJump,
            CommitTrigger.Programmatic,
            -> true
        }
    }

    fun noteCommit(trigger: CommitTrigger, nowMs: Long) {
        when (trigger) {
            is CommitTrigger.Typing -> {
                pendingKind = Kind.Typing
                pendingCaret = trigger.caret
                pendingLastTsMs = nowMs
                sealed = false
            }
            is CommitTrigger.Delete -> {
                pendingKind = Kind.Delete
                pendingCaret = trigger.caret
                pendingLastTsMs = nowMs
                sealed = false
            }
            CommitTrigger.LineBreak,
            CommitTrigger.Formatting,
            CommitTrigger.Structural,
            CommitTrigger.Paste,
            CommitTrigger.Programmatic,
            -> {
                pendingKind = null
                sealed = true
            }
            CommitTrigger.SelectionJump -> noteSelectionJump()
        }
    }

    fun noteSelectionJump() {
        pendingKind = null
        sealed = true
    }

    fun reset() {
        pendingKind = null
        pendingLastTsMs = 0L
        pendingCaret = -1
        sealed = true
    }
}
