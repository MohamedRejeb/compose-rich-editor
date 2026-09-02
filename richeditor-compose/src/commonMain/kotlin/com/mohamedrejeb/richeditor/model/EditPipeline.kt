package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.history.CommitTrigger

internal data class InputDelta(val originalRange: TextRange, val newText: String)

/**
 * Classifies input deltas into a CommitTrigger for history recording. postEditCaret is the
 * buffer's selection.min read before the apply loop; the coalescer compares consecutive
 * caret values to merge typing bursts into one undo group.
 */
private fun classifyInputDeltas(deltas: List<InputDelta>, postEditCaret: Int): CommitTrigger? {
    if (deltas.isEmpty()) return null
    val totalInserted = deltas.sumOf { it.newText.length }
    val totalDeleted = deltas.sumOf { it.originalRange.max - it.originalRange.min }
    val insertedText = deltas.joinToString("") { it.newText }
    // Checked before the net-direction comparisons: Enter over a non-collapsed selection
    // deletes more than it inserts, but it must still start its own undo group.
    if (insertedText.contains('\n')) return CommitTrigger.LineBreak
    return when {
        totalInserted > totalDeleted -> CommitTrigger.Typing(addedText = insertedText, caret = postEditCaret)
        totalDeleted > totalInserted -> CommitTrigger.Delete(caret = postEditCaret)
        totalInserted == 0 -> null
        // Same net length, non-empty: a same-length replacement (autocorrect rewrite),
        // matching classifyTextChange's Structural case. Its own undo group.
        else -> CommitTrigger.Structural
    }
}

/**
 * Walks the buffer's ChangeList and replays each delta through applyChange. The buffer
 * arrives with the user's edit already applied; this function does not mutate the buffer.
 * Deltas are applied in original-text order, each subsequent range shifted by the
 * cumulative length difference of prior deltas.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun RichTextState.applyChangeList(buffer: TextFieldBuffer) {
    val changes = buffer.changes
    val changeCount = changes.changeCount
    if (changeCount == 0) return

    val deltas = (0 until changeCount).map { i ->
        val newRange = changes.getRange(i)
        val originalRange = changes.getOriginalRange(i)
        InputDelta(
            originalRange = originalRange,
            newText = buffer.asCharSequence().substring(newRange.min, newRange.max),
        )
    }.sortedBy { it.originalRange.min }

    // Paste recognition on the delta shape: one delta whose inserted text matches the
    // clipboard's stashed plain text is the paste the clipboard manager announced.
    val pendingHtml = pendingClipboardHtml.takeIf { config.richClipboardEnabled }
    val expectedPlain = pendingClipboardPlainText
    if (pendingHtml != null && expectedPlain != null) {
        val pasteDelta = deltas.singleOrNull()?.takeIf {
            it.newText.normalizeNewlinesForPaste() == expectedPlain.normalizeNewlinesForPaste()
        }
        if (pasteDelta != null) {
            val previous = skipTextFieldStateSync
            skipTextFieldStateSync = true
            try {
                recordHistoryForInput(CommitTrigger.Paste) {
                    selection = TextRange(pasteDelta.originalRange.min, pasteDelta.originalRange.max)
                    handleRecognizedPaste(pendingHtml)
                }
            } finally {
                skipTextFieldStateSync = previous
                // pendingTextDuringSync must not leak past the batch; pendingSelectionDuringSync
                // is kept deliberately: the InputTransformation tail reads it after this returns.
                pendingTextDuringSync = null
            }
            return
        }
    }

    // The edit wasn't the announced paste (or none was pending): a stale stash must not
    // survive to misclassify a later, unrelated edit as a paste.
    pendingClipboardHtml = null
    pendingClipboardPlainText = null

    // Style inheritance when typing over a non-collapsed selection: capture before the
    // tree mutates, restyle inside the same history record (single undo entry).
    val replacedStyles = deltas.singleOrNull()
        ?.takeIf { !it.originalRange.collapsed && it.newText.isNotEmpty() }
        ?.let { captureReplacedSelectionStyles(replacedRange = it.originalRange, insertedLength = it.newText.length) }

    val postEditCaret = buffer.selection.min
    val trigger = classifyInputDeltas(deltas, postEditCaret)

    val previous = skipTextFieldStateSync
    skipTextFieldStateSync = true
    recordHistoryForInput(trigger) {
        try {
            // Shifted by how much the text actually moved, not by the delta's own arithmetic:
            // a delta landing on a list marker drops the whole "• " prefix along with the
            // character it replaced, so one in and one out still shortens the text by two.
            // Clamped as well, since a rewrite can move text the other way; a bounded edit
            // beats applyChange's bounds check throwing out of the InputTransformation.
            var offset = 0
            deltas.forEach { delta ->
                val lengthBefore = textFieldValue.text.length
                val shifted = TextRange(
                    (delta.originalRange.min + offset).coerceIn(0, lengthBefore),
                    (delta.originalRange.max + offset).coerceIn(0, lengthBefore),
                )
                applyChange(originalRange = shifted, newText = delta.newText)
                offset += textFieldValue.text.length - lengthBefore
            }
            if (replacedStyles != null) applyReplacedSelectionStyles(replacedStyles)
        } finally {
            skipTextFieldStateSync = previous
            // pendingTextDuringSync must not leak past the batch; pendingSelectionDuringSync
            // is kept deliberately: the InputTransformation reads it after this returns.
            pendingTextDuringSync = null
        }
    }

    // Arms the #779 follow-up window: a suggestion pick's trailing-space refresh arrives
    // as a bare caret step right after this edit.
    noteImeEdit(caret = textFieldValue.selection.min)
}

/**
 * Pushes text the pipeline auto-injected (list prefixes, renumbering, token labels) into the
 * BTF2 buffer, which did not see it because setTextFieldStateFromValue was suppressed during
 * the replay.
 *
 * Only the region that actually differs is replaced. Rewriting the whole buffer instead reaches
 * the Android IME as a wholesale text reset, which restarts input and makes the keyboard hide
 * and show again the moment "- " turns into a list.
 *
 * The caller clears [RichTextState.pendingSelectionDuringSync]; this function only reads it.
 */
