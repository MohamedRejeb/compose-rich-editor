package com.mohamedrejeb.richeditor.model

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
 * `deleteSurroundingText(0, 1)` + `commitText(" ")`) reaches the state as a
 * value whose net effect is unchanged text with the caret stepped across the
 * paragraph separator, so the caret jumped to the start of the next paragraph
 * and further typing landed there. Extension picks ("Thi" + tap "This") split
 * the same gesture into a word commit followed by the no-op step. Fixed by
 * materializing the space the IME believes it committed: the step is
 * recognized either by the active composition ending exactly at the boundary
 * being committed, or by immediately following an IME edit that ended at the
 * boundary; it is then transformed into a space insertion at the end of the
 * current paragraph. Plain caret navigation matches neither signal and is
 * untouched. The `setHtml` half of #779 is pinned in
 * `Issue779EmptyParagraphHtmlTest`.
 */
class Issue779ParagraphSeparatorImeTest {

    /** "This" / "" / "Signature" with the caret at the end of "This" (index 4). */
    private fun threeParagraphDoc(): RichTextState {
        val state = RichTextState()
        state.setHtml("<p>This</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(4)
        return state
    }

    /** Simulates the IME composing the word ending at the caret. */
    private fun composeWordEndingAtCaret(state: RichTextState) {
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = state.selection,
                composition = TextRange(0, state.selection.min),
            )
        )
    }

    @Test
    fun sameWordSuggestionPickAtParagraphEndStaysInParagraph() {
        val state = threeParagraphDoc()
        composeWordEndingAtCaret(state)
        // Gboard and Samsung same-word picks both coalesce to this net value:
        // unchanged text, composition committed, caret stepped over the separator.
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = TextRange(5),
                composition = null,
            )
        )
        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun caretStepWithoutCompositionIsPlainNavigation() {
        val state = threeParagraphDoc()
        state.onTextFieldValueChange(
            TextFieldValue(text = state.textFieldValue.text, selection = TextRange(5))
        )
        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepAfterPhysicalKeyEventIsPlainNavigationEvenDuringComposition() {
        // An ArrowRight press can also commit an active composition while stepping
        // the caret; a recent physical key event must disable the IME heuristic.
        val state = threeParagraphDoc()
        composeWordEndingAtCaret(state)
        state.notePhysicalKeyEvent()
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = TextRange(5),
                composition = null,
            )
        )
        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
    }

    @Test
    fun caretStepInsideParagraphDuringCompositionIsPlainNavigation() {
        // Committing a composition that does not end at a paragraph boundary must
        // not trigger the heuristic (the step is not over a separator).
        val state = threeParagraphDoc()
        state.selection = TextRange(2)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = TextRange(2),
                composition = TextRange(0, 2),
            )
        )
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = TextRange(3),
                composition = null,
            )
        )
        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun splitSuggestionPickWordCommitThenSeparatorStepStaysInParagraph() {
        // Extension pick ("Thi" -> tap "This" suggestion): Gboard delivers it as
        // two value updates. First the word commit, which also ends the
        // composition; then the trailing-space refresh, netting to a no-op caret
        // step over the separator. The step must still be recognized even though
        // the composition is already gone by then.
        val state = RichTextState()
        state.setHtml("<p>Thi</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        composeWordEndingAtCaret(state)
        // Value update 1: commitText("This") replacing the composition.
        val text = state.textFieldValue.text
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "This" + text.substring(3),
                selection = TextRange(4),
                composition = null,
            )
        )
        assertEquals("This\n\nSignature", state.toText())
        assertEquals(TextRange(4), state.selection)
        // Value update 2: setSelection(4, 5) + commitText(" ") nets to a step.
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.textFieldValue.text,
                selection = TextRange(5),
                composition = null,
            )
        )
        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun caretStepAfterEditElsewhereIsPlainNavigation() {
        // A recent IME edit that did not end at the boundary must not convert a
        // later separator step into a space commit.
        val state = threeParagraphDoc()
        val text = state.textFieldValue.text
        state.selection = TextRange(1)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text.substring(0, 1) + "x" + text.substring(1),
                selection = TextRange(2),
            )
        )
        assertEquals("Txhis\n\nSignature", state.toText())
        // Move the caret to the paragraph end, then step over the separator.
        state.onTextFieldValueChange(
            TextFieldValue(text = state.textFieldValue.text, selection = TextRange(5))
        )
        state.onTextFieldValueChange(
            TextFieldValue(text = state.textFieldValue.text, selection = TextRange(6))
        )
        assertEquals("Txhis\n\nSignature", state.toText())
        assertEquals(TextRange(6), state.selection)
    }

    @Test
    fun collapsedSpaceCommitAtBoundaryAppendsToParagraph() {
        // The reduced variant from the report: a plain space insert exactly at the
        // boundary position. Worked before the fix; pin it.
        val state = threeParagraphDoc()
        val text = state.textFieldValue.text
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text.substring(0, 4) + " " + text.substring(4),
                selection = TextRange(5),
            )
        )
        assertEquals("This \n\nSignature", state.toText())
        assertEquals(TextRange(5), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }

    @Test
    fun extendingSuggestionPickAtParagraphEndStaysInParagraph() {
        // Samsung extending pick ("Tes" -> "Testing"): net insertion with the caret
        // one past the inserted text. Covered by the existing Android-suggestion
        // heuristic; pin the resulting shape.
        val state = RichTextState()
        state.setHtml("<p>Tes</p><p><br></p><p>Signature</p>")
        state.selection = TextRange(3)
        composeWordEndingAtCaret(state)
        val text = state.textFieldValue.text
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "Testing" + text.substring(3),
                selection = TextRange(8),
                composition = null,
            )
        )
        assertEquals("Testing \n\nSignature", state.toText())
        assertEquals(TextRange(8), state.selection)
        assertEquals(3, state.richParagraphList.size)
    }
}
