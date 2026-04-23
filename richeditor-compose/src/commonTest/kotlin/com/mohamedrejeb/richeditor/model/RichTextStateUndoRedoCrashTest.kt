package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Crash-hunt suite: exercises undo/redo across a wide range of mutation sequences
 * (ordered/unordered lists, nested formatting, list levels, line breaks, interleaved
 * operations) and asserts that (a) no exception is raised and (b) after round-tripping
 * undo + redo the document returns to its post-mutation state.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateUndoRedoCrashTest {

    private fun RichTextState.typeRaw(text: String) {
        val current = this.annotatedString.text
        val sel = this.selection.min
        val newText = current.substring(0, sel) + text + current.substring(sel)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(sel + text.length)))
    }

    /** Reproduces the user-reported crash: several ordered-list items then Ctrl+Z. */
    @Test
    fun orderedListThreeItemsThenUndoDoesNotCrash() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("first")
        s.typeRaw("\n")
        s.typeRaw("second")
        s.typeRaw("\n")
        s.typeRaw("third")
        val beforeUndo = s.annotatedString.text

        // Should not crash
        s.history.undo()
        // Whatever it reverted, state must be internally consistent
        assertInternallyConsistent(s)

        // Redo it back
        s.history.redo()
        assertEquals(beforeUndo, s.annotatedString.text)
        assertInternallyConsistent(s)
    }

    @Test
    fun orderedListMultipleItemsUndoAllTheWay() {
        val s = RichTextState()
        s.toggleOrderedList()
        repeat(5) { i ->
            s.typeRaw("item$i")
            if (i != 4) s.typeRaw("\n")
        }
        // Undo every group; must not crash
        while (s.history.canUndo) {
            s.history.undo()
            assertInternallyConsistent(s)
        }
        // Redo every group back
        while (s.history.canRedo) {
            s.history.redo()
            assertInternallyConsistent(s)
        }
    }

    @Test
    fun unorderedListItemsUndoAllTheWay() {
        val s = RichTextState()
        s.toggleUnorderedList()
        repeat(4) { i ->
            s.typeRaw("bullet$i")
            if (i != 3) s.typeRaw("\n")
        }
        while (s.history.canUndo) {
            s.history.undo()
            assertInternallyConsistent(s)
        }
    }

    @Test
    fun listLevelChangesUndo() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("parent")
        s.typeRaw("\n")
        s.typeRaw("child")
        s.selection = TextRange(s.annotatedString.text.length)
        // Try to increase level — may or may not succeed depending on state, must not crash
        s.increaseListLevel()
        assertInternallyConsistent(s)
        s.history.undo()
        assertInternallyConsistent(s)
        s.history.redo()
        assertInternallyConsistent(s)
    }

    @Test
    fun mixedListAndFormattingUndo() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("hello")
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        s.typeRaw(" world")
        s.typeRaw("\n")
        s.typeRaw("second")
        s.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        s.typeRaw(" item")

        // Peel back every group
        while (s.history.canUndo) {
            s.history.undo()
            assertInternallyConsistent(s)
        }

        // Roll forward through every redo
        while (s.history.canRedo) {
            s.history.redo()
            assertInternallyConsistent(s)
        }
    }

    @Test
    fun toggleOrderedListOffUndo() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("a")
        s.typeRaw("\n")
        s.typeRaw("b")
        // Toggle list off
        s.toggleOrderedList()
        assertInternallyConsistent(s)
        // Undo the toggle-off
        s.history.undo()
        assertInternallyConsistent(s)
    }

    @Test
    fun toggleBetweenListTypesUndo() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("item")
        s.toggleUnorderedList()
        assertInternallyConsistent(s)
        s.history.undo()
        assertInternallyConsistent(s)
        s.history.redo()
        assertInternallyConsistent(s)
    }

    @Test
    fun linkInsertionAndUndo() {
        val s = RichTextState()
        s.typeRaw("go to ")
        s.addLink(text = "example", url = "https://example.com")
        s.typeRaw(" now")
        assertInternallyConsistent(s)
        while (s.history.canUndo) {
            s.history.undo()
            assertInternallyConsistent(s)
        }
        while (s.history.canRedo) {
            s.history.redo()
            assertInternallyConsistent(s)
        }
    }

    @Test
    fun heavyInterleavedSequence() {
        val s = RichTextState()
        s.typeRaw("Title")
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        s.typeRaw("\n")
        s.toggleOrderedList()
        s.typeRaw("one")
        s.typeRaw("\n")
        s.typeRaw("two")
        s.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        s.typeRaw(" underlined")
        s.typeRaw("\n")
        s.typeRaw("three")
        s.toggleOrderedList() // toggle off
        s.typeRaw("\n")
        s.typeRaw("after list")

        val finalText = s.annotatedString.text

        // Peel all the way back
        while (s.history.canUndo) {
            s.history.undo()
            assertInternallyConsistent(s)
        }

        // Roll all the way forward
        while (s.history.canRedo) {
            s.history.redo()
            assertInternallyConsistent(s)
        }
        assertEquals(finalText, s.annotatedString.text)
    }

    @Test
    fun rapidAlternatingUndoRedo() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("a")
        s.typeRaw("\n")
        s.typeRaw("b")
        s.typeRaw("\n")
        s.typeRaw("c")

        repeat(20) {
            if (s.history.canUndo) s.history.undo()
            assertInternallyConsistent(s)
            if (s.history.canRedo) s.history.redo()
            assertInternallyConsistent(s)
        }
    }

    @Test
    fun undoPastEmptyIsNoOp() {
        val s = RichTextState()
        assertFalse(s.history.canUndo)
        assertFalse(s.history.undo())
        assertFalse(s.history.redo())
        assertInternallyConsistent(s)

        s.typeRaw("x")
        s.history.undo()
        // Beyond the first group
        assertFalse(s.history.canUndo)
        assertFalse(s.history.undo())
        assertInternallyConsistent(s)
    }

    @Test
    fun deleteBackThroughListItem() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("abc")
        s.typeRaw("\n")
        s.typeRaw("def")
        // Simulate deletion by shrinking selection+text via onTextFieldValueChange
        val current = s.annotatedString.text
        val newText = current.substring(0, current.length - 1)
        s.onTextFieldValueChange(TextFieldValue(newText, TextRange(newText.length)))
        assertInternallyConsistent(s)
        s.history.undo()
        assertInternallyConsistent(s)
        s.history.redo()
        assertInternallyConsistent(s)
    }

    @Test
    fun clearHistoryDuringMutationsIsSafe() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("a")
        s.history.clear()
        assertFalse(s.history.canUndo)
        assertFalse(s.history.canRedo)
        // Continue editing
        s.typeRaw("\n")
        s.typeRaw("b")
        s.history.undo()
        assertInternallyConsistent(s)
    }

    @Test
    fun setHtmlThenUndoIsNoOp() {
        val s = RichTextState()
        s.setHtml("<ol><li>a</li><li>b</li><li>c</li></ol>")
        val afterSet = s.annotatedString.text
        // History should be empty after setHtml — undo is a no-op.
        assertFalse(s.history.canUndo)
        s.history.undo()
        assertEquals(afterSet, s.annotatedString.text)
        assertInternallyConsistent(s)

        // After setHtml we can still type + undo normally
        s.selection = TextRange(s.annotatedString.text.length)
        s.typeRaw("x")
        s.history.undo()
        assertInternallyConsistent(s)
    }

    @Test
    fun orderedListThenRestoreFromSnapshotMaintainsNumbering() {
        val s = RichTextState()
        s.toggleOrderedList()
        s.typeRaw("one")
        s.typeRaw("\n")
        s.typeRaw("two")
        s.typeRaw("\n")
        s.typeRaw("three")

        // Text contains list prefixes "1. ", "2. ", "3. "
        val full = s.annotatedString.text
        assertTrue(full.startsWith("1. one"))
        assertTrue("2. two" in full)
        assertTrue("3. three" in full)

        s.history.undo()
        assertInternallyConsistent(s)
        s.history.redo()
        assertEquals(full, s.annotatedString.text)
        assertInternallyConsistent(s)
    }

    @Test
    fun selectionOnlyChangesDoNotCrashHistory() {
        val s = RichTextState()
        s.typeRaw("hello")
        // Move caret around
        s.selection = TextRange(0)
        s.selection = TextRange(3)
        s.selection = TextRange(s.annotatedString.text.length)
        assertInternallyConsistent(s)
        s.history.undo()
        assertInternallyConsistent(s)
    }

    @Test
    fun collapsedSelectionSpanTogglesHaveNoNetEffectOnUndoStack() {
        // Starting from a fresh state: type "hello" (pushes 1 group). Then a long
        // sequence of collapsed-selection style toggles. None of them should push
        // an entry — so exactly 1 undo pops us back to empty.
        val s = RichTextState()
        s.typeRaw("hello")
        assertTrue(s.history.canUndo)
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        s.addSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        s.removeSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        s.clearSpanStyles()
        s.toggleRichSpan(com.mohamedrejeb.richeditor.model.RichSpanStyle.Code())
        s.addRichSpan(com.mohamedrejeb.richeditor.model.RichSpanStyle.Code())
        s.removeRichSpan(com.mohamedrejeb.richeditor.model.RichSpanStyle.Code())
        s.clearRichSpans()

        s.history.undo()
        assertEquals("", s.annotatedString.text)
        assertFalse(
            s.history.canUndo,
            "collapsed-selection style toggles must not push history entries",
        )
        assertInternallyConsistent(s)
    }

    @Test
    fun collapsedSelectionToggleSealsCoalescingBoundary() {
        // type "x", toggle bold (collapsed, no push but seals pending group),
        // type "y". A single undo should remove only "y" — the toggle breaks the
        // coalesced typing burst, matching VS Code / Word behavior.
        val s = RichTextState()
        s.typeRaw("x")
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        s.typeRaw("y")

        s.history.undo()
        assertEquals("x", s.annotatedString.text)
        s.history.undo()
        assertEquals("", s.annotatedString.text)
        assertInternallyConsistent(s)
    }

    @Test
    fun nonCollapsedSelectionTogglePushesHistory() {
        // With a non-collapsed selection, toggle mutates tree and MUST push.
        val s = RichTextState()
        s.typeRaw("hello")
        s.selection = TextRange(0, 5)
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        // There are now two groups on the stack: "type hello" and "bold it".
        s.history.undo()        // undo bold — text unchanged
        assertEquals("hello", s.annotatedString.text)
        assertTrue(s.history.canUndo, "should still have the typing undo available")
        s.history.undo()        // undo typing
        assertEquals("", s.annotatedString.text)
        assertInternallyConsistent(s)
    }

    @Test
    fun collapsedRangeVariantsDoNotPush() {
        val s = RichTextState()
        s.typeRaw("hello")
        val collapsed = TextRange(2)
        s.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), collapsed)
        s.removeSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), collapsed)
        s.clearSpanStyles(collapsed)
        s.addRichSpan(com.mohamedrejeb.richeditor.model.RichSpanStyle.Code(), collapsed)
        s.removeRichSpan(com.mohamedrejeb.richeditor.model.RichSpanStyle.Code(), collapsed)
        s.clearRichSpans(collapsed)

        // Only one group on the stack (the typing of "hello").
        s.history.undo()
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
        assertInternallyConsistent(s)
    }

    @Test
    fun paragraphStyleToggleUndo() {
        val s = RichTextState()
        s.typeRaw("aligned")
        s.toggleParagraphStyle(
            androidx.compose.ui.text.ParagraphStyle(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        )
        assertInternallyConsistent(s)
        s.history.undo()
        assertInternallyConsistent(s)
        s.history.redo()
        assertInternallyConsistent(s)
    }

    /**
     * Asserts the state is internally consistent: the `textFieldValue.text` that the
     * BasicTextField sees must have the same length as the `annotatedString` the
     * visual transformation produces. When these diverge, Compose crashes with
     * OffsetMapping validation errors (the original reported bug).
     *
     * Also verifies the selection stays within bounds.
     */
    private fun assertInternallyConsistent(s: RichTextState) {
        val tfvLen = s.textFieldValue.text.length
        val annotLen = s.annotatedString.text.length
        assertEquals(
            tfvLen,
            annotLen,
            "textFieldValue.text.length ($tfvLen) must match annotatedString.text.length ($annotLen). " +
                "text='${s.textFieldValue.text.replace("\n", "\\n")}' annot='${s.annotatedString.text.replace("\n", "\\n")}'",
        )
        val sel = s.textFieldValue.selection
        assertTrue(sel.start in 0..tfvLen, "selection.start=${sel.start} out of [0,$tfvLen]")
        assertTrue(sel.end in 0..tfvLen, "selection.end=${sel.end} out of [0,$tfvLen]")
    }
}