internal fun RichTextState.reconcileBufferWithModel(buffer: TextFieldBuffer) {
    val targetText = annotatedString.text
    val currentText = buffer.asCharSequence().toString()
    if (currentText == targetText) return

    // Capped at the shorter length so prefix and suffix can never overlap.
    val maxShared = minOf(currentText.length, targetText.length)
    var prefix = 0
    while (prefix < maxShared && currentText[prefix] == targetText[prefix]) prefix++
    var suffix = 0
    while (
        suffix < maxShared - prefix &&
        currentText[currentText.lastIndex - suffix] == targetText[targetText.lastIndex - suffix]
    ) suffix++

    buffer.replace(
        prefix,
        currentText.length - suffix,
        targetText.substring(prefix, targetText.length - suffix),
    )

    val targetSelection = pendingSelectionDuringSync ?: buffer.selection
    if (
        buffer.selection != targetSelection &&
        targetSelection.min >= 0 &&
        targetSelection.max <= buffer.length
    ) {
        buffer.selection = targetSelection
    }
}

/**
 * Makes a trailing empty paragraph render its line by turning the separator space in front of it
 * into a newline in the output buffer.
 *
 * The builder appends each paragraph separator inside the *previous* paragraph's range, so a
 * trailing empty paragraph gets a zero-length range at the end of the text. BTF2 styles the buffer
 * through tracked ranges and drops collapsed ones, so that paragraph renders nothing. Shifting the
 * ranges instead cannot work on an all-empty document: N empty paragraphs own only N-1 separators,
 * so one line always goes missing. A newline makes MultiParagraph split natively, no range needed.
 *
 * The substitution is output-only (the model text keeps its space) and same-length, so every style
 * offset stays valid and the caret at the end of the text lands on the new line.
 *
 * Known limitation: the trailing empty paragraph still has no range of its own, so its own
 * ParagraphStyle is not attributed until it holds a character; the substituted newline lives inside
 * the previous paragraph's range and inherits that paragraph's style. Centering an empty trailing
 * line therefore shows as a one-keystroke alignment jump: the line renders with the previous
 * paragraph's alignment until the first character turns the range non-degenerate.
 */
internal fun substituteTrailingSeparatorWithNewline(
    buffer: TextFieldBuffer,
    ranges: List<AnnotatedString.Range<ParagraphStyle>>,
): Boolean {
    val last = ranges.lastOrNull() ?: return false
    if (last.start != last.end) return false
    if (last.start != buffer.length || buffer.length == 0) return false
    if (buffer.asCharSequence()[buffer.length - 1] != ' ') return false
    buffer.replace(buffer.length - 1, buffer.length, "\n")
    return true
}

/**
 * Projects annotatedString's style ranges into the BTF2 output buffer. Collapsed paragraph ranges
 * are skipped, since BTF2 drops them anyway; the trailing one stands for a line that
 * [substituteTrailingSeparatorWithNewline] renders instead. A collapsed range anywhere else (a
 * shape only singleParagraphMode or a transient desync can produce) is deliberately unhandled and
 * simply dropped here. Inter-paragraph spacing is handled solely by LineHeightStyle.Trim.Both on
 * the editor's text style.
 *
 * The substitution runs before any addStyle call: TextFieldBuffer only tracks styles added after
 * the last edit, so styles emitted first would be discarded by the replace.
 */
internal fun RichTextState.applyRichTextStyles(buffer: TextFieldBuffer) {
    val annotated = annotatedString
    substituteTrailingSeparatorWithNewline(buffer, annotated.paragraphStyles)
    annotated.spanStyles.forEach { range ->
        if (range.start in 0..buffer.length && range.end in 0..buffer.length) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
    annotated.paragraphStyles.forEach { range ->
        if (range.start != range.end && range.start in 0..buffer.length && range.end in 0..buffer.length) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
}
