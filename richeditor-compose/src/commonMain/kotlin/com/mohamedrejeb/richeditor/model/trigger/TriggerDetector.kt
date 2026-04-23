package com.mohamedrejeb.richeditor.model.trigger

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Scan [text] backwards from [caretOffset] looking for a registered trigger,
 * producing a [TriggerQuery] describing the in-progress token insertion or
 * `null` if no trigger is active at the caret position.
 *
 * The scan terminates as soon as any of the following is true:
 *  - a character in the active trigger's `stopChars` is encountered (no activation);
 *  - a registered trigger character is found (candidate activation);
 *  - [Trigger.maxQueryLength] characters have been scanned without finding a trigger (no activation);
 *  - the start of the text is reached.
 *
 * When a trigger character is found, the function additionally validates:
 *  - word-boundary requirement, if [Trigger.requireWordBoundary] is true;
 *  - the suppressed range guard — if [suppressedRange] contains the trigger
 *    position, activation is skipped. This prevents a just-cancelled query
 *    from immediately re-activating while the caret is still within its range.
 */
@ExperimentalRichTextApi
internal fun detectActiveTrigger(
    text: String,
    caretOffset: Int,
    triggers: List<Trigger>,
    textLayoutResult: TextLayoutResult?,
    suppressedRange: TextRange?,
): TriggerQuery? {
    if (triggers.isEmpty()) return null
    if (caretOffset <= 0 || caretOffset > text.length) return null

    val triggerByChar: Map<Char, Trigger> = triggers.associateBy { it.char }

    // Use the global maximum across triggers as the scan cap. Per-trigger maxQueryLength
    // validation happens after a candidate trigger is found.
    val scanCap = triggers.maxOf { it.maxQueryLength }

    var i = caretOffset - 1
    var scanned = 0
    var lastStopCharIndex = -1

    while (i >= 0 && scanned <= scanCap) {
        val ch = text[i]

        val candidate = triggerByChar[ch]
        if (candidate != null) {
            // Verify the substring between candidate and caret contains no stop chars
            // for THIS trigger. (We may have passed over chars that would be stop chars
            // for a different trigger; each trigger defines its own set.)
            val queryStart = i + 1
            val query = text.substring(queryStart, caretOffset)
            val queryLength = caretOffset - queryStart

            if (queryLength > candidate.maxQueryLength) return null
            if (query.any { it in candidate.stopChars }) return null

            if (candidate.requireWordBoundary) {
                val prev = if (i == 0) null else text[i - 1]
                val isBoundary = prev == null || prev.isWhitespace() || prev == '\n'
                if (!isBoundary) return null
            }

            if (suppressedRange != null) {
                val triggerPos = i
                if (triggerPos in suppressedRange.min until suppressedRange.max ||
                    triggerPos == suppressedRange.min
                ) {
                    return null
                }
            }

            val range = TextRange(i, caretOffset)
            val caretRect = textLayoutResult?.let {
                runCatching { it.getCursorRect(caretOffset) }.getOrNull()
            }

            return TriggerQuery(
                triggerId = candidate.id,
                query = query,
                range = range,
                caretRect = caretRect,
            )
        }

        // A char at the current scan position is a stop char if ANY registered
        // trigger treats it as a stop char — once hit, no trigger can resume
        // backwards through this char. This matches user intuition (whitespace
        // always breaks a query regardless of which trigger is involved).
        val isAnyStopChar = triggers.any { ch in it.stopChars }
        if (isAnyStopChar) {
            lastStopCharIndex = i
            return null
        }

        i--
        scanned++
    }

    return null
}
