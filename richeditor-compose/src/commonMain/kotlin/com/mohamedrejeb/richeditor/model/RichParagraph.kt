package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichParagraph.Type.Companion.startText
import com.mohamedrejeb.richeditor.ui.test.getRichTextStyleTreeRepresentation
import com.mohamedrejeb.richeditor.utils.fastForEach
import com.mohamedrejeb.richeditor.utils.fastForEachIndexed

internal class RichParagraph(
    val key: Int = 0,
    val children: MutableList<RichSpan> = mutableListOf(),
    var paragraphStyle: ParagraphStyle = DefaultParagraphStyle,
    var type: Type = Type.Default,
) {
    interface Type {
        val style: ParagraphStyle get() = ParagraphStyle()
        val startRichSpan: RichSpan get() = RichSpan(paragraph = RichParagraph(type = this))

        val nextParagraphType: Type get() = Default

        fun copy(): Type = this

        object Default : Type

        class UnorderedList : Type {
            override val style: ParagraphStyle = ParagraphStyle(
                textIndent = TextIndent(firstLine = 20.sp, restLine = 38.sp),
                lineHeight = 20.sp,
            )
            override val startRichSpan: RichSpan = RichSpan(
                paragraph = RichParagraph(type = this),
                text = "• ",
            )
            override val nextParagraphType: Type get() = UnorderedList()

            override fun copy(): Type = UnorderedList()
        }

        data class OrderedList(
            val number: Int,
        ) : Type {
            override val style: ParagraphStyle = ParagraphStyle(
                textIndent = TextIndent(firstLine = 20.sp, restLine = 42.sp),
                lineHeight = 20.sp,
            )
            override val startRichSpan: RichSpan = RichSpan(
                paragraph = RichParagraph(type = this),
                text = "$number. ",
            )
            override val nextParagraphType: Type get() = OrderedList(number + 1)

            override fun copy(): Type = OrderedList(number)
        }

        companion object {
            val Type.startText : String get() = startRichSpan.text
        }
    }

    fun getRichSpanByTextIndex(
        paragraphIndex: Int,
        textIndex: Int,
        offset: Int = 0,
        ignoreCustomFiltering: Boolean = false,
    ): Pair<Int, RichSpan?> {
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

        // Check if the textIndex is in the startRichSpan current paragraph
        if (index > textIndex) return index to getFirstNonEmptyChild(offset = index)

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
            if (newRichSpan != null) {
                children[i] = newRichSpan
            } else {
                toRemoveIndices.add(i)
            }
            index = result.first
        }
        for (i in toRemoveIndices.lastIndex downTo 0) {
            children.removeAt(toRemoveIndices[i])
        }

        if (children.isEmpty()) return null
        return this
    }

    fun isEmpty(): Boolean {
        if (children.isEmpty()) return true
        children.fastForEach { richSpan ->
            if (!richSpan.isEmpty()) return false
        }
        return true
    }

    fun getFirstNonEmptyChild(offset: Int? = null): RichSpan? {
        children.fastForEach { richSpan ->
            if (richSpan.text.isNotEmpty()) {
                if (offset != null)
                    richSpan.textRange = TextRange(offset, offset + richSpan.text.length)
                return richSpan
            }
            else {
                val result = richSpan.getFirstNonEmptyChild(offset)
                if (result != null) return result
            }
        }
        val firstChild = children.firstOrNull()
        children.clear()
        if (firstChild != null) {
            firstChild.children.clear()
            if (offset != null)
                firstChild.textRange = TextRange(offset, offset + firstChild.text.length)
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
        children.fastForEach { childRichSpan ->
            childRichSpan.paragraph = newParagraph
            childRichSpan.updateChildrenParagraph(newParagraph)
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
        val DefaultParagraphStyle = ParagraphStyle(
            textAlign = TextAlign.Left,
            lineBreak = LineBreak.Paragraph,
        )
    }
}