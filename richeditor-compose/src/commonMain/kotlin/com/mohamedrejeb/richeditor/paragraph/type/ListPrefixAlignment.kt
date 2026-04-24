package com.mohamedrejeb.richeditor.paragraph.type

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Controls where the list marker ("1.", "10.", "•", ...) sits relative to the
 * indent gutter in ordered and unordered lists.
 */
@ExperimentalRichTextApi
public enum class ListPrefixAlignment {
    /**
     * HTML default: the marker sits inside the indent gutter and ends at the
     * content start, so markers of different widths are right-aligned
     * (the dots of "1.", "10.", and "11." line up vertically).
     *
     * When the configured indent is smaller than the marker width, the library
     * falls back to [Start] for that paragraph so the marker stays visible.
     */
    End,

    /**
     * Every list item's marker starts at the same left edge; the text after the
     * marker therefore sits at a slightly different x for single- vs double-digit
     * numbers. Useful when a uniform left edge is preferred over dot alignment.
     */
    Start,
}
