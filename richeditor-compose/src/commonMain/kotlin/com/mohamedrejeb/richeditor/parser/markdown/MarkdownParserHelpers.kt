package com.mohamedrejeb.richeditor.parser.markdown

import org.intellij.markdown.MarkdownElementTypes

/**
 * Markdown block elements.
 *
 * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
 */
internal val markdownBlockElements = setOf(
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6,
    MarkdownElementTypes.ORDERED_LIST,
    MarkdownElementTypes.UNORDERED_LIST,
)
