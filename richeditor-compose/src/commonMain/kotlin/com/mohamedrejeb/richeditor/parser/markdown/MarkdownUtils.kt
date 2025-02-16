package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.util.fastForEach
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

internal fun encodeMarkdownToRichText(
    markdown: String,
    onOpenNode: (node: ASTNode) -> Unit,
    onCloseNode: (node: ASTNode) -> Unit,
    onText: (text: String) -> Unit,
    onHtmlTag: (tag: String) -> Unit,
    onHtmlBlock: (html: String) -> Unit,
) {
    val markdownText = correctMarkdownText(markdown)

    val parser = MarkdownParser(GFMFlavourDescriptor())
    val tree = parser.buildMarkdownTreeFromString(markdownText)
    tree.children.fastForEach { node ->
        encodeMarkdownNodeToRichText(
            node = node,
            markdown = markdownText,
            onOpenNode = onOpenNode,
            onCloseNode = onCloseNode,
            onText = onText,
            onHtmlTag = onHtmlTag,
            onHtmlBlock = onHtmlBlock,
        )
    }
}

internal fun correctMarkdownText(text: String): String {
    var newText = StringBuilder()

    var pendingSpaces = 0

    var pendingTag = ""
    val lastOpenedTags = mutableListOf<String>()

    fun isCloseTag(tag: String = pendingTag) =
        tag == lastOpenedTags.lastOrNull()

    fun addPendingSpaces() {
        if (pendingSpaces > 0)
            newText.append(" ".repeat(pendingSpaces))

        pendingSpaces = 0
    }

    fun onTag(tag: String = pendingTag) {
        if (tag.isEmpty())
            return

        if (isCloseTag(tag)) {
            // On close tag

            lastOpenedTags.removeLastOrNull()
        } else {
            // On open tag

            addPendingSpaces()

            lastOpenedTags.add(tag)
        }

        newText.append(tag)

        if (tag == pendingTag)
            pendingTag = ""
    }

    fun onPendingTag() {
        while (pendingTag.isNotEmpty()) {
            val lastOpenedTag = lastOpenedTags.lastOrNull()

            if (
                lastOpenedTag == null ||
                pendingTag.first() != lastOpenedTag.first() ||
                pendingTag.length < lastOpenedTag.length
            ) {
                // Handle open tag

                val tag =
                    if (pendingTag.length >= 3)
                        pendingTag.substring(0, 3)
                    else
                        pendingTag

                val newPendingTag =
                    if (pendingTag.length >= 3)
                        pendingTag.substring(3)
                    else
                        ""

                onTag(tag)

                pendingTag = newPendingTag
            } else {
                // Handle close tag

                val tag = lastOpenedTag

                val newPendingTag =
                    pendingTag.substring(tag.length)

                onTag(tag)

                pendingTag = newPendingTag
            }
        }
    }

    fun onTextChar(char: Char) {
        onTag()

        if (pendingTag.isEmpty() || isCloseTag())
            addPendingSpaces()

        newText.append(char)
    }

    var isLineStart = false
    var isTwoSpaceIndent = false
    var isReachedFirstIndent = false
    var spaces = 0

    text.forEachIndexed { i, char ->
        // Change indent from 2 spaces to 4 spaces
        if (char == '\n') {
            isLineStart = true
        } else if (isLineStart) {
            if (char == ' ') {
                spaces++
            } else if (!isReachedFirstIndent) {
                isLineStart = false
                if (spaces == 2) {
                    newText.append("  ")
                    isTwoSpaceIndent = true
                } else {
                    isTwoSpaceIndent = false
                }

                isReachedFirstIndent = spaces >= 2

                spaces = 0
            } else {
                isLineStart = false
                if (isTwoSpaceIndent && spaces >= 2) {
                    newText.append(" ".repeat(spaces))
                }

                spaces = 0
            }
        }

        // Extract edge spaces from tags
        if (char == '*' || char == '~') {
            if (!pendingTag.all { it == char })
                onPendingTag()

            pendingTag += char

            if (pendingTag.length > 2)
                onPendingTag()
        } else if (char == ' ') {
            if (isCloseTag())
                onTag()

            pendingSpaces++
        } else {
            onTextChar(char)
        }
    }

    onTag()
    addPendingSpaces()

    return newText.toString()
}

