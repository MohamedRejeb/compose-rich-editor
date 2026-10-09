package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.history.CommitTrigger
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph

/** An inline Markdown mark and the formatting it stands for. Double marks come first so `**` wins over `*`. */
@OptIn(ExperimentalRichTextApi::class)
internal enum class InlineShortcut(val mark: String, val feature: RichTextFeature) {
    Bold("**", RichTextFeature.Bold),
    BoldUnderscore("__", RichTextFeature.Bold),
    Strikethrough("~~", RichTextFeature.Strikethrough),
    Italic("*", RichTextFeature.Italic),
    ItalicUnderscore("_", RichTextFeature.Italic),
    Code("`", RichTextFeature.CodeSpan),
}

/** A completed inline pattern: its [shortcut], and the marks at [open] and [close] around the text. */
internal class InlineShortcutMatch(val shortcut: InlineShortcut, openStart: Int, closeEnd: Int) {
    val open: TextRange = TextRange(openStart, openStart + shortcut.mark.length)
    val close: TextRange = TextRange(closeEnd - shortcut.mark.length, closeEnd)
}

/**
 * The inline pattern whose closing mark ends at [caret] in [text], the text of one paragraph, or
 * null when none does. The opening mark is the nearest one before the closing mark. The text
 * between the marks is not empty and does not start or end with a space or with the mark's own
 * character, so `****` and `** a**` are text. A single `*` or `_` next to another one is half of
 * a double mark, so `**bold*` is still text. An underscore mark must not touch a letter or digit
 * outside the pair, so `snake_case_name` is text.
 */
internal fun findInlineShortcut(text: String, caret: Int): InlineShortcutMatch? =
    InlineShortcut.entries.firstNotNullOfOrNull { matchInlineShortcut(text, caret, it) }

private fun matchInlineShortcut(text: String, caret: Int, shortcut: InlineShortcut): InlineShortcutMatch? {
    val mark = shortcut.mark
    val markChar = mark[0]
    val closeStart = caret - mark.length
    if (closeStart < 0 || !text.startsWith(mark, closeStart)) return null
    val openStart = text.lastIndexOf(mark, closeStart - mark.length - 1)
    if (openStart < 0) return null

    val content = text.substring(openStart + mark.length, closeStart)
    val edges = listOf(content.first(), content.last())
    if (edges.any { it.isWhitespace() || it == markChar }) return null

    val before = text.getOrNull(openStart - 1)
    val after = text.getOrNull(caret)
    if (mark.length == 1 && (before == markChar || after == markChar)) return null
    if (markChar == '_' && (before?.isLetterOrDigit() == true || after?.isLetterOrDigit() == true)) return null
    return InlineShortcutMatch(shortcut, openStart, caret)
}

/**
 * Applies the typing shortcut an edit may have completed, when the matching setting in
 * [RichTextConfig] is on. [inserted] is the text the edit put in front of the caret. A space
 * after `#` to `######` at a paragraph start sets the heading level; a closing inline mark
 * formats the text between the marks. The marks are removed and the caret ends after the text.
 * Runs after the edit's own history record, so the conversion is a separate undo step.
 */
internal fun RichTextState.applyTypingShortcuts(inserted: String) {
    if (inserted.isEmpty() || '\n' in inserted) return
    val selection = textFieldValue.selection
    if (!selection.collapsed) return
    val caret = selection.min
    val text = textFieldValue.text
    val at = caret - inserted.length
    if (at < 0 || !text.startsWith(inserted, at)) return

    val (paragraph, paragraphRange) = paragraphAt(caret) ?: return
    // Markdown marks are code inside a code block.
    if (paragraph.type is CodeBlock) return
    val contentStart = paragraphRange.min + paragraph.type.startRichSpan.text.length
    if (caret <= contentStart) return

    if (!applyHeadingShortcut(paragraph, contentStart, caret))
        applyInlineShortcut(TextRange(contentStart, paragraphRange.max), caret, at)
}

/** The paragraph the caret is in and its range in the text, the list marker included. */
private fun RichTextState.paragraphAt(caret: Int): Pair<RichParagraph, TextRange>? {
    var start = 0
    richParagraphList.forEach { paragraph ->
        val end = start + paragraphLength(paragraph)
        if (caret in start..end) return paragraph to TextRange(start, end)
        start = end + 1
    }
    return null
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextState.applyHeadingShortcut(paragraph: RichParagraph, contentStart: Int, caret: Int): Boolean {
    if (!config.headingTypingShortcutsEnabled || RichTextFeature.Heading !in config.features) return false
    if (paragraph.type !is DefaultParagraph) return false
    val marker = textFieldValue.text.substring(contentStart, caret)
    if (!marker.endsWith(' ') || marker.dropLast(1).any { it != '#' }) return false
    val headingStyle = HeadingStyle.fromLevel(marker.length - 1)
    if (headingStyle == HeadingStyle.Normal) return false

    recordHistoryForInput(CommitTrigger.Structural) {
        applyChange(originalRange = TextRange(contentStart, caret), newText = "")
        setHeadingStyle(headingStyle, listOf(paragraph))
    }
    return true
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichTextState.applyInlineShortcut(content: TextRange, caret: Int, insertedAt: Int) {
    if (!config.inlineTypingShortcutsEnabled) return
    val text = textFieldValue.text
    val match = findInlineShortcut(text.substring(content.min, content.max), caret - content.min) ?: return
    if (match.shortcut.feature !in config.features) return
    val open = TextRange(match.open.min + content.min, match.open.max + content.min)
    val close = TextRange(match.close.min + content.min, match.close.max + content.min)
    // The opening mark was typed by an earlier edit: a pattern inserted whole is not a shortcut.
    if (open.min >= insertedAt) return
    val isPlainText = (open.min until close.max).all {
        getRichSpanByTextIndex(it, ignoreCustomFiltering = true)?.fullStyle is RichSpanStyle.Default
    }
    if (!isPlainText) return

    val formatted = TextRange(open.min, close.min - open.length)
    recordHistoryForInput(CommitTrigger.Structural) {
        // The closing mark is removed last so the caret ends after the formatted text.
        applyChange(originalRange = open, newText = "")
        applyChange(originalRange = TextRange(formatted.max, formatted.max + close.length), newText = "")
        val spanStyle = match.shortcut.spanStyle
        if (spanStyle != null) addSpanStyle(spanStyle, formatted) else addRichSpan(RichSpanStyle.Code(), formatted)
        // What is typed next continues outside the formatting, as in Notion and Google Docs.
        if (spanStyle != null) removeSpanStyle(spanStyle) else removeRichSpan(RichSpanStyle.Code())
    }
}

private val InlineShortcut.spanStyle: SpanStyle?
    get() = when (this) {
        InlineShortcut.Bold, InlineShortcut.BoldUnderscore -> SpanStyle(fontWeight = FontWeight.Bold)
        InlineShortcut.Italic, InlineShortcut.ItalicUnderscore -> SpanStyle(fontStyle = FontStyle.Italic)
        InlineShortcut.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        InlineShortcut.Code -> null
    }
