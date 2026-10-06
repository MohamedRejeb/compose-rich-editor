package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
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
    pendingCutContent = extractDeletedSelection(buffer)
    // A collapsing copy (touch toolbar, semantics action) collapses the selection before it
    // writes the clipboard, ahead of the selection observer: the range is only known here.
    if (!buffer.originalSelection.collapsed) lastNonCollapsedSelection = buffer.originalSelection

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
        .mapNotNull { trimWholeTextRewrite(it, buffer) }
        .mapNotNull { keepSeparatorUnderTrailingSpace(it) }
    if (deltas.isEmpty()) {
        // The whole batch was a kept separator deletion: the reconciliation puts the separator
        // back and must leave the caret in front of it, or the caret would land in the next
        // paragraph and read as the caret-step form of the refresh.
        pendingSelectionDuringSync = buffer.selection
        return
    }

    // Paste recognition on the delta shape: one delta whose inserted text matches the
    // clipboard's stashed plain text is the paste the clipboard manager announced.
    val pendingHtml = pendingClipboardHtml.takeIf { config.richPasteEnabled }
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
                autoLinkInsertedText(at = pasteDelta.originalRange.min, inserted = pasteDelta.newText)
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
    val caretWasAtParagraphEnd =
        buffer.originalSelection.collapsed && isParagraphSeparatorIndex(buffer.originalSelection.min)

    val previous = skipTextFieldStateSync
    skipTextFieldStateSync = true
    var refreshed = false
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
            refreshed = materializeSpaceUnderImeCaret(buffer, deltas, caretWasAtParagraphEnd)
        } finally {
            skipTextFieldStateSync = previous
            // pendingTextDuringSync must not leak past the batch; pendingSelectionDuringSync
            // is kept deliberately: the InputTransformation reads it after this returns.
            pendingTextDuringSync = null
        }
    }

    deltas.singleOrNull()?.let { autoLinkAfterEdit(it) }

    // Arms the #779 follow-up window: a suggestion pick's trailing-space refresh arrives
    // as a bare caret step right after this edit. A refresh folded into this batch has
    // already happened, so the window is disarmed instead: a step out of the paragraph now
    // is navigation.
    if (refreshed) clearImeEditWindow() else noteImeEdit(caret = textFieldValue.selection.min)
}

/** [autoLinkInsertedText] for a replayed edit: the buffer is mid-edit, so the write stays pending. */
private fun RichTextState.autoLinkAfterEdit(delta: InputDelta) {
    val previous = skipTextFieldStateSync
    skipTextFieldStateSync = true
    try {
        autoLinkInsertedText(at = delta.originalRange.min, inserted = delta.newText)
    } finally {
        skipTextFieldStateSync = previous
        pendingTextDuringSync = null
    }
}

/**
 * Compose web commits every typed character as one change from the whole old text to the whole
 * new text. Replayed as it is, that replaces the document with a single unformatted paragraph,
 * so such a change is cut down to the part that differs. A change over the whole text that
 * matches the selection is a real replacement (select all, then type) and is left alone.
 * Returns null when nothing differs.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun trimWholeTextRewrite(delta: InputDelta, buffer: TextFieldBuffer): InputDelta? {
    val original = buffer.originalText
    val coversWholeText = delta.originalRange.min == 0 && delta.originalRange.max == original.length
    if (!coversWholeText || original.isEmpty() || delta.originalRange == buffer.originalSelection) return delta
    return differingPart(original = original, rewritten = delta.newText, caret = buffer.selection.min)
}

/**
 * The smallest single edit that turns [original] into [rewritten], or null when they are equal.
 * Where it could sit in more than one place (a character typed into a run of the same one), it
 * is the one that ends at [caret], which is where a typed character leaves the caret.
 */
internal fun differingPart(original: CharSequence, rewritten: CharSequence, caret: Int): InputDelta? {
    val shorter = minOf(original.length, rewritten.length)
    val prefix = original.commonPrefixWith(rewritten).length
    val suffix = original.commonSuffixWith(rewritten).length.coerceAtMost(shorter - prefix)
    if (prefix == original.length && prefix == rewritten.length) return null

    // The same edit also fits anywhere down to the shortest prefix a full suffix match allows.
    val fullSuffix = original.commonSuffixWith(rewritten).length
    val minPrefix = (shorter - fullSuffix).coerceIn(0, prefix)
    val shift = (rewritten.length - suffix - caret).coerceIn(0, prefix - minPrefix)

    val start = prefix - shift
    val kept = suffix + shift
    return InputDelta(
        originalRange = TextRange(start, original.length - kept),
        newText = rewritten.substring(start, rewritten.length - kept),
    )
}

