package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

interface RichTextStyle {
    fun applyStyle(spanStyle: SpanStyle): SpanStyle

    object Bold : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(fontWeight = FontWeight.Bold)
        }
    }

    object Italic : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(fontStyle = FontStyle.Italic)
        }
    }

    object Underline : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(textDecoration = TextDecoration.Underline)
        }
    }

    object Strikethrough : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(textDecoration = TextDecoration.LineThrough)
        }
    }

    data class TextColor(val color: Color) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                color = color
            )
        }
    }

    data class BackgroundColor(val color: Color) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                background = color
            )
        }
    }

    data class FontSize(val fontSize: TextUnit) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = fontSize
            )
        }
    }
}