package com.mohamedrejeb.richeditor.model.history

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * Returns a detached deep copy of this paragraph. The copied paragraph and all of its
 * descendant spans are fresh instances with no reference back to the original tree, so
 * mutating the copy cannot affect the original. Used by the history system to capture
 * immutable snapshots.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichParagraph.deepCopy(): RichParagraph {
    val copiedType = type.copy()
    val new = RichParagraph(
        key = key,
        paragraphStyle = paragraphStyle,
        type = copiedType,
        isFromLineBreak = isFromLineBreak,
    )
    children.forEach { child ->
        new.children.add(child.deepCopyInto(newParagraph = new, newParent = null))
    }
    // Paragraph type's startRichSpan carries a paragraph back-pointer that must point at
    // the freshly copied paragraph, not the one the type was created against.
    copiedType.startRichSpan.paragraph = new
    return new
}

@OptIn(ExperimentalRichTextApi::class)
private fun RichSpan.deepCopyInto(newParagraph: RichParagraph, newParent: RichSpan?): RichSpan {
    val new = RichSpan(
        key = key,
        paragraph = newParagraph,
        parent = newParent,
        text = text,
        textRange = textRange,
        spanStyle = spanStyle,
        richSpanStyle = richSpanStyle,
    )
    children.forEach { child ->
        new.children.add(child.deepCopyInto(newParagraph = newParagraph, newParent = new))
    }
    return new
}
