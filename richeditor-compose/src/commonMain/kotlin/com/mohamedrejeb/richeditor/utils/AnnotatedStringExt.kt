package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import kotlin.math.max
import kotlin.math.min

internal fun AnnotatedString.Builder.append(
    richSpanList: List<RichSpan>,
    startIndex: Int,
    text: String,
    selection: TextRange,
): Int {
    var index = startIndex
    richSpanList.forEach { richSpan ->
        index = append(
            richSpan = richSpan,
            startIndex = index,
            text = text,
            selection = selection,
        )
    }
    return index
}

internal fun AnnotatedString.Builder.append(
    richSpan: RichSpan,
    startIndex: Int,
    text: String,
    selection: TextRange,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle) {
        println("index: ${index}")
        println("richSpan.text: ${richSpan.text}")
        println("richSpan.text.length: ${richSpan.text.length}")
        val newText = text.substring(index, index + richSpan.text.length)
        richSpan.text = newText
        if (
            !selection.collapsed &&
            selection.min < index + richSpan.text.length &&
            selection.max > index
        ) {
            val beforeSelection =
                if (selection.min > index) richSpan.text.substring(0, selection.min - index)
                else ""
            val selectedText =
                richSpan.text.substring(max(0, selection.min - index), min(selection.max - index, richSpan.text.length))
            val afterSelection =
                if (selection.max - index < richSpan.text.length) richSpan.text.substring(selection.max - index)
                else ""

            println("beforeSelection: $beforeSelection")
            println("selectedText: $selectedText")
            println("afterSelection: $afterSelection")
            append(beforeSelection)
            withStyle(SpanStyle(background = Color.Transparent)) {
                append(selectedText)
            }
            append(afterSelection)
        } else {
            append(newText)
        }
        index += richSpan.text.length
        richSpan.children.forEach { richSpan ->
            index = append(
                richSpan = richSpan,
                startIndex = index,
                text = text,
                selection = selection,
            )
        }
    }
    return index
}