private fun encodeMarkdownNodeToRichText(
    node: ASTNode,
    markdown: String,
    onOpenNode: (node: ASTNode) -> Unit,
    onCloseNode: (node: ASTNode) -> Unit,
    onText: (text: String) -> Unit,
    onHtmlTag: (tag: String) -> Unit,
    onHtmlBlock: (html: String) -> Unit,
) {
    when (node.type) {
        MarkdownTokenTypes.TEXT -> onText(node.getTextInNode(markdown).toString())
        MarkdownTokenTypes.WHITE_SPACE -> onText(" ")
        MarkdownTokenTypes.SINGLE_QUOTE -> onText("'")
        MarkdownTokenTypes.DOUBLE_QUOTE -> onText("\"")
        MarkdownTokenTypes.LPAREN -> onText("(")
        MarkdownTokenTypes.RPAREN -> onText(")")
        MarkdownTokenTypes.LBRACKET -> onText("[")
        MarkdownTokenTypes.RBRACKET -> onText("]")
        MarkdownTokenTypes.LT -> onText("<")
        MarkdownTokenTypes.GT -> onText(">")
        MarkdownTokenTypes.COLON -> onText(":")
        MarkdownTokenTypes.EXCLAMATION_MARK -> onText("!")
        MarkdownTokenTypes.EMPH -> onText("*")
        GFMTokenTypes.TILDE -> onText("~")
        MarkdownElementTypes.STRONG, GFMElementTypes.STRIKETHROUGH -> {
            onOpenNode(node)
            val children = node.children.toMutableList()
            children.removeFirstOrNull()
            children.removeFirstOrNull()
            children.removeLastOrNull()
            children.removeLastOrNull()
            children.fastForEach { child ->
                encodeMarkdownNodeToRichText(
                    node = child,
                    markdown = markdown,
                    onOpenNode = onOpenNode,
                    onCloseNode = onCloseNode,
                    onText = onText,
                    onHtmlTag = onHtmlTag,
                    onHtmlBlock = onHtmlBlock,
                )
            }
            onCloseNode(node)
        }

        MarkdownElementTypes.EMPH -> {
            onOpenNode(node)
            val children = node.children.toMutableList()
            children.removeFirstOrNull()
            children.removeLastOrNull()
            children.fastForEach { child ->
                encodeMarkdownNodeToRichText(
                    node = child,
                    markdown = markdown,
                    onOpenNode = onOpenNode,
                    onCloseNode = onCloseNode,
                    onText = onText,
                    onHtmlTag = onHtmlTag,
                    onHtmlBlock = onHtmlBlock,
                )
            }
            onCloseNode(node)
        }

        MarkdownElementTypes.CODE_SPAN -> {
            onOpenNode(node)
            onText(node.getTextInNode(markdown).removeSurrounding("`").toString())
            onCloseNode(node)
        }

        MarkdownElementTypes.INLINE_LINK -> {
            onOpenNode(node)
            val text = node
                .findChildOfType(MarkdownElementTypes.LINK_TEXT)
                ?.getTextInNode(markdown)
                ?.drop(1)
                ?.dropLast(1)
                ?.toString()
            onText(text ?: "")
            onCloseNode(node)
        }

        MarkdownTokenTypes.HTML_TAG -> {
            onHtmlTag(node.getTextInNode(markdown).toString())
        }

        MarkdownElementTypes.HTML_BLOCK -> {
            onHtmlBlock(node.getTextInNode(markdown).toString())
        }

        else -> {
            onOpenNode(node)
            node.children.fastForEach { child ->
                encodeMarkdownNodeToRichText(
                    node = child,
                    markdown = markdown,
                    onOpenNode = onOpenNode,
                    onCloseNode = onCloseNode,
                    onText = onText,
                    onHtmlTag = onHtmlTag,
                    onHtmlBlock = onHtmlBlock,
                )
            }
            onCloseNode(node)
        }
    }
}