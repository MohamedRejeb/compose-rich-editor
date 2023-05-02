package com.mohamedrejeb.richeditor.model

data class RichTextPart(
    val fromIndex: Int,
    val toIndex: Int,
    val styles: Set<RichTextStyle>,
)
