package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig

internal interface ParagraphType {

    fun getStyle(config: RichTextConfig): ParagraphStyle

    val startRichSpan: RichSpan

    fun getNextParagraphType(): ParagraphType

    fun copy(): ParagraphType

    companion object {
        val ParagraphType.startText : String get() = startRichSpan.text
    }
}