package com.mohamedrejeb.richeditor.compose.spellcheck.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

internal fun <T> Flow<T>.debounceUntilQuiescent(duration: Duration): Flow<T> = channelFlow {
    var job: Job? = null
    collect { value ->
        job?.cancel()
        job = launch {
            delay(duration)
            send(value)
            job = null
        }
    }
}