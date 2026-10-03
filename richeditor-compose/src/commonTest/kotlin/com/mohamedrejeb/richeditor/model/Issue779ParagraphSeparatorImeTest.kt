package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression pins for the IME half of #779: the "trailing space refresh" a
 * suggestion pick performs at a paragraph end.
 *
 * Tapping the keyboard suggestion for a just-typed word at a paragraph end
 * (Gboard: `setSelection(n, n+1)` + `commitText(" ")`, Samsung:
 * `deleteSurroundingText(0, 1)` + `commitText(" ")`) nets to the caret stepping
 * across the paragraph separator, so the caret jumped to the start of the next
 * paragraph and further typing landed there. Fixed by materializing the space
 * the IME believes it committed.
 *
 * Under the BTF2 pipeline the word commit and the caret step can arrive together
 * or split across two channels: the word commit is a buffer change replayed by
 * `applyChangeList`, or, for a split pick, a whole-value change through the
 * legacy `onTextFieldValueChange` bridge, while the caret step is always a
 * selection change reported by the editor's selection observer. The
 * recognition therefore lives in `handleSelectionChanged`: a
 * collapsed one-character step over a paragraph separator that either commits a
 * composition ending at that boundary or immediately follows an IME edit that
 * ended there. Plain caret navigation matches neither signal and is untouched.
 * The `setHtml` half of #779 is pinned in `Issue779EmptyParagraphHtmlTest`.
 */
class Issue779ParagraphSeparatorImeTest {

