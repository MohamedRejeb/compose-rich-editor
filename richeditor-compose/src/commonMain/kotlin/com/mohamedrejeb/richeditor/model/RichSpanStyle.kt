package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

interface RichSpanStyle {
    var spanStyle: SpanStyle

    val data: MutableMap<String, String>

    /**
     * If true, the user can add new text in the edges of the span,
     * For example, if the span is "Hello" and the user adds "World" in the end, the span will be "Hello World"
     * If false, the user can't add new text in the edges of the span,
     * For example, if the span is a "Hello" link and the user adds "World" in the end, the "World" will be added in a separate a span,
     *
     * Default value is true
     */
    val acceptNewTextInTheEdges: Boolean
        get() = true

    class Link(
        override var spanStyle: SpanStyle = SpanStyle(
            color = Color.Blue,
            textDecoration = TextDecoration.Underline,
        ),
        val url: String,
    ) : RichSpanStyle {
        override val data: MutableMap<String, String> = mutableMapOf(
            "url" to url,
        )
        override val acceptNewTextInTheEdges: Boolean = false
    }

    class Default : RichSpanStyle {
        override var spanStyle: SpanStyle = SpanStyle()
        override val data: MutableMap<String, String> = mutableMapOf()
    }

    companion object {
        internal val DefaultSpanStyle = SpanStyle(
            fontSize = 16.sp,
        )

        fun fromSpanStyle(spanStyle: SpanStyle): RichSpanStyle =
            Default().apply {
                this.spanStyle = spanStyle
            }
    }
}