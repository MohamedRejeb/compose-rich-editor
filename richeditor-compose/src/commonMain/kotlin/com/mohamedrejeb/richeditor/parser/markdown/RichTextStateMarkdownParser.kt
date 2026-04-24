package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.parser.html.BrElement
import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
import com.mohamedrejeb.richeditor.parser.html.htmlElementsSpanStyleEncodeMap
import com.mohamedrejeb.richeditor.parser.utils.*
import com.mohamedrejeb.richeditor.utils.InlineContentPlaceholder
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

internal object RichTextStateMarkdownParser : RichTextStateParser<String> {

    @OptIn(ExperimentalRichTextApi::class)
    override fun encode(input: String): RichTextState {
        val openedNodes = mutableListOf<ASTNode>()
        val openedHtmlTags = mutableListOf<String>()
        val richParagraphList = mutableListOf(RichParagraph())
        var brParagraphIndices = mutableListOf<Int>()
        var currentRichSpan: RichSpan? = null
        var currentRichParagraphType: ParagraphType = DefaultParagraph()
        var currentListLevel = 0

        fun onAddLineBreak() {
            val lastParagraph = richParagraphList.lastOrNull()
            val beforeLastParagraph = richParagraphList.getOrNull(richParagraphList.lastIndex - 1)
            val lastBrIndex = brParagraphIndices.lastOrNull()
            val beforeLastBrIndex = brParagraphIndices.getOrNull(brParagraphIndices.lastIndex - 1)

            // We need this for line break to work fine with EOL
            if (
                lastParagraph?.isEmpty() != true ||
                beforeLastParagraph?.isEmpty() != true ||
                lastBrIndex == richParagraphList.lastIndex ||
                beforeLastBrIndex == richParagraphList.lastIndex - 1
            )
                richParagraphList.add(RichParagraph())

            brParagraphIndices.add(richParagraphList.lastIndex)

            currentRichSpan = null
        }

        fun onText(text: String) {
            val text = text.replace('\n', ' ')

            if (text.isEmpty()) return

            if (richParagraphList.isEmpty())
                richParagraphList.add(RichParagraph())

            val currentRichParagraph = richParagraphList.last()
            val safeCurrentRichSpan = currentRichSpan ?: RichSpan(paragraph = currentRichParagraph)

            if (safeCurrentRichSpan.children.isEmpty()) {
                safeCurrentRichSpan.text += text
            } else {
                val newRichSpan = RichSpan(
                    paragraph = currentRichParagraph,
                    parent = safeCurrentRichSpan,
                )
                newRichSpan.text = text
                safeCurrentRichSpan.children.add(newRichSpan)
            }

            if (currentRichSpan == null) {
                currentRichSpan = safeCurrentRichSpan
                currentRichParagraph.children.add(safeCurrentRichSpan)
            }

            val currentRichSpanRichSpanStyle = currentRichSpan?.richSpanStyle
            val lastOpenedNode = openedNodes.lastOrNull()

            if (lastOpenedNode?.type == MarkdownElementTypes.IMAGE && text == "!") {
                currentRichSpan?.text = ""
            }

            if (currentRichSpanRichSpanStyle is RichSpanStyle.Image) {
                currentRichSpan?.richSpanStyle =
                    RichSpanStyle.Image(
                        model = currentRichSpanRichSpanStyle.model,
                        width = currentRichSpanRichSpanStyle.width,
                        height = currentRichSpanRichSpanStyle.height,
                        contentDescription = text
                    )

                // Image owns a single placeholder char in the raw text so span
                // textRanges line up with the rendered annotated string. See #466.
                currentRichSpan?.text = InlineContentPlaceholder
            }
        }

        // Correct the markdown text first so we can use it in callbacks
        val correctedMarkdown = correctMarkdownText(input)

        encodeMarkdownToRichText(
            markdown = correctedMarkdown,
            onText = { text ->
                onText(text)
            },
            onOpenNode = { node ->
                val lastOpenedNode = openedNodes.lastOrNull()

                openedNodes.add(node)

                if (node.type == MarkdownElementTypes.LIST_ITEM) {
                    currentListLevel++
                }

                val tagSpanStyle = markdownElementsSpanStyleEncodeMap[node.type]
                val tagParagraphStyle = markdownElementsParagraphStyleEncodeMap[node.type]

                if (node.type in markdownBlockElements) {
                    val currentRichParagraph = richParagraphList.last()

                    val isList =
                        lastOpenedNode?.type == MarkdownElementTypes.ORDERED_LIST ||
                                lastOpenedNode?.type == MarkdownElementTypes.UNORDERED_LIST

                    // Get paragraph type from markdown element
                    if (currentRichParagraphType is DefaultParagraph || isList) {
                        val paragraphType = encodeRichParagraphTypeFromMarkdownElement(lastOpenedNode ?: node)
                        currentRichParagraphType = paragraphType
                    }

                    // Set paragraph type if an element is a list item
                    if (node.type == MarkdownElementTypes.LIST_ITEM) {
                        currentRichParagraphType = currentRichParagraphType.getNextParagraphType()

                        if (currentRichParagraphType is ConfigurableListLevel) {
                            (currentRichParagraphType as ConfigurableListLevel).level = currentListLevel
                        }

                        currentRichParagraph.type = currentRichParagraphType
                    }

                    // Apply paragraph style (if applicable)
                    tagParagraphStyle?.let {
                        currentRichParagraph.paragraphStyle = currentRichParagraph.paragraphStyle.merge(it)
                    }
                    // Record heading level so encoding stays semantic instead of fingerprinting.
                    if (node.type in HeadingStyle.markdownHeadingNodes) {
                        currentRichParagraph.headingStyle = when (node.type) {
                            MarkdownElementTypes.ATX_1 -> HeadingStyle.H1
                            MarkdownElementTypes.ATX_2 -> HeadingStyle.H2
                            MarkdownElementTypes.ATX_3 -> HeadingStyle.H3
                            MarkdownElementTypes.ATX_4 -> HeadingStyle.H4
                            MarkdownElementTypes.ATX_5 -> HeadingStyle.H5
                            MarkdownElementTypes.ATX_6 -> HeadingStyle.H6
                            else -> HeadingStyle.Normal
                        }
                    }

                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        currentRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (node.type != MarkdownTokenTypes.EOL) {
                    val richSpanStyle = encodeMarkdownElementToRichSpanStyle(node, correctedMarkdown)

                    if (richParagraphList.isEmpty())
                        richParagraphList.add(RichParagraph())

                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()
                    newRichSpan.richSpanStyle = richSpanStyle

                    val currentRichSpanParent = currentRichSpan?.parent

                    // Avoid nesting if the current rich span doesn't add a styling
                    if (
                        currentRichSpan?.fullSpanStyle == SpanStyle() &&
                        currentRichSpan?.fullStyle is RichSpanStyle.Default
                    ) {
                        if (currentRichSpan?.isEmpty() == true) {
                            if (currentRichSpanParent != null)
                                currentRichSpanParent.children.removeAt(currentRichSpanParent.children.lastIndex)
                            else
                                currentRichParagraph.children.removeAt(currentRichParagraph.children.lastIndex)
                        }

                        currentRichSpan = null
                    }

                    val newRichSpanParent = currentRichSpan ?: currentRichSpanParent

                    if (newRichSpanParent != null) {
                        newRichSpan.parent = newRichSpanParent
                        newRichSpanParent.children.add(newRichSpan)
                        currentRichSpan = newRichSpan
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                        currentRichSpan = newRichSpan
                    }

                    if (
                        openedNodes.getOrNull(openedNodes.lastIndex - 1)?.type != GFMElementTypes.INLINE_MATH &&
                        node.type == GFMTokenTypes.DOLLAR
                    )
                        newRichSpan.text = "$".repeat(node.endOffset - node.startOffset)
                }

                if (
                    node.type == GFMTokenTypes.GFM_AUTOLINK ||
                    node.type == MarkdownTokenTypes.CODE_LINE
                ) {
                    onText(node.getTextInNode(correctedMarkdown).toString())
                }
            },
            onCloseNode = { node ->
                openedNodes.removeLastOrNull()

                if (node.type == MarkdownElementTypes.LIST_ITEM) {
                    currentListLevel--
                }

                // Remove empty spans
                if (currentRichSpan?.isEmpty() == true) {
                    val parent = currentRichSpan?.parent
                    if (parent != null)
                        currentRichSpan?.parent?.children?.remove(currentRichSpan)
                    else
                        currentRichSpan?.paragraph?.children?.remove(currentRichSpan)
                }

                // Merge spans with only one child
                if (currentRichSpan?.text?.isEmpty() == true && currentRichSpan?.children?.size == 1) {
                    currentRichSpan?.children?.firstOrNull()?.let { child ->
                        currentRichSpan?.text = child.text
                        currentRichSpan?.spanStyle =
                            currentRichSpan?.spanStyle?.merge(child.spanStyle) ?: child.spanStyle
                        currentRichSpan?.richSpanStyle = child.richSpanStyle
                        currentRichSpan?.children?.clear()
                        currentRichSpan?.children?.addAll(child.children)
                    }
                }

                // Add new line if needed.
                // Prevent adding two consecutive new lines
                if (node.type == MarkdownTokenTypes.EOL) {
                    val lastParagraph = richParagraphList.lastOrNull()
                    val beforeLastParagraph = richParagraphList.getOrNull(richParagraphList.lastIndex - 1)
                    val lastBrParagraphIndex = brParagraphIndices.lastOrNull()
                    val beforeLastBrParagraphIndex = brParagraphIndices.getOrNull(brParagraphIndices.lastIndex - 1)

                    if (
                        lastParagraph?.isNotEmpty() == true ||
                        beforeLastParagraph?.isNotEmpty() == true ||
                        lastBrParagraphIndex == richParagraphList.lastIndex ||
                        beforeLastBrParagraphIndex == richParagraphList.lastIndex - 1
                    ) {
                        richParagraphList.add(RichParagraph())
                    }

                    currentRichSpan = null
                }

                val lastOpenedNode = openedNodes.lastOrNull()

                val isList =
                    node.type == MarkdownElementTypes.ORDERED_LIST ||
                            node.type == MarkdownElementTypes.UNORDERED_LIST

                val isLastList =
                    lastOpenedNode != null &&
                            (lastOpenedNode.type == MarkdownElementTypes.ORDERED_LIST ||
                                    lastOpenedNode.type == MarkdownElementTypes.UNORDERED_LIST ||
                                    lastOpenedNode.type == MarkdownElementTypes.LIST_ITEM)

                // Reset paragraph type
                if (isList && !isLastList) {
                    currentRichParagraphType = DefaultParagraph()
                }

                currentRichSpan = currentRichSpan?.parent
            },
            onHtmlTag = { tag ->
                val tagName = tag
                    .substringAfter("</")
                    .substringAfter("<")
                    .substringBefore(">")
                    .substringBefore(" ")
                    .trim()
                    .lowercase()

                val isClosingTag = tag.startsWith("</")

                if (isClosingTag) {
                    openedHtmlTags.removeLastOrNull()

                    if (tagName != BrElement)
                        currentRichSpan = currentRichSpan?.parent
                } else {
                    openedHtmlTags.add(tag)

                    val tagSpanStyle = htmlElementsSpanStyleEncodeMap[tagName]

                    if (tagName != BrElement) {
                        val currentRichParagraph = richParagraphList.last()
                        val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                        newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()

                        if (currentRichSpan != null) {
                            newRichSpan.parent = currentRichSpan
                            currentRichSpan?.children?.add(newRichSpan)
                        } else {
                            currentRichParagraph.children.add(newRichSpan)
                        }
                        currentRichSpan = newRichSpan
                    } else {
                        // name == "br"
                        onAddLineBreak()
                    }
                }
            },
            onHtmlBlock = {
                var html = it

                while (true) {
                    val brIndex = html.indexOf("<br>")

                    if (brIndex == -1)
                        break

                    html = html.substring(brIndex + 4)

                    onAddLineBreak()
                }

                if (html.isNotBlank())
                    richParagraphList.addAll(RichTextStateHtmlParser.encode(html).richParagraphList)

                // Todo: support HTML Block in markdown
            }
        )

        val toDeleteParagraphIndices = mutableListOf<Int>()
        var lastNonEmptyParagraphIndex = -1
        var lastBrParagraphIndex = -1

        richParagraphList.forEachIndexed { i, paragraph ->
            paragraph.trim()

            val isEmpty = paragraph.isEmpty()
            val isBr = i in brParagraphIndices

            // Delete empty paragraphs between line breaks to match Markdown rendering
            if (isBr && lastNonEmptyParagraphIndex < lastBrParagraphIndex) {
                val range = (lastBrParagraphIndex + 1)..(i - 1)

                if (!range.isEmpty())
                    toDeleteParagraphIndices.addAll(range)
            }

            if (!isEmpty)
                lastNonEmptyParagraphIndex = i

            if (isBr)
                lastBrParagraphIndex = i
        }

        toDeleteParagraphIndices.reversed().forEach { i ->
            richParagraphList.removeAt(i)
        }

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {
        val builder = StringBuilder()

        var useLineBreak = false

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
            // Append paragraph start text
            builder.appendParagraphStartText(richParagraph)

            // Read the heading prefix from the first-class field rather than fingerprinting
            // the first child's SpanStyle.
            if (richParagraph.headingStyle != HeadingStyle.Normal) {
                builder.append(richParagraph.headingStyle.markdownPrefix)
            }

            // Append paragraph children. Inside a heading paragraph the heading defaults already
            // imply bold/font-size/etc., so suppress redundant ** formatting on heading-implied
            // attributes.
            val isHeading = richParagraph.headingStyle != HeadingStyle.Normal
            richParagraph.children.fastForEach { richSpan ->
                builder.append(decodeRichSpanToMarkdown(richSpan, isHeading = isHeading))
            }

            // Append line break if needed
            val isBlank = richParagraph.isBlank()

            if (useLineBreak && isBlank)
                builder.append("<br>")

            useLineBreak = isBlank

            if (index < richTextState.richParagraphList.lastIndex) {
                // Append new line
                builder.appendLine()

                // CommonMark requires a list block to be preceded by a blank line when it
                // follows a non-list paragraph; otherwise a lone `-` underneath a non-empty
                // line is parsed as a setext H2 underline (turning the paragraph into a
                // heading and dropping the list). See #441.
                val nextParagraph = richTextState.richParagraphList[index + 1]
                if (
                    !isBlank &&
                    !richParagraph.type.isList() &&
                    nextParagraph.type.isList()
                ) {
                    builder.appendLine()
                }
            }
        }

        return correctMarkdownText(builder.toString())
    }

    private fun ParagraphType.isList(): Boolean =
        this is OrderedList || this is UnorderedList

    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeRichSpanToMarkdown(
        richSpan: RichSpan,
        isHeading: Boolean = false,
    ): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Check if span is blank
        val isBlank = richSpan.isBlank()

        // Convert span style to CSS string
        val markdownOpen = mutableListOf<String>()
        val markdownClose = mutableListOf<String>()

        // Bold is based off fontWeight. Skip the ** markers inside headings since headings
        // already imply bold; emitting ** would produce `# **Title**` which round-trips back to
        // a double-bold span.
        if (!isHeading && (richSpan.spanStyle.fontWeight?.weight ?: 400) > 400) {
            markdownOpen += "**"
            markdownClose += "**"
        }

        if (richSpan.spanStyle.fontStyle == FontStyle.Italic) {
            markdownOpen += "*"
            markdownClose += "*"
        }

        if (richSpan.spanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true) {
            markdownOpen += "~~"
            markdownClose += "~~"
        }

        if (richSpan.spanStyle.textDecoration?.contains(TextDecoration.Underline) == true) {
            markdownOpen += "<u>"
            markdownClose += "</u>"
        }

        // Append markdown open
        if (!isBlank && markdownOpen.isNotEmpty())
            stringBuilder.append(markdownOpen.joinToString(separator = ""))

        // Apply rich span style to markdown
        val spanMarkdown = decodeMarkdownElementFromRichSpan(richSpan.text, richSpan.richSpanStyle)

        // Append text
        stringBuilder.append(spanMarkdown)

        // Append children
        richSpan.children.fastForEach { child ->
            stringBuilder.append(decodeRichSpanToMarkdown(child, isHeading = isHeading))
        }

        // Append markdown close
        if (!isBlank && markdownClose.isNotEmpty())
            stringBuilder.append(markdownClose.reversed().joinToString(separator = ""))

        return stringBuilder.toString()
    }

