package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.utils.isSpecifiedFieldsEquals

public class RichSpanStyle(
    val key: Int = 0,
    val children: SnapshotStateList<RichSpanStyle> = mutableStateListOf(),
    val paragraph: RichParagraphStyle,
    val parent: RichSpanStyle? = null,
    var text: String = "",
    var textRange: TextRange = TextRange(start = 0, end = 0),
    var fullTextRange: TextRange = TextRange(start = 0, end = 0),
    var spanStyle: SpanStyle = SpanStyle(),
) {
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

    fun getSpanStyleByTextIndex(
        textIndex: Int,
        offset: Int = 0,
    ): Pair<Int, RichSpanStyle?> {
        var index = offset

        // Set start text range
        textRange = TextRange(start = index, end = index + text.length)
        index += this.text.length

        // Check if the text index is in the start text range
        if (textIndex in textRange)
            return index to this

        // Check if the text index is in the children
        children.forEach { richSpanStyle ->
            val result = richSpanStyle.getSpanStyleByTextIndex(
                textIndex = textIndex,
                offset = index,
            )
            if (result.second != null)
                return result
            else
                index = result.first
        }

        fullTextRange = TextRange(start = textRange.min, end = index)
        return index to null
    }

    fun removeTextRange(textRange: TextRange): RichSpanStyle? {
        if (textRange.min <= this.textRange.min && textRange.max >= fullTextRange.max) return null

        // Remove text from start
        if (textRange.min in this.textRange || textRange.max in this.textRange) {
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
            val richSpanStyle = children[i]
            val result = richSpanStyle.removeTextRange(textRange)
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

    fun getClosestSpanStyle(spanStyle: SpanStyle): RichSpanStyle? {
        println(spanStyle)
        println(this.spanStyle)
        if (spanStyle.isSpecifiedFieldsEquals(this.fullSpanStyle)) return this

        return parent?.getClosestSpanStyle(spanStyle)
    }

    override fun toString(): String {
        return "RichSpanStyle(text='$text')"
    }
}