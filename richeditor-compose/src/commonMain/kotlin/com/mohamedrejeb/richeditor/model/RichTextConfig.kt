package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration

class RichTextConfig internal constructor(
    private val onConfigChanged: () -> Unit,
) {
    var linkColor: Color = Color.Blue
        set(value) {
            field = value
            onConfigChanged()
        }

    var linkTextDecoration: TextDecoration = TextDecoration.Underline
        set(value) {
            field = value
            onConfigChanged()
        }

    var codeSpanColor: Color = Color.Unspecified
        set(value) {
            field = value
            onConfigChanged()
        }

    var codeSpanBackgroundColor: Color = Color.Transparent
        set(value) {
            field = value
            onConfigChanged()
        }

    var codeSpanStrokeColor: Color = Color.LightGray
        set(value) {
            field = value
            onConfigChanged()
        }

    var listIndent: Int = DefaultListIndent
        set(value) {
            field = value
            onConfigChanged()
        }
}

internal const val DefaultListIndent = 38