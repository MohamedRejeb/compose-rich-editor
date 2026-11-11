package com.mohamedrejeb.richeditor.paragraph

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
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

/**
 * The extra room a code block gets above its first line and below its last, so its background
 * does not sit tight on the code or on the text around it. A text layout has no space between
 * paragraphs, so the room is a taller first and last line. Null for any other paragraph.
 */
internal fun List<RichParagraph>.codeBlockEdgeStyle(index: Int): ParagraphStyle? {
    val type = getOrNull(index)?.type as? CodeBlock ?: return null
    val isFirst = type.isBlockStart || getOrNull(index - 1)?.type !is CodeBlock
    val next = getOrNull(index + 1)?.type as? CodeBlock
    val isLast = next == null || next.isBlockStart
    return when {
        isFirst && isLast -> CodeBlockOnlyLineStyle
        isFirst -> CodeBlockFirstLineStyle
        isLast -> CodeBlockLastLineStyle
        else -> null
    }
}

private val CodeBlockEdgeLineHeight = 1.75.em
private val CodeBlockOnlyLineHeight = 2.1.em

private val CodeBlockFirstLineStyle = ParagraphStyle(
    lineHeight = CodeBlockEdgeLineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.None),
)

private val CodeBlockLastLineStyle = ParagraphStyle(
    lineHeight = CodeBlockEdgeLineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Top, LineHeightStyle.Trim.None),
)

private val CodeBlockOnlyLineStyle = ParagraphStyle(
    lineHeight = CodeBlockOnlyLineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)
