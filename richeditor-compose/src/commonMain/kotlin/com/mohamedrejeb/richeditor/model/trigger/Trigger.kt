package com.mohamedrejeb.richeditor.model.trigger

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextConfig

/**
 * Default characters that cancel an in-progress trigger query.
 *
 * When the user types any of these characters after a trigger character (e.g. `@`),
 * the active query is cleared and the text remains as plain characters.
 */
public val DefaultTriggerStopChars: Set<Char> = setOf(' ', '\n', '\t')

/**
 * Defines a single-character trigger that activates a token-insertion flow.
 *
 * A [Trigger] describes a feature like `@mentions`, `#hashtags`, or `/commands`:
 * when the user types the [char], the editor enters "query mode" and collects
 * characters until a [stopChars] character is typed or a token is committed
 * via [com.mohamedrejeb.richeditor.model.RichTextState.insertToken].
 *
 * Register triggers on [com.mohamedrejeb.richeditor.model.RichTextState.registerTrigger].
 *
 * @property id Stable identifier for this trigger. Used as the key for matching
 * [TriggerQuery.triggerId] and [Token.triggerId], and for round-trip
 * serialization to HTML/Markdown. Must be unique per [RichTextState].
 * @property char The single character that activates this trigger. Must be unique
 * across all registered triggers on the same state.
 * @property style Applied to committed [Token] spans of this trigger.
 * Receives the live [RichTextConfig] so colors can follow the editor theme.
 * @property drawStyle Optional custom draw logic (e.g. background pill) applied
 * beneath the token text. Follows the same contract as
 * [com.mohamedrejeb.richeditor.model.RichSpanStyle.drawCustomStyle].
 * @property stopChars Characters that cancel an in-progress query. Defaults to
 * whitespace ([DefaultTriggerStopChars]).
 * @property requireWordBoundary When true, the trigger only activates when the
 * preceding character is whitespace, a paragraph boundary, or nothing. This
 * prevents activation inside words (e.g. `foo@bar` does not trigger a mention).
 * Defaults to true.
 * @property maxQueryLength Maximum number of characters permitted after the
 * trigger character before the query is abandoned. Defaults to 50.
 */
@ExperimentalRichTextApi
public class Trigger(
    public val id: String,
    public val char: Char,
    public val style: (RichTextConfig) -> SpanStyle = { SpanStyle(color = it.linkColor) },
    public val drawStyle: (DrawScope.(TextLayoutResult, TextRange, RichTextConfig, Float, Float) -> Unit)? = null,
    public val stopChars: Set<Char> = DefaultTriggerStopChars,
    public val requireWordBoundary: Boolean = true,
    public val maxQueryLength: Int = 50,
) {
    init {
        require(id.isNotEmpty()) { "Trigger id must not be empty" }
        require(':' !in id) { "Trigger id must not contain ':' (used as Markdown round-trip separator)" }
        require(char !in stopChars) { "Trigger char '$char' cannot be one of its stopChars" }
        require(maxQueryLength > 0) { "maxQueryLength must be positive, got $maxQueryLength" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Trigger) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Trigger(id='$id', char='$char')"
}
