package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import com.mohamedrejeb.richeditor.model.*
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.parser.html.*
import com.mohamedrejeb.richeditor.parser.html.CssDecoder
import com.mohamedrejeb.richeditor.utils.customMerge
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
        val richParagraphList = mutableListOf<RichParagraph>()
        var currentRichSpan: RichSpan? = null
        var lastClosedNode: ASTNode? = null

        encodeMarkdownToRichText(
            markdown = input,
            onText = { text ->
                println("onText: $text")
                if (text.isEmpty()) return@encodeMarkdownToRichText

                if (lastClosedNode?.type in markdownBlockElements) {
                    if (text.isBlank()) return@encodeMarkdownToRichText
                    lastClosedNode = null
                    currentRichSpan = null
                    richParagraphList.add(RichParagraph())
                }

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
                println("onOpenNode: ${node.type}")
                openedNodes.add(node)

                val tagSpanStyle = markdownElementsSpanStyleEncodeMap[node.type]

                if (node.type in markdownBlockElements) {
                    stringBuilder.append(' ')

                    val newRichParagraph = RichParagraph()
                    var paragraphType: RichParagraph.Type = RichParagraph.Type.Default
                    /*if (name == "li") {
                        skipText = false
                        openedNodes.getOrNull(openedNodes.lastIndex - 1)?.first?.let { lastOpenedTag ->
                            paragraphType = RichTextStateHtmlParser.encodeHtmlElementToRichParagraphType(lastOpenedTag)
                        }
                    }*/
                    newRichParagraph.type = paragraphType
                    richParagraphList.add(newRichParagraph)

                    val newRichSpan = RichSpan(paragraph = newRichParagraph)
                    newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        newRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (node.type != MarkdownTokenTypes.EOL) {
                    if (lastClosedNode?.type in markdownBlockElements) {
                        lastClosedNode = null
                        currentRichSpan = null
                        richParagraphList.add(RichParagraph())
                    }

                    val richSpanStyle = encodeMarkdownElementToRichSpanStyle(node, input)

                    if (richParagraphList.isEmpty())
                        richParagraphList.add(RichParagraph())

                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = tagSpanStyle ?: SpanStyle()
                    newRichSpan.style = richSpanStyle

                    if (currentRichSpan != null) {
                        if (currentRichSpan?.isEmpty() == true) {
                            currentRichSpan?.spanStyle = currentRichSpan?.spanStyle?.merge(newRichSpan.spanStyle) ?: newRichSpan.spanStyle
                            currentRichSpan?.style = newRichSpan.style
                        } else {
                            newRichSpan.parent = currentRichSpan
                            currentRichSpan?.children?.add(newRichSpan)
                            currentRichSpan = newRichSpan
                        }
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                        currentRichSpan = newRichSpan
                    }
                }

                lastClosedNode = null
            },
            onCloseNode = { node ->
                println("onCloseNode: ${node.type}")
                openedNodes.removeLastOrNull()
                currentStyles.removeLastOrNull()
                lastClosedNode = node

                if (currentRichSpan?.isEmpty() == true) {
                    val parent = currentRichSpan?.parent
                    if (parent != null)
                        currentRichSpan?.parent?.children?.remove(currentRichSpan)
                    else
                        currentRichSpan?.paragraph?.children?.remove(currentRichSpan)
                }
                currentRichSpan = currentRichSpan?.parent
            }
        )

        richParagraphList.fastForEachIndexed { index, richParagraph ->
            println("Paragraph $index: ${richParagraph.children.size} children")
            println(richParagraph)
        }

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    override fun decode(richTextState: RichTextState): String {
        val builder = StringBuilder()

        var lastParagraphGroupTagName: String? = null

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
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
            ) builder.append("<$paragraphGroupTagName>")

            // Create paragraph tag name
            val paragraphTagName =
                if (paragraphGroupTagName == "ol" || paragraphGroupTagName == "ul") "li"
                else "p"

            // Create paragraph css
            val paragraphCssMap = CssDecoder.decodeParagraphStyleToCssStyleMap(richParagraph.paragraphStyle)
            val paragraphCss = CssDecoder.decodeCssStyleMap(paragraphCssMap)

            // Append paragraph opening tag
            builder.append("<$paragraphTagName style=\"$paragraphCss\">")

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
        }

        return builder.toString()
    }

    private fun decodeRichSpanToHtml(richSpan: RichSpan): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Get HTML element and attributes
        val spanHtml = decodeHtmlElementFromRichSpanStyle(richSpan.style)
        val tagName = spanHtml.first
        val tagAttributes = spanHtml.second

        // Convert attributes map to HTML string
        val tagAttributesStringBuilder = StringBuilder()
        tagAttributes.forEach { (key, value) ->
            tagAttributesStringBuilder.append(" $key=\"$value\"")
        }

        // Convert span style to CSS string
        val spanCssMap = CssDecoder.decodeSpanStyleToCssStyleMap(richSpan.spanStyle)
        val spanCss = CssDecoder.decodeCssStyleMap(spanCssMap)

        val isRequireOpeningTag = tagName != "span" || tagAttributes.isNotEmpty() || spanCss.isNotEmpty()

        if (isRequireOpeningTag) {
            // Append HTML element with attributes and style
            stringBuilder.append("<$tagName$tagAttributesStringBuilder")
            if (spanCss.isNotEmpty()) stringBuilder.append(" style=\"$spanCss\"")
            stringBuilder.append(">")
        }

        // Append text
        stringBuilder.append(richSpan.text)

        // Append children
        richSpan.children.fastForEach { child ->
            stringBuilder.append(decodeRichSpanToHtml(child))
        }

        if (isRequireOpeningTag) {
            // Append closing HTML element
            stringBuilder.append("</$tagName>")
        }

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
            MarkdownElementTypes.LINK_DEFINITION -> {
                val destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(markdown)?.toString()
                RichSpanStyle.Link(url = destination ?: "")
            }
            else -> RichSpanStyle.Default
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