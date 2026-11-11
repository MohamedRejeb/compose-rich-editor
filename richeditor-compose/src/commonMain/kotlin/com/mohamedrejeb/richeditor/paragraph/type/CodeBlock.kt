package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * One line of a code block. Consecutive code lines form one block; [isBlockStart] is set on
 * the first line so that two blocks next to each other stay two blocks. [language] is kept as
 * it was written, so a language the library cannot colour still round-trips.
 */
internal class CodeBlock(
    val language: String? = null,
    val isBlockStart: Boolean = true,
) : ParagraphType {

    override fun getStyle(config: RichTextConfig): ParagraphStyle = Style

    @OptIn(ExperimentalRichTextApi::class)
    override val startRichSpan: RichSpan =
        RichSpan(paragraph = RichParagraph(type = this))

    override fun getNextParagraphType(): ParagraphType =
        CodeBlock(language = language, isBlockStart = false)

    override fun copy(): ParagraphType =
        CodeBlock(language = language, isBlockStart = isBlockStart)

    override fun equals(other: Any?): Boolean =
        this === other || (other is CodeBlock && language == other.language && isBlockStart == other.isBlockStart)

    override fun hashCode(): Int = 31 * language.hashCode() + isBlockStart.hashCode()

    companion object {
        val Inset: TextUnit = 12.sp
        private val Style = ParagraphStyle(textIndent = TextIndent(firstLine = Inset, restLine = Inset))
    }
}
