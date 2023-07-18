package com.mohamedrajeb.richeditor.markdown

import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

fun RichTextState.setMarkdown(markdown: String) {
    val parser = Parser(mutableListOf())
    parser.parseMarkdown(markdown)
    updateRichParagraphList(parser.paragraphs)
}

class MarkdownRichTextStateParser(
//    val onBackground: Color,
    val paragraphs: MutableList<RichParagraph>
) {


    fun getLastOrNewParagraph(): RichParagraph {
        if (paragraphs.isEmpty()) {
            val para = RichParagraph()
            paragraphs.add(para)
            return para
        }
        return paragraphs.last()
    }

    fun addToLastOrCreateNew(richSpan: RichSpan) {
        getLastOrNewParagraph().children.add(richSpan)
    }

    fun MutableList<RichSpan>.getLastOrNewDefaultRichSpan(paragraph: RichParagraph): RichSpan {
//        val last = lastOrNull()
//        if (last == null || last.spanStyle == SpanStyle()) {
        val new = RichSpan(
            paragraph = paragraph
        )
        add(new)
        return new
//        }
//        return last
    }


}

internal typealias Parser = MarkdownRichTextStateParser

fun Parser.parseMarkdown(markdown: String) {
    val parser = MarkdownParser(GFMFlavourDescriptor())
    val tree = parser.buildMarkdownTreeFromString(markdown)
    for (node in tree.children) {
        if (!importIntoState(
                node = node,
                markdown
            )
        ) {
            for (child in node.children) {
                importIntoState(
                    node = node,
                    markdown
                )
            }
        }
    }
}

private fun Parser.importIntoState(
    node: ASTNode,
    content: String
): Boolean {


    fun appendText(text: String) {
        addToLastOrCreateNew(
            RichSpan(
                text = text,
                paragraph = getLastOrNewParagraph()
            )
        )
    }

    fun appendText(text: Char) {
        appendText(text.toString())
    }

    var handled = true
    when (node.type) {
        MarkdownTokenTypes.TEXT -> {
            val text = node.getTextInNode(content).toString()
//            print("TEXT->$text")
            appendText(text)
        }
//        MarkdownTokenTypes.EOL -> Spacer(Modifier.padding(4.dp))
        MarkdownElementTypes.CODE_FENCE -> {
//            val lang = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)?.trim()
//                    ?.toString() ?: "js"
//            val start = node.children[2].startOffset
//            val end = node.children[node.children.size - 2].endOffset
//            val codeValue = content.subSequence(start, end).trim().toString()
            // TODO
        }

        MarkdownElementTypes.ATX_1 -> {
            val paragraph = getLastOrNewParagraph()
            paragraph.children.add(
                RichSpan(
                    paragraph = paragraph,
                    children = buildSpans(content = content, paragraph, node),
                    spanStyle = MarkdownSpanStyles.H1SPanStyle
                )
            )
        }

        MarkdownElementTypes.ATX_2 -> {
//            print("H2->INSIDE->")
            val paragraph = getLastOrNewParagraph()
            paragraph.children.add(
                RichSpan(
                    paragraph = paragraph,
                    children = buildSpans(content = content, paragraph, node),
                    spanStyle = MarkdownSpanStyles.H2SPanStyle
                )
            )
        }

        MarkdownElementTypes.ATX_3 -> {
            val paragraph = getLastOrNewParagraph()
            addToLastOrCreateNew(
                RichSpan(
                    children = buildSpans(content = content, paragraph, node),
                    paragraph = paragraph,
                    spanStyle = MarkdownSpanStyles.H3SPanStyle
                )
            )
        }

        MarkdownElementTypes.ATX_4 -> {
            val paragraph = getLastOrNewParagraph()
            addToLastOrCreateNew(
                RichSpan(
                    children = buildSpans(content = content, paragraph, node),
                    paragraph = paragraph,
                    spanStyle = MarkdownSpanStyles.H4SPanStyle
                )
            )
        }

        MarkdownElementTypes.ATX_5 -> {
            val paragraph = getLastOrNewParagraph()
            addToLastOrCreateNew(
                RichSpan(
                    children = buildSpans(content = content, paragraph, node),
                    paragraph = paragraph,
                    spanStyle = MarkdownSpanStyles.H5SPanStyle
                )
            )
        }

        MarkdownElementTypes.ATX_6 -> {
            val paragraph = getLastOrNewParagraph()
            addToLastOrCreateNew(
                RichSpan(
                    children = buildSpans(content = content, paragraph, node),
                    paragraph = paragraph,
                    spanStyle = MarkdownSpanStyles.H6SPanStyle
                )
            )
        }
//        MarkdownElementTypes.BLOCK_QUOTE -> MarkdownBlockQuote(content, this, color = color)
        MarkdownElementTypes.PARAGRAPH -> {
//            print("PARA->")
            val paragraph = getLastOrNewParagraph()
            addToLastOrCreateNew(
                RichSpan(
                    children = buildSpans(content = content, paragraph, node),
                    paragraph = paragraph,
                )
            )
        }

        MarkdownElementTypes.ORDERED_LIST -> {
            node.orderedList(
                parser = this,
                content = content
            )

        }

        MarkdownElementTypes.UNORDERED_LIST -> {
            node.unorderedList(
                parser = this,
                content = content
            )
        }

        MarkdownElementTypes.IMAGE -> {
            val link = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                ?.toString()
            //todo add image
        }

        MarkdownElementTypes.LINK_DEFINITION -> {
            val linkLabel =
                node.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getTextInNode(content)?.toString()
            if (linkLabel != null) {
                val destination =
                    node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                        ?.toString()
                addToLastOrCreateNew(
                    RichSpan(
                        text = linkLabel,
                        paragraph = getLastOrNewParagraph(),
                        spanStyle = MarkdownSpanStyles.linkSpanStyle,
                        style = RichSpanStyle.Link(destination ?: "")
                    )
                )
            }
        }

        MarkdownTokenTypes.EOL -> appendText('\n')
//        MarkdownTokenTypes.HARD_LINE_BREAK -> appendText("\n")
        else -> handled = false
    }
    return handled
}

