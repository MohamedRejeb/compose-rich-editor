package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class OneSpaceParagraph : ParagraphType {
    override val style: ParagraphStyle =
        ParagraphStyle()

    @OptIn(ExperimentalRichTextApi::class)
    override val startRichSpan: RichSpan =
        RichSpan(
            paragraph = RichParagraph(type = this),
            text = " "
        )

    override fun getNextParagraphType(): ParagraphType =
        OneSpaceParagraph()

    override fun copy(): ParagraphType =
        OneSpaceParagraph()
}