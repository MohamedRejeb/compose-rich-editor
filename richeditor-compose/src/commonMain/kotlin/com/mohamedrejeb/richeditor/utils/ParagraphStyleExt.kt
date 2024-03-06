package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal fun ParagraphStyle.unmerge(
    other: ParagraphStyle?,
): ParagraphStyle {
    if (other == null) return this

    return ParagraphStyle(
        textAlign = if (other.textAlign != TextAlign.Unspecified) RichParagraph.DefaultParagraphStyle.textAlign else this.textAlign,
        textDirection = if (other.textDirection != TextDirection.Unspecified) RichParagraph.DefaultParagraphStyle.textDirection else this.textDirection,
        lineHeight = if (other.lineHeight.isSpecified) RichParagraph.DefaultParagraphStyle.lineHeight else this.lineHeight,
        textIndent = if (other.textIndent != null) RichParagraph.DefaultParagraphStyle.textIndent else this.textIndent,
        platformStyle = if (other.platformStyle != null) RichParagraph.DefaultParagraphStyle.platformStyle else this.platformStyle,
        lineHeightStyle = if (other.lineHeightStyle != null) RichParagraph.DefaultParagraphStyle.lineHeightStyle else this.lineHeightStyle,
        lineBreak = if (other.lineBreak != LineBreak.Unspecified) RichParagraph.DefaultParagraphStyle.lineBreak else this.lineBreak,
        hyphens = if (other.hyphens != Hyphens.Unspecified) RichParagraph.DefaultParagraphStyle.hyphens else this.hyphens,
    )
}

internal fun ParagraphStyle.isSpecifiedFieldsEquals(other: ParagraphStyle? = null): Boolean {
    if (other == null) return false

    if (other.textAlign != TextAlign.Unspecified && this.textAlign != other.textAlign) return false
    if (other.textDirection != TextDirection.Unspecified && this.textDirection != other.textDirection) return false
    if (!other.lineHeight.isUnspecified && this.lineHeight != other.lineHeight) return false
    if (other.textIndent != null && this.textIndent != other.textIndent) return false
    if (other.platformStyle != null && this.platformStyle != other.platformStyle) return false
    if (other.lineHeightStyle != null && this.lineHeightStyle != other.lineHeightStyle) return false
    if (other.lineBreak != LineBreak.Unspecified && this.lineBreak != other.lineBreak) return false
    if (other.hyphens != Hyphens.Unspecified && this.hyphens != other.hyphens) return false

    return true
}