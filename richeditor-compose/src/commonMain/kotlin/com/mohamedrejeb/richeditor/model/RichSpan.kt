package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.utils.isSpecifiedFieldsEquals

public class RichSpan(
    val key: Int = 0,
    val children: SnapshotStateList<RichSpan> = mutableStateListOf(),
    var paragraph: RichParagraph,
    var parent: RichSpan? = null,
    var text: String = "",
    var textRange: TextRange = TextRange(start = 0, end = 0),
    var spanStyle: SpanStyle = SpanStyle(),
) {
    val fullTextRange: TextRange get() {
        var textRange = this.textRange
        var lastChild: RichSpan? = this
        while (true) {
            lastChild = lastChild?.children?.lastOrNull()
            if (lastChild == null) break
            textRange = TextRange(
                start = textRange.min,
                end = lastChild.textRange.max
            )
        }
        return textRange
    }

    val fullSpanStyle: SpanStyle
        get() {
        var spanStyle = this.spanStyle
        var parent = this.parent

        while (parent != null) {
            spanStyle = parent.spanStyle.merge(spanStyle)
            parent = parent.parent
        }

        return spanStyle
    }

    val isFirstInParagraph: Boolean get() {
        var current: RichSpan
        var parent: RichSpan = this

        while (true) {
            current = parent
            parent = current.parent ?: break

            if (parent.children.firstOrNull() != current || parent.text.isNotEmpty()) return false
        }

        return paragraph.children.firstOrNull() == current
    }

    val isLastInParagraph: Boolean get() {
        var current: RichSpan
        var parent: RichSpan = this

        if (!isChildrenEmpty()) return false

        while (true) {
            current = parent
            parent = current.parent ?: break

            if (parent.children.lastOrNull() != current) return false
        }

        return paragraph.children.lastOrNull() == current
    }

    fun isEmpty(): Boolean = text.isEmpty() && isChildrenEmpty()

    private fun isChildrenEmpty(): Boolean =
        children.all { richSpan ->
            richSpan.text.isEmpty() && richSpan.isChildrenEmpty()
        }

    internal fun getFirstNonEmptyChild(): RichSpan? {
        children.forEach { richSpan ->
            if (richSpan.text.isNotEmpty()) return richSpan
            else {
                val result = richSpan.getFirstNonEmptyChild()
                if (result != null) return result
            }
        }
        return null
    }

    fun getRichSpanByTextIndex(
        textIndex: Int,
        offset: Int = 0,
    ): Pair<Int, RichSpan?> {
        var index = offset

        // Set start text range
        textRange = TextRange(start = index, end = index + text.length)
        index += this.text.length

        // Check if the text index is in the start text range
        if (
            (textIndex in textRange || (isFirstInParagraph && textIndex + 1 == textRange.min))
        ) {
            return if (text.isEmpty()) {
                index to paragraph.getFirstNonEmptyChild()
            } else {
                index to this
            }
        }

        // Check if the text index is in the children
        children.forEach { richSpan ->
            val result = richSpan.getRichSpanByTextIndex(
                textIndex = textIndex,
                offset = index,
            )
            if (result.second != null)
                return result
            else
                index = result.first
        }

        return index to null
    }

    fun getRichSpanListByTextRange(
        searchTextRange: TextRange,
        offset: Int = 0,
    ): Pair<Int, List<RichSpan>> {
        var index = offset
        val richSpanList = mutableListOf<RichSpan>()

        // Set start text range
        textRange = TextRange(start = index, end = index + text.length)
        index += this.text.length

        // Check if the text index is in the start text range
        if (searchTextRange.min < textRange.max && searchTextRange.max > textRange.min)
            richSpanList.add(this)

        // Check if the text index is in the children
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

    fun removeTextRange(textRange: TextRange): RichSpan? {
        if (textRange.min <= this.textRange.min && textRange.max >= fullTextRange.max) return null

        // Remove text from start
        if (textRange.min in this.textRange || (textRange.max - 1) in this.textRange) {
            val startFirstHalf = 0 until (textRange.min - this.textRange.min)
            val startSecondHalf = (textRange.max - this.textRange.min) until (this.textRange.max - this.textRange.min)
            val newStartText =
                (if (startFirstHalf.isEmpty()) "" else text.substring(startFirstHalf)) +
                (if (startSecondHalf.isEmpty()) "" else text.substring(startSecondHalf))
            this.textRange = TextRange(start = this.textRange.min, end = this.textRange.min + newStartText.length)
            text = newStartText
        }

        // Remove text from children
        for (i in children.lastIndex downTo 0) {
            val richSpan = children[i]
            val result = richSpan.removeTextRange(textRange)
            if (result == null) {
                children.removeAt(i)
            } else {
                children[i] = result
            }
        }

        // Check if the rich span style is empty
        if (text.isEmpty() && children.isEmpty()) {
            return null
        }

        return this
    }

    fun getClosestRichSpan(spanStyle: SpanStyle): RichSpan? {
        if (spanStyle.isSpecifiedFieldsEquals(this.fullSpanStyle, strict = true)) return this

        return parent?.getClosestRichSpan(spanStyle)
    }

    fun getMaxFontSize(): TextUnit {
        var height = if (spanStyle.fontSize.isSpecified) spanStyle.fontSize else 0.sp
        children.forEach { richSpan ->
            val childHeight = richSpan.getMaxFontSize()
            if (childHeight.isSpecified && childHeight > height) {
                height = childHeight
            }
        }
        return height
    }

    override fun toString(): String {
        return "richSpan(text='$text', textRange=$textRange, fullTextRange=$fullTextRange)"
    }

    companion object {
        val DefaultSpanStyle = SpanStyle(
            fontSize = 16.sp,
        )
    }
}