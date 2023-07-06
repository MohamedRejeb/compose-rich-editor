package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

public class RichParagraph(
    val key: Int = 0,
    val children: SnapshotStateList<RichSpan> = mutableStateListOf(),
    var paragraphStyle: ParagraphStyle = DefaultParagraphStyle,
) {
    fun getRichSpanByTextIndex(
        paragraphIndex: Int,
        textIndex: Int,
        offset: Int = 0,
    ): Pair<Int, RichSpan?> {
        var index = offset

        // If the paragraph is not the first one, we add 1 to the index which stands for the line break
        if (paragraphIndex > 0) index++

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
        paragraphIndex: Int,
        searchTextRange: TextRange,
        offset: Int = 0,
    ): Pair<Int, List<RichSpan>> {
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
            println(result)
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

    companion object {
        val DefaultParagraphStyle = ParagraphStyle(
            textAlign = TextAlign.Left,
            lineBreak = LineBreak.Heading,
        )
    }
}