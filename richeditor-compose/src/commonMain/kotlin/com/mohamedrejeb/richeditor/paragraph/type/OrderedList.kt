package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class OrderedList(
    number: Int,
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

    override var style: ParagraphStyle =
        getNewParagraphStyle()

    private fun getNewParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = (38 - startTextWidth.value).sp,
                restLine = 38.sp
            )
        )

    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    private fun getNewStartRichSpan() =
        RichSpan(
            paragraph = RichParagraph(type = this),
            text = "$number. ",
            spanStyle = startTextSpanStyle
        )

    override fun getNextParagraphType(): ParagraphType =
        OrderedList(
            number = number + 1,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth
        )

    override fun copy(): ParagraphType =
        OrderedList(
            number = number,
            startTextSpanStyle = startTextSpanStyle,
            startTextWidth = startTextWidth
        )
}