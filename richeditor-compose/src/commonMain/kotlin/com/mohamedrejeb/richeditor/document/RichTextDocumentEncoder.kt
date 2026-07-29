package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.isSpecified
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.utils.diff

/** U+FFFC, the placeholder each inline image occupies in a [RichTextBlock]'s text. */
internal const val InlineImagePlaceholder: Char = '￼'

@OptIn(ExperimentalRichTextApi::class)
internal object RichTextDocumentEncoder {

    fun encode(state: RichTextState): RichTextDocument =
        RichTextDocument(
            blocks = state.richParagraphList
                .map { paragraph -> encodeParagraph(paragraph) }
                .ifEmpty { listOf(RichTextBlock(text = "")) },
        )

    private data class Leaf(
        val start: Int,
        val endExclusive: Int,
        val spanStyle: SpanStyle,
        val richSpanStyle: RichSpanStyle,
    )

    private fun encodeParagraph(paragraph: RichParagraph): RichTextBlock {
        val text = StringBuilder()
        val leaves = mutableListOf<Leaf>()
        paragraph.children.forEach { collectLeaves(it, text, leaves) }

        val type = when (val paragraphType = paragraph.type) {
            is OrderedList -> RichTextBlockType.ListItem(
                ordered = true,
                indent = paragraphType.level - 1,
                startNumber = paragraphType.startFrom.takeIf { it != 1 },
            )
            is UnorderedList -> RichTextBlockType.ListItem(
                ordered = false,
                indent = paragraphType.level - 1,
            )
            else -> RichTextBlockType.Paragraph
        }

        // Heading visuals are baked into the paragraph style by parsers and toggleHeading;
        // subtract them the same way the HTML exporter does so they never leak into the
        // canonical document (heading identity is carried by headingLevel).
        val headingStyle = paragraph.headingStyle
        val style =
            if (headingStyle == HeadingStyle.Normal)
                paragraph.paragraphStyle
            else
                paragraph.paragraphStyle.diff(headingStyle.defaultParagraphStyle)

        return RichTextBlock(
            text = text.toString(),
            type = type,
            spans = extractMarks(leaves, paragraph),
            headingLevel = headingStyle.level,
            textAlign = style.textAlign,
            textDirection = style.textDirection,
            lineHeight = style.lineHeight,
            // List indentation is derived from config, not content; only keep user indent
            // for non-list paragraphs (mirror RichTextStateHtmlParser.decode).
            textIndent = style.textIndent.takeIf { type is RichTextBlockType.Paragraph },
            isLineBreak = paragraph.isFromLineBreak,
        )
    }

    private fun collectLeaves(span: RichSpan, out: StringBuilder, leaves: MutableList<Leaf>) {
        val richStyle = span.richSpanStyle
        val text = when {
            richStyle is RichSpanStyle.Image ->
                if (richStyle.model is String) InlineImagePlaceholder.toString() else ""
            else -> span.text
        }
        if (text.isNotEmpty()) {
            leaves += Leaf(
                start = out.length,
                endExclusive = out.length + text.length,
                spanStyle = span.fullSpanStyle,
                richSpanStyle = span.fullStyle,
            )
            out.append(text)
        }
        span.children.forEach { collectLeaves(it, out, leaves) }
    }

