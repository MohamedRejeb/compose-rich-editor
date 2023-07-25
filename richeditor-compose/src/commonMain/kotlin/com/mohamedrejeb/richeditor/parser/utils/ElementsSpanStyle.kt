package com.mohamedrejeb.richeditor.parser.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

internal val BoldSpanStyle = SpanStyle(fontWeight = FontWeight.Bold)
internal val ItalicSpanStyle = SpanStyle(fontStyle = FontStyle.Italic)
internal val UnderlineSpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
internal val StrikethroughSpanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
internal val SubscriptSpanStyle = SpanStyle(baselineShift = BaselineShift.Subscript)
internal val SuperscriptSpanStyle = SpanStyle(baselineShift = BaselineShift.Superscript)
internal val MarkSpanStyle = SpanStyle(background = Color.Yellow)
internal val SmallSpanStyle = SpanStyle(fontSize = 0.8f.em)
internal val H1SPanStyle = SpanStyle(fontSize = 2.em, fontWeight = FontWeight.Bold)
internal val H2SPanStyle = SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)
internal val H3SPanStyle = SpanStyle(fontSize = 1.17.em, fontWeight = FontWeight.Bold)
internal val H4SPanStyle = SpanStyle(fontSize = 1.12.em, fontWeight = FontWeight.Bold)
internal val H5SPanStyle = SpanStyle(fontSize = 0.83.em, fontWeight = FontWeight.Bold)
internal val H6SPanStyle = SpanStyle(fontSize = 0.75.em, fontWeight = FontWeight.Bold)