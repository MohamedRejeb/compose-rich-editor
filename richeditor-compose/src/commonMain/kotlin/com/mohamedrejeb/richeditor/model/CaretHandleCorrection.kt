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

    val paragraphEnd = caret.start - 1
    val lastLine = layout.getLineForOffset(paragraphEnd)
    val cameFromLastLine =
        previous.start <= paragraphEnd && layout.getLineForOffset(previous.start) == lastLine
    val handleIsPastParagraphEnd =
        if (cameFromLastLine) {
            previous.start > layout.getLineStart(lastLine)
        } else {
            val startLine = layout.getLineForOffset(caret.start)
            val previousLine = layout.getLineForOffset(previous.start)
            val headingForStart =
                previous.start == caret.start + 1 ||
                    (beforePrevious != null &&
                        previousLine == startLine &&
                        layout.getLineForOffset(beforePrevious) == startLine &&
                        previous.start < beforePrevious)
            val canBeRightOfParagraphEnd =
                previous.start == layout.getLineEnd(previousLine, visibleEnd = true) ||
                    layout.advanceInLine(previous.start) > layout.advanceInLine(paragraphEnd)
            !headingForStart && canBeRightOfParagraphEnd
        }
    if (!handleIsPastParagraphEnd) return false

    lastCaretHandleStep = previous.start to paragraphEnd
    setTextFieldStateFromValue(text = text, selection = TextRange(paragraphEnd))
    return true
}

/** How far [offset] is from the start of its line, in either text direction. */
private fun TextLayoutResult.advanceInLine(offset: Int): Float {
    val lineStart = getLineStart(getLineForOffset(offset))
    return abs(
        getHorizontalPosition(offset, usePrimaryDirection = true) -
            getHorizontalPosition(lineStart, usePrimaryDirection = true)
    )
}
