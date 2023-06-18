package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange

public class RichParagraphStyle(
    val key: Int = 0,
    val children: SnapshotStateList<RichSpanStyle> = mutableStateListOf(),
    var paragraphStyle: ParagraphStyle = ParagraphStyle(),
) {
    fun getSpanStyle(key: Int): RichSpanStyle? {
        return children.firstOrNull { it.key == key }
    }

    fun getSpanStyleByTextIndex(
        textIndex: Int,
        offset: Int = 0,
    ): Pair<Int, RichSpanStyle?> {
        var index = offset
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
        return index to null
    }

    fun getSpanStyleListByTextRange(
        searchTextRange: TextRange,
        offset: Int = 0,
    ): Pair<Int, List<RichSpanStyle>> {
        var index = offset
        val richSpanStyleList = mutableListOf<RichSpanStyle>()
        children.forEach { richSpanStyle ->
            val result = richSpanStyle.getSpanStyleListByTextRange(
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanStyleList.addAll(result.second)
            index = result.first
        }
        return index to richSpanStyleList
    }

    fun removeTextRange(textRange: TextRange): RichParagraphStyle? {
        println("removeTextRange: $textRange")
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