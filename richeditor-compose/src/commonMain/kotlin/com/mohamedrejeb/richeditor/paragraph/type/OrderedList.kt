package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
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
    startTextWidth: TextUnit = 0.sp
) : ParagraphType {

    var number = number
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan()
        }

    var startTextSpanStyle = startTextSpanStyle
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var indent = initialIndent

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.listIndent != indent) {
            indent = config.listIndent
            style = getNewParagraphStyle()
        }

        return style
    }

    private fun getNewParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = (indent - startTextWidth.value).sp,
                restLine = indent.sp
            )
        )

    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan() =
        RichSpan(
            paragraph = RichParagraph(type = this),
            text = "$number. ",
            spanStyle = startTextSpanStyle
        )

    override fun getNextParagraphType(): ParagraphType =
        OrderedList(
            number = number + 1,
            initialIndent = indent,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth
        )

    override fun copy(): ParagraphType =
        OrderedList(
            number = number,
            initialIndent = indent,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedList) return false

        if (indent != other.indent) return false

        return true
    }

    override fun hashCode(): Int {
        return indent
    }
}
