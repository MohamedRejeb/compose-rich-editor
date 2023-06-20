package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.model.RichSpan

internal fun AnnotatedString.Builder.append(
    richSpanList: List<RichSpan>,
    startIndex: Int,
    text: String,
): Int {
    var index = startIndex
    richSpanList.forEach { richSpan ->
        index = append(
            richSpan = richSpan,
            startIndex = index,
            text = text
        )
    }
    return index
}

internal fun AnnotatedString.Builder.append(
    richSpan: RichSpan,
    startIndex: Int,
    text: String,
): Int {
    var index = startIndex
    withStyle(richSpan.spanStyle) {
        val newText = text.substring(index, index + richSpan.text.length)
        richSpan.text = newText
        append(newText)
        index += richSpan.text.length
        richSpan.children.forEach { richSpan ->
            index = append(
                richSpan = richSpan,
                startIndex = index,
                text = text
            )
        }
    }
    return index
}