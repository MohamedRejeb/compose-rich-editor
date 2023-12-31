package com.mohamedrejeb.richeditor.gesture

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.ViewConfiguration
import com.mohamedrejeb.richeditor.utils.fastAny
import com.mohamedrejeb.richeditor.utils.fastForEach
import kotlinx.coroutines.coroutineScope

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange
): PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        change = awaitFirstDown()
    } while (change.uptimeMillis < minUptime)
    change
}

internal suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
    consumeDown: (Offset) -> Boolean,
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (!consumeDown(down.position)) return@awaitEachGesture
        down.consume()
        val longPressTimeout = onLongPress?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)
        var upOrCancel: PointerInputChange? = null
        try {
            // wait for first tap up or long press
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation()
            }
            upOrCancel?.consume()
        } catch (_: PointerEventTimeoutCancellationException) {
            onLongPress?.invoke(down.position)
            consumeUntilUp()
        }

        if (upOrCancel != null) {
            // tap was successful.
            if (onDoubleTap == null) {
                onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
            } else {
                // check for second tap
                val secondDown = awaitSecondDown(upOrCancel)

                if (secondDown == null) {
                    onTap?.invoke(upOrCancel.position) // no valid second tap started
                } else {
                    try {
                        // Might have a long second press as the second tap
                        withTimeout(longPressTimeout) {
                            val secondUp = waitForUpOrCancellation()
                            if (secondUp != null) {
                                secondUp.consume()
                                onDoubleTap(secondUp.position)
                            } else {
                                onTap?.invoke(upOrCancel.position)
                            }
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        // The first tap was valid, but the second tap is a long press.
                        // notify for the first tap
                        onTap?.invoke(upOrCancel.position)

                        // notify for the long press
                        onLongPress?.invoke(secondDown.position)
                        consumeUntilUp()
                    }
                }
            }
        }
    }
}