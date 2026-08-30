package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
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
            var offset = 0
            deltas.forEach { delta ->
                val shifted = TextRange(
                    delta.originalRange.min + offset,
                    delta.originalRange.max + offset,
                )
                applyChange(originalRange = shifted, newText = delta.newText)
                offset += delta.newText.length - (delta.originalRange.max - delta.originalRange.min)
            }
            if (replacedStyles != null) applyReplacedSelectionStyles(replacedStyles)
        } finally {
            skipTextFieldStateSync = previous
            // pendingTextDuringSync must not leak past the batch; pendingSelectionDuringSync
            // is kept deliberately: the InputTransformation reads it after this returns.
            pendingTextDuringSync = null
        }
    }
}

/**
 * Projects annotatedString's style ranges into the BTF2 output buffer. The annotatedString
 * already encodes all ranges correctly; nothing else is needed here. Inter-paragraph
 * spacing is handled solely by LineHeightStyle.Trim.Both on the editor's text style.
 */
internal fun RichTextState.applyRichTextStyles(buffer: TextFieldBuffer) {
    val annotated = annotatedString
    annotated.spanStyles.forEach { range ->
        if (range.start in 0..buffer.length && range.end in 0..buffer.length) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
    annotated.paragraphStyles.forEach { range ->
        if (range.start in 0..buffer.length && range.end in 0..buffer.length) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
}
