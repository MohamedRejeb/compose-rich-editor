package com.mohamedrejeb.richeditor.model.trigger

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Snapshot of an in-progress trigger query.
 *
 * When the user types a registered trigger character followed by zero or more
 * query characters, [com.mohamedrejeb.richeditor.model.RichTextState.activeTriggerQuery]
 * exposes this value. Consumers (typically a suggestions popup) observe it to:
 *
 *  - identify which trigger is active ([triggerId]);
 *  - fetch matching suggestions for the current [query];
 *  - anchor UI at [caretRect];
 *  - know which text range will be replaced on commit ([range]).
 *
 * @property triggerId Id of the [Trigger] that produced this query.
 * @property query Characters typed after the trigger character, excluding the
 * trigger character itself. Empty when the user just typed the trigger char.
 * @property range Raw-text range spanning the trigger character and the query.
 * On commit, this entire range is replaced with the committed token text.
 * @property caretRect The cursor's bounding rect at query time, in the editor's
 * coordinate space. Null if no text layout is available yet. Consumers use
 * this to position a popup.
 */
@ExperimentalRichTextApi
public data class TriggerQuery(
    public val triggerId: String,
    public val query: String,
    public val range: TextRange,
    public val caretRect: Rect?,
)
