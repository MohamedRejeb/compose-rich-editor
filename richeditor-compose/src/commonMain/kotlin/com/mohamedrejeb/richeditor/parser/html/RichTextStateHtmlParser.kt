package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.model.*
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.utils.customMerge

internal object RichTextStateHtmlParser : RichTextStateParser<String> {

    override fun encode(input: String): RichTextState {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        val stringBuilder = StringBuilder()
        val currentStyles: MutableList<RichTextStyle> = mutableListOf()
        val richParagraphList = mutableListOf<RichParagraph>()
        var currentRichSpan: RichSpan? = null
        var lastClosedTag: String? = null

        var skipText = false

        val handler = KsoupHtmlHandler
            .Builder()
            .onText {
                println("onText: $it")
                if (skipText) return@onText

                val lastOpenedTag = openedTags.lastOrNull()?.first
                if (lastOpenedTag in skippedHtmlElements) return@onText

                val addedText = removeHtmlTextExtraSpaces(
                    input = it,
                    trimStart = stringBuilder.lastOrNull() == ' ' || stringBuilder.lastOrNull() == '\n',
                )
                println("addedText: $addedText")
                if (addedText.isEmpty()) return@onText

                if (lastClosedTag in htmlBlockElements) {
                    println("entering lastClosedTag")
                    if (addedText.isBlank()) return@onText
                    println("passing lastClosedTag")
                    lastClosedTag = null
                    currentRichSpan = null
                    richParagraphList.add(RichParagraph())
                }

                stringBuilder.append(addedText)

                if (richParagraphList.isEmpty())
                    richParagraphList.add(RichParagraph())

                val currentRichParagraph = richParagraphList.last()
                val safeCurrentRichSpan = currentRichSpan ?: RichSpan(paragraph = currentRichParagraph)

                if (safeCurrentRichSpan.children.isEmpty()) {
                    safeCurrentRichSpan.text += addedText
                } else {
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.text = addedText
                    safeCurrentRichSpan.children.add(newRichSpan)
                }

                if (currentRichSpan == null) {
                    currentRichSpan = safeCurrentRichSpan
                    currentRichParagraph.children.add(safeCurrentRichSpan)
                }
            }
            .onOpenTag { name, attributes, _ ->
                println("onOpenTag: $name")
                openedTags.add(name to attributes)

                if (name == "ul" || name == "ol") {
                    skipText = true
                    return@onOpenTag
                }

                val cssStyleMap = attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                val cssSpanStyle = CssEncoder.parseCssStyleMapToSpanStyle(cssStyleMap)
                val tagSpanStyle = htmlElementsSpanStyleEncodeMap[name]

                if (name in htmlBlockElements) {
                    stringBuilder.append(' ')

                    val newRichParagraph = RichParagraph()
                    var paragraphType: RichParagraph.Type = RichParagraph.Type.Default
                    if (name == "li") {
                        skipText = false
                        openedTags.getOrNull(openedTags.lastIndex - 1)?.first?.let { lastOpenedTag ->
                            paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag)
                        }
                    }
                    val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap)

                    newRichParagraph.paragraphStyle = cssParagraphStyle
                    newRichParagraph.type = paragraphType
                    richParagraphList.add(newRichParagraph)

                    val newRichSpan = RichSpan(paragraph = newRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        newRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (name != "br") {
                    if (lastClosedTag in htmlBlockElements) {
                        lastClosedTag = null
                        currentRichSpan = null
                        richParagraphList.add(RichParagraph())
                        println("added new paragraph")
                    }

                    val richSpanStyle = encodeHtmlElementToRichSpanStyle(name, attributes)

                    if (richParagraphList.isEmpty())
                        richParagraphList.add(RichParagraph())

                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)
                    newRichSpan.style = richSpanStyle

                    if (currentRichSpan != null) {
                        newRichSpan.parent = currentRichSpan
                        currentRichSpan?.children?.add(newRichSpan)
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                    }
                    currentRichSpan = newRichSpan
                }

                when (name) {
                    "br" -> {
                        stringBuilder.append(' ')

                        if (richParagraphList.isEmpty())
                            richParagraphList.add(RichParagraph())

                        val currentRichParagraph = richParagraphList.last()
                        val newParagraph = RichParagraph(paragraphStyle = currentRichParagraph.paragraphStyle)
                        richParagraphList.add(newParagraph)
                        currentRichSpan = null
                    }
                }

                lastClosedTag = null
            }
            .onCloseTag { name, _ ->
                println("onCloseTag: $name")
                openedTags.removeLastOrNull()
                currentStyles.removeLastOrNull()
                lastClosedTag = name

                if (name == "ul" || name == "ol") {
                    skipText = true
                    return@onCloseTag
                }

                currentRichSpan = currentRichSpan?.parent
            }
            .build()

