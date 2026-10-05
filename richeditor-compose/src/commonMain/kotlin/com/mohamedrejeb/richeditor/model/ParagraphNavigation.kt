package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText

// Paragraph-wise selection and navigation answered from the paragraph list. The framework
// finds paragraph boundaries by searching the visible text for newlines, and the editor's
// text has none (paragraphs are joined by a one-character separator), so left alone a triple
// click selects the whole document and the paragraph keys jump to its ends.

/**
 * The text of the paragraph that contains [offset]: from after its list prefix to before its
 * separator. A boundary offset (a separator's position) belongs to the paragraph it ends.
 */
internal fun RichTextState.paragraphTextRangeAt(offset: Int): TextRange? {
    val index = paragraphIndexAt(offset) ?: return null
    return paragraphTextRange(index)
}

/**
 * Where the previous or next paragraph key moves the caret, mirroring the framework: to the
 * current paragraph's own start or end, and from there to the neighbouring paragraph's.
 */
internal fun RichTextState.paragraphNavigationTarget(selection: TextRange, up: Boolean): Int? {
    if (up) {
        val index = paragraphIndexAt(selection.min) ?: return null
        val start = paragraphTextRange(index).start
        return if (start == selection.min && index > 0) paragraphTextRange(index - 1).start else start
    }
    val index = paragraphIndexAt(selection.max) ?: return null
    val end = paragraphTextRange(index).end
    return if (end == selection.max && index < richParagraphList.lastIndex)
        paragraphTextRange(index + 1).end
    else
        end
}

/**
 * Replaces the whole-document selection a triple click produces with the pressed paragraph.
 * Runs inside the InputTransformation, so the corrected selection is the only one committed.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun RichTextState.correctTripleClickSelection(buffer: TextFieldBuffer) {
    if (!tripleClickArmed) return
    tripleClickArmed = false

    if (singleParagraphMode || buffer.changes.changeCount != 0) return
    val selection = buffer.selection
    if (selection.min != 0 || selection.max != buffer.length) return

    val press = pressForCaretCorrection() ?: return
    val layout = textLayoutResult ?: return
    if (!layout.isForModelText(buffer.length)) return

    var offset = layout.getOffsetForPosition(press).coerceAtMost(buffer.length)
    // A press past the end of a line reports the next paragraph's start (see
    // [correctPressCaret]); the pressed line decides.
    val pressedLine = layout.getLineForVerticalPosition(
        press.y.coerceIn(0f, layout.size.height.toFloat())
    )
    if (isLaterParagraphStart(offset) && layout.getLineForOffset(offset) > pressedLine) offset -= 1

    val paragraph = paragraphTextRangeAt(offset) ?: return
    if (paragraph != selection) buffer.selection = paragraph
}

private fun RichTextState.paragraphIndexAt(offset: Int): Int? {
    richParagraphList.forEachIndexed { index, _ ->
        if (offset <= paragraphTextRange(index).end) return index
    }
    return null
}

private fun RichTextState.paragraphTextRange(index: Int): TextRange {
    val paragraph = richParagraphList[index]
    val start = paragraph.type.startRichSpan.textRange.min + paragraph.type.startText.length
    val end =
        if (index == richParagraphList.lastIndex) textFieldValue.text.length
        else richParagraphList[index + 1].type.startRichSpan.textRange.min - 1
    return TextRange(start, end)
}
