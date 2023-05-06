package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * A style that can be applied to a [RichTextPart].
 * @see RichTextPart
 */
interface RichTextStyle {
    /**
     * Applies the style to the given [spanStyle].
     * @param spanStyle the [SpanStyle] to apply the style to.
     * @return the [SpanStyle] with the style applied.
     * @see SpanStyle
     */
    fun applyStyle(spanStyle: SpanStyle): SpanStyle

    /**
     * [Bold] implementation of [RichTextStyle] that applies a bold style to the text.
     * @see RichTextStyle
     */
    object Bold : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(fontWeight = FontWeight.Bold)
        }
    }

    /**
     * [Italic] implementation of [RichTextStyle] that applies an italic style to the text.
     * @see RichTextStyle
     */
    object Italic : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(fontStyle = FontStyle.Italic)
        }
    }

    /**
     * [Underline] implementation of [RichTextStyle] that applies an underline to the text.
     * @see RichTextStyle
     */
    object Underline : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(textDecoration = TextDecoration.Underline)
        }
    }

    /**
     * [Strikethrough] implementation of [RichTextStyle] that applies a strikethrough to the text.
     * @see RichTextStyle
     */
    object Strikethrough : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(textDecoration = TextDecoration.LineThrough)
        }
    }

    /**
     * [TextColor] implementation of [RichTextStyle] that applies a color to the text.
     * @param color the color to apply.
     * @see RichTextStyle
     */
    data class TextColor(val color: Color) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                color = color
            )
        }
    }

    /**
     * [BackgroundColor] implementation of [RichTextStyle] that applies a background color to the text.
     * @param color the color to apply.
     * @see RichTextStyle
     */
    data class BackgroundColor(val color: Color) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                background = color
            )
        }
    }

    /**
     * [FontSize] implementation of [RichTextStyle] that applies a font size to the text.
     * @param fontSize the font size to apply.
     * @see RichTextStyle
     */
    data class FontSize(val fontSize: TextUnit) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = fontSize
            )
        }
    }
}