private fun ASTNode.listItems(
    parser: Parser,
    content: String,
    level: Int,
    listItem: ASTNode.(index: Int) -> Unit,
) {
    var index = -1
    for (child in children) {
        index++
        when (child.type) {
            MarkdownElementTypes.LIST_ITEM -> {
                child.listItem(index)
                if (child.children.isNotEmpty()) {
                    child.listItems(
                        parser,
                        content = content,
                        level = level,
                        listItem,
                    )
                }
            }

            MarkdownElementTypes.ORDERED_LIST -> child.orderedList(
                parser = parser,
                content = content,
                level = level + 1
            )

            MarkdownElementTypes.UNORDERED_LIST -> child.unorderedList(
                parser = parser,
                content = content,
                level = level + 1
            )
        }
    }
}

internal fun ASTNode.unorderedList(
    parser: Parser,
    content: String,
    level: Int = 0
) {
    val paragraph = RichParagraph(
        type = RichParagraph.Type.UnorderedList
    )
//    print("UNORDERED_LIST->")
    listItems(
        parser = parser,
        content = content,
        level = level
    ) { listItemIndex ->

        fun children(): MutableList<RichSpan> {
//            print("LISTITEM->")
            return parser.buildSpans(
                content = content,
                parent = paragraph,
                children = children.filter { it.type != MarkdownTokenTypes.EOL }
            )
        }

        fun appendTextListItem() {
            paragraph.children.add(
                RichSpan(
                    children = children().apply {
                        if(listItemIndex > -1){
                            add(RichSpan(text = "\n", paragraph = paragraph))
                        }
                    },
                    paragraph = paragraph,
                )
            )
        }

        appendTextListItem()
    }
    paragraph.let { parser.paragraphs.add(it) }
}

internal fun ASTNode.orderedList(
    parser: Parser,
    content: String,
    level: Int = 0
) {
    var paragraph: RichParagraph? = null
    listItems(
        parser = parser,
        content = content,
        level = level
    ) {
//        for (i in 0..level) {
        // TODO multiple levels aren't supported
//        }
        val listNumber = findChildOfType(MarkdownTokenTypes.LIST_NUMBER)?.getTextInNode(content)
        if (paragraph == null) {
            paragraph = RichParagraph(
                type = RichParagraph.Type.OrderedList(listNumber.toString().toIntOrNull() ?: 0)
            )
        }
        paragraph!!.children.addAll(
            parser.buildSpans(
                content,
                paragraph!!,
                children.filter { it.type != MarkdownTokenTypes.EOL }
            )
        )
    }
    paragraph?.let { parser.paragraphs.add(it) }
}