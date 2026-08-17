package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@ExperimentalRichTextApi
public sealed interface RichTextBlockType {

    public data object Paragraph : RichTextBlockType

    /**
     * A list item. [indent] is the 0-based nesting depth. [startNumber] restarts ordered
     * numbering at this item (only meaningful for ordered lists). Negative values are
     * allowed, matching the HTML `start` attribute.
     */
    public data class ListItem(
        public val ordered: Boolean,
        public val indent: Int = 0,
        public val startNumber: Int? = null,
    ) : RichTextBlockType {
        init {
            require(indent >= 0) { "indent must be >= 0, was $indent" }
            require(startNumber == null || ordered) { "startNumber requires an ordered list" }
        }
    }
}
