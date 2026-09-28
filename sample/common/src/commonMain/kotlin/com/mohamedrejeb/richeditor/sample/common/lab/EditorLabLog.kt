package com.mohamedrejeb.richeditor.sample.common.lab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.time.TimeMark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

private const val MAX_LOG_ENTRIES = 60
private val LogHeight = 200.dp

internal data class LabLogEntry(
    val elapsedMillis: Long,
    val message: String,
)

/**
 * Records changes of the state the manual checks ask about, newest first. The time is when
 * the change was observed on the main thread, not when it was written: only a gap of more
 * than a frame orders two entries, and values overwritten in between are never seen.
 */
internal class LabLog(private val start: TimeMark) {
    var entries: List<LabLogEntry> by mutableStateOf(emptyList())
        private set

    fun add(message: String) {
        val entry = LabLogEntry(
            elapsedMillis = start.elapsedNow().inWholeMilliseconds,
            message = message,
        )
        entries = (listOf(entry) + entries).take(MAX_LOG_ENTRIES)
    }

    fun clear() {
        entries = emptyList()
    }

    /**
     * Collects until cancelled. Each flow drops the value it reads at subscription, so
     * whatever was loaded just before this call is never logged.
     */
    suspend fun record(state: RichTextState, pageScroll: ScrollState) {
        merge(
            changesOf("selection") { state.selection.describe() },
            changesOf("composition") { state.composition?.describe() ?: "none" },
            changesOf("bold") { state.isBold().toString() },
            changesOf("list") { state.isList.toString() },
            restingValuesOf("editor scroll", state.scrollState),
            restingValuesOf("page scroll", pageScroll),
            changesOf("length") { state.annotatedString.text.length.toString() },
        ).collect(::add)
    }
}

@Composable
internal fun LabLogPanel(
    log: LabLog,
    modifier: Modifier = Modifier,
) {
    LabPanel(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Event log (newest first)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = log::clear,
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                Text("Clear")
            }
        }

        // No item keys: with keys the list would stay anchored to the row that was on top
        // and every new entry would land above the viewport.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(LogHeight),
        ) {
            items(items = log.entries) { entry ->
                Text(
                    text = "${entry.elapsedMillis} ms  ${entry.message}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun TextRange.describe(): String =
    if (collapsed) "caret $start" else "$start..$end"

internal fun RichTextState.isBold(): Boolean =
    currentSpanStyle.fontWeight == FontWeight.Bold

/**
 * A scroll changes every frame, which would push everything else out of the log: only the
 * value it comes to rest at is recorded.
 */
private fun restingValuesOf(name: String, scrollState: ScrollState): Flow<String> =
    snapshotFlow { scrollState.isScrollInProgress }
        .drop(1)
        .filter { inProgress -> !inProgress }
        .map { "$name = ${scrollState.value}" }

private fun changesOf(name: String, read: () -> String): Flow<String> =
    snapshotFlow(read)
        .drop(1)
        .map { value -> "$name = $value" }
