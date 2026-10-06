package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * A style drawn over a range of the text without being part of the document, such as a match
 * of a find-in-text feature. See [RichTextState.highlights].
 *
 * @param range offsets in [RichTextState.annotatedString]'s text. A reversed range is read as
 * its min and max.
 * @param style the style drawn over the range, on top of the text's own styles.
 */
@ExperimentalRichTextApi
@Immutable
public class RichTextHighlight(
    public val range: TextRange,
    public val style: SpanStyle,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is RichTextHighlight && range == other.range && style == other.style)

    override fun hashCode(): Int = 31 * range.hashCode() + style.hashCode()

    override fun toString(): String = "RichTextHighlight(range=$range, style=$style)"
}

/**
 * The highlights as style ranges of a text of [textLength] characters, in list order: each range
 * is clamped to the text and dropped when nothing is left of it. A background is made
 * transparent under a non-collapsed [selection], like a span background is, so the selection
 * stays visible.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun List<RichTextHighlight>.toSpanStyleRanges(
    textLength: Int,
    selection: TextRange = TextRange.Zero,
): List<AnnotatedString.Range<SpanStyle>> =
    flatMap { highlight ->
        val start = highlight.range.min.coerceIn(0, textLength)
        val end = highlight.range.max.coerceIn(0, textLength)
        val maskStart = selection.min.coerceIn(start, end)
        val maskEnd = selection.max.coerceIn(start, end)
        if (maskStart == maskEnd || highlight.style.background == Color.Unspecified) {
            listOf(AnnotatedString.Range(highlight.style, start, end))
        } else {
            listOf(
                AnnotatedString.Range(highlight.style, start, maskStart),
                AnnotatedString.Range(highlight.style.copy(background = Color.Transparent), maskStart, maskEnd),
                AnnotatedString.Range(highlight.style, maskEnd, end),
            )
        }
    }.filter { it.start < it.end }

/** This text with [highlights] added after its own styles, so a highlight wins where they overlap. */
@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.withHighlights(highlights: List<RichTextHighlight>): AnnotatedString {
    val ranges = highlights.toSpanStyleRanges(textLength = length)
    if (ranges.isEmpty()) return this
    return AnnotatedString.Builder(this).apply {
        ranges.forEach { addStyle(it.item, it.start, it.end) }
    }.toAnnotatedString()
}
