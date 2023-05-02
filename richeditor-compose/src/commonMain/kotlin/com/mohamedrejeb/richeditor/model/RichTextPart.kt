package com.mohamedrejeb.richeditor.model

internal data class RichTextPart(
    val fromIndex: Int,
    val toIndex: Int,
    val styles: Set<RichTextStyle>,
)
