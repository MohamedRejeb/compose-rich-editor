package com.mohamedrejeb.richeditor.paragraph.type

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Controls how the prefix of a list item ("•", "1.", etc.) inherits span styles
 * from the paragraph's text.
 *
 * @see com.mohamedrejeb.richeditor.model.RichTextConfig.listMarkerStyleBehavior
 */
@ExperimentalRichTextApi
public enum class ListMarkerStyleBehavior {
    /**
     * The marker inherits the paragraph's typography (color, font size, font
     * family, font weight, font style, letter spacing) but drops decorations
     * that are tied to the text content itself (underline, strikethrough,
     * background highlight, baseline shift, shadow, geometric transforms).
     *
     * Default. Matches the behavior of Google Docs and similar editors: bold
     * and font size apply to the bullet, but underline does not.
     */
    InheritFromText,

    /**
     * The marker always renders with the default span style, regardless of the
     * paragraph's first span. Useful for editors that want every bullet to
     * look the same no matter what formatting is applied to the text.
     */
    AlwaysDefault,
}
