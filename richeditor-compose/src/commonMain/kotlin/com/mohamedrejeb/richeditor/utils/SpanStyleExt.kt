package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import com.mohamedrejeb.richeditor.model.RichSpan

/**
 * Merge two [SpanStyle]s together.
 * It behaves like [SpanStyle.merge] but it also merges [TextDecoration]s.
 * Which is not the case in [SpanStyle.merge].
 * So if the two [SpanStyle]s have different [TextDecoration]s, they will be combined.
 */
internal fun SpanStyle.customMerge(
    other: SpanStyle?,
    textDecoration: TextDecoration? = null
): SpanStyle {
    if (other == null) return this

    val firstTextDecoration = textDecoration ?: this.textDecoration
    val secondTextDecoration = other.textDecoration

    return if (
        firstTextDecoration != null &&
        secondTextDecoration != null &&
        firstTextDecoration != secondTextDecoration
    ) {
        this.merge(
            other.copy(
                textDecoration = TextDecoration.combine(
                    listOf(
                        firstTextDecoration,
                        secondTextDecoration
                    )
                )
            )
        )
    } else {
        this.merge(other)
    }
}

/**
 * Creates a new [SpanStyle] that contains only the properties that are different
 * between this [SpanStyle] and the [other] [SpanStyle].
 *
 * Properties that are the same in both styles are set to their default/unspecified values
 * in the resulting [SpanStyle].
 *
 * This is useful for identifying the "delta" or the additional styles applied on top
 * of a base style (e.g., finding user-added bold/italic on a heading style).
 *
 * @param other The [SpanStyle] to compare against.
 * @return A new [SpanStyle] containing only the differing properties.
 */
internal fun SpanStyle.diff(
    other: SpanStyle,
): SpanStyle {
    return SpanStyle(
        color = if (this.color != other.color) this.color else Color.Unspecified,
        fontFamily = if (this.fontFamily != other.fontFamily) this.fontFamily else null,
        fontSize = if (this.fontSize != other.fontSize) this.fontSize else TextUnit.Unspecified,
        fontWeight = if (this.fontWeight != other.fontWeight) this.fontWeight else null,
        fontStyle = if (this.fontStyle != other.fontStyle) this.fontStyle else null,
        fontSynthesis = if (this.fontSynthesis != other.fontSynthesis) this.fontSynthesis else null,
        fontFeatureSettings = if (this.fontFeatureSettings != other.fontFeatureSettings)
            this.fontFeatureSettings else null,
        letterSpacing = if (this.letterSpacing != other.letterSpacing) this.letterSpacing else
            TextUnit.Unspecified,
        baselineShift = if (this.baselineShift != other.baselineShift) this.baselineShift else null,
        textGeometricTransform = if (this.textGeometricTransform != other.textGeometricTransform)
            this.textGeometricTransform else null,
        localeList = if (this.localeList != other.localeList) this.localeList else null,
        background = if (this.background != other.background) this.background else Color.Unspecified,
        // For TextDecoration, we want the decorations present in 'this' but not in 'other'
        textDecoration = other.textDecoration?.let { this.textDecoration?.minus(it) },
        shadow = if (this.shadow != other.shadow) this.shadow else null,
    )
}

internal fun SpanStyle.unmerge(
    other: SpanStyle?,
): SpanStyle {
    if (other == null) return this

    return SpanStyle(
        color = if (other.color.isSpecified) Color.Unspecified else this.color,
        fontFamily = if (other.fontFamily != null) null else this.fontFamily,
        fontSize = if (other.fontSize.isSpecified) TextUnit.Unspecified else this.fontSize,
        fontWeight = if (other.fontWeight != null) null else this.fontWeight,
        fontStyle = if (other.fontStyle != null) null else this.fontStyle,
        fontSynthesis = if (other.fontSynthesis != null) null else this.fontSynthesis,
        fontFeatureSettings = if (other.fontFeatureSettings != null) null else this.fontFeatureSettings,
        letterSpacing = if (other.letterSpacing.isSpecified) TextUnit.Unspecified else this.letterSpacing,
        baselineShift = if (other.baselineShift != null) null else this.baselineShift,
        textGeometricTransform = if (other.textGeometricTransform != null) null else this.textGeometricTransform,
        localeList = if (other.localeList != null) null else this.localeList,
        background = if (other.background.isSpecified) Color.Unspecified else this.background,
        textDecoration = if (other.textDecoration != null && other.textDecoration == this.textDecoration) {
            null
        } else if (
            other.textDecoration != null &&
            this.textDecoration != null &&
            other.textDecoration!! in this.textDecoration!!
        ) {
            this.textDecoration!! - other.textDecoration!!
        } else this.textDecoration,
        shadow = if (other.shadow != null) null else this.shadow,
    )
}

internal fun SpanStyle.isSpecifiedFieldsEquals(other: SpanStyle? = null, strict: Boolean = false): Boolean {
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
    if (strict) {
        if (other.textDecoration != null && this.textDecoration != other.textDecoration) return false
    } else {
        if (
            (other.textDecoration != null &&
                    this.textDecoration == null) ||
            (other.textDecoration != null &&
                    this.textDecoration != null &&
                    other.textDecoration!! !in this.textDecoration!! &&
                    this.textDecoration!! !in other.textDecoration!!)
        ) return false
    }
    if (other.shadow != null && this.shadow != other.shadow) return false

    return true
}