package com.mohamedrejeb.richeditor.model.history

/**
 * Classifies the kind of state mutation that produced a history commit.
 *
 * The coalescer uses the trigger to decide whether a new commit extends the pending
 * undo group (e.g. consecutive [Typing]) or forces a new group (e.g. [LineBreak],
 * [Formatting]).
 */
internal sealed interface CommitTrigger {
    /** Character insertion (typing, IME text commit). */
    data class Typing(val addedText: String, val caret: Int) : CommitTrigger

    /** Character deletion (backspace, forward-delete). */
    data class Delete(val caret: Int) : CommitTrigger

    /** Enter key / line-break insertion - always its own undo group. */
    data object LineBreak : CommitTrigger

    /** Toolbar-style formatting change (bold, color, link, list toggle). */
    data object Formatting : CommitTrigger

    /** Structural change (image, atomic token, list level, large range removal). */
    data object Structural : CommitTrigger

    /** Clipboard paste (plain or HTML). */
    data object Paste : CommitTrigger

    /** Selection-only move - does not push a snapshot; just seals the pending group. */
    data object SelectionJump : CommitTrigger

    /** Programmatic replacement (`setHtml`, `setMarkdown`, `setConfig`) - clears stacks. */
    data object Programmatic : CommitTrigger
}
