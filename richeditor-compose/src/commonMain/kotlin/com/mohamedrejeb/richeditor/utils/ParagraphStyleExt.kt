package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified

internal fun ParagraphStyle.unmerge(
    other: ParagraphStyle?,
): ParagraphStyle {
    if (other == null) return this

    return ParagraphStyle(
        textAlign = if (other.textAlign != null) null else this.textAlign,
        textDirection = if (other.textDirection != null) null else this.textDirection,
        lineHeight = if (other.lineHeight.isSpecified) TextUnit.Unspecified else this.lineHeight,
        textIndent = if (other.textIndent != null) null else this.textIndent,
        platformStyle = if (other.platformStyle != null) null else this.platformStyle,
        lineHeightStyle = if (other.lineHeightStyle != null) null else this.lineHeightStyle,
        lineBreak = if (other.lineBreak != null) null else this.lineBreak,
        hyphens = if (other.hyphens != null) null else this.hyphens,
    )
}

internal fun ParagraphStyle.isSpecifiedFieldsEquals(other: ParagraphStyle? = null): Boolean {
    if (other == null) return false

    if (other.textAlign != null && this.textAlign != other.textAlign) return false
    if (other.textDirection != null && this.textDirection != other.textDirection) return false
    if (!other.lineHeight.isUnspecified && this.lineHeight != other.lineHeight) return false
    if (other.textIndent != null && this.textIndent != other.textIndent) return false
    if (other.platformStyle != null && this.platformStyle != other.platformStyle) return false
    if (other.lineHeightStyle != null && this.lineHeightStyle != other.lineHeightStyle) return false
    if (other.lineBreak != null && this.lineBreak != other.lineBreak) return false
    if (other.hyphens != null && this.hyphens != other.hyphens) return false

    return true
}