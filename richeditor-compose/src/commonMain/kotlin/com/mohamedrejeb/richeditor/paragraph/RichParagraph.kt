package com.mohamedrejeb.richeditor.paragraph

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.ui.test.getRichTextStyleTreeRepresentation

internal class RichParagraph(
    val key: Int = 0,
    val children: MutableList<RichSpan> = mutableListOf(),
    var paragraphStyle: ParagraphStyle = DefaultParagraphStyle,
    var type: ParagraphType = DefaultParagraph(),
) {

    @OptIn(ExperimentalRichTextApi::class)
    fun getRichSpanByTextIndex(
        paragraphIndex: Int,
        textIndex: Int,
        offset: Int = 0,
        ignoreCustomFiltering: Boolean = false,
    ): Pair<Int, RichSpan?> {
        var index = offset

        // If the paragraph is not the first one, we add 1 to the index which stands for the line break
        if (paragraphIndex > 0)
            index++

        // Set the startRichSpan paragraph and textRange to ensure that it has the correct and latest values
        type.startRichSpan.paragraph = this
        type.startRichSpan.textRange = TextRange(index, index + type.startText.length)

        // Add the startText length to the index
        index += type.startText.length

        // If the paragraph is empty, we add a RichSpan to avoid skipping the paragraph when searching
        if (children.isEmpty())
            children.add(
                RichSpan(
                    paragraph = this,
                    textRange = TextRange(index),
                )
            )

        // Check if the textIndex is in the startRichSpan current paragraph
        if (index > textIndex)
            return index to getFirstNonEmptyChild(offset = index)

        children.fastForEach { richSpan ->
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

    @OptIn(ExperimentalRichTextApi::class)
    fun getRichSpanListByTextRange(
        paragraphIndex: Int,
        searchTextRange: TextRange,
        offset: Int = 0,
    ): Pair<Int, List<RichSpan>> {
        var index = offset

        // If the paragraph is not the first one, we add 1 to the index which stands for the line break
        if (paragraphIndex > 0) index++

        // Set the startRichSpan paragraph and textRange to ensure that it has the correct and latest values
        type.startRichSpan.paragraph = this
        type.startRichSpan.textRange = TextRange(index, index + type.startText.length)

        // Add the startText length to the index
        index += type.startText.length

        // If the paragraph is empty, we add a RichSpan to avoid skipping the paragraph when searching
        if (children.isEmpty()) children.add(
            RichSpan(
                paragraph = this,
                textRange = TextRange(index),
            )
        )

        val richSpanList = mutableListOf<RichSpan>()
        children.fastForEach { richSpan ->
            val result = richSpan.getRichSpanListByTextRange(
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanList.addAll(result.second)
            index = result.first
        }
        return index to richSpanList
    }

    fun removeTextRange(
        textRange: TextRange,
        offset: Int,
    ): RichParagraph? {
        var index = offset
        val toRemoveIndices = mutableListOf<Int>()

        for (i in 0..children.lastIndex) {
            val child = children[i]
            val result = child.removeTextRange(textRange, index)
            val newRichSpan = result.second

            if (newRichSpan != null)
                children[i] = newRichSpan
            else
                toRemoveIndices.add(i)

            index = result.first
        }

        for (i in toRemoveIndices.lastIndex downTo 0) {
            children.removeAt(toRemoveIndices[i])
        }

        if (children.isEmpty())
            return null

        return this
    }

    fun getTextRange(): TextRange {
        var start = type.startRichSpan.textRange.min
        var end = 0

        if (type.startRichSpan.text.isNotEmpty())
            end += type.startRichSpan.text.length

        children.lastOrNull()?.let { richSpan ->
            end = richSpan.fullTextRange.end
        }

        return TextRange(start, end)
    }

    fun isEmpty(ignoreStartRichSpan: Boolean = true): Boolean {
        if (!ignoreStartRichSpan && !type.startRichSpan.isEmpty()) return false

        if (children.isEmpty()) return true
        children.fastForEach { richSpan ->
            if (!richSpan.isEmpty()) return false
        }
        return true
    }

    fun isNotEmpty(ignoreStartRichSpan: Boolean = true): Boolean = !isEmpty(ignoreStartRichSpan)

    fun isBlank(ignoreStartRichSpan: Boolean = true): Boolean {
        if (!ignoreStartRichSpan && !type.startRichSpan.isBlank()) return false

        if (children.isEmpty()) return true
        children.fastForEach { richSpan ->
            if (!richSpan.isBlank()) return false
        }
        return true
    }

    fun isNotBlank(ignoreStartRichSpan: Boolean = true): Boolean = !isBlank(ignoreStartRichSpan)

    fun getStartTextSpanStyle(): SpanStyle? {
        children.fastForEach { richSpan ->
            if (richSpan.text.isNotEmpty()) {
                return richSpan.spanStyle
            } else {
                val result = richSpan.getStartTextSpanStyle(SpanStyle())

                if (result != null)
                    return result
            }
        }

        val firstChild = children.firstOrNull()

        children.clear()

        if (firstChild != null) {
            firstChild.children.clear()

            children.add(firstChild)
        }

        return firstChild?.spanStyle
    }

    fun getFirstNonEmptyChild(offset: Int = -1): RichSpan? {
        children.fastForEach { richSpan ->
            if (richSpan.text.isNotEmpty()) {
                if (offset != -1)
                    richSpan.textRange = TextRange(offset, offset + richSpan.text.length)

                return richSpan
            } else {
                val result = richSpan.getFirstNonEmptyChild(offset)

                if (result != null)
                    return result
            }
        }

        val firstChild = children.firstOrNull()

        children.clear()

        if (firstChild != null) {
            firstChild.children.clear()

            if (offset != -1)
                firstChild.textRange = TextRange(offset, offset + firstChild.text.length)

            children.add(firstChild)
        }

        return firstChild
    }

    fun getLastNonEmptyChild(): RichSpan? {
        for (i in children.lastIndex downTo 0) {
            val richSpan = children[i]
            if (richSpan.text.isNotEmpty())
                return richSpan

            val result = richSpan.getLastNonEmptyChild()
            if (result != null)
                return result
        }

        return null
    }

    /**
     * Trim the rich paragraph
     */
    fun trim() {
        val isEmpty = trimStart()
        if (!isEmpty)
            trimEnd()
    }

    /**
     * Trim the start of the rich paragraph
     *
     * @return True if the rich paragraph is empty after trimming, false otherwise
     */
    fun trimStart(): Boolean {
        children.fastForEach { richSpan ->
            val isEmpty = richSpan.trimStart()

            if (!isEmpty)
                return false
        }

        return true
    }

    /**
     * Trim the end of the rich paragraph
     *
     * @return True if the rich paragraph is empty after trimming, false otherwise
     */
    fun trimEnd(): Boolean {
        children.fastForEachReversed { richSpan ->
            val isEmpty = richSpan.trimEnd()

            if (!isEmpty)
                return false
        }

        return true
    }

    /**
     * Update the paragraph of the children recursively
     *
     * @param newParagraph The new paragraph
     */
    fun updateChildrenParagraph(newParagraph: RichParagraph) {
        children.fastForEach { childRichSpan ->
            childRichSpan.paragraph = newParagraph
            childRichSpan.updateChildrenParagraph(newParagraph)
        }
    }

    fun removeEmptyChildren() {
        val toRemoveIndices = mutableListOf<Int>()

        children.fastForEachIndexed { index, richSpan ->
            if (richSpan.isEmpty())
                toRemoveIndices.add(index)
            else
                richSpan.removeEmptyChildren()
        }

        toRemoveIndices.fastForEachReversed {
            children.removeAt(it)
        }
    }

    fun copy(): RichParagraph {
        val newParagraph = RichParagraph(
            paragraphStyle = paragraphStyle,
            type = type.copy(),
        )
        children.fastForEach { childRichSpan ->
            val newRichSpan = childRichSpan.copy(newParagraph)
            newRichSpan.paragraph = newParagraph
            newParagraph.children.add(newRichSpan)
        }
        return newParagraph
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(" - Start Text: ${type.startRichSpan}")
        stringBuilder.appendLine()
        children.fastForEachIndexed { index, richTextStyle ->
            getRichTextStyleTreeRepresentation(stringBuilder, index, richTextStyle, " -")
        }
        return stringBuilder.toString()
    }

    companion object {
        val DefaultParagraphStyle = ParagraphStyle()
    }
}