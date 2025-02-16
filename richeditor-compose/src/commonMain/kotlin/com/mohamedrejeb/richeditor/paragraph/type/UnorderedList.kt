package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.DefaultUnorderedListStyleType
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class UnorderedList private constructor(
    initialIndent: Int = DefaultListIndent,
    startTextSpanStyle: SpanStyle = SpanStyle(),
    startTextWidth: TextUnit = 0.sp,
    initialNestedLevel: Int = 1,
    initialStyleType: UnorderedListStyleType = DefaultUnorderedListStyleType,
): ParagraphType, ConfigurableStartTextWidth, ConfigurableNestedLevel {

    constructor(
        initialNestedLevel: Int = 1,
    ): this(
        initialIndent = DefaultListIndent,
        initialNestedLevel = initialNestedLevel,
    )

    constructor(
        config: RichTextConfig,
        startTextSpanStyle: SpanStyle = SpanStyle(),
        initialNestedLevel: Int = 1,
    ): this(
        initialIndent = config.unorderedListIndent,
        startTextSpanStyle = startTextSpanStyle,
        initialNestedLevel = initialNestedLevel,
        initialStyleType = config.unorderedListStyleType,
    )

    var startTextSpanStyle = startTextSpanStyle
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan(startRichSpan.textRange)
        }

    override var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var indent = initialIndent
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    override var nestedLevel = initialNestedLevel
        set(value) {
            field = value
            style = getNewParagraphStyle()
            startRichSpan = getNewStartRichSpan()
        }

    private var styleType = initialStyleType
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan()
        }

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.unorderedListIndent != indent) {
            indent = config.unorderedListIndent
        }

        if (config.unorderedListStyleType != styleType) {
            styleType = config.unorderedListStyleType
        }

        return style
    }

    private fun getNewParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = (indent * nestedLevel).sp,
                restLine = ((indent * nestedLevel) + startTextWidth.value).sp
            )
        )

    @OptIn(ExperimentalRichTextApi::class)
    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan(textRange: TextRange = TextRange(0)): RichSpan {
        val prefixIndex =
            (nestedLevel - 1).coerceIn(styleType.prefixes.indices)

        val prefix = styleType.prefixes
            .getOrNull(prefixIndex)
            ?: "•"

        val text = "$prefix "

        return RichSpan(
            paragraph = RichParagraph(type = this),
            text = text,
            spanStyle = startTextSpanStyle,
            textRange = TextRange(
                textRange.min,
                textRange.min + text.length
            )
        )
    }

    override fun getNextParagraphType(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
            initialStyleType = styleType,
        )

    override fun copy(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
            initialStyleType = styleType,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnorderedList) return false

        if (indent != other.indent) return false
        if (startTextSpanStyle != other.startTextSpanStyle) return false
        if (startTextWidth != other.startTextWidth) return false
        if (nestedLevel != other.nestedLevel) return false
        if (styleType != other.styleType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + startTextSpanStyle.hashCode()
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + nestedLevel
        result = 31 * result + styleType.hashCode()
        return result
    }
}
