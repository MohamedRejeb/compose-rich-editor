package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

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

    object Red : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                color = Color.Red
            )
        }
    }

    object Strikethrough : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(textDecoration = TextDecoration.LineThrough)
        }
    }
}