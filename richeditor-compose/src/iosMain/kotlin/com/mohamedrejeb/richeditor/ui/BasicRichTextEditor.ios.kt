package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.mohamedrejeb.richeditor.model.RichTextState

internal actual fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    textStyle: TextStyle,
    density: Density,
): Modifier = this