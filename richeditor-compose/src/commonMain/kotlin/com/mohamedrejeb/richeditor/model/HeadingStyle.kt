package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import org.intellij.markdown.MarkdownElementTypes

/**
 * Represents the heading level applied to a [com.mohamedrejeb.richeditor.paragraph.RichParagraph].
 *
 * Heading is a paragraph-level concept: every paragraph has exactly one [HeadingStyle], stored
 * directly on the paragraph (see [com.mohamedrejeb.richeditor.paragraph.RichParagraph.headingStyle]).
 * Persisting the level rather than fingerprinting the visual style means heading identity survives
 * theme changes, font customisation, and round-trips through HTML/Markdown.
 *
 * Each heading level carries:
 *  - a [level] (0 for [Normal], 1..6 for [H1]..[H6])
 *  - a [markdownPrefix] used when serialising to Markdown (`# `, `## `, ...)
 *  - an [htmlTag] used when serialising to HTML (`h1`..`h6`, or null for [Normal])
 *  - a [defaultSpanStyle] applied to every span in the paragraph so the editor renders
 *    appropriately. The defaults are framework-agnostic (em-based) so the core library does
 *    not depend on Material 2/3.
 */
public enum class HeadingStyle(
    public val level: Int,
    public val markdownPrefix: String,
    public val htmlTag: String?,
    internal val defaultSpanStyle: SpanStyle,
    internal val defaultParagraphStyle: ParagraphStyle = ParagraphStyle(),
) {
    /** Standard, non-heading paragraph. */
    Normal(level = 0, markdownPrefix = "", htmlTag = null, defaultSpanStyle = SpanStyle()),

    /** Level 1 heading (`<h1>`, `# `). */
    H1(1, "# ", "h1", SpanStyle(fontSize = 2.em, fontWeight = FontWeight.Bold)),

    /** Level 2 heading (`<h2>`, `## `). */
    H2(2, "## ", "h2", SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)),

    /** Level 3 heading (`<h3>`, `### `). */
    H3(3, "### ", "h3", SpanStyle(fontSize = 1.17.em, fontWeight = FontWeight.Bold)),

    /** Level 4 heading (`<h4>`, `#### `). */
    H4(4, "#### ", "h4", SpanStyle(fontSize = 1.12.em, fontWeight = FontWeight.Bold)),

    /** Level 5 heading (`<h5>`, `##### `). */
    H5(5, "##### ", "h5", SpanStyle(fontSize = 0.83.em, fontWeight = FontWeight.Bold)),

    /** Level 6 heading (`<h6>`, `###### `). */
    H6(6, "###### ", "h6", SpanStyle(fontSize = 0.75.em, fontWeight = FontWeight.Bold));

    /** Visual [SpanStyle] applied to spans inside a paragraph carrying this heading level. */
    public fun getSpanStyle(): SpanStyle = defaultSpanStyle

    /** Visual [ParagraphStyle] applied to a paragraph carrying this heading level. */
    public fun getParagraphStyle(): ParagraphStyle = defaultParagraphStyle

    public companion object {
        /** HTML heading tag names (`h1`..`h6`). */
        internal val headingTags: Set<String> = setOf("h1", "h2", "h3", "h4", "h5", "h6")

        /** Markdown ATX heading element types. */
        internal val markdownHeadingNodes = setOf(
            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6,
        )

        /** Returns the [HeadingStyle] for [level] (1..6), or [Normal] for any other value. */
        public fun fromLevel(level: Int): HeadingStyle =
            entries.firstOrNull { it.level == level } ?: Normal

        /** Returns the [HeadingStyle] for an HTML heading [tag] (`h1`..`h6`), or [Normal]. */
        public fun fromHtmlTag(tag: String): HeadingStyle =
            entries.firstOrNull { it.htmlTag == tag } ?: Normal
    }
}
