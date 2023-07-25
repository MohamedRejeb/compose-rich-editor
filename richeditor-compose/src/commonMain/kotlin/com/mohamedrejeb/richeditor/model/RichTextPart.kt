package com.mohamedrejeb.richeditor.model

/**
 * A representation of a part of a rich text.
 * @param fromIndex the start index of the part (inclusive).
 * @param toIndex the end index of the part (inclusive).
 * @param styles the styles to apply to the part.
 * @see RichTextStyle
 */
@Deprecated(
    message = "Old class used with RichTextValue",
    level = DeprecationLevel.WARNING,
)
internal data class RichTextPart(
    val fromIndex: Int,
    val toIndex: Int,
    val styles: Set<RichTextStyle>,
)
