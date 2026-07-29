package com.mohamedrejeb.richeditor.document

/**
 * Immutable, structural snapshot of rich text content.
 *
 * Two [RichTextDocument]s are equal when their visible content and styling are equal,
 * independent of how the editor represented them internally. Suitable for test assertions,
 * persistence, and change observation.
 */
public data class RichTextDocument(
    public val blocks: List<RichTextBlock>,
) {
    init {
        require(blocks.isNotEmpty()) { "RichTextDocument must contain at least one block" }
    }

    public companion object {
        /** A document with a single empty paragraph, the same as a fresh editor state. */
        public fun empty(): RichTextDocument =
            RichTextDocument(blocks = listOf(RichTextBlock(text = "")))
    }
}
