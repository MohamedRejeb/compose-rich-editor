package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.ksoup.entities.KsoupEntities
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.*
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.parser.utils.*
import com.mohamedrejeb.richeditor.utils.customMerge
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

internal object RichTextStateHtmlParser : RichTextStateParser<String> {

    @OptIn(ExperimentalRichTextApi::class)
    override fun encode(input: String): RichTextState {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        val stringBuilder = StringBuilder()
        val richParagraphList = mutableListOf(RichParagraph())
        val lineBreakParagraphIndexSet = mutableSetOf<Int>()
        val toKeepEmptyParagraphIndexSet = mutableSetOf<Int>()
        var currentRichSpan: RichSpan? = null

        val handler = KsoupHtmlHandler
            .Builder()
            .onText {
                // In html text inside ul/ol tags is skipped
                val lastOpenedTag = openedTags.lastOrNull()?.first
                if (lastOpenedTag == "ul" || lastOpenedTag == "ol") return@onText

                if (lastOpenedTag in skippedHtmlElements) return@onText

                val addedText = KsoupEntities.decodeHtml(
                    removeHtmlTextExtraSpaces(
                        input = it,
                        trimStart = stringBuilder.lastOrNull() == null || stringBuilder.lastOrNull() == ' ' || stringBuilder.lastOrNull() == '\n',
                    )
                )

                if (addedText.isBlank()) return@onText

                stringBuilder.append(addedText)

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
                val lastOpenedTag = openedTags.lastOrNull()?.first

                openedTags.add(name to attributes)

                if (name == "ul" || name == "ol" || name in skippedHtmlElements) {
                    // Todo: Apply ul/ol styling if exists
                    return@onOpenTag
                }

                if (name == "body") {
                    stringBuilder.clear()
                    richParagraphList.clear()
                    println("add paragraph on body")
                    richParagraphList.add(RichParagraph())
                    currentRichSpan = null
                }

                val cssStyleMap = attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                val cssSpanStyle = CssEncoder.parseCssStyleMapToSpanStyle(cssStyleMap)
                val tagSpanStyle = htmlElementsSpanStyleEncodeMap[name]

                val currentRichParagraph = richParagraphList.lastOrNull()
                val isCurrentRichParagraphBlank = currentRichParagraph?.isBlank() == true
                val isCurrentTagBlockElement = name in htmlBlockElements
                val isLastOpenedTagBlockElement = lastOpenedTag in htmlBlockElements

                // For <li> tags inside <ul> or <ol> tags
                if (
                    lastOpenedTag != null &&
                    isCurrentTagBlockElement &&
                    isLastOpenedTagBlockElement &&
                    name == "li" &&
                    currentRichParagraph != null &&
                    currentRichParagraph.type is DefaultParagraph &&
                    isCurrentRichParagraphBlank
                ) {
                    val paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag)
                    currentRichParagraph.type = paragraphType

                    val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap)
                    currentRichParagraph.paragraphStyle = currentRichParagraph.paragraphStyle.merge(cssParagraphStyle)
                }

