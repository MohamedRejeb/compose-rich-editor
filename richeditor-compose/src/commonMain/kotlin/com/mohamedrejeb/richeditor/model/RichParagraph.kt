package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.LineBreak

public class RichParagraph(
    val key: Int = 0,
    val children: SnapshotStateList<RichSpan> = mutableStateListOf(),
    var paragraphStyle: ParagraphStyle = ParagraphStyle(
        lineBreak = LineBreak.Heading,
    ),
) {
    fun getRichSpanByTextIndex(
        textIndex: Int,
        offset: Int = 0,
        hasNextParagraph: Boolean = false,
    ): Pair<Int, RichSpan?> {
        var index = offset
        children.forEach { richSpan ->
            val result = richSpan.getRichSpanByTextIndex(
                textIndex = textIndex,
                offset = index,
            )
            if (result.second != null) {
                return if (
                    result.second?.isLastInParagraph == true &&
                    result.second?.textRange?.max == textIndex + 1 &&
                    hasNextParagraph
                )
                    result.first to null
                else
                    result
            }
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
}