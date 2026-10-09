package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@ExperimentalRichTextApi
public sealed interface RichTextBlockType {

    public data object Paragraph : RichTextBlockType

    /**
     * A list item. [indent] is the 0-based nesting depth. [startNumber] restarts ordered
     * numbering at this item (only meaningful for ordered lists). Negative values are
     * allowed, matching the HTML `start` attribute. [listStyleType] is the CSS
     * `list-style-type` keyword of the item's list (`lower-alpha`, `square`, ...), or null
     * when the list follows the editor's configured style.
     */
    public data class ListItem(
        public val ordered: Boolean,
        public val indent: Int = 0,
        public val startNumber: Int? = null,
        public val listStyleType: String? = null,
    ) : RichTextBlockType {
        init {
            require(indent >= 0) { "indent must be >= 0, was $indent" }
            require(startNumber == null || ordered) { "startNumber requires an ordered list" }
        }
    }

    /**
     * One line of a code block. Consecutive code lines form one block; [isBlockStart] marks its
     * first line, so two blocks next to each other stay apart. [language] is the tag as written
     * (`kotlin`, `js`, ...), or null when the block names none.
     */
    public data class CodeBlock(
        public val language: String? = null,
        public val isBlockStart: Boolean = true,
    ) : RichTextBlockType
}