                if (isCurrentTagBlockElement) {
                    val newRichParagraph =
                        if (isCurrentRichParagraphBlank)
                            currentRichParagraph ?: RichParagraph()
                        else
                            RichParagraph()

                    var paragraphType: ParagraphType = DefaultParagraph()
                    if (name == "li" && lastOpenedTag != null) {
                        paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag)
                    }
                    val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap)

                    newRichParagraph.paragraphStyle = newRichParagraph.paragraphStyle.merge(cssParagraphStyle)
                    newRichParagraph.type = paragraphType

                    if (!isCurrentRichParagraphBlank) {
                        stringBuilder.append(' ')

                        println("add paragraph on $name")
                        richParagraphList.add(newRichParagraph)
                    }

                    val newRichSpan = RichSpan(paragraph = newRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        newRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (name != BrElement) {
                    val richSpanStyle = encodeHtmlElementToRichSpanStyle(name, attributes)

                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)
                    newRichSpan.richSpanStyle = richSpanStyle

                    if (currentRichSpan != null) {
                        newRichSpan.parent = currentRichSpan
                        currentRichSpan?.children?.add(newRichSpan)
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                    }
                    currentRichSpan = newRichSpan
                } else {
                    // name == "br"
                    stringBuilder.append(' ')

                    val newParagraph =
                        if (richParagraphList.isEmpty())
                            RichParagraph()
                        else
                            RichParagraph(paragraphStyle = richParagraphList.last().paragraphStyle)

                    println("add paragraph on br")
                    richParagraphList.add(newParagraph)

                    if (richParagraphList.lastIndex > 0)
                        lineBreakParagraphIndexSet.add(richParagraphList.lastIndex - 1)

                    lineBreakParagraphIndexSet.add(richParagraphList.lastIndex)

                    // Keep the same style when having a line break in the middle of a paragraph,
                    // Ex: <h1>Hello<br>World!</h1>
                    if (isLastOpenedTagBlockElement && !isCurrentRichParagraphBlank)
                        currentRichSpan?.let { richSpan ->
                            val newRichSpan = richSpan.copy(
                                text = "",
                                textRange = TextRange.Zero,
                                paragraph = newParagraph,
                                children = mutableListOf(),
                            )

                            newParagraph.children.add(newRichSpan)

                            currentRichSpan = newRichSpan
                        }
                    else
                        currentRichSpan = null
                }
            }
            .onCloseTag { name, _ ->
                openedTags.removeLastOrNull()

                val isCurrentRichParagraphBlank = richParagraphList.lastOrNull()?.isBlank() == true
                val isCurrentTagBlockElement = name in htmlBlockElements && name != "li"

                if (isCurrentTagBlockElement && !isCurrentRichParagraphBlank) {
                    stringBuilder.append(' ')

                    val newParagraph =
                        if (richParagraphList.isEmpty())
                            RichParagraph()
                        else
                            RichParagraph(paragraphStyle = richParagraphList.last().paragraphStyle)

                    println("add paragraph on $name")
                    richParagraphList.add(newParagraph)

                    toKeepEmptyParagraphIndexSet.add(richParagraphList.lastIndex)

                    currentRichSpan = null
                }

                if (name == "ul" || name == "ol" || name in skippedHtmlElements)
                    return@onCloseTag

                if (name != BrElement)
                    currentRichSpan = currentRichSpan?.parent
            }
            .build()

        val parser = KsoupHtmlParser(
            handler = handler
        )

        parser.write(input)
        parser.end()

        for (i in richParagraphList.lastIndex downTo 0) {
            // Keep empty paragraphs if they are line breaks <br> or by block html elements
            if (i in lineBreakParagraphIndexSet || (i != richParagraphList.lastIndex && i in toKeepEmptyParagraphIndexSet))
                continue

            // Remove empty paragraphs
            if (richParagraphList[i].isBlank())
                richParagraphList.removeAt(i)
        }

        richParagraphList.forEach { richParagraph ->
            richParagraph.removeEmptyChildren()
        }

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {
        val builder = StringBuilder()

        var lastParagraphGroupTagName: String? = null
        var isLastParagraphEmpty = false

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
            val isParagraphEmpty = richParagraph.isEmpty()
            val paragraphGroupTagName = decodeHtmlElementFromRichParagraphType(richParagraph.type)

            // Close last paragraph group tag if needed
            if (
                (lastParagraphGroupTagName == "ol" || lastParagraphGroupTagName == "ul") &&
                (lastParagraphGroupTagName != paragraphGroupTagName)
            ) builder.append("</$lastParagraphGroupTagName>")

            // Open new paragraph group tag if needed
            if (
                (paragraphGroupTagName == "ol" || paragraphGroupTagName == "ul") &&
                lastParagraphGroupTagName != paragraphGroupTagName
            )
                builder.append("<$paragraphGroupTagName>")
            // Add line break if the paragraph is empty
            else if (isParagraphEmpty) {
                val skipAddingBr =
                    isLastParagraphEmpty && richParagraph.isEmpty() && index == richTextState.richParagraphList.lastIndex

                if (!skipAddingBr)
                    builder.append("<$BrElement>")

                isLastParagraphEmpty = isParagraphEmpty

                return@fastForEachIndexed
            }


            // Create paragraph tag name
            val paragraphTagName =
                if (paragraphGroupTagName == "ol" || paragraphGroupTagName == "ul") "li"
                else "p"

            // Create paragraph css
            val paragraphCssMap = CssDecoder.decodeParagraphStyleToCssStyleMap(richParagraph.paragraphStyle)
            val paragraphCss = CssDecoder.decodeCssStyleMap(paragraphCssMap)

            // Append paragraph opening tag
            builder.append("<$paragraphTagName")
            if (paragraphCss.isNotBlank()) builder.append(" style=\"$paragraphCss\"")
            builder.append(">")

            // Append paragraph children
            richParagraph.children.fastForEach { richSpan ->
                builder.append(decodeRichSpanToHtml(richSpan))
            }

            // Append paragraph closing tag
            builder.append("</$paragraphTagName>")

            // Save last paragraph group tag name
            lastParagraphGroupTagName = paragraphGroupTagName

            // Close last paragraph group tag if needed
            if (
                (lastParagraphGroupTagName == "ol" || lastParagraphGroupTagName == "ul") &&
                index == richTextState.richParagraphList.lastIndex
            ) builder.append("</$lastParagraphGroupTagName>")

            isLastParagraphEmpty = isParagraphEmpty
        }

        return builder.toString()
    }

    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeRichSpanToHtml(richSpan: RichSpan, parentFormattingTags: List<String> = emptyList()): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Get HTML element and attributes
        val spanHtml = decodeHtmlElementFromRichSpanStyle(richSpan.richSpanStyle)
        val tagName = spanHtml.first
        val tagAttributes = spanHtml.second

        // Convert attributes map to HTML string
        val tagAttributesStringBuilder = StringBuilder()
        tagAttributes.forEach { (key, value) ->
            tagAttributesStringBuilder.append(" $key=\"$value\"")
        }

        // Convert span style to CSS string
        val htmlStyleFormat = CssDecoder.decodeSpanStyleToHtmlStylingFormat(richSpan.spanStyle)
        val spanCss = CssDecoder.decodeCssStyleMap(htmlStyleFormat.cssStyleMap)
        val htmlTags = htmlStyleFormat.htmlTags.filter { it !in parentFormattingTags }

        val isRequireOpeningTag = tagName != "span" || tagAttributes.isNotEmpty() || spanCss.isNotEmpty()

        if (isRequireOpeningTag) {
            // Append HTML element with attributes and style
            stringBuilder.append("<$tagName$tagAttributesStringBuilder")
            if (spanCss.isNotEmpty()) stringBuilder.append(" style=\"$spanCss\"")
            stringBuilder.append(">")
        }

        htmlTags.forEach {
            stringBuilder.append("<$it>")
        }

        // Append text
        stringBuilder.append(KsoupEntities.encodeHtml(richSpan.text))

        // Append children
        richSpan.children.fastForEach { child ->
            stringBuilder.append(
                decodeRichSpanToHtml(
                    richSpan = child,
                    parentFormattingTags = parentFormattingTags + htmlTags,
                )
            )
        }

        htmlTags.reversed().forEach {
            stringBuilder.append("</$it>")
        }

        if (isRequireOpeningTag) {
            // Append closing HTML element
            stringBuilder.append("</$tagName>")
        }

        return stringBuilder.toString()
    }

    /**
     * Encodes HTML elements to [RichSpanStyle].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun encodeHtmlElementToRichSpanStyle(
        tagName: String,
        attributes: Map<String, String>,
    ): RichSpanStyle =
        when (tagName) {
            "a" ->
                RichSpanStyle.Link(url = attributes["href"].orEmpty())

            CodeSpanTagName, OldCodeSpanTagName ->
                RichSpanStyle.Code()

            "img" ->
                RichSpanStyle.Image(
                    model = attributes["src"].orEmpty(),
                    width = (attributes["width"]?.toIntOrNull() ?: 0).sp,
                    height = (attributes["height"]?.toIntOrNull() ?: 0).sp,
                    contentDescription = attributes["alt"] ?: ""
                )

            else ->
                RichSpanStyle.Default
        }

    /**
     * Decodes HTML elements from [RichSpanStyle].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeHtmlElementFromRichSpanStyle(
        richSpanStyle: RichSpanStyle,
    ): Pair<String, Map<String, String>> =
        when (richSpanStyle) {
            is RichSpanStyle.Link ->
                "a" to mapOf(
                    "href" to richSpanStyle.url,
                    "target" to "_blank"
                )

            is RichSpanStyle.Code ->
                CodeSpanTagName to emptyMap()

            is RichSpanStyle.Image ->
                if (richSpanStyle.model is String)
                    "img" to mapOf(
                        "src" to richSpanStyle.model,
                        "width" to richSpanStyle.width.value.toString(),
                        "height" to richSpanStyle.height.value.toString(),
                    )
                else
                    "span" to emptyMap()

            else ->
                "span" to emptyMap()
        }

    /**
     * Encodes HTML elements to [ParagraphType].
     */
    private fun encodeHtmlElementToRichParagraphType(
        tagName: String,
    ): ParagraphType {
        return when (tagName) {
            "ul" -> UnorderedList()
            "ol" -> OrderedList(number = 1)
            else -> DefaultParagraph()
        }
    }

    /**
     * Decodes HTML elements from [ParagraphType].
     */
    private fun decodeHtmlElementFromRichParagraphType(
        richParagraphType: ParagraphType,
    ): String {
        return when (richParagraphType) {
            is UnorderedList -> "ul"
            is OrderedList -> "ol"
            else -> "p"
        }
    }

}