    private fun StringBuilder.appendParagraphStartText(paragraph: RichParagraph) {
        when (val type = paragraph.type) {
            is OrderedList ->
                append("  ".repeat(type.level - 1) + "${type.number}. ")

            is UnorderedList ->
                append("  ".repeat(type.level - 1) + "- ")

            else ->
                Unit
        }
    }

    /**
     * Encodes Markdown elements to [SpanStyle].
     * Some Markdown elements have both an associated SpanStyle and ParagraphStyle.
     * Ensure both the [SpanStyle] (via [markdownElementsSpanStyleEncodeMap] - if applicable) and
     * [androidx.compose.ui.text.ParagraphStyle] (via [markdownElementsParagraphStyleEncodeMap] - if applicable)
     * are applied to the text.
     * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
     */
    private val markdownElementsSpanStyleEncodeMap = mapOf(
        MarkdownElementTypes.STRONG to BoldSpanStyle,
        MarkdownElementTypes.EMPH to ItalicSpanStyle,
        GFMElementTypes.STRIKETHROUGH to StrikethroughSpanStyle,
        MarkdownElementTypes.ATX_1 to H1SpanStyle,
        MarkdownElementTypes.ATX_2 to H2SpanStyle,
        MarkdownElementTypes.ATX_3 to H3SpanStyle,
        MarkdownElementTypes.ATX_4 to H4SpanStyle,
        MarkdownElementTypes.ATX_5 to H5SpanStyle,
        MarkdownElementTypes.ATX_6 to H6SpanStyle,
    )

