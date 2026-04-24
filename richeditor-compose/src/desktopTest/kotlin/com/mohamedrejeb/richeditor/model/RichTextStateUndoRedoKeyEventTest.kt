package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class, InternalComposeUiApi::class)
class RichTextStateUndoRedoKeyEventTest {

    private fun RichTextState.typeRaw(text: String) {
        val current = this.annotatedString.text
        val sel = this.selection.min
        val newText = current.substring(0, sel) + text + current.substring(sel)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(sel + text.length)))
    }

    private fun ctrlZ(shift: Boolean = false): KeyEvent = KeyEvent(
        key = Key.Z,
        type = KeyEventType.KeyDown,
        isCtrlPressed = true,
        isShiftPressed = shift,
    )

    private fun metaZ(shift: Boolean = false): KeyEvent = KeyEvent(
        key = Key.Z,
        type = KeyEventType.KeyDown,
        isMetaPressed = true,
        isShiftPressed = shift,
    )

    @Test
    fun ctrlZTriggersUndo() {
        val s = RichTextState()
        s.typeRaw("hello")
        assertTrue(s.onPreviewKeyEvent(ctrlZ()))
        assertEquals("", s.annotatedString.text)
    }

    @Test
    fun cmdZTriggersUndo() {
        val s = RichTextState()
        s.typeRaw("hello")
        assertTrue(s.onPreviewKeyEvent(metaZ()))
        assertEquals("", s.annotatedString.text)
    }

    @Test
    fun ctrlShiftZTriggersRedo() {
        val s = RichTextState()
        s.typeRaw("hello")
        s.history.undo()
        assertTrue(s.onPreviewKeyEvent(ctrlZ(shift = true)))
        assertEquals("hello", s.annotatedString.text)
    }

    @Test
    fun cmdShiftZTriggersRedo() {
        val s = RichTextState()
        s.typeRaw("hello")
        s.history.undo()
        assertTrue(s.onPreviewKeyEvent(metaZ(shift = true)))
        assertEquals("hello", s.annotatedString.text)
    }

    @Test
    fun shortcutConsumedEvenWhenStackEmpty() {
        val s = RichTextState()
        // Shortcut is consumed even with nothing to undo so BasicTextField's native
        // undo can't run against plain-text state.
        assertTrue(s.onPreviewKeyEvent(ctrlZ()))
        assertEquals("", s.annotatedString.text)
    }

    @Test
    fun shortcutDisabledWhenSuppressed() {
        val s = RichTextState()
        s.typeRaw("x")
        s.suppressUndoShortcuts = true
        assertFalse(s.onPreviewKeyEvent(ctrlZ()))
        assertEquals("x", s.annotatedString.text)
    }
}
