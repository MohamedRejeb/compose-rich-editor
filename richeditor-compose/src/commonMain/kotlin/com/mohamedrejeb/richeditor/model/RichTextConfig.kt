package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration

class RichTextConfig internal constructor(
    private val updateText: () -> Unit,
) {
    var linkColor: Color = Color.Blue
        set(value) {
            field = value
            updateText()
        }

    var linkTextDecoration: TextDecoration = TextDecoration.Underline
        set(value) {
            field = value
            updateText()
        }

    var codeSpanColor: Color = Color.Unspecified
        set(value) {
            field = value
            updateText()
        }

    var codeSpanBackgroundColor: Color = Color.Transparent
        set(value) {
            field = value
            updateText()
        }

    var codeSpanStrokeColor: Color = Color.LightGray
        set(value) {
            field = value
            updateText()
        }

    var listIndent: Int = DefaultListIndent
        set(value) {
            field = value
            updateText()
        }

    /**
     * Whether to preserve the style when the line is empty.
     * The line can be empty when the user deletes all the characters
     * or when the user presses `enter` to create a new line.
     *
     * Default is `true`.
     */
    var preserveStyleOnEmptyLine: Boolean = true
}

internal const val DefaultListIndent = 38