package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.utils.customMerge

/**
 * The paragraphs this string describes. A line break (`\n`, `\r\n` or `\r`) separates
 * paragraphs, and so does the edge of a `ParagraphStyle` range inside a line, which is where
 * Compose breaks the paragraph when it lays the string out. `SpanStyle` ranges, `ParagraphStyle`
 * ranges, [Bullet], [LinkAnnotation.Url] and the legacy `UrlAnnotation` are read; every other
 * annotation is dropped.
 *
 * A [Bullet] makes the paragraph that starts where its range starts an unordered list item,
 * the one paragraph Compose draws the bullet on. The text indent that makes room for the bullet
 * gives the item its level and is then dropped, since the list type indents the item.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.toRichParagraphs(): List<RichParagraph> {
    val paragraphStyles = paragraphStyles.filter { it.start < it.end }
    val spanStyles = spanStyles.filter { it.start < it.end }
    val annotations = annotationRanges()
    val links = annotations.mapNotNull { range ->
        range.item.urlOrNull()?.let { AnnotatedString.Range(it, range.start, range.end) }
    }
    val bullets = annotations.filter { it.item is Bullet }
    var bulletIndents = emptyList<TextUnit>()

    return paragraphBounds(paragraphStyles).map { (start, end) ->
        val paragraph = RichParagraph()
        val paragraphStyle = paragraphStyles
            .filter { it.covers(start, end) }
            .fold(RichParagraph.DefaultParagraphStyle) { style, range -> style.merge(range.item) }
        if (bullets.any { it.start == start }) {
            bulletIndents = bulletIndents.withBulletIndent(paragraphStyle.textIndent?.firstLine)
            paragraph.type = UnorderedList(initialLevel = bulletIndents.size.coerceAtLeast(1))
            paragraph.paragraphStyle = paragraphStyle.copy(textIndent = null)
        } else {
            // A later line of a bullet's range continues its item and keeps the list open.
            if (bullets.none { it.covers(start, end) }) bulletIndents = emptyList()
            paragraph.paragraphStyle = paragraphStyle
        }

        val cuts = buildSet {
            add(start)
            add(end)
            (spanStyles + links).forEach { range ->
                add(range.start.coerceIn(start, end))
                add(range.end.coerceIn(start, end))
            }
        }.sorted()
        cuts.zipWithNext().forEach { (spanStart, spanEnd) ->
            val url = links.lastOrNull { it.covers(spanStart, spanEnd) }?.item
            paragraph.children += RichSpan(
                paragraph = paragraph,
                text = text.substring(spanStart, spanEnd),
                // Ranges apply in list order, so a later (nested) range wins property by property.
                spanStyle = spanStyles
                    .filter { it.covers(spanStart, spanEnd) }
                    .fold(SpanStyle()) { style, range -> style.customMerge(range.item) },
                richSpanStyle = url?.let { RichSpanStyle.Link(url = it) } ?: RichSpanStyle.Default,
            )
        }
        if (paragraph.children.isEmpty()) {
            paragraph.children += RichSpan(paragraph = paragraph)
        }
        paragraph
    }
}

/**
 * The non-empty annotation ranges in list order. The list is internal to Compose and [Bullet]
 * has no typed getter, so the ranges are collected while `mapAnnotations` visits them.
 */
private fun AnnotatedString.annotationRanges(): List<AnnotatedString.Range<out AnnotatedString.Annotation>> =
    buildList {
        mapAnnotations { range ->
            if (range.start < range.end) add(range)
            range
        }
    }

@OptIn(ExperimentalTextApi::class)
@Suppress("DEPRECATION")
private fun AnnotatedString.Annotation.urlOrNull(): String? = when (this) {
    is LinkAnnotation.Url -> url
    is UrlAnnotation -> url
    else -> null
}

/**
 * The indentations of the list levels open after a bulleted paragraph indented by [indent],
 * given the ones open before it, outermost first; the paragraph's level is the size of the
 * result. A paragraph indented further than the one before it opens a level, and one indented
 * less returns to the level with its indentation, which is how Compose nests bullet lists.
 * No level is open after an unspecified indentation, and one in another unit starts over.
 */
private fun List<TextUnit>.withBulletIndent(indent: TextUnit?): List<TextUnit> =
    if (indent == null || indent.isUnspecified) emptyList()
    else takeWhile { it.type == indent.type && it.value < indent.value } + indent

/**
 * Whether this range covers the non-empty piece `[start, end)`, which no range edge cuts, or
 * for an empty line the line break at `start`.
 */
private fun AnnotatedString.Range<*>.covers(start: Int, end: Int): Boolean =
    if (start < end) this.start < end && this.end > start
    else this.start <= start && this.end > start

/** The `[start, end)` bounds of each paragraph, line breaks excluded. */
private fun AnnotatedString.paragraphBounds(
    paragraphStyles: List<AnnotatedString.Range<ParagraphStyle>>,
): List<Pair<Int, Int>> = buildList {
    var lineStart = 0
    var index = 0
    while (index <= text.length) {
        val char = text.getOrNull(index)
        if (char != null && char != '\n' && char != '\r') {
            index++
            continue
        }
        val lineEnd = index
        val cuts = buildSet {
            add(lineStart)
            add(lineEnd)
            paragraphStyles.forEach { range ->
                if (range.start in lineStart..lineEnd) add(range.start)
                if (range.end in lineStart..lineEnd) add(range.end)
            }
        }.sorted()
        if (cuts.size == 1) add(lineStart to lineEnd)
        else addAll(cuts.zipWithNext())

        index += if (char == '\r' && text.getOrNull(index + 1) == '\n') 2 else 1
        lineStart = index
    }
}
