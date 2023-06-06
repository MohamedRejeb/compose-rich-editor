package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

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
            return spanStyle.copy(
                textDecoration = if (
                    spanStyle.textDecoration == TextDecoration.LineThrough ||
                    spanStyle.textDecoration == TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                ) {
                    TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                } else {
                    TextDecoration.Underline
                }
            )
        }
    }

    /**
     * [Strikethrough] implementation of [RichTextStyle] that applies a strikethrough to the text.
     * @see RichTextStyle
     */
    object Strikethrough : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                textDecoration = if (
                    spanStyle.textDecoration == TextDecoration.Underline ||
                    spanStyle.textDecoration == TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                ) {
                    TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                } else {
                    TextDecoration.LineThrough
                }
            )
        }
    }

    /**
     * [Superscript] implementation of [RichTextStyle] that applies a superscript to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_sup.asp">Superscript</a>
     */
    object Superscript : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                baselineShift = BaselineShift.Superscript,
                fontSize =
                if (spanStyle.fontSize.isSpecified) spanStyle.fontSize * 0.8f
                else TextUnit.Unspecified
            )
        }
    }

    /**
     * [Subscript] implementation of [RichTextStyle] that applies a subscript to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_sub.asp">Subscript</a>
     */
    object Subscript : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                baselineShift = BaselineShift.Subscript,
                fontSize =
                if (spanStyle.fontSize.isSpecified) spanStyle.fontSize * 0.8f
                else TextUnit.Unspecified
            )
        }
    }

    /**
     * [Small] implementation of [RichTextStyle] that applies a small style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_small.asp">Small</a>
     */
    object Small : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize =
                if (spanStyle.fontSize.isSpecified) spanStyle.fontSize * 0.8f
                else TextUnit.Unspecified
            )
        }
    }

    /**
     * [Mark] implementation of [RichTextStyle] that applies a mark style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_mark.asp">Mark</a>
     */
    object Mark : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                background = Color.Yellow
            )
        }
    }

    /**
     * [H1] implementation of [RichTextStyle] that applies a H1 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H1</a>
     */
    object H1 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [H2] implementation of [RichTextStyle] that applies a H2 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H2</a>
     */
    object H2 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [H3] implementation of [RichTextStyle] that applies a H3 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H3</a>
     */
    object H3 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [H4] implementation of [RichTextStyle] that applies a H4 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H4</a>
     */
    object H4 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [H5] implementation of [RichTextStyle] that applies a H5 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H5</a>
     */
    object H5 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [H6] implementation of [RichTextStyle] that applies a H6 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H6</a>
     */
    object H6 : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /**
     * [Normal] implementation of [RichTextStyle] that applies a H6 style to the text.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_hn.asp">H6</a>
     */
    object Normal : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
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


    /**
     * [Hyperlink] implementation of [RichTextStyle] that applies hyperlink to the text.
     * @param url is the link present in href="<Link>"
     */
    data class Hyperlink(val url: String) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle.copy(
                color = Color(0xFF5C8AFF),  // Blue color
                textDecoration = TextDecoration.Underline
            )
        }
    }

    object NormalText : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return SpanStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }

    /**
     * [UnorderedList] implementation of [RichTextStyle] that applies an unordered list style to the text.
     * Note: This implementation only prepends a bullet symbol. Actual list functionality may require additional logic.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_ul.asp">Unordered List</a>
     */
    object UnorderedList : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle
        }
    }

    /**
     * [UnorderedList] implementation of [RichTextStyle] that applies an unordered list style to the text.
     * Note: This implementation only prepends a bullet symbol. Actual list functionality may require additional logic.
     *
     * @see RichTextStyle
     * @see <a href="https://www.w3schools.com/tags/tag_ol.asp">Unordered List</a>
     */
    object OrderedList : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle
        }
    }

    object UnorderedListItem : RichTextStyle {

        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            // Prepend a bullet symbol to the text of the list item.
            return spanStyle
        }
    }

    /**
     * [OrderedListItem] implementation of [RichTextStyle] that applies an ordered list item style to the text.
     *
     * @param position the position of the item in the ordered list.
     * @see RichTextStyle
     */
    data class OrderedListItem(val position: Int) : RichTextStyle {
        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
            return spanStyle
        }
    }


}