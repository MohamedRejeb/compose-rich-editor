package com.mohamedrejeb.richeditor.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

internal object MarkdownSpanStyles {
    val linkSpanStyle = SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)
    val italic = SpanStyle(fontStyle = FontStyle.Italic)
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    val lineThrough = SpanStyle(textDecoration = TextDecoration.LineThrough)
    val H1SPanStyle = SpanStyle(fontSize = 2.em, fontWeight = FontWeight.Bold)
    val H2SPanStyle = SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)
    val H3SPanStyle = SpanStyle(fontSize = 1.17.em, fontWeight = FontWeight.Bold)
    val H4SPanStyle = SpanStyle(fontSize = 1.12.em, fontWeight = FontWeight.Bold)
    val H5SPanStyle = SpanStyle(fontSize = 0.83.em, fontWeight = FontWeight.Bold)
    val H6SPanStyle = SpanStyle(fontSize = 0.75.em, fontWeight = FontWeight.Bold)
}

/**
 * Appends Styles for all the children nodes of the [parent]
 */
internal fun Parser.buildSpans(content: String, parent : RichParagraph, node: ASTNode): MutableList<RichSpan> {
    return buildSpans(content, parent,node.children)
}

//val TESTMARKDOWN = """# My first heading
//
//## My second heading
//
//### My third heading
//
//#### My fourth heading
//
//##### My fifth heading
//
//###### My sixth heading
//
//## My *italic* heading with [link]()
//
//This is my normal text
//
//This text will contain **bold** and *italic* items.
//
//A line through ~~something~~ and
//
//You can also make the text ***italicandbold***
//
//The link can be [normal]() [*italic*]() , [**bold**]() or [***bold and italic***]()
//A normal link [link](http://example.com)
//
//Here's an auto link http://a.b
//Auto link italic *http://a.b*
//Auto link bold **http://a.b**
//Auto link italicandbold ***http://a.b***
//
//Here's some text that get's added fast
//'"()[]<>:!something`"""


/**
 * Appends styles for [node] and its children
 */
