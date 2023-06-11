package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.model.RichSpanStyle

internal fun AnnotatedString.Builder.append(
    richSpanStyleList: List<RichSpanStyle>,
    startIndex: Int,
    text: String,
): Int {
    var index = startIndex
    richSpanStyleList.forEach { richSpanStyle ->
        index = append(
            richSpanStyle = richSpanStyle,
            startIndex = index,
            text = text
        )
    }
    return index
}

internal fun AnnotatedString.Builder.append(
    richSpanStyle: RichSpanStyle,
    startIndex: Int,
    text: String,
): Int {
    var index = startIndex
    withStyle(richSpanStyle.spanStyle) {
        val newText = text.substring(index, index + richSpanStyle.text.length)
        richSpanStyle.text = newText
        append(newText)
        println("text: $newText")
        println("start: $index")
        println("end: ${index + richSpanStyle.text.length}")
        index += richSpanStyle.text.length

        richSpanStyle.children.forEach { richSpanStyle ->
            index = append(
                richSpanStyle = richSpanStyle,
                startIndex = index,
                text = text
            )
        }
    }
    return index
}