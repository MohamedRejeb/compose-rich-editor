package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.SpanStyle
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

    // Filled in by the marks task; block-level encoding carries no marks yet.
    private fun extractMarks(leaves: List<Leaf>, paragraph: RichParagraph): List<RichTextSpanMark> =
        emptyList()
}