/**
 * Encodes HTML elements to [SpanStyle].
 *
 * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
 */
internal val htmlElementsSpanStyleEncodeMap = mapOf(
    "b" to BoldSpanStyle,
    "strong" to BoldSpanStyle,
    "i" to ItalicSpanStyle,
    "em" to ItalicSpanStyle,
    "u" to UnderlineSpanStyle,
    "ins" to UnderlineSpanStyle,
    "s" to StrikethroughSpanStyle,
    "strike" to StrikethroughSpanStyle,
    "del" to StrikethroughSpanStyle,
    "sub" to SubscriptSpanStyle,
    "sup" to SuperscriptSpanStyle,
    "mark" to MarkSpanStyle,
    "small" to SmallSpanStyle,
    "h1" to H1SpanStyle,
    "h2" to H2SpanStyle,
    "h3" to H3SpanStyle,
    "h4" to H4SpanStyle,
    "h5" to H5SpanStyle,
    "h6" to H6SpanStyle,
)

/**
 * Decodes HTML elements from [SpanStyle].
 *
 * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
 */
internal val htmlElementsSpanStyleDecodeMap = mapOf(
    BoldSpanStyle to "b",
    ItalicSpanStyle to "i",
    UnderlineSpanStyle to "u",
    StrikethroughSpanStyle to "s",
    SubscriptSpanStyle to "sub",
    SuperscriptSpanStyle to "sup",
    MarkSpanStyle to "mark",
    SmallSpanStyle to "small",
    H1SpanStyle to "h1",
    H2SpanStyle to "h2",
    H3SpanStyle to "h3",
    H4SpanStyle to "h4",
    H5SpanStyle to "h5",
    H6SpanStyle to "h6",
)

internal const val CodeSpanTagName = "code"
internal const val OldCodeSpanTagName = "code-span"