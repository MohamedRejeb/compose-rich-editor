package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.math.abs

/**
 * Puts a caret that a caret handle step moved onto the next paragraph's start back on the end
 * of the paragraph above it, when that is where the handle is, and reports whether it did.
 *
 * The handle lives in a popup, so the editor never sees its pointer, and a hit in the empty
 * space after a paragraph's last line reports the next paragraph's first offset (see
 * [correctPressCaret]). A keyboard cursor step from a paragraph end has the same offsets, so
 * this runs only when the text field reports a handle move, which it does through the haptic
 * it plays after every handle step and never for the keyboard.
 *
 * Without the pointer, where the caret came from decides:
 * - From the paragraph's own last line: held, unless it came from that line's start. A handle
 *   moved straight down along the leading edge, or down from an empty paragraph, means the
 *   next line.
 * - From any other line: held when the handle can be right of the paragraph's end, which is
 *   when the caret was further along its line than that end is, or rested on its line's end
 *   (the handle may then be anywhere past it). A handle moved vertically from there lands in
 *   the empty space. Not held when the caret was travelling back along the next paragraph's
 *   first line, by a single step or over the last two steps: that is heading for its start.
 */
internal fun RichTextState.holdCaretHandleOnParagraphEnd(): Boolean {
    val caret = textFieldState.selection
    val previous = selectionBeforeUserSelectionChange

    // Where the step before this one came from, when the two are consecutive.
    val beforePrevious = lastCaretHandleStep?.takeIf { it.second == previous.start }?.first
    lastCaretHandleStep = if (caret.collapsed && previous.collapsed) previous.start to caret.start else null

    if (singleParagraphMode || !caret.collapsed || !previous.collapsed) return false
    if (!isLaterParagraphStart(caret.start)) return false

    val text = textFieldState.text.toString()
    val layout = textLayoutResult ?: return false
    if (layout.layoutInput.text.length != text.length) return false

    val step = CaretHandleStep(from = previous.start, to = caret.start, before = beforePrevious)
    if (!layout.handleIsPastParagraphEnd(step)) return false

    val paragraphEnd = caret.start - 1
    lastCaretHandleStep = previous.start to paragraphEnd
    setTextFieldStateFromValue(text = text, selection = TextRange(paragraphEnd))
    return true
}

/** A caret handle step onto a paragraph start [to], from [from], which was reached from [before]. */
private class CaretHandleStep(val from: Int, val to: Int, val before: Int?)

private fun TextLayoutResult.handleIsPastParagraphEnd(step: CaretHandleStep): Boolean {
    val paragraphEnd = step.to - 1
    val lastLine = getLineForOffset(paragraphEnd)
    if (step.from <= paragraphEnd && getLineForOffset(step.from) == lastLine)
        return step.from > getLineStart(lastLine)

    return !isHeadingForStart(step) && canBeRightOf(paragraphEnd, step.from)
}

/** Whether the caret was travelling back along the paragraph's first line towards its start. */
private fun TextLayoutResult.isHeadingForStart(step: CaretHandleStep): Boolean {
    if (step.from == step.to + 1) return true
    val before = step.before ?: return false
    val startLine = getLineForOffset(step.to)
    return getLineForOffset(step.from) == startLine &&
        getLineForOffset(before) == startLine &&
        step.from < before
}

/** Whether a handle whose caret is at [offset] can be further along than [paragraphEnd] is. */
private fun TextLayoutResult.canBeRightOf(paragraphEnd: Int, offset: Int): Boolean =
    offset == getLineEnd(getLineForOffset(offset), visibleEnd = true) ||
        advanceInLine(offset) > advanceInLine(paragraphEnd)

/** How far [offset] is from the start of its line, in either text direction. */
private fun TextLayoutResult.advanceInLine(offset: Int): Float {
    val lineStart = getLineStart(getLineForOffset(offset))
    return abs(
        getHorizontalPosition(offset, usePrimaryDirection = true) -
            getHorizontalPosition(lineStart, usePrimaryDirection = true)
    )
}
