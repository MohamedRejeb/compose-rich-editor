package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.parser.RichTextParser

internal object RichTextHtmlParser : RichTextParser<String> {

    override fun encode(input: String): RichTextValue {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        var text = ""
        val currentStyles: MutableList<RichTextStyle> = mutableListOf()
        val parts: MutableList<RichTextPart> = mutableListOf()
        var listCounter = 1
        val listStyleStack: MutableList<RichTextStyle> = mutableListOf()
        val handler = KsoupHtmlHandler
            .Builder()
            .onText {
                val lastOpenedTag = openedTags.lastOrNull()?.first
                if (lastOpenedTag in skippedHtmlElements) return@onText

                val addedText = removeHtmlTextExtraSpaces(
                    input = it,
                    trimStart = text.lastOrNull() == ' ' || text.lastOrNull() == '\n',
                )
                text += addedText

                parts.add(
                    RichTextPart(
                        fromIndex = text.length - addedText.length,
                        toIndex = text.lastIndex,
                        styles = currentStyles.toSet()
                    )
                )
            }
            .onOpenTag { name, attributes, _ ->
                if (name == "br") {
                    text += "\n"
                    return@onOpenTag
                }
                openedTags.add(name to attributes)

                val cssStyleMap =
                    attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                val cssSpanStyle = CssEncoder.parseCssStyleMapToSpanStyle(cssStyleMap)
                val richTextStyle = htmlElementsStyleEncodeMap[name]

                if (cssSpanStyle != SpanStyle() || richTextStyle != null) {
                    when (name) {
                        "h1" -> currentStyles.add(RichTextStyle.H1)
                        "h2" -> currentStyles.add(RichTextStyle.H2)
                        "h3" -> currentStyles.add(RichTextStyle.H3)
                        "h4" -> currentStyles.add(RichTextStyle.H4)
                        "h5" -> currentStyles.add(RichTextStyle.H5)
                        "h6" -> currentStyles.add(RichTextStyle.H6)
                        "ul" -> currentStyles.add(RichTextStyle.UnorderedList)
                    }
                }

                if (
                    text.lastOrNull() != null &&
                    text.lastOrNull()?.toString() != "\n" &&
                    name in htmlBlockElements
                ) {
                    text += "\n"
                }

                if (name == "a" && name in htmlInlineElements) {
                    val href = attributes["href"] ?: ""
                    currentStyles.add(RichTextStyle.Hyperlink(href))
                }

            }
            .onCloseTag { name, _ ->
                openedTags.removeLastOrNull()
                currentStyles.removeLastOrNull()

                if (name in htmlBlockElements) {
                    text += "\n"
                }

                if (name == "a" && name in htmlInlineElements) {
                    text += " "
                    currentStyles.removeLastOrNull()
                }

                // If it's a heading, add a new line after the closing tag
                if (name in setOf("h1", "h2", "h3", "h4", "h5", "h6")) {
                    text += "\n"
                }
            }
            .build()

        val parser = KsoupHtmlParser(
            handler = handler
        )

        parser.write(input)
        parser.end()
        return RichTextValue(
            textFieldValue = TextFieldValue(text),
            currentStyles = currentStyles.toSet(),
            parts = parts
        )
    }

