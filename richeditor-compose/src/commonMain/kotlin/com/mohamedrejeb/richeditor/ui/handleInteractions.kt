package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

public enum class InteractionType { PrimaryClick, SecondaryClick, Tap, DoubleTap }

/**
 * Provide a unified callback for listening for different types of interactions
 */
internal fun Modifier.handleInteractions(
    enabled: Boolean = true,
    onInteraction: ((InteractionType, Offset) -> Boolean)? = null
): Modifier = composed {
    this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (!enabled) continue

                    if (event.type == PointerEventType.Press) {
                        val eventChange = event.changes.first()
                        val position = eventChange.position

                        when (eventChange.type) {
                            PointerType.Touch -> {
                                onInteraction?.invoke(
                                    InteractionType.Tap,
                                    eventChange.position
                                )
                            }

                            PointerType.Mouse -> {
                                if (event.buttons.isPrimaryPressed) {
                                    val consumed =
                                        onInteraction?.invoke(
                                            InteractionType.PrimaryClick,
                                            position
                                        )
                                            ?: false
                                    if (consumed) {
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (event.buttons.isSecondaryPressed) {
                                    val consumed =
                                        onInteraction?.invoke(
                                            InteractionType.SecondaryClick,
                                            position
                                        )
                                            ?: false
                                    if (consumed) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}