package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle

internal fun List<RichSpan>.getCommonStyle(strict: Boolean = false): SpanStyle? {
    if (this.isEmpty()) return null

    val startIndex = this.indexOfFirst { it.text.isNotEmpty() }
    val firstSpanStyle = this.getOrNull(startIndex)?.fullSpanStyle ?: return null
    val otherStyleList = this.drop(startIndex + 1)

    var color: Color = firstSpanStyle.color
    var fontFamily: FontFamily? = firstSpanStyle.fontFamily
    var fontSize: TextUnit = firstSpanStyle.fontSize
    var fontWeight: FontWeight? = firstSpanStyle.fontWeight
    var fontStyle: FontStyle? = firstSpanStyle.fontStyle
    var fontSynthesis: FontSynthesis? = firstSpanStyle.fontSynthesis
    var fontFeatureSettings: String? = firstSpanStyle.fontFeatureSettings
    var letterSpacing: TextUnit = firstSpanStyle.letterSpacing
    var baselineShift: BaselineShift? = firstSpanStyle.baselineShift
    var textGeometricTransform: TextGeometricTransform? = firstSpanStyle.textGeometricTransform
    var localeList: LocaleList? = firstSpanStyle.localeList
    var background: Color = firstSpanStyle.background
    var textDecoration: TextDecoration? = firstSpanStyle.textDecoration
    var shadow: Shadow? = firstSpanStyle.shadow

    otherStyleList.fastForEach {
        if (it.text.isEmpty()) return@fastForEach

        val otherSpanStyle = it.fullSpanStyle
        if (otherSpanStyle.color != color) color = Color.Unspecified
        if (otherSpanStyle.fontFamily != fontFamily) fontFamily = null
        if (otherSpanStyle.fontSize != fontSize) fontSize = TextUnit.Unspecified
        if (otherSpanStyle.fontWeight != fontWeight) fontWeight = null
        if (otherSpanStyle.fontStyle != fontStyle) fontStyle = null
        if (otherSpanStyle.fontSynthesis != fontSynthesis) fontSynthesis = null
        if (otherSpanStyle.fontFeatureSettings != fontFeatureSettings) fontFeatureSettings = null
        if (otherSpanStyle.letterSpacing != letterSpacing) letterSpacing = TextUnit.Unspecified
        if (otherSpanStyle.baselineShift != baselineShift) baselineShift = null
        if (otherSpanStyle.textGeometricTransform != textGeometricTransform) textGeometricTransform = null
        if (otherSpanStyle.localeList != localeList) localeList = null
        if (otherSpanStyle.background != background) background = Color.Unspecified
        textDecoration = otherSpanStyle.textDecoration?.getCommonDecoration(textDecoration, strict)
        if (otherSpanStyle.shadow != shadow) shadow = null
    }

    return SpanStyle(
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSynthesis = fontSynthesis,
        fontFeatureSettings = fontFeatureSettings,
        letterSpacing = letterSpacing,
        baselineShift = baselineShift,
        textGeometricTransform = textGeometricTransform,
        localeList = localeList,
        background = background,
        textDecoration = textDecoration,
        shadow = shadow,
    )
}

internal fun List<RichSpan>.getCommonRichStyle(): RichSpanStyle? {
    var richSpanStyle: RichSpanStyle? = null

    for (index in indices) {
        val item = get(index)
        if (richSpanStyle == null) {
            richSpanStyle = item.style
        } else if (richSpanStyle::class != item.style::class) {
            richSpanStyle = null
            break
        }
    }

    return richSpanStyle
}