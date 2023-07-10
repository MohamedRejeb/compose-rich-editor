package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

public class RichParagraph(
    val key: Int = 0,
    val children: MutableList<RichSpan> = mutableListOf(),
    var paragraphStyle: ParagraphStyle = DefaultParagraphStyle,
) {
    fun getRichSpanByTextIndex(
        paragraphIndex: Int,
        textIndex: Int,
        offset: Int = 0,
        ignoreCustomFiltering: Boolean = false,
    ): Pair<Int, RichSpan?> {
        // If the paragraph is empty, we add a RichSpan to avoid skipping the paragraph when searching
        if (children.isEmpty()) children.add(RichSpan(paragraph = this))

        var index = offset

        // If the paragraph is not the first one, we add 1 to the index which stands for the line break
        if (paragraphIndex > 0) index++

        children.forEach { richSpan ->
            val result = richSpan.getRichSpanByTextIndex(
                textIndex = textIndex,
                offset = index,
                ignoreCustomFiltering = ignoreCustomFiltering,
            )
            if (result.second != null)
                return result
            else
                index = result.first
        }
        return index to null
    }

    fun getRichSpanListByTextRange(
        paragraphIndex: Int,
        searchTextRange: TextRange,
        offset: Int = 0,
    ): Pair<Int, List<RichSpan>> {
        // If the paragraph is empty, we add a RichSpan to avoid skipping the paragraph when searching
        if (children.isEmpty()) children.add(RichSpan(paragraph = this))

        var index = offset

        // If the paragraph is not the first one, we add 1 to the index which stands for the line break
        if (paragraphIndex > 0) index++

        val richSpanList = mutableListOf<RichSpan>()
        children.forEach { richSpan ->
            val result = richSpan.getRichSpanListByTextRange(
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanList.addAll(result.second)
            index = result.first
        }
        return index to richSpanList
    }

    fun removeTextRange(textRange: TextRange): RichParagraph? {
        for (i in children.lastIndex downTo 0) {
            val child = children[i]
            val result = child.removeTextRange(textRange)
            if (result != null) {
                children[i] = result
            } else {
                children.removeAt(i)
            }
        }

        if (children.isEmpty()) return null
        return this
    }

    fun getMaxFontSize(): TextUnit {
        var height = 0.sp
        children.forEach { richSpan ->
            val childHeight = richSpan.getMaxFontSize()
            if (childHeight.isSpecified && childHeight > height) {
                height = childHeight
            }
        }
        return height
    }

    fun isEmpty(): Boolean {
        if (children.isEmpty()) return true
        children.forEach { richSpan ->
            if (!richSpan.isEmpty()) return false
        }
        return true
    }

    fun getFirstNonEmptyChild(): RichSpan? {
        children.forEach { richSpan ->
            if (richSpan.text.isNotEmpty()) return richSpan
            else {
                val result = richSpan.getFirstNonEmptyChild()
                if (result != null) return result
            }
        }
        val firstChild = children.firstOrNull()
        children.clear()
        if (firstChild != null) {
            firstChild.children.clear()
            children.add(firstChild)
        }
        return firstChild
    }

    /**
     * Update the paragraph of the children recursively
     *
     * @param newParagraph The new paragraph
     */
    fun updateChildrenParagraph(newParagraph: RichParagraph) {
        children.forEach { childRichSpan ->
            childRichSpan.paragraph = newParagraph
            childRichSpan.updateChildrenParagraph(newParagraph)
        }
    }

    fun copy(): RichParagraph {
        val newParagraph = RichParagraph(
            paragraphStyle = paragraphStyle,
        )
        children.forEach { childRichSpan ->
            val newRichSpan = childRichSpan.copy(newParagraph)
            newRichSpan.paragraph = newParagraph
            newParagraph.children.add(newRichSpan)
        }
        return newParagraph
    }

    companion object {
        val DefaultParagraphStyle = ParagraphStyle(
            textAlign = TextAlign.Left,
            lineBreak = LineBreak.Heading,
        )
    }
}