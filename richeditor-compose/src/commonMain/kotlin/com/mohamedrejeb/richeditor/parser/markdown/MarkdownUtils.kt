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
    onCodeBlock: (language: String?, lines: List<String>) -> Unit,
) {

    val parser = MarkdownParser(GFMFlavourDescriptor())
    val tree = parser.buildMarkdownTreeFromString(markdown)
    tree.children.fastForEach { node ->
        encodeMarkdownNodeToRichText(
            node = node,
            markdown = markdown,
            onOpenNode = onOpenNode,
            onCloseNode = onCloseNode,
            onText = onText,
            onHtmlTag = onHtmlTag,
            onHtmlBlock = onHtmlBlock,
            onCodeBlock = onCodeBlock,
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
    // Tracks whether any non-whitespace content has been emitted on the current
    // line. A `*` is a bullet-list marker (not an emphasis delimiter) when it
    // appears before any other content on its line and is followed by a space,
    // newline, or end-of-input. See #637.
    var hasLineContent = false

    text.forEachIndexed { i, char ->
        // Change indent from 2 spaces to 4 spaces
        if (char == '\n') {
            isLineStart = true
            hasLineContent = false
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
            val nextChar = text.getOrNull(i + 1)
            val isBulletMarker =
                char == '*' &&
                    !hasLineContent &&
                    pendingTag.isEmpty() &&
                    (nextChar == null || nextChar == ' ' || nextChar == '\n')

            if (isBulletMarker) {
                // Emit the star verbatim as a list-item marker without folding the
                // surrounding spaces into a paired emphasis delimiter.
                addPendingSpaces()
                newText.append(char)
                hasLineContent = true
            } else {
                if (!pendingTag.all { it == char })
                    onPendingTag()

                pendingTag += char

                if (pendingTag.length > 2)
                    onPendingTag()

                hasLineContent = true
            }
        } else if (char == ' ') {
            if (isCloseTag())
                onTag()

            pendingSpaces++
        } else {
            onTextChar(char)
            if (char != '\n') {
                hasLineContent = true
            }
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
    onCodeBlock: (language: String?, lines: List<String>) -> Unit,
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
                    onCodeBlock = onCodeBlock,
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
                    onCodeBlock = onCodeBlock,
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
            val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)
            if (node.parent?.type == MarkdownElementTypes.IMAGE) {
                // The label of an image is its alt text, taken as written.
                onText(linkText?.getTextInNode(markdown)?.drop(1)?.dropLast(1)?.toString() ?: "")
            } else {
                // The label of a link is inline content: an image, bold, code and so on.
                val children = linkText?.children.orEmpty().toMutableList()
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
                        onCodeBlock = onCodeBlock,
                    )
                }
            }
            onCloseNode(node)
        }

        MarkdownTokenTypes.HTML_TAG -> {
            onHtmlTag(node.getTextInNode(markdown).toString())
        }

        MarkdownElementTypes.HTML_BLOCK -> {
            onHtmlBlock(node.getTextInNode(markdown).toString())
        }

        MarkdownElementTypes.CODE_FENCE -> {
            val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
                ?.getTextInNode(markdown)?.toString()?.trim()?.ifEmpty { null }
            onCodeBlock(language, codeFenceLines(node, markdown))
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
                    onCodeBlock = onCodeBlock,
                )
            }
            onCloseNode(node)
        }
    }
}

/**
 * The lines of a fenced block. Each line of code is one CODE_FENCE_CONTENT token and each line
 * end is one EOL token, so two EOL tokens in a row are an empty line.
 */
private fun codeFenceLines(node: ASTNode, markdown: String): List<String> {
    val lines = mutableListOf<String>()
    var current: StringBuilder? = null
    var started = false
    var ended = false
    node.children.fastForEach { child ->
        if (ended) return@fastForEach
        when (child.type) {
            MarkdownTokenTypes.CODE_FENCE_END -> ended = true
            MarkdownTokenTypes.EOL -> {
                if (started) current?.let { lines += it.toString() }
                started = true
                current = StringBuilder()
            }
            MarkdownTokenTypes.CODE_FENCE_CONTENT ->
                if (started) current?.append(child.getTextInNode(markdown))
        }
    }
    // The line that was open when the block ended: the closing fence's own line for a closed
    // block (empty, dropped), or the last line of code for an unclosed one.
    current?.toString()?.takeIf { it.isNotEmpty() }?.let { lines += it }
    return lines
}