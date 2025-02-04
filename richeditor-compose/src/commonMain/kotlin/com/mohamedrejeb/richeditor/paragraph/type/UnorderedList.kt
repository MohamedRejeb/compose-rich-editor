package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class UnorderedList(
    initialIndent: Int = DefaultListIndent,
    startTextWidth: TextUnit = 0.sp,
    initialNestedLevel: Int = 1,
): ParagraphType, ConfigurableStartTextWidth, ConfigurableNestedLevel {

    override var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getParagraphStyle()
        }

    private var indent = initialIndent

    override var nestedLevel = initialNestedLevel
        set(value) {
            field = value
            style = getParagraphStyle()
        }

    private var style: ParagraphStyle =
        getParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.unorderedListIndent != indent) {
            indent = config.unorderedListIndent
            style = getParagraphStyle()
        }

        return style
    }

    private fun getParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = (indent * nestedLevel).sp,
                restLine = ((indent * nestedLevel) + startTextWidth.value).sp
            )
        )

    @OptIn(ExperimentalRichTextApi::class)
    override var startRichSpan: RichSpan =
        RichSpan(
            paragraph = RichParagraph(type = this),
            text = "• ",
        )

    override fun getNextParagraphType(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
        )

    override fun copy(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialNestedLevel = nestedLevel,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnorderedList) return false

        if (indent != other.indent) return false
        if (startTextWidth != other.startTextWidth) return false
        if (nestedLevel != other.nestedLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + nestedLevel
        return result
    }
}
