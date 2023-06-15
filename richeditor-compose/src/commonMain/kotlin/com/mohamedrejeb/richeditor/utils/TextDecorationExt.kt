package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.style.TextDecoration

operator fun TextDecoration.minus(decoration: TextDecoration): TextDecoration =
    if (this == decoration)
        TextDecoration.None
    else if (decoration in this)
        if (decoration.mask == TextDecoration.LineThrough.mask) TextDecoration.Underline
        else TextDecoration.LineThrough
    else
        TextDecoration.None

internal fun TextDecoration.getCommonDecoration(
    other: TextDecoration?,
    strict: Boolean = false
): TextDecoration? =
    if (other == null)
        null
    else if (strict)
        if (this != other) null
        else this
    else if (this in other)
        this
    else if (other in this)
        other
    else
        null