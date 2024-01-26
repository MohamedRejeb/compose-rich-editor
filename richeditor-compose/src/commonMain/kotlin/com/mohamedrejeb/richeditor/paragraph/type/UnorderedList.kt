package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class UnorderedList : ParagraphType {

    override var style: ParagraphStyle =
        getParagraphStyle()

    private fun getParagraphStyle() =
        ParagraphStyle(
            textIndent = TextIndent(
                firstLine = 38.sp,
                restLine = 38.sp
            )
        )

    override var startRichSpan: RichSpan =
        RichSpan(
            paragraph = RichParagraph(type = this),
            text = "• ",
        )

    override fun getNextParagraphType(): ParagraphType =
        UnorderedList()

    override fun copy(): ParagraphType =
        UnorderedList()
}