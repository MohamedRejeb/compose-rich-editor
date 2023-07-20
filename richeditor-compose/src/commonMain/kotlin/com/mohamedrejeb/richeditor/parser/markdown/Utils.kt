package com.mohamedrejeb.richeditor.parser.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.ast.ASTNode

/**
 * Tag used to indicate an url for inline content. Required for click handling.
 */
internal const val TAG_URL = "MARKDOWN_URL"

/**
 * Tag used to indicate an image url for inline content. Required for rendering.
 */
internal const val TAG_IMAGE_URL = "MARKDOWN_IMAGE_URL"

/**
 * Find a child node recursive
 */
internal fun ASTNode.findChildOfTypeRecursive(type: IElementType): ASTNode? {
    for (it in children) {
        if (it.type == type) {
            return it
        } else {
            val found = it.findChildOfTypeRecursive(type)
            if (found != null) {
                return found
            }
        }
    }
    return null
}


/**
 * Helper function to drop the first and last element in the children list.
 * E.g. we don't want to render the brackets of a link
 */
internal fun List<ASTNode>.innerList(): List<ASTNode> = this.subList(1, this.size - 1)