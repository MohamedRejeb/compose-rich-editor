package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified

internal fun SpanStyle.unmerge(other: SpanStyle? = null): SpanStyle {
    if (other == null) return this

    return SpanStyle(
        color = if (other.color.isSpecified) Color.Unspecified else this.color,
        fontFamily = if (other.fontFamily != null) null else this.fontFamily,
        fontSize = if (!other.fontSize.isUnspecified) TextUnit.Unspecified else this.fontSize,
        fontWeight = if (other.fontWeight != null) null else this.fontWeight,
        fontStyle = if (other.fontStyle != null) null else this.fontStyle,
        fontSynthesis = if (other.fontSynthesis != null) null else this.fontSynthesis,
        fontFeatureSettings = if (other.fontFeatureSettings != null) null else this.fontFeatureSettings,
        letterSpacing = if (!other.letterSpacing.isUnspecified) {
            TextUnit.Unspecified
        } else {
            this.letterSpacing
        },
        baselineShift = if (other.baselineShift != null) null else this.baselineShift,
        textGeometricTransform = if (other.textGeometricTransform != null) null else this.textGeometricTransform,
        localeList = if (other.localeList != null) null else this.localeList,
        background = if (other.background.isSpecified) Color.Unspecified else this.background ,
        textDecoration = if (other.textDecoration != null) null else this.textDecoration,
        shadow = if (other.shadow != null) null else this.shadow,
    )
}

internal fun SpanStyle.isSpecifiedFieldsEquals(other: SpanStyle? = null): Boolean {
    if (other == null) return false

    if (other.color.isSpecified && this.color != other.color) return false
    if (other.fontFamily != null && this.fontFamily != other.fontFamily) return false
    if (!other.fontSize.isUnspecified && this.fontSize != other.fontSize) return false
    if (other.fontWeight != null && this.fontWeight != other.fontWeight) return false
    if (other.fontStyle != null && this.fontStyle != other.fontStyle) return false
    if (other.fontSynthesis != null && this.fontSynthesis != other.fontSynthesis) return false
    if (other.fontFeatureSettings != null && this.fontFeatureSettings != other.fontFeatureSettings) return false
    if (!other.letterSpacing.isUnspecified && this.letterSpacing != other.letterSpacing) return false
    if (other.baselineShift != null && this.baselineShift != other.baselineShift) return false
    if (other.textGeometricTransform != null && this.textGeometricTransform != other.textGeometricTransform) return false
    if (other.localeList != null && this.localeList != other.localeList) return false
    if (other.background.isSpecified && this.background != other.background) return false
    if (other.textDecoration != null && this.textDecoration != other.textDecoration) return false
    if (other.shadow != null && this.shadow != other.shadow) return false

    return true
}