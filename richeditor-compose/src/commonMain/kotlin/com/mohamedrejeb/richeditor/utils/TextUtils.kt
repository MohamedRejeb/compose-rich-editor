package com.mohamedrejeb.richeditor.utils

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import kotlin.text.buildString
import kotlin.text.appendLine
import kotlin.text.StringBuilder

@OptIn(ExperimentalRichTextApi::class)
internal fun toText(richParagraphList: List<RichParagraph>): String =
    buildString {
        richParagraphList.fastForEachIndexed { i, richParagraphStyle ->
            append(richParagraphStyle.type.startText)

            append(
                richSpanList = richParagraphStyle.children,
            )

            if (i != richParagraphList.lastIndex)
                appendLine()
        }
    }

@OptIn(ExperimentalRichTextApi::class)
internal fun StringBuilder.append(
    richSpanList: List<RichSpan>,
) {
    richSpanList.fastForEach { richSpanStyle ->
        append(richSpanStyle.text)

        if (richSpanStyle.children.isNotEmpty())
            append(
                richSpanList = richSpanStyle.children,
            )
    }
}