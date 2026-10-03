package com.mohamedrejeb.richeditor.model

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Blocks the test for [millis] on every target. The IME follow-up windows are measured on
 * the monotonic clock, so a test that has to outlast one needs real time to pass, and
 * `Thread.sleep` only exists on the JVM.
 */
internal fun sleepMillis(millis: Long) {
    val mark = TimeSource.Monotonic.markNow()
    while (mark.elapsedNow() < millis.milliseconds) {
        // Spin: the common test source set has no blocking sleep.
    }
}