    override fun decode(richTextValue: RichTextValue): String {
        val text = richTextValue.textFieldValue.text
        val parts = richTextValue.parts.sortedBy { it.fromIndex }

        val builder = StringBuilder()

        for (part in parts) {
            val partText = text.substring(part.fromIndex, part.toIndex + 1).replace("\n", "<br>")
            val partStyles = part.styles.toMutableSet()

            var tagName: String
            var tagStyle = ""

            // Handle hyperlink separately, extract href attribute and tag name
            if (partStyles.any { it is RichTextStyle.Hyperlink }) {
                val hyperlinkStyle =
                    partStyles.first { it is RichTextStyle.Hyperlink } as RichTextStyle.Hyperlink
                tagName = "a"
                tagStyle = " href=\"${hyperlinkStyle.url}\""
                partStyles.remove(hyperlinkStyle)
            } else {
                tagName =
                    partStyles
                        .firstOrNull { htmlElementsStyleDecodeMap.containsKey(it) }
                        ?.let {
                            partStyles.remove(it)
                            htmlElementsStyleDecodeMap[it]
                        }
                        ?: if (part.fromIndex > 0 && text[part.fromIndex - 1] != '\n') "span" else "p"

                tagStyle = if (partStyles.isEmpty()) "" else {
                    val stylesToApply = partStyles
                        .fold(SpanStyle()) { acc, richTextStyle -> richTextStyle.applyStyle(acc) }

                    val cssStyleMap = CssDecoder.decodeSpanStyleToCssStyleMap(stylesToApply)
                    " style=\"${CssDecoder.decodeCssStyleMap(cssStyleMap)}\""
                }
            }

            builder.append("<$tagName$tagStyle>$partText</$tagName>")

            // If it's a heading, add a new line after the closing tag
            if (tagName in setOf("h1", "h2", "h3", "h4", "h5", "h6")) {
                builder.append("\n")
            }
        }

        return builder.toString()
    }

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
    private val htmlInlineElements = setOf(
        "a",
        "abbr",
        "acronym",
        "b",
        "bdo",
        "big",
        "br",
        "button",
        "cite",
        "code",
        "dfn",
        "em",
        "i",
        "img",
        "input",
        "kbd",
        "label",
        "map",
        "object",
        "q",
        "samp",
        "script",
        "select",
        "small",
        "span",
        "strong",
        "sub",
        "sup",
        "textarea",
        "time",
        "tt",
        "var"
    )

    /**
     * HTML block elements.
     *
     * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
     */
    private val htmlBlockElements = setOf(
        "address",
        "article",
        "aside",
        "blockquote",
        "canvas",
        "dd",
        "div",
        "dl",
        "fieldset",
        "figcaption",
        "figure",
        "footer",
        "form",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "header",
        "hgroup",
        "hr",
        "li",
        "main",
        "nav",
        "noscript",
        "ol",
        "p",
        "pre",
        "section",
        "table",
        "tfoot",
        "ul",
        "video"
    )

    /**
     * Encodes HTML elements to [RichTextStyle].
     *
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val htmlElementsStyleEncodeMap = mapOf(
        "h1" to RichTextStyle.H1,
        "h2" to RichTextStyle.H2,
        "h3" to RichTextStyle.H3,
        "h4" to RichTextStyle.H4,
        "h5" to RichTextStyle.H5,
        "h6" to RichTextStyle.H6,
        "b" to RichTextStyle.Bold,
        "strong" to RichTextStyle.Bold,
        "i" to RichTextStyle.Italic,
        "em" to RichTextStyle.Italic,
        "u" to RichTextStyle.Underline,
        "ins" to RichTextStyle.Underline,
        "strike" to RichTextStyle.Strikethrough,
        "del" to RichTextStyle.Strikethrough,
        "sub" to RichTextStyle.Subscript,
        "sup" to RichTextStyle.Superscript,
        "mark" to RichTextStyle.Mark,
        "small" to RichTextStyle.Small,
    )

    /**
     * Encodes HTML elements to [RichTextStyle].
     *
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val htmlElementsStyleDecodeMap = mapOf(
        RichTextStyle.H1 to "h1",
        RichTextStyle.H2 to "h2",
        RichTextStyle.H3 to "h3",
        RichTextStyle.H4 to "h4",
        RichTextStyle.H5 to "h5",
        RichTextStyle.H6 to "h6",
        RichTextStyle.Bold to "b",
        RichTextStyle.Italic to "i",
        RichTextStyle.Underline to "u",
        RichTextStyle.Strikethrough to "strike",
        RichTextStyle.Subscript to "sub",
        RichTextStyle.Superscript to "sup",
        RichTextStyle.Mark to "mark",
        RichTextStyle.Small to "small",
        RichTextStyle.UnorderedList to "ul",
        RichTextStyle.UnorderedListItem to "li"
    )

    /**
     * HTML elements that should be skipped.
     */
    private val skippedHtmlElements = setOf(
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

}