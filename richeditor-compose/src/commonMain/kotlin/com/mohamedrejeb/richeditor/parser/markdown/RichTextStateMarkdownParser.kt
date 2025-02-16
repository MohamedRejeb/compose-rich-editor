package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
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

                currentRichSpan?.text = ""
            }
        }

        encodeMarkdownToRichText(
            markdown = input,
            onText = { text ->
                onText(text)
            },
            onOpenNode = { node ->
                openedNodes.add(node)

                if (node.type == MarkdownElementTypes.LIST_ITEM) {
                    currentListLevel++
                }

                val tagSpanStyle = markdownElementsSpanStyleEncodeMap[node.type]

                if (node.type in markdownBlockElements) {
                    val currentRichParagraph = richParagraphList.last()

                    // Get paragraph type from markdown element
                    if (currentRichParagraphType is DefaultParagraph) {
                        val paragraphType = encodeRichParagraphTypeFromMarkdownElement(node)
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

                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        currentRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (node.type != MarkdownTokenTypes.EOL) {
                    val richSpanStyle = encodeMarkdownElementToRichSpanStyle(node, input)

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
                    onText(node.getTextInNode(input).toString())
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

                val lastOpenedNodes = openedNodes.lastOrNull()

                val isList =
                    node.type == MarkdownElementTypes.ORDERED_LIST ||
                            node.type == MarkdownElementTypes.UNORDERED_LIST

                val isLastList =
                    lastOpenedNodes != null &&
                            (lastOpenedNodes.type == MarkdownElementTypes.ORDERED_LIST ||
                                    lastOpenedNodes.type == MarkdownElementTypes.UNORDERED_LIST ||
                                    lastOpenedNodes.type == MarkdownElementTypes.LIST_ITEM)

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

            var isHeading = false

            richParagraph.getFirstNonEmptyChild()?.let { firstNonEmptyChild ->
                if (firstNonEmptyChild.text.isNotEmpty()) {
                    // Append markdown line start text
                    val lineStartText = getMarkdownLineStartTextFromFirstRichSpan(firstNonEmptyChild)
                    builder.append(lineStartText)
                    isHeading = lineStartText.startsWith('#')
                }
            }

            // Append paragraph children
            richParagraph.children.fastForEach { richSpan ->
                builder.append(decodeRichSpanToMarkdown(richSpan, isHeading))
            }

            // Append line break if needed
            val isBlank = richParagraph.isBlank()

            if (useLineBreak && isBlank)
                builder.append("<br>")

            useLineBreak = isBlank

            if (index < richTextState.richParagraphList.lastIndex) {
                // Append new line
                builder.appendLine()
            }
        }

        return correctMarkdownText(builder.toString())
    }

    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeRichSpanToMarkdown(
        richSpan: RichSpan,
        isHeading: Boolean,
    ): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Check if span is blank
        val isBlank = richSpan.isBlank()

        // Convert span style to CSS string
        val markdownOpen = mutableListOf<String>()
        val markdownClose = mutableListOf<String>()

        // Ignore adding bold `**` for heading since it's already bold
        if ((richSpan.spanStyle.fontWeight?.weight ?: 400) > 400 && !isHeading) {
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
            stringBuilder.append(decodeRichSpanToMarkdown(child, isHeading))
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
     *
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

                if (isImage)
                    RichSpanStyle.Image(
                        model = destination,
                        width = 0.sp,
                        height = 0.sp,
                    )
                else
                    RichSpanStyle.Link(url = destination)
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
            else -> text
        }
    }

    /**
     * Returns the markdown line start text from the first [RichSpan].
     * This is used to determine the markdown line start text from the first [RichSpan] spanStyle.
     * For example, if the first [RichSpan] spanStyle is [H1SpanStyle], the markdown line start text will be "# ".
     */
    private fun getMarkdownLineStartTextFromFirstRichSpan(firstRichSpan: RichSpan): String {
        if ((firstRichSpan.spanStyle.fontWeight?.weight ?: 400) <= 400) return ""
        val fontSize = firstRichSpan.spanStyle.fontSize

        return if (fontSize.isEm) {
            when {
                fontSize >= H1SpanStyle.fontSize -> "# "
                fontSize >= H2SpanStyle.fontSize -> "## "
                fontSize >= H3SpanStyle.fontSize -> "### "
                fontSize >= H4SpanStyle.fontSize -> "#### "
                fontSize >= H5SpanStyle.fontSize -> "##### "
                fontSize >= H6SpanStyle.fontSize -> "###### "
                else -> ""
            }
        } else {
            when {
                fontSize.value >= H1SpanStyle.fontSize.value * 16 -> "# "
                fontSize.value >= H2SpanStyle.fontSize.value * 16 -> "## "
                fontSize.value >= H3SpanStyle.fontSize.value * 16 -> "### "
                fontSize.value >= H4SpanStyle.fontSize.value * 16 -> "#### "
                fontSize.value >= H5SpanStyle.fontSize.value * 16 -> "##### "
                fontSize.value >= H6SpanStyle.fontSize.value * 16 -> "###### "
                else -> ""
            }
        }
    }

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