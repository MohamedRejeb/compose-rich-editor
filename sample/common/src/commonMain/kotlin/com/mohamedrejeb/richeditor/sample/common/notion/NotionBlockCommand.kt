package com.mohamedrejeb.richeditor.sample.common.notion

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * The set of block transformations exposed in the Notion-style slash menu.
 *
 * Every command here maps onto public `RichTextState` APIs. A couple of blocks
 * (Quote, Divider, Code) are visual approximations because the core library
 * doesn't yet model those as first-class paragraph types - the sample makes
 * the compromise explicit rather than hiding it.
 */
internal enum class NotionBlockCommand(
    val id: String,
    val label: String,
    val description: String,
    val keywords: List<String>,
    val iconLabel: String,
) {
    Text(
        id = "text",
        label = "Text",
        description = "Just start writing with plain text.",
        keywords = listOf("text", "paragraph", "plain"),
        iconLabel = "T",
    ),
    Heading1(
        id = "h1",
        label = "Heading 1",
        description = "Big section heading.",
        keywords = listOf("heading", "header", "h1", "title"),
        iconLabel = "H1",
    ),
    Heading2(
        id = "h2",
        label = "Heading 2",
        description = "Medium section heading.",
        keywords = listOf("heading", "header", "h2", "subtitle"),
        iconLabel = "H2",
    ),
    Heading3(
        id = "h3",
        label = "Heading 3",
        description = "Small section heading.",
        keywords = listOf("heading", "header", "h3"),
        iconLabel = "H3",
    ),
    BulletedList(
        id = "bulleted",
        label = "Bulleted list",
        description = "Create a simple bulleted list.",
        keywords = listOf("bullet", "unordered", "list", "ul"),
        iconLabel = "•",
    ),
    NumberedList(
        id = "numbered",
        label = "Numbered list",
        description = "Create a list with numbering.",
        keywords = listOf("number", "ordered", "list", "ol", "1"),
        iconLabel = "1.",
    ),
    Quote(
        id = "quote",
        label = "Quote",
        description = "Capture a quote.",
        keywords = listOf("quote", "blockquote", "citation"),
        iconLabel = "“",
    ),
    Divider(
        id = "divider",
        label = "Divider",
        description = "Visually divide blocks.",
        keywords = listOf("divider", "hr", "line", "rule"),
        iconLabel = "—",
    ),
    Code(
        id = "code",
        label = "Code",
        description = "Capture a code snippet.",
        keywords = listOf("code", "mono", "snippet"),
        iconLabel = "</>",
    );

    fun matches(query: String): Boolean {
        if (query.isEmpty()) return true
        val lowered = query.lowercase()
        if (label.lowercase().contains(lowered)) return true
        return keywords.any { it.contains(lowered) }
    }
}

private const val DIVIDER_TEXT = "────────────────"

/**
 * Mutates [state] to apply [command] to the paragraph that currently contains the
 * caret. Assumes any trigger query text (e.g. "/head") has already been removed
 * by the caller; see [NotionSlashMenu] for that orchestration.
 */
internal fun applyNotionBlockCommand(
    state: RichTextState,
    command: NotionBlockCommand,
) {
    when (command) {
        NotionBlockCommand.Text -> {
            state.setHeadingStyle(HeadingStyle.Normal)
            state.removeUnorderedList()
            state.removeOrderedList()
        }
        NotionBlockCommand.Heading1 -> {
            state.removeUnorderedList()
            state.removeOrderedList()
            state.setHeadingStyle(HeadingStyle.H1)
        }
        NotionBlockCommand.Heading2 -> {
            state.removeUnorderedList()
            state.removeOrderedList()
            state.setHeadingStyle(HeadingStyle.H2)
        }
        NotionBlockCommand.Heading3 -> {
            state.removeUnorderedList()
            state.removeOrderedList()
            state.setHeadingStyle(HeadingStyle.H3)
        }
        NotionBlockCommand.BulletedList -> {
            state.setHeadingStyle(HeadingStyle.Normal)
            state.addUnorderedList()
        }
        NotionBlockCommand.NumberedList -> {
            state.setHeadingStyle(HeadingStyle.Normal)
            state.addOrderedList()
        }
        NotionBlockCommand.Quote -> {
            state.setHeadingStyle(HeadingStyle.Normal)
            state.removeUnorderedList()
            state.removeOrderedList()
            state.toggleSpanStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = NotionColors.TextSecondary,
                )
            )
        }
        NotionBlockCommand.Divider -> {
            state.addTextAfterSelection("$DIVIDER_TEXT\n")
        }
        NotionBlockCommand.Code -> {
            state.toggleCodeSpan()
        }
    }
}
