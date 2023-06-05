package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.model.RichSpanStyle

internal fun AnnotatedString.Builder.append(
    richSpanStyleList: List<RichSpanStyle>,
) {
    richSpanStyleList.forEach { richSpanStyle ->
        append(richSpanStyle)
    }
}

internal fun AnnotatedString.Builder.append(
    richSpanStyle: RichSpanStyle,
) {
    withStyle(richSpanStyle.spanStyle) {
        append(richSpanStyle.text)
        richSpanStyle.children.forEach { richSpanStyle ->
            append(richSpanStyle)
        }
    }
}