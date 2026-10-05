package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark

/**
 * The content of this document that [features] allows. Marks and block attributes of a
 * disallowed feature are dropped and their text kept, except images, whose placeholder
 * character goes with them; a block emptied that way is dropped too. Returns this instance
 * when [features] allows everything.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichTextDocument.restrictedTo(features: Set<RichTextFeature>): RichTextDocument {
    if (features.containsAll(RichTextFeature.entries)) return this
    val restricted = blocks.mapNotNull { it.restrictedTo(features) }
    return if (restricted.isEmpty()) RichTextDocument.empty() else RichTextDocument(restricted)
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextBlock.restrictedTo(features: Set<RichTextFeature>): RichTextBlock? {
    val block = if (RichTextFeature.Image in features) this else withoutImages() ?: return null
    return block.copy(
        type = block.type.restrictedTo(features),
        spans = block.spans.filter { it.feature()?.let(features::contains) == true },
        headingLevel = if (RichTextFeature.Heading in features) block.headingLevel else 0,
        textAlign = if (RichTextFeature.ParagraphStyle in features) block.textAlign else TextAlign.Unspecified,
        textDirection = if (RichTextFeature.ParagraphStyle in features) block.textDirection else TextDirection.Unspecified,
        lineHeight = if (RichTextFeature.ParagraphStyle in features) block.lineHeight else TextUnit.Unspecified,
        textIndent = if (RichTextFeature.ParagraphStyle in features) block.textIndent else null,
    )
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextBlockType.restrictedTo(features: Set<RichTextFeature>): RichTextBlockType {
    val feature = when (this) {
        is RichTextBlockType.ListItem -> if (ordered) RichTextFeature.OrderedList else RichTextFeature.UnorderedList
        is RichTextBlockType.TaskItem -> RichTextFeature.TaskList
        RichTextBlockType.Paragraph -> return this
    }
    return if (feature in features) this else RichTextBlockType.Paragraph
}

/**
 * This block without its image marks and their placeholder characters, the other marks
 * shifted over the removed characters, or null when no text remains.
 */
@OptIn(ExperimentalRichTextApi::class)
private fun RichTextBlock.withoutImages(): RichTextBlock? {
    val images = spans.filterIsInstance<RichTextSpanMark.Image>()
    if (images.isEmpty()) return this

    val kept = BooleanArray(text.length) { true }
    images.forEach { image -> image.range.forEach { kept[it] = false } }
    val newText = text.filterIndexed { index, _ -> kept[index] }
    if (newText.isEmpty()) return null

    // keptBefore[i] is the number of kept characters before index i, i.e. the new index of
    // the first kept character at or after i.
    val keptBefore = IntArray(text.length + 1)
    for (i in text.indices) keptBefore[i + 1] = keptBefore[i] + if (kept[i]) 1 else 0

    val shifted = spans.filter { it !is RichTextSpanMark.Image }.mapNotNull { mark ->
        val first = keptBefore[mark.range.first]
        val lastExclusive = keptBefore[mark.range.last + 1]
        if (lastExclusive <= first) null else mark.withRange(first until lastExclusive)
    }
    return copy(text = newText, spans = shifted)
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextSpanMark.feature(): RichTextFeature? = when (this) {
    is RichTextSpanMark.Bold -> RichTextFeature.Bold
    is RichTextSpanMark.Italic -> RichTextFeature.Italic
    is RichTextSpanMark.Underline -> RichTextFeature.Underline
    is RichTextSpanMark.Strikethrough -> RichTextFeature.Strikethrough
    is RichTextSpanMark.CodeSpan -> RichTextFeature.CodeSpan
    is RichTextSpanMark.Link -> RichTextFeature.Link
    is RichTextSpanMark.TextColor -> RichTextFeature.TextColor
    is RichTextSpanMark.Highlight -> RichTextFeature.Highlight
    is RichTextSpanMark.FontSize -> RichTextFeature.FontSize
    is RichTextSpanMark.FontWeight -> RichTextFeature.FontWeight
    is RichTextSpanMark.LetterSpacing -> RichTextFeature.LetterSpacing
    is RichTextSpanMark.BaselineShift -> RichTextFeature.BaselineShift
    is RichTextSpanMark.Shadow -> RichTextFeature.Shadow
    is RichTextSpanMark.Image -> RichTextFeature.Image
    is RichTextSpanMark.Token -> RichTextFeature.Token
    is RichTextSpanMark.Custom -> RichTextFeature.CustomSpanStyle
    is RichTextSpanMark.Unknown -> null
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextSpanMark.withRange(range: IntRange): RichTextSpanMark = when (this) {
    is RichTextSpanMark.Bold -> copy(range = range)
    is RichTextSpanMark.Italic -> copy(range = range)
    is RichTextSpanMark.Underline -> copy(range = range)
    is RichTextSpanMark.Strikethrough -> copy(range = range)
    is RichTextSpanMark.CodeSpan -> copy(range = range)
    is RichTextSpanMark.Link -> copy(range = range)
    is RichTextSpanMark.TextColor -> copy(range = range)
    is RichTextSpanMark.Highlight -> copy(range = range)
    is RichTextSpanMark.FontSize -> copy(range = range)
    is RichTextSpanMark.FontWeight -> copy(range = range)
    is RichTextSpanMark.LetterSpacing -> copy(range = range)
    is RichTextSpanMark.BaselineShift -> copy(range = range)
    is RichTextSpanMark.Shadow -> copy(range = range)
    is RichTextSpanMark.Image -> copy(range = range)
    is RichTextSpanMark.Token -> copy(range = range)
    is RichTextSpanMark.Custom -> copy(range = range)
    is RichTextSpanMark.Unknown -> copy(range = range)
}
