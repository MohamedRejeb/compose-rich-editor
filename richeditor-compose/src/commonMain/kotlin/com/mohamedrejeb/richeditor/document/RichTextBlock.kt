package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * One paragraph of content. [text] is the flattened paragraph text (list prefixes excluded,
 * one U+FFFC placeholder per inline image). Styling is carried by [spans] as inclusive
 * character ranges over [text].
 */
@ExperimentalRichTextApi
public data class RichTextBlock(
    public val text: String,
    public val type: RichTextBlockType = RichTextBlockType.Paragraph,
    public val spans: List<RichTextSpanMark> = emptyList(),
    public val headingLevel: Int = 0,
    public val textAlign: TextAlign = TextAlign.Unspecified,
    public val textDirection: TextDirection = TextDirection.Unspecified,
    public val lineHeight: TextUnit = TextUnit.Unspecified,
    public val textIndent: TextIndent? = null,
    public val isLineBreak: Boolean = false,
) {
    init {
        require(headingLevel in 0..6) { "headingLevel must be in 0..6, was $headingLevel" }
        spans.forEach { mark ->
            require(!mark.range.isEmpty()) { "Span mark range must not be empty: ${mark.range}" }
            require(mark.range.first >= 0 && mark.range.last < text.length) {
                "Span mark ${mark.range} is out of bounds for text of length ${text.length}"
            }
        }
    }
}
