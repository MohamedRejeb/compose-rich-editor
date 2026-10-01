package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedrejeb.richeditor.model.RichTextState

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    density: Density,
    layoutDirection: LayoutDirection,
): Modifier = this
    .onPointerEvent(PointerEventType.Press) {
        state.onSelectionGestureStart()
    }
    .onPointerEvent(PointerEventType.Release) {
        state.onSelectionGestureEnd()
    }