package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle

/** A styling mark over an inclusive character [range] of a [RichTextBlock]'s text. */
@ExperimentalRichTextApi
public sealed interface RichTextSpanMark {
    public val range: IntRange

    public data class Bold(override val range: IntRange) : RichTextSpanMark

    public data class Italic(override val range: IntRange) : RichTextSpanMark

    public data class Underline(override val range: IntRange) : RichTextSpanMark

    public data class Strikethrough(override val range: IntRange) : RichTextSpanMark

    public data class CodeSpan(override val range: IntRange) : RichTextSpanMark

    public data class Link(
        override val range: IntRange,
        public val url: String,
    ) : RichTextSpanMark

    public data class TextColor(
        override val range: IntRange,
        public val argb: Long,
    ) : RichTextSpanMark

    public data class Highlight(
        override val range: IntRange,
        public val argb: Long,
    ) : RichTextSpanMark

    public data class FontSize(
        override val range: IntRange,
        public val size: TextUnit,
    ) : RichTextSpanMark

    public data class FontWeight(
        override val range: IntRange,
        public val weight: Int,
    ) : RichTextSpanMark

    public data class LetterSpacing(
        override val range: IntRange,
        public val size: TextUnit,
    ) : RichTextSpanMark

    public data class BaselineShift(
        override val range: IntRange,
        public val multiplier: Float,
    ) : RichTextSpanMark

    public data class Shadow(
        override val range: IntRange,
        public val argb: Long,
        public val offsetX: Float,
        public val offsetY: Float,
        public val blurRadius: Float,
    ) : RichTextSpanMark

    public data class Image(
        override val range: IntRange,
        public val url: String,
        public val width: TextUnit = TextUnit.Unspecified,
        public val height: TextUnit = TextUnit.Unspecified,
        public val description: String? = null,
    ) : RichTextSpanMark

    public data class Token(
        override val range: IntRange,
        public val trigger: String,
        public val id: String,
        public val label: String,
    ) : RichTextSpanMark

    /** A mark kind this library version does not understand; preserved by the JSON codec. */
    public data class Unknown(
        override val range: IntRange,
        public val kind: String,
        public val rawJson: String,
    ) : RichTextSpanMark

    /**
     * An app-defined [RichSpanStyle] over [range], carried by instance. Equality follows the
     * style's own equals, which also drives coalescing of adjacent runs. In-memory only: the
     * JSON codec skips Custom marks because it cannot serialize arbitrary styles.
     */
    @ExperimentalRichTextApi
    public data class Custom(
        override val range: IntRange,
        public val style: RichSpanStyle,
    ) : RichTextSpanMark
}