internal fun Parser.buildSpans(
    content: String,
    parent : RichParagraph,
    children: List<ASTNode>
): MutableList<RichSpan> {
    val list = mutableListOf<RichSpan>()
    for (child in children) {
//        print("CHILD(${child.type})->")
        when (child.type) {
            MarkdownElementTypes.PARAGRAPH -> {
                // this paragraph is triggered when a list item is being written , right after the list bullet
                list.add(
                    RichSpan(
                        children = buildSpans(content,parent, child),
                        paragraph = parent,
                    )
                )
            }

            MarkdownElementTypes.IMAGE -> {
                // TODO
//                child.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)
//                    ?.let {
//                        appendInlineContent(TAG_IMAGE_URL, it.getTextInNode(content).toString())
//                    }
            }

            MarkdownElementTypes.EMPH -> {
//                print("IN EMPH->")
                list.add(
                    RichSpan(
                        paragraph = parent,
                        children = buildSpans(content,parent, child),
                        spanStyle = MarkdownSpanStyles.italic
                    )
                )
//                print("OUT EMPH->")
            }

            MarkdownElementTypes.STRONG -> {
                list.add(
                    RichSpan(
                        paragraph = parent,
                        children = buildSpans(content,parent, child),
                        spanStyle = MarkdownSpanStyles.bold
                    )
                )
            }

            MarkdownElementTypes.CODE_SPAN -> {
                list.add(
                    RichSpan(
                        paragraph = parent,
                        children = buildSpans(content,parent, child.children.innerList()),
                        style = RichSpanStyle.Code()
                    )
                )
            }

            MarkdownTokenTypes.LIST_BULLET -> {
//                list.getLastOrNewDefaultRichSpan(parent).text += '*'
            }

            MarkdownElementTypes.AUTOLINK -> list.add(appendAutoLink(content, parent,child))
            MarkdownElementTypes.INLINE_LINK -> appendMarkdownLink(content,parent, child)?.let { list.add(it) }
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> appendMarkdownLink(content,parent, child)?.let { list.add(it) }
            MarkdownElementTypes.FULL_REFERENCE_LINK -> appendMarkdownLink(content,parent, child)?.let { list.add(it) }
            GFMElementTypes.STRIKETHROUGH -> {
                list.add(
                    RichSpan(
                        paragraph = parent,
                        children = buildSpans(content,parent, child.children),
                        spanStyle = MarkdownSpanStyles.lineThrough
                    )
                )
            }

            MarkdownTokenTypes.ATX_HEADER -> {
                // TODO
            }

            MarkdownTokenTypes.ATX_CONTENT -> {
                list.addAll(buildSpans(content,parent,child))
            }

            MarkdownTokenTypes.TEXT -> {
                list.getLastOrNewDefaultRichSpan(parent).text += (child.getTextInNode(content).toString())
            }
            // TODO handling html tags as text
            MarkdownTokenTypes.HTML_TAG -> list.getLastOrNewDefaultRichSpan(parent).text += (child.getTextInNode(content).toString())
            GFMTokenTypes.GFM_AUTOLINK -> if (child.parent == MarkdownElementTypes.LINK_TEXT) {
                list.getLastOrNewDefaultRichSpan(parent).text += (child.getTextInNode(content).toString())
            } else list.add(appendAutoLink(content, parent,child))

            MarkdownTokenTypes.SINGLE_QUOTE -> list.getLastOrNewDefaultRichSpan(parent).text += '\''
            MarkdownTokenTypes.DOUBLE_QUOTE -> list.getLastOrNewDefaultRichSpan(parent).text += ('\"')
            MarkdownTokenTypes.LPAREN -> list.getLastOrNewDefaultRichSpan(parent).text += ('(')
            MarkdownTokenTypes.RPAREN -> list.getLastOrNewDefaultRichSpan(parent).text += (')')
            MarkdownTokenTypes.LBRACKET -> list.getLastOrNewDefaultRichSpan(parent).text += ('[')
            MarkdownTokenTypes.RBRACKET -> list.getLastOrNewDefaultRichSpan(parent).text += (']')
            MarkdownTokenTypes.LT -> list.getLastOrNewDefaultRichSpan(parent).text += ('<')
            MarkdownTokenTypes.GT -> list.getLastOrNewDefaultRichSpan(parent).text += ('>')
            MarkdownTokenTypes.COLON -> list.getLastOrNewDefaultRichSpan(parent).text += (':')
            MarkdownTokenTypes.EXCLAMATION_MARK -> list.getLastOrNewDefaultRichSpan(parent).text += ('!')
            MarkdownTokenTypes.BACKTICK -> list.getLastOrNewDefaultRichSpan(parent).text += ('`')
            MarkdownTokenTypes.HARD_LINE_BREAK -> list.getLastOrNewDefaultRichSpan(parent).text += ("\n\n")
            MarkdownTokenTypes.EOL -> list.getLastOrNewDefaultRichSpan(parent).text += ('\n')
            MarkdownTokenTypes.WHITE_SPACE -> list.getLastOrNewDefaultRichSpan(parent).text += (' ')
//            else -> println("Missed on ${child.type}")
        }
    }
    return list
}

internal fun Parser.appendAutoLink(content: String, parent : RichParagraph, node: ASTNode): RichSpan {
    val destination = node.getTextInNode(content).toString()
    return RichSpan(
        paragraph = parent,
        spanStyle = MarkdownSpanStyles.linkSpanStyle,
        style = RichSpanStyle.Link(url = destination),
        text = node.getTextInNode(content).toString(),
    )
}

internal fun Parser.appendMarkdownLink(content: String, parent : RichParagraph, node: ASTNode): RichSpan? {
    val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.children?.innerList() ?: return null
    val destination =
        node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)?.toString()
    val linkLabel = node.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getTextInNode(content)?.toString()
    return RichSpan(
        paragraph = getLastOrNewParagraph(),
        spanStyle = MarkdownSpanStyles.linkSpanStyle,
        style = RichSpanStyle.Link(url = destination ?: ""),
        text = linkLabel ?: "",
        children = buildSpans(content, parent,linkText)
    )
}