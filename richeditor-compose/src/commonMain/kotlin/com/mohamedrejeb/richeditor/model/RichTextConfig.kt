package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration

internal data class RichTextConfig(
    val linkColor: Color = Color.Blue,
    val linkTextDecoration: TextDecoration = TextDecoration.Underline,
    val codeColor: Color = Color.Unspecified,
    val codeBackgroundColor: Color = Color.Transparent,
    val codeStrokeColor: Color = Color.LightGray,
)