package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.*
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.parser.utils.*
import com.mohamedrejeb.richeditor.utils.fastForEach
import com.mohamedrejeb.richeditor.utils.fastForEachIndexed
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

internal object RichTextStateMarkdownParser : RichTextStateParser<String> {

    override fun encode(input: String): RichTextState {
        val openedNodes = mutableListOf<ASTNode>()
        val stringBuilder = StringBuilder()
        val currentStyles: MutableList<RichTextStyle> = mutableListOf()
        val richParagraphList = mutableListOf(RichParagraph())
        var currentRichSpan: RichSpan? = null
        var currentRichParagraphType: RichParagraph.Type = RichParagraph.Type.Default

        encodeMarkdownToRichText(
            markdown = input,
            onText = { text ->
                if (text.isEmpty()) return@encodeMarkdownToRichText

                stringBuilder.append(text)

                if (richParagraphList.isEmpty())
                    richParagraphList.add(RichParagraph())

                val currentRichParagraph = richParagraphList.last()
                val safeCurrentRichSpan = currentRichSpan ?: RichSpan(paragraph = currentRichParagraph)

                if (safeCurrentRichSpan.children.isEmpty()) {
                    safeCurrentRichSpan.text += text
                } else {
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.text = text
                    safeCurrentRichSpan.children.add(newRichSpan)
                }

                if (currentRichSpan == null) {
                    currentRichSpan = safeCurrentRichSpan
                    currentRichParagraph.children.add(safeCurrentRichSpan)
                }
            },
            onOpenNode = { node ->
                openedNodes.add(node)

                val tagSpanStyle = markdownElementsSpanStyleEncodeMap[node.type]

                if (node.type in markdownBlockElements) {
                    stringBuilder.append(' ')

                    val currentRichParagraph = richParagraphList.last()

                    // Get paragraph type from markdown element
                    if (currentRichParagraphType == RichParagraph.Type.Default) {
                        val paragraphType = encodeRichParagraphTypeFromMarkdownElement(node)
                        currentRichParagraphType = paragraphType
                    }

                    // Set paragraph type if an element is a list item
                    if (node.type == MarkdownElementTypes.LIST_ITEM) {
                        currentRichParagraph.type = currentRichParagraphType.nextParagraphType
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
                    newRichSpan.style = richSpanStyle

                    if (currentRichSpan != null) {
                        newRichSpan.parent = currentRichSpan
                        currentRichSpan?.children?.add(newRichSpan)
                        currentRichSpan = newRichSpan
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                        currentRichSpan = newRichSpan
                    }
                }
            },
            onCloseNode = { node ->
                openedNodes.removeLastOrNull()
                currentStyles.removeLastOrNull()

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
                        currentRichSpan?.spanStyle = currentRichSpan?.spanStyle?.merge(child.spanStyle) ?: child.spanStyle
                        currentRichSpan?.style = child.style
                        currentRichSpan?.children?.clear()
                    }
                }

                // Add new line if needed.
                // Prevent adding two consecutive new lines
                if (node.type == MarkdownTokenTypes.EOL) {
                    val lastParagraph = richParagraphList.lastOrNull()
                    val beforeLastParagraph = richParagraphList.getOrNull(richParagraphList.lastIndex - 1)
                    if (lastParagraph?.isEmpty() != true || beforeLastParagraph?.isEmpty() != true)
                        richParagraphList.add(RichParagraph())

                    currentRichSpan = null
                }

                // Reset paragraph type
                if (
                    node.type == MarkdownElementTypes.ORDERED_LIST ||
                    node.type == MarkdownElementTypes.UNORDERED_LIST
                ) {
                    currentRichParagraphType = RichParagraph.Type.Default
                }

                currentRichSpan = currentRichSpan?.parent
            },
            onHtml = { html ->
                // Todo: support HTML in markdown
            }
        )

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {
        val builder = StringBuilder()

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
            // Append paragraph start text
            builder.append(richParagraph.type.startRichSpan.text)

            richParagraph.getFirstNonEmptyChild()?.let { firstNonEmptyChild ->
                if (firstNonEmptyChild.text.isNotEmpty()) {
                    // Append markdown line start text
                    builder.append(getMarkdownLineStartTextFromFirstRichSpan(firstNonEmptyChild))
                }
            }

            // Append paragraph children
            richParagraph.children.fastForEach { richSpan ->
                builder.append(decodeRichSpanToMarkdown(richSpan))
            }

            if (index < richTextState.richParagraphList.lastIndex) {
                // Append new line
                builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun decodeRichSpanToMarkdown(richSpan: RichSpan): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Convert span style to CSS string
        var markdownOpen = ""
        if ((richSpan.spanStyle.fontWeight?.weight ?: 400) > 400) markdownOpen += "**"
        if (richSpan.spanStyle.fontStyle == FontStyle.Italic) markdownOpen += "*"
        if (richSpan.spanStyle.textDecoration == TextDecoration.LineThrough) markdownOpen += "~~"

        // Append markdown open
        stringBuilder.append(markdownOpen)

        // Apply rich span style to markdown
        val spanMarkdown = decodeMarkdownElementFromRichSpan(richSpan.text, richSpan.style)

        // Append text
        stringBuilder.append(spanMarkdown)

        // Append children
        richSpan.children.fastForEach { child ->
            stringBuilder.append(decodeRichSpanToMarkdown(child))
        }

        // Append markdown close
        stringBuilder.append(markdownOpen.reversed())

        return stringBuilder.toString()
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
        MarkdownElementTypes.ATX_1 to H1SPanStyle,
        MarkdownElementTypes.ATX_2 to H2SPanStyle,
        MarkdownElementTypes.ATX_3 to H3SPanStyle,
        MarkdownElementTypes.ATX_4 to H4SPanStyle,
        MarkdownElementTypes.ATX_5 to H5SPanStyle,
        MarkdownElementTypes.ATX_6 to H6SPanStyle,
    )

    /**
     * Encodes Markdown elements to [RichSpanStyle].
     */
    private fun encodeMarkdownElementToRichSpanStyle(
        node: ASTNode,
        markdown: String,
    ): RichSpanStyle {
        return when (node.type) {
            MarkdownElementTypes.INLINE_LINK -> {
                val destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(markdown)?.toString()
                RichSpanStyle.Link(url = destination ?: "")
            }
            MarkdownElementTypes.CODE_SPAN -> RichSpanStyle.Code()
            else -> RichSpanStyle.Default
        }
    }

    /**
     * Encode [RichParagraph.Type] from Markdown [ASTNode].
     */
    private fun encodeRichParagraphTypeFromMarkdownElement(
        node: ASTNode,
    ): RichParagraph.Type {
        return when (node.type) {
            MarkdownElementTypes.UNORDERED_LIST -> RichParagraph.Type.UnorderedList()
            MarkdownElementTypes.ORDERED_LIST -> RichParagraph.Type.OrderedList(0)
            else -> RichParagraph.Type.Default
        }
    }

    /**
     * Decodes HTML elements from [RichSpan].
     */
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
     * For example, if the first [RichSpan] spanStyle is [H1SPanStyle], the markdown line start text will be "# ".
     */
    private fun getMarkdownLineStartTextFromFirstRichSpan(firstRichSpan: RichSpan): String {
        if ((firstRichSpan.spanStyle.fontWeight?.weight ?: 400) <= 400) return ""
        val fontSize = firstRichSpan.spanStyle.fontSize

        return if (fontSize.isEm) {
            when {
                fontSize >= H1SPanStyle.fontSize -> "# "
                fontSize >= H1SPanStyle.fontSize -> "## "
                fontSize >= H1SPanStyle.fontSize -> "### "
                fontSize >= H1SPanStyle.fontSize -> "#### "
                fontSize >= H1SPanStyle.fontSize -> "##### "
                fontSize >= H1SPanStyle.fontSize -> "###### "
                else -> ""
            }
        } else {
            when {
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "# "
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "## "
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "### "
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "#### "
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "##### "
                fontSize.value >= H1SPanStyle.fontSize.value * 16 -> "###### "
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