/**
 * An IME puts a space after a word by replacing the character that follows it when that
 * character is already a space: Gboard selects it and commits " ", Samsung deletes it and
 * commits " ". At a paragraph end that character is the paragraph separator, which the IME
 * cannot tell from a space, and replaying the delta verbatim would merge the paragraphs
 * (#779). The space the IME meant goes inside the paragraph and the separator stays.
 *
 * When the delete and the commit arrive as two passes, the first is a bare one-character
 * deletion of the separator. It is kept only while the IME is mid-commit at that boundary
 * (the same signals as the caret-step form), so a Delete key at a paragraph end still joins
 * the paragraphs.
 */
private fun RichTextState.keepSeparatorUnderTrailingSpace(delta: InputDelta): InputDelta? {
    val range = delta.originalRange
    if (range.collapsed) return delta
    if (delta.newText.endsWith(' ') && isParagraphSeparatorIndex(range.max - 1) && !isWithinPhysicalKeyWindow())
        return InputDelta(TextRange(range.min, range.max - 1), delta.newText)
    val deletesOnlyTheSeparator =
        delta.newText.isEmpty() && range.max - range.min == 1 && isParagraphSeparatorIndex(range.min)
    if (deletesOnlyTheSeparator && imeJustEditedAt(range.min) && !isWithinPhysicalKeyWindow())
        return null
    return delta
}

/**
 * A commit at a paragraph end after which the IME's caret sits one past the paragraph's new
 * end: it stepped over the separator, which it takes for the space after its word (#779).
 * The commit may shrink the paragraph (a token rewritten by a shorter suggestion arrives as
 * a minimal diff with the caret number untouched), so the caret is compared with the
 * paragraph end after the replay, not with the model's own caret. When the step is folded
 * into the same batch as the commit, no selection change is ever observed and the selection
 * observer's form of the refresh never runs. The space the IME believes in is materialized
 * inside the paragraph and the caret stays where the IME put it. Returns whether it did.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun RichTextState.materializeSpaceUnderImeCaret(
    buffer: TextFieldBuffer,
    deltas: List<InputDelta>,
    caretWasAtParagraphEnd: Boolean,
): Boolean {
    if (!caretWasAtParagraphEnd || !buffer.selection.collapsed) return false
    if (deltas.last().newText.isEmpty() || deltas.any { '\n' in it.newText }) return false
    val boundary = buffer.selection.min - 1
    if (boundary < 0 || boundary < textFieldValue.selection.min) return false
    if (!isParagraphSeparatorIndex(boundary) || isWithinPhysicalKeyWindow()) return false
    applyChange(originalRange = TextRange(boundary), newText = " ")
    return true
}

/**
 * The selected content, when the edit in [buffer] deletes exactly the selection and nothing
 * else. Must run before the replay, while the model still holds that content.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun RichTextState.extractDeletedSelection(buffer: TextFieldBuffer): RichTextState? {
    val changes = buffer.changes
    if (changes.changeCount != 1 || !changes.getRange(0).collapsed) return null

    val deleted = changes.getOriginalRange(0)
    val selection = buffer.originalSelection
    if (deleted.collapsed || deleted.min != selection.min || deleted.max != selection.max) return null

    return extractRangeState(deleted)
}

/**
 * Pushes text the pipeline auto-injected (list prefixes, renumbering, token labels) into the
 * BTF2 buffer, which did not see it because setTextFieldStateFromValue was suppressed during
 * the replay.
 *
 * Only the region that actually differs is replaced, so the buffer records one local change
 * instead of a whole-text rewrite. The selection is then set from
 * [RichTextState.pendingSelectionDuringSync] when it fits the new text.
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
 * What the output buffer appends for a trailing empty paragraph: a zero-width space.
 *
 * The builder appends each paragraph separator inside the *previous* paragraph's range, so a
 * trailing empty paragraph gets a zero-length range at the end of the text, and BTF2 drops
 * collapsed style ranges. The anchor gives that paragraph one character to carry its
 * ParagraphStyle and its font, so its line renders with its own alignment and height.
 *
 * It is output-only and always last, so every model offset is also a valid layout offset. The
 * layout text is one character longer than the model text while it is there, see
 * [isForModelText].
 */