    /**
     * Encodes the Markdown elements to [androidx.compose.ui.text.ParagraphStyle].
     * Some Markdown elements have both an associated SpanStyle and ParagraphStyle.
     * Ensure both the [SpanStyle] (via [markdownElementsSpanStyleEncodeMap] - if applicable) and
     * [androidx.compose.ui.text.ParagraphStyle] (via [markdownElementsParagraphStyleEncodeMap] if applicable)
     * are applied to the text.
     * @see <a href="https://github.com/chrisalley/markdown-garden/blob/master/source/guides/headers/atx-headers.md">ATX Header formatting</a>
     */
    private val markdownElementsParagraphStyleEncodeMap = mapOf(
        MarkdownElementTypes.ATX_1 to H1ParagraphStyle,
        MarkdownElementTypes.ATX_2 to H2ParagraphStyle,
        MarkdownElementTypes.ATX_3 to H3ParagraphStyle,
        MarkdownElementTypes.ATX_4 to H4ParagraphStyle,
        MarkdownElementTypes.ATX_5 to H5ParagraphStyle,
        MarkdownElementTypes.ATX_6 to H6ParagraphStyle,
    )

    /**
     * Encodes Markdown elements to [RichSpanStyle].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun encodeMarkdownElementToRichSpanStyle(
        node: ASTNode,
        markdown: String,
    ): RichSpanStyle {
        val isImage = node.parent?.type == MarkdownElementTypes.IMAGE

        return when (node.type) {
            GFMTokenTypes.GFM_AUTOLINK -> {
                val destination = node.getTextInNode(markdown).toString()
                RichSpanStyle.Link(url = destination)
            }

            MarkdownElementTypes.INLINE_LINK -> {
                val destination = node
                    .findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getTextInNode(markdown)
                    ?.toString()
                    .orEmpty()

                val linkLabel = node
                    .findChildOfType(MarkdownElementTypes.LINK_TEXT)
                    ?.getTextInNode(markdown)
                    ?.toString()
                    ?.removeSurrounding("[", "]")
                    .orEmpty()

                val token = parseTokenDestination(destination, linkLabel)

                when {
                    token != null -> token
                    isImage ->
                        RichSpanStyle.Image(
                            model = destination,
                            width = 0.sp,
                            height = 0.sp,
                        )
                    else ->
                        RichSpanStyle.Link(url = destination)
                }
            }

            MarkdownElementTypes.CODE_SPAN ->
                RichSpanStyle.Code()

            else ->
                RichSpanStyle.Default
        }
    }

    /**
     * Encode [ParagraphType] from Markdown [ASTNode].
     */
    private fun encodeRichParagraphTypeFromMarkdownElement(
        node: ASTNode,
    ): ParagraphType {
        return when (node.type) {
            MarkdownElementTypes.UNORDERED_LIST -> UnorderedList()
            MarkdownElementTypes.ORDERED_LIST -> OrderedList(0)
            else -> DefaultParagraph()
        }
    }