    private fun extractMarks(leaves: List<Leaf>, paragraph: RichParagraph): List<RichTextSpanMark> {
        if (leaves.isEmpty()) return emptyList()

        // Heading visuals are structural, carried by headingLevel; strip them from the
        // leaf styles exactly like the HTML exporter does before emitting marks.
        val headingStyle = paragraph.headingStyle
        val runsSource =
            if (headingStyle == HeadingStyle.Normal)
                leaves
            else
                leaves.map { it.copy(spanStyle = it.spanStyle.diff(headingStyle.defaultSpanStyle)) }

        val marks = mutableListOf<RichTextSpanMark>()
        marks += booleanRuns(runsSource) { it.spanStyle.fontWeight == FontWeight.Bold }
            .map { RichTextSpanMark.Bold(it) }
        marks += booleanRuns(runsSource) { it.spanStyle.fontStyle == FontStyle.Italic }
            .map { RichTextSpanMark.Italic(it) }
        marks += booleanRuns(runsSource) { it.spanStyle.textDecoration?.contains(TextDecoration.Underline) == true }
            .map { RichTextSpanMark.Underline(it) }
        marks += booleanRuns(runsSource) { it.spanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true }
            .map { RichTextSpanMark.Strikethrough(it) }
        marks += booleanRuns(runsSource) { it.richSpanStyle is RichSpanStyle.Code }
            .map { RichTextSpanMark.CodeSpan(it) }
        marks += valueRuns(runsSource) { (it.richSpanStyle as? RichSpanStyle.Link)?.url }
            .map { (range, url) -> RichTextSpanMark.Link(range, url) }
        marks += valueRuns(runsSource) { it.spanStyle.color.takeIf { color -> color.isSpecified }?.toArgbLong() }
            .map { (range, argb) -> RichTextSpanMark.TextColor(range, argb) }
        marks += valueRuns(runsSource) { it.spanStyle.background.takeIf { color -> color.isSpecified }?.toArgbLong() }
            .map { (range, argb) -> RichTextSpanMark.Highlight(range, argb) }
        marks += valueRuns(runsSource) { it.spanStyle.fontSize.takeIf { size -> size.isSpecified } }
            .map { (range, size) -> RichTextSpanMark.FontSize(range, size) }
        marks += valueRuns(runsSource) {
            it.spanStyle.fontWeight?.weight?.takeIf { weight -> weight != 400 && weight != 700 }
        }.map { (range, weight) -> RichTextSpanMark.FontWeight(range, weight) }
        marks += valueRuns(runsSource) { it.spanStyle.letterSpacing.takeIf { size -> size.isSpecified } }
            .map { (range, size) -> RichTextSpanMark.LetterSpacing(range, size) }
        marks += valueRuns(runsSource) {
            it.spanStyle.baselineShift?.multiplier?.takeIf { multiplier -> multiplier != 0f }
        }.map { (range, multiplier) -> RichTextSpanMark.BaselineShift(range, multiplier) }
        marks += valueRuns(runsSource) { it.spanStyle.shadow }
            .map { (range, shadow) ->
                RichTextSpanMark.Shadow(
                    range = range,
                    argb = shadow.color.toArgbLong(),
                    offsetX = shadow.offset.x,
                    offsetY = shadow.offset.y,
                    blurRadius = shadow.blurRadius,
                )
            }
        runsSource.forEach { leaf ->
            when (val rich = leaf.richSpanStyle) {
                is RichSpanStyle.Image -> (rich.model as? String)?.let { url ->
                    marks += RichTextSpanMark.Image(
                        range = leaf.start..leaf.endExclusive - 1,
                        url = url,
                        width = rich.width,
                        height = rich.height,
                        description = rich.contentDescription?.takeIf { it.isNotEmpty() },
                    )
                }
                is RichSpanStyle.Token -> marks += RichTextSpanMark.Token(
                    range = leaf.start..leaf.endExclusive - 1,
                    trigger = rich.triggerId,
                    id = rich.id,
                    label = rich.label,
                )
                else -> Unit
            }
        }
        return marks.sortedWith(compareBy({ it.range.first }, { kindOrder(it) }, { it.range.last }))
    }

    private fun booleanRuns(leaves: List<Leaf>, predicate: (Leaf) -> Boolean): List<IntRange> =
        valueRuns(leaves) { leaf -> true.takeIf { predicate(leaf) } }.map { it.first }

    /** Merges adjacent leaves carrying an equal non-null selected value into inclusive ranges. */
    private fun <V : Any> valueRuns(leaves: List<Leaf>, selector: (Leaf) -> V?): List<Pair<IntRange, V>> {
        val runs = mutableListOf<Pair<IntRange, V>>()
        var currentStart = -1
        var currentEnd = -1
        var currentValue: V? = null
        leaves.forEach { leaf ->
            val value = selector(leaf)
            if (value != null && value == currentValue && leaf.start == currentEnd) {
                currentEnd = leaf.endExclusive
            } else {
                currentValue?.let { runs += (currentStart..currentEnd - 1) to it }
                currentValue = value
                currentStart = leaf.start
                currentEnd = leaf.endExclusive
            }
        }
        currentValue?.let { runs += (currentStart..currentEnd - 1) to it }
        return runs
    }

    private fun kindOrder(mark: RichTextSpanMark): Int = when (mark) {
        is RichTextSpanMark.Bold -> 0
        is RichTextSpanMark.Italic -> 1
        is RichTextSpanMark.Underline -> 2
        is RichTextSpanMark.Strikethrough -> 3
        is RichTextSpanMark.CodeSpan -> 4
        is RichTextSpanMark.Link -> 5
        is RichTextSpanMark.TextColor -> 6
        is RichTextSpanMark.Highlight -> 7
        is RichTextSpanMark.FontSize -> 8
        is RichTextSpanMark.FontWeight -> 9
        is RichTextSpanMark.LetterSpacing -> 10
        is RichTextSpanMark.BaselineShift -> 11
        is RichTextSpanMark.Shadow -> 12
        is RichTextSpanMark.Image -> 13
        is RichTextSpanMark.Token -> 14
        is RichTextSpanMark.Unknown -> 15
    }

    private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
}
