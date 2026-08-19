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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateHistoryGroupTest {

    private val bold = SpanStyle(fontWeight = FontWeight.Bold)
    private val italic = SpanStyle(fontStyle = FontStyle.Italic)
    private val underline = SpanStyle(textDecoration = TextDecoration.Underline)

    private fun RichTextState.typeRaw(text: String) {
        val current = this.annotatedString.text
        val sel = this.selection.min
        val newText = current.substring(0, sel) + text + current.substring(sel)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(sel + text.length)))
    }

    @Test
    fun disjointSpanStylesInOneGroupUndoAndRedoAsOneStep() {
        val s = RichTextState()
        s.typeRaw("one two three")

        s.history.group {
            s.addSpanStyle(bold, TextRange(0, 3))
            s.addSpanStyle(bold, TextRange(8, 13))
        }
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(0, 3)).fontWeight)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(8, 13)).fontWeight)

        // One undo reverts both ranges simultaneously.
        assertTrue(s.history.undo())
        assertEquals("one two three", s.annotatedString.text)
        assertNull(s.getSpanStyle(TextRange(0, 3)).fontWeight)
        assertNull(s.getSpanStyle(TextRange(8, 13)).fontWeight)

        // One redo reapplies both ranges simultaneously.
        assertTrue(s.history.redo())
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(0, 3)).fontWeight)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(8, 13)).fontWeight)

        // Exactly one entry for the group plus one for the typed text.
        assertTrue(s.history.undo())
        assertTrue(s.history.undo())
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun groupMixingTextReplacementAndFormattingUndoesAsOneStep() {
        val s = RichTextState()
        s.typeRaw("hello world")

        s.history.group {
            s.replaceTextRange(TextRange(0, 5), "goodbye")
            s.addSpanStyle(italic, TextRange(0, 7))
        }
        assertEquals("goodbye world", s.annotatedString.text)
        assertEquals(FontStyle.Italic, s.getSpanStyle(TextRange(0, 7)).fontStyle)

        assertTrue(s.history.undo())
        assertEquals("hello world", s.annotatedString.text)
        assertNull(s.getSpanStyle(TextRange(0, 5)).fontStyle)

        assertTrue(s.history.redo())
        assertEquals("goodbye world", s.annotatedString.text)
        assertEquals(FontStyle.Italic, s.getSpanStyle(TextRange(0, 7)).fontStyle)
    }

    @Test
    fun nestedGroupsJoinTheOutermostGroup() {
        val s = RichTextState()
        s.typeRaw("abc")

        s.history.group {
            s.addSpanStyle(bold, TextRange(0, 1))
            s.history.group {
                s.addSpanStyle(italic, TextRange(1, 2))
            }
            s.addSpanStyle(underline, TextRange(2, 3))
        }

        assertTrue(s.history.undo())
        assertNull(s.getSpanStyle(TextRange(0, 1)).fontWeight)
        assertNull(s.getSpanStyle(TextRange(1, 2)).fontStyle)
        assertNull(s.getSpanStyle(TextRange(2, 3)).textDecoration)

        // Only the typed text remains on the stack.
        assertTrue(s.history.undo())
        assertFalse(s.history.canUndo)
    }

    @Test
    fun emptyGroupAddsNoEntryAndReturnsBlockResult() {
        val s = RichTextState()
        s.typeRaw("abc")

        val result = s.history.group { 42 }
        assertEquals(42, result)

        assertTrue(s.history.undo())
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun commitsAfterGroupFormFreshEntries() {
        val s = RichTextState()
        s.typeRaw("abc")

        s.history.group {
            s.addSpanStyle(bold, TextRange(0, 3))
        }
        s.typeRaw("d")
        assertEquals("abcd", s.annotatedString.text)

        // Typing after the group is its own entry; the group stays intact.
        assertTrue(s.history.undo())
        assertEquals("abc", s.annotatedString.text)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(0, 3)).fontWeight)

        assertTrue(s.history.undo())
        assertNull(s.getSpanStyle(TextRange(0, 3)).fontWeight)

        assertTrue(s.history.undo())
        assertEquals("", s.annotatedString.text)
        assertFalse(s.history.canUndo)
    }

    @Test
    fun groupSealsPendingTypingCoalescing() {
        val s = RichTextState()
        s.typeRaw("ab")
        s.history.group { }
        s.typeRaw("c")
        assertEquals("abc", s.annotatedString.text)

        // Without the seal "c" would coalesce into the "ab" burst.
        assertTrue(s.history.undo())
        assertEquals("ab", s.annotatedString.text)
        assertTrue(s.history.undo())
        assertEquals("", s.annotatedString.text)
    }

    @Test
    fun throwingGroupKeepsCommittedWorkAsOneEntryAndRethrows() {
        val s = RichTextState()
        s.typeRaw("abc")

        val thrown = assertFailsWith<IllegalStateException> {
            s.history.group {
                s.addSpanStyle(bold, TextRange(0, 1))
                s.addSpanStyle(bold, TextRange(2, 3))
                error("boom")
            }
        }
        assertEquals("boom", thrown.message)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(0, 1)).fontWeight)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(2, 3)).fontWeight)

        assertTrue(s.history.undo())
        assertNull(s.getSpanStyle(TextRange(0, 1)).fontWeight)
        assertNull(s.getSpanStyle(TextRange(2, 3)).fontWeight)
        assertEquals("abc", s.annotatedString.text)
    }

    @Test
    fun groupWithRichSpanAndRemovalUndoesAsOneStep() {
        val s = RichTextState()
        s.typeRaw("link here")

        s.history.group {
            s.addRichSpan(RichSpanStyle.Link(url = "https://example.com"), TextRange(0, 4))
            s.addSpanStyle(bold, TextRange(5, 9))
        }
        assertTrue(s.getRichSpanStyle(TextRange(0, 4)) is RichSpanStyle.Link)
        assertEquals(FontWeight.Bold, s.getSpanStyle(TextRange(5, 9)).fontWeight)

        assertTrue(s.history.undo())
        assertTrue(s.getRichSpanStyle(TextRange(0, 4)) is RichSpanStyle.Default)
        assertNull(s.getSpanStyle(TextRange(5, 9)).fontWeight)

        assertTrue(s.history.undo())
        assertFalse(s.history.canUndo)
    }
}
