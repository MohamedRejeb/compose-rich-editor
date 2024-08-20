package com.mohamedrejeb.richeditor.parser.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

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
internal val H1SpanStyle = SpanStyle(fontSize = 2.em, fontWeight = FontWeight.Bold)
internal val H2SpanStyle = SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)
internal val H3SpanStyle = SpanStyle(fontSize = 1.17.em, fontWeight = FontWeight.Bold)
internal val H4SpanStyle = SpanStyle(fontSize = 1.12.em, fontWeight = FontWeight.Bold)
internal val H5SpanStyle = SpanStyle(fontSize = 0.83.em, fontWeight = FontWeight.Bold)
internal val H6SpanStyle = SpanStyle(fontSize = 0.75.em, fontWeight = FontWeight.Bold)