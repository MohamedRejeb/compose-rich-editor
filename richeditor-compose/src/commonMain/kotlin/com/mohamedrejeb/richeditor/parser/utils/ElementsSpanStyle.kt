package com.mohamedrejeb.richeditor.parser.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.mohamedrejeb.richeditor.model.HeadingParagraphStyle

internal val MarkBackgroundColor = Color.Yellow
internal val SmallFontSize = 0.8f.em

internal val BoldSpanStyle = SpanStyle(fontWeight = FontWeight.Bold)
internal val ItalicSpanStyle = SpanStyle(fontStyle = FontStyle.Italic)
internal val UnderlineSpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
internal val StrikethroughSpanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
internal val SubscriptSpanStyle = SpanStyle(baselineShift = BaselineShift.Subscript)
internal val SuperscriptSpanStyle = SpanStyle(baselineShift = BaselineShift.Superscript)
internal val MarkSpanStyle = SpanStyle(background = MarkBackgroundColor)
internal val SmallSpanStyle = SpanStyle(fontSize = SmallFontSize)
internal val H1SpanStyle = HeadingParagraphStyle.H1.getSpanStyle()
internal val H2SpanStyle = HeadingParagraphStyle.H2.getSpanStyle()
internal val H3SpanStyle = HeadingParagraphStyle.H3.getSpanStyle()
internal val H4SpanStyle = HeadingParagraphStyle.H4.getSpanStyle()
internal val H5SpanStyle = HeadingParagraphStyle.H5.getSpanStyle()
internal val H6SpanStyle = HeadingParagraphStyle.H6.getSpanStyle()