        val parser = KsoupHtmlParser(
            handler = handler
        )

        parser.write(input)
        parser.end()

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {

        val builder = StringBuilder()

        builder.append("<p>")


        return builder.toString()
    }

    /**
     * Encodes HTML elements to [SpanStyle].
     *
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val htmlElementsSpanStyleEncodeMap = mapOf(
        "b" to BoldSpanStyle,
        "strong" to BoldSpanStyle,
        "i" to ItalicSpanStyle,
        "em" to ItalicSpanStyle,
        "u" to UnderlineSpanStyle,
        "ins" to UnderlineSpanStyle,
        "strike" to StrikethroughSpanStyle,
        "del" to StrikethroughSpanStyle,
        "sub" to SubscriptSpanStyle,
        "sup" to SuperscriptSpanStyle,
        "mark" to MarkSpanStyle,
        "small" to SmallSpanStyle,
        "h1" to H1SPanStyle,
        "h2" to H2SPanStyle,
        "h3" to H3SPanStyle,
        "h4" to H4SPanStyle,
        "h5" to H5SPanStyle,
        "h6" to H6SPanStyle,
    )

    /**
     * Decodes HTML elements from [SpanStyle].
     *
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val htmlElementsSpanStyleDecodeMap = mapOf(
        BoldSpanStyle to "b",
        ItalicSpanStyle to "i",
        UnderlineSpanStyle to "u",
        StrikethroughSpanStyle to "strike",
        SubscriptSpanStyle to "sub",
        SuperscriptSpanStyle to "sup",
        MarkSpanStyle to "mark",
        SmallSpanStyle to "small",
        H1SPanStyle to "h1",
        H2SPanStyle to "h2",
        H3SPanStyle to "h3",
        H4SPanStyle to "h4",
        H5SPanStyle to "h5",
        H6SPanStyle to "h6",
    )

    /**
     * Encodes HTML elements to [RichSpanStyle].
     */
    private fun encodeHtmlElementToRichSpanStyle(
        tagName: String,
        attributes: Map<String, String>,
    ): RichSpanStyle {
        return when (tagName) {
            "a" -> {
                val href = attributes["href"] ?: ""
                return RichSpanStyle.Link(url = href)
            }
            else -> RichSpanStyle.Default()
        }
    }

    /**
     * Decodes HTML elements from [RichSpanStyle].
     */
    private fun decodeHtmlElementFromRichSpanStyle(
        richSpanStyle: RichSpanStyle,
    ): Pair<String, Map<String, String>> {
        return when (richSpanStyle) {
            is RichSpanStyle.Link -> {
                return "a" to mapOf(
                    "href" to richSpanStyle.url,
                    "target" to "_blank"
                )
            }
            else -> "span" to emptyMap()
        }
    }

    /**
     * Encodes HTML elements to [RichParagraph.Type].
     */
    private fun encodeHtmlElementToRichParagraphType(
        tagName: String,
    ): RichParagraph.Type {
        return when (tagName) {
            "ul" -> RichParagraph.Type.UnorderedList
            "ol" -> RichParagraph.Type.OrderedList(1)
            else -> RichParagraph.Type.Default
        }
    }

    /**
     * Decodes HTML elements from [RichParagraph.Type].
     */
    private fun decodeHtmlElementFromRichParagraphType(
        richParagraphType: RichParagraph.Type,
    ): String {
        return when (richParagraphType) {
            is RichParagraph.Type.UnorderedList -> "ul"
            is RichParagraph.Type.OrderedList -> "ol"
            else -> "p"
        }
    }

}