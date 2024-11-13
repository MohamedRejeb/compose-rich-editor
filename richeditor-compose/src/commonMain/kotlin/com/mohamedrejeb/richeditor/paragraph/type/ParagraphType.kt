package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig

public interface ParagraphType {

    public fun getStyle(config: RichTextConfig): ParagraphStyle

    public val startRichSpan: RichSpan

    public fun getNextParagraphType(): ParagraphType

    public fun copy(): ParagraphType

    public companion object {
        public val ParagraphType.startText : String get() = startRichSpan.text
    }
}