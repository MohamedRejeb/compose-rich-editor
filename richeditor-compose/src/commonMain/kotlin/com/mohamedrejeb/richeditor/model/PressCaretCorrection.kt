package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange

/**
 * Moves a caret that a press past the end of a paragraph put on the next paragraph back onto
 * the pressed line.
 *
 * Every paragraph is laid out on its own, and the platform hit test lets the caret sit after
 * the final character only on the last line of a layout. With one layout per paragraph that
 * is every paragraph's last line, so a press in the empty space after it reports the
 * paragraph's end offset. That offset is also the next paragraph's first offset, and it
 * renders there.
 *
 * Runs inside the InputTransformation, so the corrected caret is the only one committed.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun RichTextState.correctPressCaret(buffer: TextFieldBuffer) {
    pressCorrectedCaret = null

    val press = pressForCaretCorrection() ?: return
    if (singleParagraphMode || buffer.changes.changeCount != 0) return

    val caret = buffer.selection
    if (!caret.collapsed || !isLaterParagraphStart(caret.start)) return

    val layout = textLayoutResult ?: return
    if (!layout.isForModelText(buffer.length)) return

    val pressedLine = layout.getLineForVerticalPosition(
        press.y.coerceIn(0f, layout.size.height.toFloat())
    )
    if (layout.getLineForOffset(caret.start) <= pressedLine) return

    // One back is the separator's own position, which renders at the end of the pressed line.
    buffer.selection = TextRange(caret.start - 1)
    pressCorrectedCaret = caret.start - 1
}
