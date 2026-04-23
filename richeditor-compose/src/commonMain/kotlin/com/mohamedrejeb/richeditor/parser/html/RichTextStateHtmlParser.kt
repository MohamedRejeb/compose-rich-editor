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
import com.mohamedrejeb.richeditor.utils.InlineContentPlaceholder
import com.mohamedrejeb.richeditor.utils.customMerge
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel

internal object RichTextStateHtmlParser : RichTextStateParser<String> {

    @OptIn(ExperimentalRichTextApi::class)
    override fun encode(input: String): RichTextState {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        val stringBuilder = StringBuilder()
        val richParagraphList = mutableListOf(RichParagraph())
        val lineBreakParagraphIndexSet = mutableSetOf<Int>()
        val toKeepEmptyParagraphIndexSet = mutableSetOf<Int>()
        var currentRichSpan: RichSpan? = null
        var currentListLevel = 0
        // Tracks the next item number per list nesting level for ordered lists.
        // Key = list level (1-based), Value = next number to assign.
        val orderedListCounters = mutableMapOf<Int, Int>()
        // Tracks the explicit start value per list level (from <ol start="N">).
        // Only set when start != 1. Used to propagate startFrom to the first OrderedList item.
        val orderedListStartValues = mutableMapOf<Int, Int>()

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
                        trimStart = stringBuilder.lastOrNull() == null || stringBuilder.lastOrNull()?.isWhitespace() == true || stringBuilder.lastOrNull() == '\n',
                    )
                )

                if (addedText.isEmpty()) return@onText

                stringBuilder.append(addedText)

                val currentRichParagraph = richParagraphList.last()
                val safeCurrentRichSpan = currentRichSpan ?: RichSpan(paragraph = currentRichParagraph)

                if (safeCurrentRichSpan.children.isEmpty()) {
                    safeCurrentRichSpan.text += addedText
                } else {
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.text = addedText
                    newRichSpan.parent = safeCurrentRichSpan
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

                if (name in skippedHtmlElements) {
                    return@onOpenTag
                }

                if (name == "ul" || name == "ol") {
                    currentListLevel += 1
                    if (name == "ol") {
                        val startAttr = attributes["start"]?.toIntOrNull() ?: 1
                        orderedListCounters[currentListLevel] = startAttr
                        if (startAttr != 1) {
                            orderedListStartValues[currentListLevel] = startAttr
                        }
                    }
                    return@onOpenTag
                }

                if (name == "body") {
                    stringBuilder.clear()
                    richParagraphList.clear()
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

                // Handle <li value="N"> attribute — overrides the counter for this item
                if (name == "li" && lastOpenedTag == "ol") {
                    val valueAttr = attributes["value"]?.toIntOrNull()
                    if (valueAttr != null) {
                        orderedListCounters[currentListLevel] = valueAttr
                    }
                }

                // For <li> tags inside <ul> or <ol> tags — reuse blank current paragraph
                val isFirstLiInBlankParagraph =
                    lastOpenedTag != null &&
                    isCurrentTagBlockElement &&
                    isLastOpenedTagBlockElement &&
                    name == "li" &&
                    currentRichParagraph != null &&
                    currentRichParagraph.type is DefaultParagraph &&
                    isCurrentRichParagraphBlank

                if (isFirstLiInBlankParagraph) {
                    val paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag!!, currentListLevel, orderedListCounters, orderedListStartValues)
                    currentRichParagraph.type = paragraphType

                    val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap, attributes)
                    currentRichParagraph.paragraphStyle = currentRichParagraph.paragraphStyle.merge(cssParagraphStyle)
                }

                if (isCurrentTagBlockElement) {
                    val newRichParagraph =
                        if (isCurrentRichParagraphBlank)
                            currentRichParagraph
                        else
                            RichParagraph()

                    // Only assign paragraph type if not already handled above
                    val paragraphType: ParagraphType =
                        if (isFirstLiInBlankParagraph)
                            currentRichParagraph.type
                        else if (name == "li" && lastOpenedTag != null)
                            encodeHtmlElementToRichParagraphType(lastOpenedTag, currentListLevel, orderedListCounters, orderedListStartValues)
                        else
                            DefaultParagraph()

                    val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap, attributes)

                    newRichParagraph.paragraphStyle = newRichParagraph.paragraphStyle.merge(cssParagraphStyle)
                    newRichParagraph.type = paragraphType

                    // A block element (<p>, <h1>, etc.) opening on a blank paragraph
                    // from a <br> should not carry the linebreak flag
                    if (isCurrentRichParagraphBlank && newRichParagraph.isFromLineBreak && name != "li") {
                        newRichParagraph.isFromLineBreak = false
                    }

                    if (!isCurrentRichParagraphBlank) {
                        stringBuilder.append(' ')

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

                    // Image spans own a single inline-content placeholder char in the
                    // raw text so span textRanges line up with the rendered annotated
                    // string. See #466.
                    if (richSpanStyle is RichSpanStyle.Image) {
                        newRichSpan.text = InlineContentPlaceholder
                    }

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
                            RichParagraph(isFromLineBreak = true)
                        else
                            RichParagraph(
                                paragraphStyle = richParagraphList.last().paragraphStyle,
                                isFromLineBreak = true,
                            )

                    richParagraphList.add(newParagraph)

                    if (richParagraphList.lastIndex > 0)
                        lineBreakParagraphIndexSet.add(richParagraphList.lastIndex - 1)

                    lineBreakParagraphIndexSet.add(richParagraphList.lastIndex)

                    // Keep the same style when having a line break in the middle of a paragraph,
                    // Ex: <h1>Hello<br>World!</h1>
                    if (currentRichSpan != null && openedTags.isNotEmpty()) {
                        currentRichSpan = null

                        openedTags.forEach { (name, attributes) ->
                            val cssStyleMap = attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                            val cssSpanStyle = CssEncoder.parseCssStyleMapToSpanStyle(cssStyleMap)
                            val tagSpanStyle = htmlElementsSpanStyleEncodeMap[name]
                            val tagWithCssSpanStyle = cssSpanStyle.customMerge(tagSpanStyle)
                            val richSpanStyle = encodeHtmlElementToRichSpanStyle(name, attributes)

                            val newRichSpan = RichSpan(
                                children = mutableListOf(),
                                paragraph = newParagraph,
                                parent = currentRichSpan,
                                text = "",
                                textRange = TextRange.Zero,
                                spanStyle = tagWithCssSpanStyle,
                                richSpanStyle = richSpanStyle,
                            )

                            if (currentRichSpan == null) {
                                newParagraph.children.add(newRichSpan)
                            } else {
                                currentRichSpan?.children?.add(newRichSpan)
                            }

                            currentRichSpan = newRichSpan
                        }

                    }
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

                    richParagraphList.add(newParagraph)

                    toKeepEmptyParagraphIndexSet.add(richParagraphList.lastIndex)

                    currentRichSpan = null
                }

                if (name == "ul" || name == "ol") {
                    if (name == "ol") {
                        orderedListCounters.remove(currentListLevel)
                        orderedListStartValues.remove(currentListLevel)
                    }
                    currentListLevel = (currentListLevel - 1).coerceAtLeast(0)
                    return@onCloseTag
                }

                if (name in skippedHtmlElements)
                    return@onCloseTag

                // Finalize Token labels: the richSpanStyle was created at open-tag time
                // with an empty label; fill it now that the inner text has accumulated.
                val closingSpan = currentRichSpan
                val closingStyle = closingSpan?.richSpanStyle
                if (name == "span" && closingStyle is RichSpanStyle.Token && closingStyle.label.isEmpty()) {
                    val label = closingSpan.text.ifEmpty {
                        // Fall back to collecting text from any accumulated children
                        // (rare: nested inline styling inside a token).
                        closingSpan.children.joinToString("") { it.text }
                    }
                    if (label.isNotEmpty()) {
                        closingSpan.richSpanStyle = RichSpanStyle.Token(
                            triggerId = closingStyle.triggerId,
                            id = closingStyle.id,
                            label = label,
                        )
                    }
                }

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
            if (!richParagraph.isEmpty())
                richParagraph.removeEmptyChildren()
        }

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {
        if (richTextState.richParagraphList.isEmpty())
            return "<p></p>"

        if (
            richTextState.richParagraphList.size == 1 &&
            richTextState.richParagraphList.first().isEmpty()
        )
            return "<p></p>"

        val builder = StringBuilder()

        val openedListTagNames = mutableListOf<String>()
        var lastParagraphGroupTagName: String? = null
        var lastParagraphGroupLevel = 0
        var isLastParagraphEmpty = false

        var currentListLevel = 0

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
            val richParagraphType = richParagraph.type
            val isParagraphEmpty = richParagraph.isEmpty()
            val paragraphGroupTagName = decodeHtmlElementFromRichParagraphType(richParagraph.type)

            val paragraphLevel =
                if (richParagraphType is ConfigurableListLevel)
                    richParagraphType.level
                else
                    0

            val isParagraphList = paragraphGroupTagName in listOf("ol", "ul")
            val isLastParagraphList = lastParagraphGroupTagName in listOf("ol", "ul")

            fun isCloseParagraphGroup(): Boolean {
                if (!isLastParagraphList)
                    return false

                if (paragraphLevel > lastParagraphGroupLevel)
                    return false

                if (
                    lastParagraphGroupTagName == paragraphGroupTagName &&
                    paragraphLevel == lastParagraphGroupLevel
                )
                    return false

                return true
            }

            fun isCloseAllOpenedTags(): Boolean {
                if (isParagraphList)
                    return false

                if (!isLastParagraphList)
                    return false

                return true
            }

            fun isOpenParagraphGroup(): Boolean {
                if (!isParagraphList)
                    return false

                if (
                    isLastParagraphList &&
                    paragraphGroupTagName == openedListTagNames.lastOrNull() &&
                    paragraphLevel < lastParagraphGroupLevel
                )
                    return false

                if (
                    isLastParagraphList &&
                    paragraphLevel == lastParagraphGroupLevel &&
                    paragraphGroupTagName == lastParagraphGroupTagName
                )
                    return false

                return true
            }

            if (isCloseAllOpenedTags()) {
                openedListTagNames.fastForEachReversed {
                    builder.append("</$it>")
                }
                openedListTagNames.clear()
            } else if (isCloseParagraphGroup()) {
                // Close last paragraph group tag
                builder.append("</$lastParagraphGroupTagName>")
                openedListTagNames.removeLastOrNull()

                // We can move from nested level: 3 to nested level: 1,
                // for this case we need to close more than one tag
                if (
                    isLastParagraphList &&
                    paragraphLevel < lastParagraphGroupLevel
                ) {
                    repeat(lastParagraphGroupLevel - paragraphLevel) {
                        openedListTagNames.removeLastOrNull()?.let {
                            builder.append("</$it>")
                        }
                    }
                }
            }

            if (isOpenParagraphGroup()) {
                if (paragraphGroupTagName == "ol" && richParagraphType is OrderedList && richParagraphType.startFrom > 1) {
                    builder.append("<ol start=\"${richParagraphType.startFrom}\">")
                } else {
                    builder.append("<$paragraphGroupTagName>")
                }
                openedListTagNames.add(paragraphGroupTagName)
            }

            currentListLevel = paragraphLevel

            fun isLineBreak(): Boolean {
                if (!isParagraphEmpty)
                    return false

                if (isParagraphList && lastParagraphGroupTagName != paragraphGroupTagName)
                    return false

                return true
            }

            // Add line break if the paragraph is empty
            if (isLineBreak()) {
                val skipAddingBr =
                    isLastParagraphEmpty && richParagraph.isEmpty() && index == richTextState.richParagraphList.lastIndex

                if (!skipAddingBr)
                    builder.append("<$BrElement>")
            } else {
                // Create paragraph tag name
                val paragraphTagName =
                    if (paragraphGroupTagName == "ol" || paragraphGroupTagName == "ul") "li"
                    else "p"

                // If this paragraph came from a <br>, emit <br> + content inline
                // without opening a new <p> tag (the previous <p> is still open)
                if (richParagraph.isFromLineBreak && index > 0) {
                    builder.append("<$BrElement>")
                    richParagraph.children.fastForEach { richSpan ->
                        builder.append(decodeRichSpanToHtml(richSpan))
                    }
                } else {
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
                }

                // Check if the next paragraph is also a <br> continuation — if so, don't close yet
                val nextParagraph = richTextState.richParagraphList.getOrNull(index + 1)
                val nextIsLineBreakContinuation = nextParagraph != null &&
                    nextParagraph.isFromLineBreak &&
                    !nextParagraph.isEmpty()

                if (!nextIsLineBreakContinuation) {
                    builder.append("</$paragraphTagName>")
                }
            }

            // Save last paragraph group tag name
            lastParagraphGroupTagName = paragraphGroupTagName
            lastParagraphGroupLevel = paragraphLevel

            isLastParagraphEmpty = isParagraphEmpty
        }

        // Close the remaining list tags
        openedListTagNames.fastForEachReversed {
            builder.append("</$it>")
        }
        openedListTagNames.clear()

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

        // Convert attributes map to HTML string. Values MUST be escaped to avoid producing
        // malformed or XSS-capable HTML when attribute content comes from user data
        // (e.g. Token ids, link hrefs, image alt text). See REVIEW.md §5.
        val tagAttributesStringBuilder = StringBuilder()
        tagAttributes.forEach { (key, value) ->
            tagAttributesStringBuilder.append(" $key=\"${escapeHtmlAttribute(value)}\"")
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

            "span" -> {
                val triggerId = attributes["data-trigger"]
                val tokenId = attributes["data-id"]
                if (!triggerId.isNullOrEmpty() && !tokenId.isNullOrEmpty()) {
                    // Label is filled in at close-tag time once the inner text has accumulated.
                    RichSpanStyle.Token(
                        triggerId = triggerId,
                        id = tokenId,
                        label = "",
                    )
                } else {
                    RichSpanStyle.Default
                }
            }

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

            is RichSpanStyle.Token ->
                "span" to mapOf(
                    "data-trigger" to richSpanStyle.triggerId,
                    "data-id" to richSpanStyle.id,
                )

            else ->
                "span" to emptyMap()
        }

    /**
     * Encodes HTML elements to [ParagraphType].
     */
    private fun encodeHtmlElementToRichParagraphType(
        tagName: String,
        listLevel: Int,
        orderedListCounters: MutableMap<Int, Int>,
        orderedListStartValues: MutableMap<Int, Int>,
    ): ParagraphType {
        return when (tagName) {
            "ul" -> UnorderedList(initialLevel = listLevel)
            "ol" -> {
                val number = orderedListCounters[listLevel] ?: 1
                orderedListCounters[listLevel] = number + 1
                // Set startFrom on the first item from <ol start="N">, default 1
                val startFrom = orderedListStartValues.remove(listLevel) ?: 1
                OrderedList(
                    number = number,
                    initialLevel = listLevel,
                    startFrom = startFrom,
                )
            }
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

    /**
     * Escape a value for inclusion inside a double-quoted HTML attribute.
     * Replaces the characters that are unsafe in that context.
     */
    private fun escapeHtmlAttribute(value: String): String {
        if (value.isEmpty()) return value
        val needsEscape = value.any { it == '&' || it == '"' || it == '<' || it == '>' }
        if (!needsEscape) return value
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '"' -> sb.append("&quot;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
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