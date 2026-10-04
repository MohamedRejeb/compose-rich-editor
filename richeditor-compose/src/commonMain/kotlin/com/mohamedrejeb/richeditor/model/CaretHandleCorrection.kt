package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange

/**
 * Puts a caret that a caret handle step moved onto the next paragraph's start back on the end
 * of the paragraph it left, and reports whether it did.
 *
 * The handle lives in a popup, so the editor never sees its pointer, and a hit in the empty
 * space after a paragraph's last line reports the next paragraph's first offset (see
 * [correctPressCaret]). A keyboard cursor step from a paragraph end has the same offsets, so
 * this runs only when the text field reports a handle move, which it does through the haptic
 * it plays after every handle step and never for the keyboard.
 *
 * The step is held when the caret came from the last line of the previous paragraph and not
 * from that line's start: a handle moved straight down along the left edge, or down from an
 * empty paragraph, means the next line.
 */
internal fun RichTextState.holdCaretHandleOnParagraphEnd(): Boolean {
    val caret = textFieldState.selection
    val previous = selectionBeforeUserSelectionChange
    if (singleParagraphMode || !caret.collapsed || !previous.collapsed) return false
    if (!isLaterParagraphStart(caret.start)) return false

    val paragraphEnd = caret.start - 1
    if (previous.start > paragraphEnd) return false

    val text = textFieldState.text.toString()
    val layout = textLayoutResult ?: return false
    if (layout.layoutInput.text.length != text.length) return false

    val lastLine = layout.getLineForOffset(paragraphEnd)
    if (layout.getLineForOffset(previous.start) != lastLine) return false
    if (previous.start <= layout.getLineStart(lastLine)) return false

    setTextFieldStateFromValue(text = text, selection = TextRange(paragraphEnd))
    return true
}
