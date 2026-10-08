package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel

/**
 * This text, built from [paragraphs], with [spacing] of vertical space between them.
 *
 * Compose stacks ParagraphStyle ranges with no gap and has no spacing property, so the separator
 * character that ends a paragraph is moved out of its range into a one-line paragraph of its own
 * whose line height is the gap. The text and every offset stay the same.
 *
 * An empty paragraph is the gap itself instead of adding a line to it, and items of a list stay
 * together. For read-only text only: a text field would draw the caret at the end of a paragraph
 * inside the gap line, because that offset is the start of the gap paragraph.
 */
internal fun AnnotatedString.withParagraphSpacing(
    paragraphs: List<RichParagraph>,
    spacing: TextUnit,
): AnnotatedString {
    if (spacing.isUnspecified || paragraphs.size < 2) return this

    val starts = paragraphs.map { it.type.startRichSpan.textRange.min }
    val indexByStart = starts.withIndex().associate { (index, start) -> start to index }
    // A paragraph with no list prefix and no text: only its separator, or nothing when it is last.
    fun isEmpty(index: Int) =
        if (index == paragraphs.lastIndex) starts[index] == length
        else starts[index + 1] - starts[index] == 1
    fun isListItem(index: Int) = paragraphs[index].type is ConfigurableListLevel
    val gapStyle = ParagraphStyle(
        lineHeight = spacing,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    return flatMapAnnotations { range ->
        val index = indexByStart[range.start]
        when {
            range.item !is ParagraphStyle || index == null || index == paragraphs.lastIndex ->
                listOf(range)

            isEmpty(index) ->
                listOf(AnnotatedString.Range(gapStyle, range.start, range.end))

            isEmpty(index + 1) || (isListItem(index) && isListItem(index + 1)) ->
                listOf(range)

            else ->
                listOf(
                    AnnotatedString.Range(range.item, range.start, range.end - 1),
                    AnnotatedString.Range(gapStyle, range.end - 1, range.end),
                )
        }
    }
}
