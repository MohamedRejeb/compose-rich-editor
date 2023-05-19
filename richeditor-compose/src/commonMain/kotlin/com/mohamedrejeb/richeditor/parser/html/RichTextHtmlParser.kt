package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.parser.RichTextParser

object RichTextHtmlParser : RichTextParser {

    override fun encode(input: String): RichTextValue {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        var text = ""
        val currentStyles: MutableList<RichTextStyle> = mutableListOf()
        val parts: MutableList<RichTextPart> = mutableListOf()

        val handler = KsoupHtmlHandler
            .Builder()
            .onText {
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
            .onOpenTag { name, attributes, isImplied ->
                openedTags.add(name to attributes)

                val cssStyleMap = attributes["style"]?.let { CssHelper.parseCssStyle(it) } ?: emptyMap()
                val cssSpanStyle = CssHelper.parseCssStyleMapToSpanStyle(cssStyleMap)
                val richTextStyle = htmlElementsStyleMap[name]

                if (cssSpanStyle != SpanStyle() || richTextStyle != null) {
                    val tagRichTextStyle = object : RichTextStyle {
                        override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
                            val tagSpanStyle = richTextStyle?.applyStyle(cssSpanStyle) ?: cssSpanStyle
                            return spanStyle.merge(tagSpanStyle)
                        }
                    }

                    currentStyles.add(tagRichTextStyle)
                }

                if (text.lastOrNull()?.toString() != "\n" && name in htmlBlockElements) {
                    text += "\n"
                }

                when (name) {
                    "br" -> {
                        text += "\n"
                    }
                }
            }
            .onCloseTag { name, isImplied ->
                openedTags.removeLastOrNull()
                currentStyles.removeLastOrNull()

                if (name in htmlBlockElements) {
                    text += "\n"
                }

                when (name) {
                    "br" -> {
                        text += "\n"
                    }
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

    override fun decode(input: RichTextValue): String {
        TODO("Not yet implemented")
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
        "a", "abbr", "acronym", "b", "bdo", "big", "br", "button", "cite", "code", "dfn", "em", "i", "img", "input",
        "kbd", "label", "map", "object", "q", "samp", "script", "select", "small", "span", "strong", "sub", "sup",
        "textarea", "time", "tt", "var"
    )

    /**
     * HTML block elements.
     *
     * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
     */
    private val htmlBlockElements = setOf(
        "address", "article", "aside", "blockquote", "canvas", "dd", "div", "dl", "fieldset", "figcaption",
        "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "li", "main", "nav",
        "noscript", "ol", "p", "pre", "section", "table", "tfoot", "ul", "video"
    )

    /**
     * HTML elements that have a style.
     *
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val htmlElementsStyleMap = mapOf(
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
        "h1" to RichTextStyle.H1,
        "h2" to RichTextStyle.H2,
        "h3" to RichTextStyle.H3,
        "h4" to RichTextStyle.H4,
        "h5" to RichTextStyle.H5,
        "h6" to RichTextStyle.H6,
    )

}