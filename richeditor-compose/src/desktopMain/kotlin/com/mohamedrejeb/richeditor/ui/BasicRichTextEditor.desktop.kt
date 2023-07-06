package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.mohamedrejeb.richeditor.model.RichTextState

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    textStyle: TextStyle,
    density: Density,
): Modifier = this
    .onPointerEvent(PointerEventType.Press) {
        val pressPosition = it.changes.firstOrNull()?.position ?: return@onPointerEvent

        adjustTextIndicatorOffset(
            pressPosition = pressPosition,
            state = state,
            contentPadding = contentPadding,
            textStyle = textStyle,
            density = density
        )
    }