package com.mohamedrejeb.richeditor.paragraph

import androidx.compose.ui.text.SpanStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock

/** The paragraph index range of each code block: a run of code lines, cut where a line starts a new block. */
internal fun List<RichParagraph>.codeBlockGroups(): List<IntRange> {
    val groups = mutableListOf<IntRange>()
    var start = -1
    forEachIndexed { index, paragraph ->
        val type = paragraph.type as? CodeBlock
        if (start != -1 && (type == null || type.isBlockStart)) {
            groups += start until index
            start = -1
        }
        if (type != null && start == -1) start = index
    }
    if (start != -1) groups += start..lastIndex
    return groups
}

/** The text of this paragraph without its list prefix and without any styling. */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichParagraph.plainText(): String =
    buildString { children.forEach { appendPlainText(it) } }

@OptIn(ExperimentalRichTextApi::class)
private fun StringBuilder.appendPlainText(span: RichSpan) {
    append(span.text)
    span.children.forEach { appendPlainText(it) }
}

/** The paragraphs of a code block holding [lines]. A block always has at least one line. */
@OptIn(ExperimentalRichTextApi::class)
internal fun codeBlockOf(language: String?, lines: List<String>): List<RichParagraph> =
    lines.ifEmpty { listOf("") }.mapIndexed { index, line ->
        RichParagraph(type = CodeBlock(language = language, isBlockStart = index == 0)).also { paragraph ->
            paragraph.children.add(RichSpan(paragraph = paragraph, text = line))
        }
    }

/** These paragraphs as plain lines continuing the code block [type] is a line of. */
@OptIn(ExperimentalRichTextApi::class)
internal fun List<RichParagraph>.asCodeLinesOf(type: CodeBlock): List<RichParagraph> =
    map { source ->
        RichParagraph(type = CodeBlock(language = type.language, isBlockStart = false)).also { paragraph ->
            paragraph.children.add(RichSpan(paragraph = paragraph, text = source.plainText()))
        }
    }

/** The style every character of this paragraph starts from. */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichParagraph.baseSpanStyle(config: RichTextConfig): SpanStyle =
    if (type is CodeBlock) RichSpanStyle.DefaultSpanStyle.merge(SpanStyle(fontFamily = config.codeBlockFontFamily))
    else RichSpanStyle.DefaultSpanStyle
