package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateUndoRedoTest {

    private fun RichTextState.typeRaw(text: String) {
        val current = this.annotatedString.text
        val sel = this.selection.min
        val newText = current.substring(0, sel) + text + current.substring(sel)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(sel + text.length)))
    }

    @Test
    fun typingThenUndoRemovesTypedText() {
        val s = RichTextState()
        s.typeRaw("hello")
        assertEquals("hello", s.annotatedString.text)
        assertTrue(s.history.canUndo)
        s.history.undo()
        assertEquals("", s.annotatedString.text)
    }

    @Test
    fun collapsedSelectionToggleDoesNotPushUndoStep() {
        // Toggling a style while the caret is collapsed only updates the staged-style
        // bag (i.e. what the NEXT typed character gets). It should NOT create a phantom
        // undo step - undoing a "stage bold" would be confusing since it has no visible
        // effect. The toggle DOES seal the pending typing group so the burst before
        // and the burst after the toggle are separate undo steps (matches VS Code / Word).
        val s = RichTextState()
        s.typeRaw("x")
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) // caret collapsed
        s.typeRaw("y")
        assertEquals("xy", s.annotatedString.text)
        // Two groups: "type x" and "type y" (bold toggle pushed nothing but sealed).
        s.history.undo()
        assertEquals("x", s.annotatedString.text)
        s.history.undo()
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun nonCollapsedSelectionToggleIsItsOwnUndoStep() {
        // With a non-collapsed selection, toggling a style mutates existing text and
        // MUST be its own undo step.
        val s = RichTextState()
        s.typeRaw("hello")
        s.selection = TextRange(0, 5)
        s.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        assertTrue(s.history.canUndo)
        s.history.undo() // undoes the bold (text stays "hello")
        assertEquals("hello", s.annotatedString.text)
        s.history.undo() // undoes the "type hello"
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun addLinkThenUndoRemovesLink() {
        val s = RichTextState()
        s.typeRaw("visit ")
        s.addLink(text = "site", url = "https://example.com")
        assertTrue("site" in s.annotatedString.text)
        s.history.undo()
        assertFalse("site" in s.annotatedString.text)
    }

    @Test
    fun setHtmlClearsHistory() {
        val s = RichTextState()
        s.typeRaw("abc")
        assertTrue(s.history.canUndo)
        s.setHtml("<p>replaced</p>")
        assertFalse(s.history.canUndo)
        assertFalse(s.history.canRedo)
    }

    @Test
    fun undoThenRedoReproducesOriginalState() {
        val s = RichTextState()
        s.typeRaw("hello")
        val textAfterType = s.annotatedString.text
        s.history.undo()
        assertEquals("", s.annotatedString.text)
        s.history.redo()
        assertEquals(textAfterType, s.annotatedString.text)
    }

    @Test
    fun historyLimitCapsUndoStack() {
        val s = RichTextState(historyLimit = 2)
        // 3 paragraph-style toggles = 3 undo groups; limit keeps only last 2.
        // We use ordered-list toggle (operates on the caret's paragraph even when
        // the selection is collapsed, so it always pushes a history entry).
        s.toggleOrderedList()
        s.toggleUnorderedList()
        s.toggleOrderedList()
        assertTrue(s.history.undo())
        assertTrue(s.history.undo())
        assertFalse(s.history.undo())
    }

    @Test
    fun orderedListToggleThenUndoRestoresParagraphType() {
        val s = RichTextState()
        s.typeRaw("item")
        assertFalse(s.annotatedString.text.startsWith("1."))
        s.toggleOrderedList()
        assertTrue(s.annotatedString.text.startsWith("1."))
        s.history.undo()
        assertFalse(s.annotatedString.text.startsWith("1."))
    }

    @Test
    fun commitAfterUndoClearsRedo() {
        val s = RichTextState()
        s.typeRaw("a")
        s.history.undo()
        assertTrue(s.history.canRedo)
        s.typeRaw("b")
        assertFalse(s.history.canRedo)
    }

    @Test
    fun typingAfterLineBreakIsNewUndoGroup() {
        val s = RichTextState()
        s.typeRaw("hello")
        s.typeRaw("\n")
        s.typeRaw("world")
        // One undo removes "world" only, not "hello\n"
        s.history.undo()
        assertTrue(s.annotatedString.text.startsWith("hello"))
        assertFalse("world" in s.annotatedString.text)
    }
}
