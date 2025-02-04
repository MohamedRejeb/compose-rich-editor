package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class OrderedList(
    number: Int,
    initialIndent: Int = DefaultListIndent,
    startTextSpanStyle: SpanStyle = SpanStyle(),
    startTextWidth: TextUnit = 0.sp,
    initialNestedLevel: Int = 1,
) : ParagraphType, ConfigurableStartTextWidth, ConfigurableNestedLevel {

    var number = number
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan(startRichSpan.textRange)
        }

    var startTextSpanStyle = startTextSpanStyle
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    override var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var indent = initialIndent

    override var nestedLevel = initialNestedLevel
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.orderedListIndent != indent) {
            indent = config.orderedListIndent
            style = getNewParagraphStyle()
        }

        return style
    }

    private fun getNewParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = ((indent * nestedLevel) - startTextWidth.value).sp,
                restLine = (indent * nestedLevel).sp
            )
        )

    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan(textRange: TextRange = TextRange(0)): RichSpan {
        val text = "$number. "

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
        OrderedList(
            number = number + 1,
            initialIndent = indent,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
        )

    override fun copy(): ParagraphType =
        OrderedList(
            number = number,
            initialIndent = indent,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedList) return false

        if (number != other.number) return false
        if (indent != other.indent) return false
        if (startTextSpanStyle != other.startTextSpanStyle) return false
        if (startTextWidth != other.startTextWidth) return false
        if (nestedLevel != other.nestedLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + number
        result = 31 * result + indent
        result = 31 * result + startTextSpanStyle.hashCode()
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + nestedLevel
        return result
    }
}