    /**
     * Decodes Markdown elements from [RichSpan].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeMarkdownElementFromRichSpan(
        text: String,
        richSpanStyle: RichSpanStyle,
    ): String {
        return when (richSpanStyle) {
            is RichSpanStyle.Link -> "[$text](${richSpanStyle.url})"
            is RichSpanStyle.Code -> "`$text`"
            is RichSpanStyle.Token -> {
                // Pseudo-link syntax: [label](trigger:triggerId:id)
                val label = richSpanStyle.label.ifEmpty { text }
                "[$label]($TokenDestinationPrefix${richSpanStyle.triggerId}:${richSpanStyle.id})"
            }
            is RichSpanStyle.Image -> {
                // Standard Markdown image syntax `![alt](url)`. Only models
                // that are strings (URLs) round-trip to Markdown; other
                // painter models have no representable form and are
                // dropped. The raw `text` at this point is the inline-
                // content placeholder char and must not leak into the
                // output.
                val model = richSpanStyle.model
                if (model is String) {
                    val alt = richSpanStyle.contentDescription.orEmpty()
                    "![$alt]($model)"
                } else {
                    ""
                }
            }
            else -> text
        }
    }

    /**
     * Parses a link destination of the form `trigger:<triggerId>:<id>` into a [RichSpanStyle.Token].
     * Returns `null` if the destination doesn't match the token shape.
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun parseTokenDestination(
        destination: String,
        label: String,
    ): RichSpanStyle.Token? {
        if (!destination.startsWith(TokenDestinationPrefix)) return null
        val payload = destination.removePrefix(TokenDestinationPrefix)
        val separatorIndex = payload.indexOf(':')
        if (separatorIndex <= 0) return null
        val triggerId = payload.substring(0, separatorIndex)
        val id = payload.substring(separatorIndex + 1)
        if (triggerId.isEmpty() || id.isEmpty()) return null
        return RichSpanStyle.Token(
            triggerId = triggerId,
            id = id,
            label = label,
        )
    }

    private const val TokenDestinationPrefix = "trigger:"

    /**
     * Markdown block elements.
     *
     * @see <a href="https://www.w3schools.com/html/html_blocks.asp">HTML blocks</a>
     */
    private val markdownBlockElements = setOf(
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.ORDERED_LIST,
        MarkdownElementTypes.UNORDERED_LIST,
        MarkdownElementTypes.LIST_ITEM,
    )

}