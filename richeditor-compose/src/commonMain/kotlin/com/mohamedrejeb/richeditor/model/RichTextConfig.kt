package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType

public class RichTextConfig internal constructor(
    private val updateText: () -> Unit,
) {
    public var linkColor: Color = Color.Blue
        set(value) {
            field = value
            updateText()
        }

    public var linkTextDecoration: TextDecoration = TextDecoration.Underline
        set(value) {
            field = value
            updateText()
        }

    public var codeSpanColor: Color = Color.Unspecified
        set(value) {
            field = value
            updateText()
        }

    public var codeSpanBackgroundColor: Color = Color.Transparent
        set(value) {
            field = value
            updateText()
        }

    public var codeSpanStrokeColor: Color = Color.LightGray
        set(value) {
            field = value
            updateText()
        }

    /**
     * The indent for ordered lists.
     */
    public var orderedListIndent: Int = DefaultListIndent
        set(value) {
            field = value
            updateText()
        }

    /**
     * The indent for unordered lists.
     */
    public var unorderedListIndent: Int = DefaultListIndent
        set(value) {
            field = value
            updateText()
        }

    /**
     * The indent for both ordered and unordered lists.
     *
     * This property is a shortcut for setting both [orderedListIndent] and [unorderedListIndent].
     */
    public var listIndent: Int = DefaultListIndent
        get() {
            if (orderedListIndent == unorderedListIndent)
                field = orderedListIndent

            return field
        }
        set(value) {
            field = value
            orderedListIndent = value
            unorderedListIndent = value
        }

    /**
     * The prefixes for unordered lists items.
     *
     * The prefixes are used in order if the list is nested.
     *
     * For example, if the list is nested twice, the first prefix is used for the first level,
     * the second prefix is used for the second level, and so on.
     *
     * If the list is nested more than the number of prefixes, the last prefix is used.
     *
     * The default prefixes are `•`, `◦`, and `▪`.
     */
    public var unorderedListStyleType: UnorderedListStyleType = DefaultUnorderedListStyleType
        set(value) {
            field = value
            updateText()
        }

    public var orderedListStyleType: OrderedListStyleType = DefaultOrderedListStyleType
        set(value) {
            field = value
            updateText()
        }

    /**
     * Whether to preserve the style when the line is empty.
     * The line can be empty when the user deletes all the characters
     * or when the user presses `enter` to create a new line.
     *
     * Default is `true`.
     */
    public var preserveStyleOnEmptyLine: Boolean = true

    /**
     * Whether to exit the list when pressing Enter on an empty list item.
     * When true, pressing Enter on an empty list item will convert it to a normal paragraph.
     * When false, pressing Enter on an empty list item will create a new list item.
     *
     * Default is `true`.
     */
    public var exitListOnEmptyItem: Boolean = true
}

internal const val DefaultListIndent = 38

internal val DefaultUnorderedListStyleType =
    UnorderedListStyleType.from("•", "◦", "▪")

internal val DefaultOrderedListStyleType: OrderedListStyleType =
    OrderedListStyleType.Multiple(
        OrderedListStyleType.Decimal,
        OrderedListStyleType.LowerRoman,
        OrderedListStyleType.LowerAlpha,
    )
