package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    textStyle: TextStyle,
    density: Density,
    layoutDirection: LayoutDirection,
    scope: CoroutineScope,
): Modifier = this
    .onPointerEvent(PointerEventType.Press) {
        val pressPosition = it.changes.firstOrNull()?.position ?: return@onPointerEvent
        val topPadding = with(density) { contentPadding.calculateTopPadding().toPx() }
        val startPadding = with(density) { contentPadding.calculateStartPadding(layoutDirection).toPx() }

        scope.launch {
            adjustTextIndicatorOffset(
                pressPosition = pressPosition,
                state = state,
                topPadding = topPadding,
                startPadding = startPadding,
            )
        }
    }