package com.mocoding.richeditor.common.model

import com.mocoding.richeditor.common.model.RichTextStyle

data class RichTextPart(
    val fromIndex: Int,
    val toIndex: Int,
    val styles: Set<RichTextStyle>,
)