internal const val EmptyLineAnchor: String = "\u200B"

/** Whether this layout was computed for a model text of [modelLength] characters. */
internal fun TextLayoutResult.isForModelText(modelLength: Int): Boolean {
    val text = layoutInput.text.text
    return text.length == modelLength ||
        (text.length == modelLength + EmptyLineAnchor.length && text.endsWith(EmptyLineAnchor))
}

/**
 * Projects annotatedString's style ranges into the BTF2 output buffer.
 *
 * A trailing empty paragraph has a collapsed range, which BTF2 would drop: it gets the
 * [EmptyLineAnchor] and its ParagraphStyle on it. A collapsed range anywhere else (a shape only
 * singleParagraphMode or a transient desync can produce) is deliberately unhandled and simply
 * dropped here. Inter-paragraph spacing comes from the caller's text style alone: each paragraph
 * range is laid out with the caller's `lineHeight` and `lineHeightStyle` as given.
 *
 * The anchor is appended before any addStyle call: TextFieldBuffer only tracks styles added
 * after the last edit, so styles emitted first would be discarded by the append.
 */
internal fun RichTextState.applyRichTextStyles(buffer: TextFieldBuffer) {
    val annotated = annotatedString
    val modelLength = buffer.length
    val paragraphRanges = annotated.paragraphStyles
    val trailingEmpty = trailingEmptyParagraphRange(paragraphRanges, modelLength)
    if (trailingEmpty != null) appendEmptyLineAnchor(buffer)

    annotated.spanStyles.forEach { range ->
        if (range.start in 0..modelLength && range.end in 0..modelLength) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
    paragraphRanges.forEach { range ->
        if (range.start != range.end && range.start in 0..modelLength && range.end in 0..modelLength) {
            buffer.addStyle(range.item, range.start, range.end)
        }
    }
    if (trailingEmpty != null && buffer.length > modelLength) {
        buffer.addStyle(trailingEmpty.item, modelLength, buffer.length)
    }

    applyEmptyParagraphFonts(buffer, paragraphRanges, modelLength)
}

/**
 * Adds the [EmptyLineAnchor] by replacing the separator in front of it with the separator and
 * the anchor, not by inserting after it. BTF2 gives inserted output text two caret positions for
 * one model position, and the first Backspace or arrow key after a tap on the line would only
 * switch between them. A replacement maps both of its ends to distinct model offsets.
 *
 * An empty document has no separator to replace. Inserting is harmless there: no key can move
 * or delete anything.
 */
private fun appendEmptyLineAnchor(buffer: TextFieldBuffer) {
    val length = buffer.length
    when {
        length == 0 -> buffer.append(EmptyLineAnchor)
        buffer.asCharSequence()[length - 1] == ParagraphSeparator ->
            buffer.replace(length - 1, length, ParagraphSeparator + EmptyLineAnchor)
    }
}

private const val ParagraphSeparator = ' '

internal fun trailingEmptyParagraphRange(
    ranges: List<AnnotatedString.Range<ParagraphStyle>>,
    textLength: Int,
): AnnotatedString.Range<ParagraphStyle>? =
    ranges.lastOrNull()?.takeIf { it.start == it.end && it.start == textLength }

/**
 * An empty paragraph has no text to give its line a font, so the line and the caret on it would
 * fall back to the editor's text style (#369). Its one rendered character, the separator after it
 * or the [EmptyLineAnchor], takes the font the next typed character would have.
 */
private fun RichTextState.applyEmptyParagraphFonts(
    buffer: TextFieldBuffer,
    paragraphRanges: List<AnnotatedString.Range<ParagraphStyle>>,
    modelLength: Int,
) {
    if (paragraphRanges.size != richParagraphList.size) return
    val caret = selection.takeIf { it.collapsed }?.min

    richParagraphList.forEachIndexed { index, paragraph ->
        if (!paragraph.isEmpty(ignoreStartRichSpan = false)) return@forEachIndexed
        val range = paragraphRanges[index]
        val charStart = if (range.start == range.end) modelLength else range.end - 1
        if (charStart != range.start || charStart >= buffer.length) return@forEachIndexed

        val style = (if (caret == charStart) currentSpanStyle else paragraph.getStartTextSpanStyle())
            ?: return@forEachIndexed
        buffer.addStyle(style.fontOnly(), charStart, charStart + 1)
    }
}

private fun SpanStyle.fontOnly(): SpanStyle =
    SpanStyle(
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSynthesis = fontSynthesis,
    )
