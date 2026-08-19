package com.mohamedrejeb.richeditor.parser.html

/**
 * Collapses runs of ASCII whitespace from HTML text content, matching the
 * "white-space: normal" rule that browsers apply by default.
 *
 * The non-breaking space (U+00A0, `&nbsp;`) is intentionally not collapsed
 * or trimmed: per the HTML spec it is not part of the collapsible whitespace
 * set, and authors use it to preserve visual leading or repeated whitespace
 * (e.g. indentation, see issue #388). The collapsing regex is therefore
 * restricted to ASCII whitespace, and `trimStart` relies on Kotlin's
 * `Char.isWhitespace`, which also returns false for U+00A0.
 *
 * @param input the input to remove extra spaces from.
 * @return the input without extra spaces.
 */
internal fun removeHtmlTextExtraSpaces(input: String, trimStart: Boolean = false): String {
    return input
        .replace('\n', ' ')
        .replace("[ \\t\\r\\u000C]+".toRegex(), " ")
        .let {
            if (trimStart)
                it.trimStart(::isCollapsibleHtmlWhitespace)
            else
                it
        }
}

/**
 * Whether [c] is the kind of whitespace that browsers collapse in HTML text
 * content (`white-space: normal`). This matches ASCII whitespace only;
 * non-breaking spaces (U+00A0) and other Unicode space characters are
 * preserved so leading or repeated visual whitespace authored with `&nbsp;`
 * survives encode/decode (see issue #388).
 *
 * `Char.isWhitespace()` cannot be used directly because Kotlin treats
 * U+00A0 as whitespace via `Character.isSpaceChar` on JVM, which would
 * trim out the very characters this function is meant to preserve.
 */
internal fun isCollapsibleHtmlWhitespace(c: Char): Boolean = when (c) {
    ' ', '\t', '\n', '\r', '\u000C' -> true
    else -> false
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
    "html",
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

internal const val BrElement = "br"

/** Span attribute carrying a registered custom style's kind. */
internal const val CustomSpanKindAttr = "data-richeditor-kind"

/** Span attribute carrying a registered custom style's attributes as an escaped k=v payload. */
internal const val CustomSpanAttrsAttr = "data-richeditor-attrs"

/**
 * Encodes descriptor attributes as `key=value` pairs joined by `&`, percent-escaping the
 * structural characters so keys and values survive verbatim. HTML attribute names are
 * case-insensitive, which is why the map rides in one attribute value instead of one
 * attribute per key.
 */
internal fun encodeCustomSpanAttrs(attributes: Map<String, String>): String =
    attributes.entries.joinToString("&") { (key, value) ->
        "${escapeCustomSpanToken(key)}=${escapeCustomSpanToken(value)}"
    }

internal fun decodeCustomSpanAttrs(payload: String): Map<String, String> =
    if (payload.isEmpty()) {
        emptyMap()
    } else {
        payload.split("&").associate { entry ->
            val separator = entry.indexOf('=')
            if (separator == -1) {
                unescapeCustomSpanToken(entry) to ""
            } else {
                unescapeCustomSpanToken(entry.substring(0, separator)) to
                    unescapeCustomSpanToken(entry.substring(separator + 1))
            }
        }
    }

private fun escapeCustomSpanToken(value: String): String =
    value.replace("%", "%25").replace("&", "%26").replace("=", "%3D")

private fun unescapeCustomSpanToken(value: String): String =
    value.replace("%3D", "=").replace("%26", "&").replace("%25", "%")