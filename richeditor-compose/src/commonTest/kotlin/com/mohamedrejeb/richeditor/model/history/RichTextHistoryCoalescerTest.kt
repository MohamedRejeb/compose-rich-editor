package com.mohamedrejeb.richeditor.model.history

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichTextHistoryCoalescerTest {

    @Test
    fun firstCommitAlwaysStartsNewGroup() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("a", 1), nowMs = 0L))
    }

    @Test
    fun consecutiveTypingWithinWindowCoalesces() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertFalse(c.shouldStartNewGroup(CommitTrigger.Typing("b", 2), nowMs = 100L))
    }

    @Test
    fun typingOutsideWindowBreaksGroup() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("b", 2), nowMs = 600L))
    }

    @Test
    fun typingThenDeleteBreaksGroup() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Delete(0), nowMs = 10L))
    }

    @Test
    fun deleteThenDeleteWithinWindowCoalesces() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Delete(5), nowMs = 0L)
        assertFalse(c.shouldStartNewGroup(CommitTrigger.Delete(4), nowMs = 100L))
    }

    @Test
    fun nonContinuousCaretBreaksTypingGroup() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("b", 10), nowMs = 50L))
    }

    @Test
    fun lineBreakAlwaysStartsNewGroupAndSealsPending() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.LineBreak, nowMs = 50L))
        c.noteCommit(CommitTrigger.LineBreak, nowMs = 50L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("b", 2), nowMs = 60L))
    }

    @Test
    fun formattingAlwaysStartsNewGroupAndSealsPending() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Formatting, nowMs = 50L))
        c.noteCommit(CommitTrigger.Formatting, nowMs = 50L)
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("b", 2), nowMs = 60L))
    }

    @Test
    fun selectionJumpSealsPendingWithoutStartingGroup() {
        val c = RichTextHistoryCoalescer(windowMs = 500L)
        c.noteCommit(CommitTrigger.Typing("a", 1), nowMs = 0L)
        c.noteSelectionJump()
        assertTrue(c.shouldStartNewGroup(CommitTrigger.Typing("b", 2), nowMs = 10L))
    }
}
