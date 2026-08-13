package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.utils.InlineContentPlaceholder
import com.mohamedrejeb.richeditor.utils.customMerge

@OptIn(ExperimentalRichTextApi::class)
internal object RichTextDocumentDecoder {

    fun decode(document: RichTextDocument): List<RichParagraph> {
        val paragraphs = document.blocks.map { decodeBlock(it) }
        applyOrderedNumbers(paragraphs, document.blocks)
        return paragraphs
    }

    private fun decodeBlock(block: RichTextBlock): RichParagraph {
        val paragraph = RichParagraph()
        paragraph.paragraphStyle = ParagraphStyle(
            textAlign = block.textAlign,
            textDirection = block.textDirection,
            lineHeight = block.lineHeight,
            textIndent = block.textIndent,
        )
        paragraph.isFromLineBreak = block.isLineBreak
        val type = block.type
        if (type is RichTextBlockType.ListItem) {
            // Constructed like RichTextStateHtmlParser does for <li>; the real number is
            // recomputed in applyOrderedNumbers.
            paragraph.type =
                if (type.ordered) {
                    OrderedList(
                        number = 1,
                        initialLevel = type.indent + 1,
                        startFrom = type.startNumber ?: 1,
                    )
                } else {
                    UnorderedList(initialLevel = type.indent + 1)
                }
        }

        val cuts = buildSet {
            add(0)
            add(block.text.length)
            block.spans.forEach { mark ->
                add(mark.range.first)
                add(mark.range.last + 1)
            }
        }.sorted()
        cuts.zipWithNext().forEach { (segStart, segEnd) ->
            if (segStart >= segEnd) return@forEach
            val covering = block.spans.filter { it.range.first <= segStart && segEnd - 1 <= it.range.last }
            val richStyle = covering.richSpanStyleWithPrecedence()
            paragraph.children += RichSpan(
                paragraph = paragraph,
                // Image spans own a single inline-content placeholder char in the raw text
                // so span textRanges line up with the rendered annotated string (see #466
                // and RichTextStateHtmlParser's img handling).
                text = if (richStyle is RichSpanStyle.Image) InlineContentPlaceholder
                       else block.text.substring(segStart, segEnd),
                spanStyle = covering.mergedSpanStyle(),
                richSpanStyle = richStyle,
            )
        }
        if (paragraph.children.isEmpty()) {
            paragraph.children += RichSpan(paragraph = paragraph)
        }

        // Bake heading visuals the way parsers do: heading defaults act as the base style
        // and user marks win over them, so explicit overrides (font size, weight 400) are
        // never clobbered on decode. Direct headingStyle assignment is the parser path.
        if (block.headingLevel > 0) {
            val headingStyle = HeadingStyle.fromLevel(block.headingLevel)
            paragraph.headingStyle = headingStyle
            paragraph.children.forEach { span ->
                span.spanStyle = headingStyle.defaultSpanStyle.customMerge(span.spanStyle)
            }
            paragraph.paragraphStyle = headingStyle.defaultParagraphStyle.merge(paragraph.paragraphStyle)
        }
        return paragraph
    }

    private fun List<RichTextSpanMark>.mergedSpanStyle(): SpanStyle {
        var style = SpanStyle()
        val decorations = mutableListOf<TextDecoration>()
        forEach { mark ->
            when (mark) {
                is RichTextSpanMark.Bold -> style = style.copy(fontWeight = FontWeight.Bold)
                is RichTextSpanMark.FontWeight -> style = style.copy(fontWeight = FontWeight(mark.weight))
                is RichTextSpanMark.Italic -> style = style.copy(fontStyle = FontStyle.Italic)
                is RichTextSpanMark.Underline -> decorations += TextDecoration.Underline
                is RichTextSpanMark.Strikethrough -> decorations += TextDecoration.LineThrough
                is RichTextSpanMark.TextColor -> style = style.copy(color = Color(mark.argb.toInt()))
                is RichTextSpanMark.Highlight -> style = style.copy(background = Color(mark.argb.toInt()))
                is RichTextSpanMark.FontSize -> style = style.copy(fontSize = mark.size)
                is RichTextSpanMark.LetterSpacing -> style = style.copy(letterSpacing = mark.size)
                is RichTextSpanMark.BaselineShift -> style = style.copy(baselineShift = BaselineShift(mark.multiplier))
                is RichTextSpanMark.Shadow -> style = style.copy(
                    shadow = Shadow(
                        color = Color(mark.argb.toInt()),
                        offset = Offset(mark.offsetX, mark.offsetY),
                        blurRadius = mark.blurRadius,
                    ),
                )
                is RichTextSpanMark.Link, is RichTextSpanMark.CodeSpan,
                is RichTextSpanMark.Image, is RichTextSpanMark.Token,
                is RichTextSpanMark.Unknown -> Unit
            }
        }
        if (decorations.isNotEmpty()) {
            style = style.copy(textDecoration = TextDecoration.combine(decorations))
        }
        return style
    }

    /** Precedence when several rich-span marks cover one segment: Image, Token, Link, CodeSpan. */
    private fun List<RichTextSpanMark>.richSpanStyleWithPrecedence(): RichSpanStyle {
        (firstOrNull { it is RichTextSpanMark.Image } as? RichTextSpanMark.Image)?.let { image ->
            return RichSpanStyle.Image(
                model = image.url,
                width = if (image.width.isSpecified) image.width else 0.sp,
                height = if (image.height.isSpecified) image.height else 0.sp,
                contentDescription = image.description.orEmpty(),
            )
        }
        (firstOrNull { it is RichTextSpanMark.Token } as? RichTextSpanMark.Token)?.let { token ->
            return RichSpanStyle.Token(triggerId = token.trigger, id = token.id, label = token.label)
        }
        (firstOrNull { it is RichTextSpanMark.Link } as? RichTextSpanMark.Link)?.let { link ->
            return RichSpanStyle.Link(url = link.url)
        }
        if (any { it is RichTextSpanMark.CodeSpan }) return RichSpanStyle.Code()
        return RichSpanStyle.Default
    }

    private fun applyOrderedNumbers(paragraphs: List<RichParagraph>, blocks: List<RichTextBlock>) {
        // Mirrors RichTextStateHtmlParser's ordered list counter semantics.
        val counters = mutableMapOf<Int, Int>()
        paragraphs.forEachIndexed { index, paragraph ->
            when (val type = paragraph.type) {
                is OrderedList -> {
                    counters.keys.filter { it > type.level }.forEach(counters::remove)
                    val startNumber = (blocks[index].type as? RichTextBlockType.ListItem)?.startNumber
                    val number = startNumber ?: ((counters[type.level] ?: 0) + 1)
                    type.number = number
                    counters[type.level] = number
                }
                is UnorderedList -> {
                    counters.keys.filter { it >= type.level }.forEach(counters::remove)
                }
                else -> counters.clear()
            }
        }
    }
}
