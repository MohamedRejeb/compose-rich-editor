package com.mohamedrejeb.richeditor.sample.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.mohamedrejeb.richeditor.model.RichTextStyle

data class CustomStyle(
    val color: Color,
    val background: Color
): RichTextStyle {
    override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
        return spanStyle.copy(
            color = color,
            background = background
        )
    }
}