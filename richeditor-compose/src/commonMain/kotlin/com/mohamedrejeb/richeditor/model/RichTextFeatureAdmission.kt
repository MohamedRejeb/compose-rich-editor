package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextDocumentDecoder
import com.mohamedrejeb.richeditor.document.RichTextDocumentEncoder
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * The parsed [paragraphs] with everything outside [RichTextConfig.features] stripped, ready
 * for insertion or loading. The HTML and Markdown loads and inserts pass through here, and a
 * recognized paste reaches it through [RichTextState.insertHtml]; a document load filters the
 * document itself instead. Returns [paragraphs] untouched when every feature is allowed, so
 * the default path never pays the round trip.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichTextState.admit(paragraphs: List<RichParagraph>): List<RichParagraph> {
    val features = config.features
    if (features.containsAll(RichTextFeature.entries)) return paragraphs
    val document = RichTextDocumentEncoder.encode(paragraphs).restrictedTo(features)
    return RichTextDocumentDecoder.decode(document)
}

/** Whether [features] allows this rich span style. [RichSpanStyle.Default] always is. */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichSpanStyle.isAllowedBy(features: Set<RichTextFeature>): Boolean = when (this) {
    is RichSpanStyle.Default -> true
    is RichSpanStyle.Link -> RichTextFeature.Link in features
    is RichSpanStyle.Code -> RichTextFeature.CodeSpan in features
    is RichSpanStyle.Image -> RichTextFeature.Image in features
    is RichSpanStyle.Token -> RichTextFeature.Token in features
    else -> RichTextFeature.CustomSpanStyle in features
}

/**
 * This style with the fields of disallowed features cleared. A brush counts as text color.
 * Fields no feature governs (font family, synthesis, feature settings, geometric transform,
 * locale, draw style) pass through.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun SpanStyle.restrictedTo(features: Set<RichTextFeature>): SpanStyle {
    if (features.containsAll(RichTextFeature.entries)) return this
    val weightFeature = if (fontWeight == FontWeight.Bold) RichTextFeature.Bold else RichTextFeature.FontWeight
    val restricted = SpanStyle(
        color = if (RichTextFeature.TextColor in features) color else Color.Unspecified,
        fontSize = if (RichTextFeature.FontSize in features) fontSize else TextUnit.Unspecified,
        fontWeight = if (weightFeature in features) fontWeight else null,
        fontStyle = if (RichTextFeature.Italic in features) fontStyle else null,
        fontSynthesis = fontSynthesis,
        fontFamily = fontFamily,
        fontFeatureSettings = fontFeatureSettings,
        letterSpacing = if (RichTextFeature.LetterSpacing in features) letterSpacing else TextUnit.Unspecified,
        baselineShift = if (RichTextFeature.BaselineShift in features) baselineShift else null,
        textGeometricTransform = textGeometricTransform,
        localeList = localeList,
        background = if (RichTextFeature.Highlight in features) background else Color.Unspecified,
        textDecoration = textDecoration?.restrictedTo(features),
        shadow = if (RichTextFeature.Shadow in features) shadow else null,
        platformStyle = platformStyle,
        drawStyle = drawStyle,
    )
    val brush = brush
    return if (brush != null && RichTextFeature.TextColor in features) restricted.copy(brush = brush, alpha = alpha)
    else restricted
}

@OptIn(ExperimentalRichTextApi::class)
private fun TextDecoration.restrictedTo(features: Set<RichTextFeature>): TextDecoration? {
    if (this == TextDecoration.None) return this
    val parts = buildList {
        if (TextDecoration.Underline in this@restrictedTo && RichTextFeature.Underline in features) add(TextDecoration.Underline)
        if (TextDecoration.LineThrough in this@restrictedTo && RichTextFeature.Strikethrough in features) add(TextDecoration.LineThrough)
    }
    return if (parts.isEmpty()) null else TextDecoration.combine(parts)
}
