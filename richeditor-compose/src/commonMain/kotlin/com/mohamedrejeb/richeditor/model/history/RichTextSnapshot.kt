package com.mohamedrejeb.richeditor.model.history

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * Immutable point-in-time snapshot of a `RichTextState`. All stored paragraphs are
 * detached deep copies - safe to hold and safe to restore from repeatedly.
 */
@OptIn(ExperimentalRichTextApi::class)
internal class RichTextSnapshot private constructor(
    val paragraphs: List<RichParagraph>,
    val selection: TextRange,
    val composition: TextRange?,
    val toAddSpanStyle: SpanStyle,
    val toAddRichSpanStyle: RichSpanStyle,
    val timestampMs: Long,
) {
    companion object {
        fun capture(
            paragraphs: List<RichParagraph>,
            selection: TextRange,
            composition: TextRange?,
            toAddSpanStyle: SpanStyle,
            toAddRichSpanStyle: RichSpanStyle,
            timestampMs: Long,
        ): RichTextSnapshot = RichTextSnapshot(
            paragraphs = paragraphs.map { it.deepCopy() },
            selection = selection,
            composition = composition,
            toAddSpanStyle = toAddSpanStyle,
            toAddRichSpanStyle = toAddRichSpanStyle,
            timestampMs = timestampMs,
        )
    }
}
