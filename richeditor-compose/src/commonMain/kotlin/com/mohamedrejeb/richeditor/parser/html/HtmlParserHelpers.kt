package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.RichTextStyle

/**
 * Removes extra spaces from the given input. Because in HTML, extra spaces are ignored as well as new lines.
 *
 * @param input the input to remove extra spaces from.
 * @return the input without extra spaces.
 */
internal fun removeHtmlTextExtraSpaces(input: String, trimStart: Boolean = false): String {
    return input
        .replace("\n", " ")
        .replace("\\s+".toRegex(), " ")
        .let {
            if (trimStart) it.trimStart()
            else it
        }
}

/**
 * HTML inline elements.
 *
 * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
 */
internal val htmlInlineElements = setOf(
    "a", "abbr", "acronym", "b", "bdo", "big", "br", "button", "cite", "code", "dfn", "em", "i", "img", "input",
    "kbd", "label", "map", "object", "q", "samp", "script", "select", "small", "span", "strong", "sub", "sup",
    "textarea", "time", "tt", "var"
)

/**
 * HTML block elements.
 *
 * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
 */
internal val htmlBlockElements = setOf(
    "address", "article", "aside", "blockquote", "canvas", "dd", "div", "dl", "fieldset", "figcaption",
    "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "li", "main", "nav",
    "noscript", "ol", "p", "pre", "section", "table", "tfoot", "ul", "video"
)

/**
 * HTML elements that should be skipped.
 */
internal val skippedHtmlElements = setOf(
    "head",
    "meta",
    "title",
    "style",
    "script",
    "noscript",
    "link",
    "base",
    "template",
)