    /** "This" / "" / "Signature" with the caret at the end of "This" (index 4). */
    private fun threeParagraphDoc(): RichTextState {
        val state = RichTextState()
        state.setHtml("<p>This</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(4)
        return state
    }

    /**
     * Mimics an IME edit reaching the state the way `BasicRichTextEditor` delivers it:
     * a buffer change replayed by [applyChangeList], followed by the reconciliation the
     * `InputTransformation` tail performs.
     */
    private fun RichTextState.imeEdit(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        setTextFieldStateFromValue(text = annotatedString.text, selection = textFieldValue.selection)
        pendingSelectionDuringSync = null
    }

    /**
     * Mimics BTF2 moving the caret on its own: the buffer's selection changes and the
     * editor's selection observer reports it. Deliberately not the `selection` setter,
     * which is the programmatic path and never carries an IME gesture.
     */
    private fun RichTextState.platformCaretStep(to: Int) {
        setTextFieldStateFromValue(text = textFieldState.text.toString(), selection = TextRange(to))
        handleSelectionChanged(TextRange(to), fromGestureObserver = true)
    }

    @Test
    fun bufferChannelWordCommitThenSeparatorStepStaysInParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        // The IME commits the word it was composing; the pick's trailing-space
        // refresh then arrives as a bare caret step over the separator.
        state.imeEdit { replace(3, 3, "s") }
        assertEquals("This\n\nSignature", state.toText())

        state.platformCaretStep(5)

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    /**
     * The composition branch of the check. A composition can end without any text or caret
     * change, which runs neither the InputTransformation nor the selection observer, so the
     * editor reports composition changes through [RichTextState.handleCompositionChanged].
     */
    private fun RichTextState.composing(range: TextRange?) {
        handleCompositionChanged(range)
    }

    @Test
    fun caretStepLongAfterTheCompositionEndedIsPlainNavigation() {
        // Type "s" at the paragraph end with the word composing, pause, let the keyboard
        // commit the word on its own, pause again, then step the caret with the keyboard's
        // cursor control. That step is navigation, not a suggestion pick's space refresh.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }
        state.composing(TextRange(0, 4))
        Thread.sleep(WINDOW_LAPSE_MS)
        state.composing(null)
        Thread.sleep(WINDOW_LAPSE_MS)

        state.platformCaretStep(5)

        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun separatorStepRightAfterTheCompositionEndedStaysInParagraph() {
        // The same pick delivered as two IME calls instead of one batch: the commit is
        // observed on its own, the caret step follows within the frame.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }
        state.composing(TextRange(0, 4))
        Thread.sleep(WINDOW_LAPSE_MS)
        state.composing(null)

        state.platformCaretStep(5)

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun separatorStepThatEndsTheCompositionInTheSameBatchStaysInParagraph() {
        // The single-batch pick: the composition is still the last thing observed when the
        // step arrives, and the buffer's composition is already gone.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }
        state.composing(TextRange(0, 4))
        Thread.sleep(WINDOW_LAPSE_MS)

        state.platformCaretStep(5)

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun caretStepWithoutARecentImeEditIsPlainNavigation() {
        val state = threeParagraphDoc()

        state.platformCaretStep(5)

        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepAfterPhysicalKeyEventIsPlainNavigation() {
        // An ArrowRight press can also commit an active composition while stepping
        // the caret; a recent physical key event must disable the IME heuristic.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }
        state.notePhysicalKeyEvent()

        state.platformCaretStep(5)

        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepAfterPointerPressIsPlainNavigation() {
        // A tap on the first character of the next paragraph right after an IME edit at the
        // paragraph end steps the caret across the separator, the shape of a pick's space
        // refresh; the press the editor saw just before marks it as the user's.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }
        state.pointerDown()

        state.platformCaretStep(5)

        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepLongAfterPointerPressIsStillASpaceRefresh() {
        // The press memory is short: a pick that follows a press by more than the window
        // is recognized as before.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.pointerDown()
        Thread.sleep(WINDOW_LAPSE_MS)
        state.imeEdit { replace(3, 3, "s") }

        state.platformCaretStep(5)

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepInsideParagraphIsPlainNavigation() {
        // An IME edit that does not end at a paragraph boundary must not turn the
        // following step into a space commit (the step is not over a separator).
        val state = threeParagraphDoc()
        state.selection = TextRange(2)
        state.imeEdit { replace(2, 2, "s") }
        assertEquals("Thsis\n\nSignature", state.toText())

        state.platformCaretStep(4)

        assertEquals("Thsis\n\nSignature", state.toText())
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun splitSuggestionPickWordCommitThenSeparatorStepStaysInParagraph() {
        // Extension pick ("Thi" -> tap "This" suggestion) whose word-commit half
        // arrives through the legacy value bridge instead of the buffer: the bridge
        // must arm the follow-up window the same way.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        val text = state.textFieldValue.text
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "This" + text.substring(3),
                selection = TextRange(4),
            )
        )
        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(4), state.selection)

        state.platformCaretStep(5)

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun caretStepAfterEditElsewhereIsPlainNavigation() {
        // A recent IME edit that did not end at the boundary must not convert a
        // later separator step into a space commit.
        val state = threeParagraphDoc()
        state.selection = TextRange(1)
        state.imeEdit { replace(1, 1, "x") }
        assertEquals("Txhis\n\nSignature", state.toText())

        // Move the caret to the paragraph end, then step over the separator.
        state.platformCaretStep(5)
        state.platformCaretStep(6)

        assertEquals("Txhis\n\nSignature", state.toText())
        assertEquals(TextRange(6), state.selection)
    }

    @Test
    fun programmaticCaretStepAtBoundaryIsNotAnImeRefresh() {
        // The public selection setter is not an IME gesture, so the same shape must
        // move the caret without materializing anything.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        state.imeEdit { replace(3, 3, "s") }

        state.selection = TextRange(5)

        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun collapsedSpaceCommitAtBoundaryAppendsToParagraph() {
        // The reduced variant from the report: a plain space insert exactly at the
        // boundary position, which must join the current paragraph.
        val state = threeParagraphDoc()

        state.imeEdit { replace(4, 4, " ") }

        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun extendingSuggestionPickAtParagraphEndStaysInParagraph() {
        // Samsung extending pick ("Tes" -> "Testing"): the IME inserts the missing
        // characters and reports a caret one past them, believing it also committed
        // the trailing space that the separator swallowed.
        val state = RichTextState()
        state.setHtml("<p>Tes</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)

        state.imeEdit { replace(3, 3, "ting") }
        state.platformCaretStep(8)

        assertEquals("Testing \n\nSignature", state.toText())
        assertEquals(TextRange(8), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    /** A pointer press as the editor's passive pointer observer reports it. */
    private fun RichTextState.pointerDown() {
        onSelectionGesturePointerDown(
            position = Offset.Zero,
            uptimeMillis = 0L,
            doubleTapTimeoutMillis = 300L,
            slop = 8f,
            shiftPressed = false,
        )
    }

    private companion object {
        /** Longer than the 300 ms follow-up window the check keeps after an IME edit. */
        const val WINDOW_LAPSE_MS = 400L
    }
}
