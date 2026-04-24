package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.history.CommitTrigger
import com.mohamedrejeb.richeditor.model.history.RichTextHistory
import com.mohamedrejeb.richeditor.model.history.RichTextHistoryHost
import com.mohamedrejeb.richeditor.model.history.RichTextSnapshot
import com.mohamedrejeb.richeditor.model.history.deepCopy
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.model.trigger.TriggerQuery
import com.mohamedrejeb.richeditor.model.trigger.detectActiveTrigger
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.*
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
import com.mohamedrejeb.richeditor.parser.markdown.RichTextStateMarkdownParser
import com.mohamedrejeb.richeditor.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.reflect.KClass

private val RichTextStateHistoryClockStart = kotlin.time.TimeSource.Monotonic.markNow()

@Composable
public fun rememberRichTextState(
    historyLimit: Int = 100,
    coalesceWindowMs: Long = 500L,
): RichTextState {
    return rememberSaveable(saver = RichTextState.Saver) {
        RichTextState(historyLimit = historyLimit, coalesceWindowMs = coalesceWindowMs)
    }
}

@OptIn(ExperimentalRichTextApi::class)
public class RichTextState internal constructor(
    initialRichParagraphList: List<RichParagraph>,
    historyLimit: Int = 100,
    coalesceWindowMs: Long = 500L,
) {
    public constructor(
        historyLimit: Int = 100,
        coalesceWindowMs: Long = 500L,
    ) : this(listOf(RichParagraph()), historyLimit, coalesceWindowMs)

    /**
     * Undo/redo history for this editor. Snapshots the full rich-text tree
     * (paragraphs, spans, list prefixes, selection, staged styles) and overrides
     * `BasicTextField`'s native undo so rich content never desyncs from plain text.
     */
    public val history: RichTextHistory = RichTextHistory(
        host = HistoryHostImpl(),
        limit = historyLimit,
        coalesceWindowMs = coalesceWindowMs,
    )

    /**
     * When true, mutating entry points skip history capture. Used while restoring a
     * snapshot (so the restore itself is not recorded) and as a public opt-out for
     * internal replays.
     */
    private var suppressHistoryRecording: Boolean = false

    /**
     * Re-entrancy guard: prevents inner public-API calls from recording their own
     * history entries when they happen inside an outer public call that is already
     * recording.
     */
    private var historyRecordingDepth: Int = 0

    /**
     * When true, [onPreviewKeyEvent] does not intercept `Ctrl/Cmd+Z/Y`. Controlled
     * by `BasicRichTextEditor`'s `undoBehavior` parameter.
     */
    internal var suppressUndoShortcuts: Boolean = false

    internal val richParagraphList = mutableStateListOf<RichParagraph>()
    internal var visualTransformation: VisualTransformation by mutableStateOf(VisualTransformation.None)
    internal var textFieldValue by mutableStateOf(TextFieldValue())
        private set

    internal val inlineContentMap = mutableStateMapOf<String, InlineTextContent>()
    internal val usedInlineContentMapKeys = mutableSetOf<String>()

    /**
     * Pending HTML from clipboard, set by platform clipboard managers during [getClipEntry].
     * Consumed by [onTextFieldValueChange] on the next text addition (paste).
     */
    internal var pendingClipboardHtml: String? = null

    /**
     * The last non-collapsed selection. Updated whenever the selection changes from a
     * non-collapsed range to a different value. Used by clipboard managers on platforms
     * (e.g. Android) where the selection collapses before [setClipEntry] is called.
     */
    internal var lastNonCollapsedSelection: TextRange = TextRange.Zero

    /**
     * Returns the best available selection for copy operations.
     * Prefers the current selection if it's non-collapsed, otherwise falls back
     * to [lastNonCollapsedSelection]. Returns null if neither is usable.
     */
    internal val copySelection: TextRange?
        get() {
            if (!selection.collapsed) return selection
            if (!lastNonCollapsedSelection.collapsed) return lastNonCollapsedSelection
            return null
        }

    /**
     * Whether the text field is currently focused.
     * Updated by [BasicRichTextEditor] via [onFocusChanged].
     */
    internal var isFocused: Boolean = false

    /**
     * The annotated string representing the rich text.
     */
    public var annotatedString: AnnotatedString by mutableStateOf(AnnotatedString(text = ""))
        private set

    /**
     * The selection of the rich text.
     */
    public var selection: TextRange
        get() = textFieldValue.selection
        set(value) {
            if (value.min >= 0 && value.max <= textFieldValue.text.length) {
                val newTextFieldValue = textFieldValue.copy(selection = value)
                updateTextFieldValue(newTextFieldValue)
            }
        }

    public val composition: TextRange? get() = textFieldValue.composition

    // --- Triggers (mentions, hashtags, commands, ...) ---
    //
    // Triggers live on RichTextState (not on editor composables) so tokens
    // render correctly in read-only views and so programmatic insertion
    // works without a mounted editor.

    private val _triggers: MutableList<Trigger> = mutableStateListOf()

    /**
     * Registered triggers on this state. Use [registerTrigger] and [unregisterTrigger] to mutate.
     */
    @ExperimentalRichTextApi
    public val triggers: List<Trigger> get() = _triggers

    private var _activeTriggerQuery: TriggerQuery? by mutableStateOf(null)

    /**
     * Snapshot of the in-progress trigger query, or `null` if no trigger is active.
     *
     * Observed by suggestion UI composables (e.g. `TriggerSuggestions`) to know when to render
     * a popup, what trigger is active, what query text to match against, and where to anchor.
     *
     * Updated automatically after every text edit and selection change.
     */
    @ExperimentalRichTextApi
    public val activeTriggerQuery: TriggerQuery? get() = _activeTriggerQuery

    /**
     * Text range that was dismissed by [cancelActiveTrigger]. Used to suppress
     * immediate re-activation if the cursor is still within the dismissed range.
     */
    private var suppressedTriggerRange: TextRange? = null

    /**
     * Window-space position of the hosted [BasicTextField]. Captured via
     * [androidx.compose.ui.layout.onPlaced] on the editor's modifier
     * chain; used by suggestion popups to anchor relative to the caret in window coords.
     */
    internal var textFieldWindowPosition: Offset = Offset.Zero

    /**
     * Key handler installed by the currently-visible trigger suggestions popup.
     * Consulted by [onPreviewKeyEvent] before the editor's default behavior, so
     * ↑/↓/Enter/Esc can drive the popup instead of reaching the text field.
     */
    internal var triggerKeyHandler: ((KeyEvent) -> Boolean)? = null

    /**
     * Register a [trigger] on this state. Triggers that share a character with an already
     * registered trigger are rejected with [IllegalArgumentException] to keep detection deterministic.
     *
     * Registering a trigger with a previously used [Trigger.id] replaces the previous registration.
     */
    @ExperimentalRichTextApi
    public fun registerTrigger(trigger: Trigger) {
        val charCollision = _triggers.firstOrNull { it.char == trigger.char && it.id != trigger.id }
        require(charCollision == null) {
            "Trigger char '${trigger.char}' is already registered by trigger id='${charCollision?.id}'"
        }
        val existingIndex = _triggers.indexOfFirst { it.id == trigger.id }
        if (existingIndex >= 0) {
            // Remove + add instead of set() - Trigger's equals compares by id, so
            // list.set(i, trigger) may be a no-op under equality-tracking list
            // implementations even though the values differ.
            _triggers.removeAt(existingIndex)
            _triggers.add(existingIndex, trigger)
        } else {
            _triggers.add(trigger)
        }
    }

    /**
     * Remove the trigger registered under [id]. No-op if not registered.
     * If the currently active query belongs to [id], it is cleared.
     */
    @ExperimentalRichTextApi
    public fun unregisterTrigger(id: String) {
        _triggers.removeAll { it.id == id }
        if (_activeTriggerQuery?.triggerId == id) {
            _activeTriggerQuery = null
        }
    }

    /**
     * Internal lookup from trigger id to its [Trigger] definition, used by the render
     * path to resolve token styling. Returns `null` for unknown ids (e.g. a [RichSpanStyle.Token]
     * parsed from HTML whose trigger isn't registered on the receiving state).
     */
    internal fun findTrigger(id: String): Trigger? =
        _triggers.firstOrNull { it.id == id }

    /**
     * Commit a token for the currently active trigger query.
     *
     * Replaces the active query's raw-text range (trigger char + query chars) with an
     * atomic [RichSpanStyle.Token] span followed by a trailing space, then clears the query.
     *
     * @param triggerId Must match [activeTriggerQuery]'s triggerId.
     * @param id Stable identity for the referenced entity (e.g. "u123").
     * @param label Display text of the token. Must start with the trigger's character.
     * @throws IllegalStateException if no matching query is active.
     * @throws IllegalArgumentException if [label] does not start with the trigger character.
     */
    @ExperimentalRichTextApi
    public fun insertToken(triggerId: String, id: String, label: String): Unit = recordHistory(CommitTrigger.Structural) {
        val query = _activeTriggerQuery
        checkNotNull(query) { "No active trigger query to commit" }
        check(query.triggerId == triggerId) {
            "Active query is for '${query.triggerId}', not '$triggerId'"
        }
        val trigger = findTrigger(triggerId)
        checkNotNull(trigger) { "Trigger '$triggerId' is not registered" }
        require(label.isNotEmpty() && label.first() == trigger.char) {
            "Token label must start with trigger char '${trigger.char}', got '$label'"
        }
        performInsertToken(query = query, triggerId = triggerId, id = id, label = label)
    }

    /**
     * Dismiss the active trigger query without inserting a token. Leaves the typed text in place
     * (e.g. `@moh` stays as plain characters) and suppresses immediate re-detection until the
     * cursor leaves the typed range.
     */
    @ExperimentalRichTextApi
    public fun cancelActiveTrigger() {
        val query = _activeTriggerQuery ?: return
        suppressedTriggerRange = query.range
        _activeTriggerQuery = null
    }

    /**
     * Splice a [RichSpanStyle.Token] into the tree at [query]'s range in a single atomic edit:
     *
     *  1. remove the old query chars (e.g. "@moh") via the standard edit pipeline;
     *  2. build an atomic Token span + trailing plain-text space span manually;
     *  3. attach both as children of the span at the caret, which guarantees they
     *     land in the same paragraph regardless of document structure;
     *  4. update the raw text and caret in a single [updateTextFieldValue] pass.
     *
     * The earlier two-`addTextAtIndex` implementation was fragile around paragraph
     * boundaries: the trailing-space call could route the space into the next
     * paragraph when the Token sat at a paragraph end. `addRichSpanAtPosition`
     * inherits the target paragraph from the span at the caret, so the space
     * always lands next to the Token.
     */
    private fun performInsertToken(
        query: TriggerQuery,
        triggerId: String,
        id: String,
        label: String,
    ) {
        _activeTriggerQuery = null
        suppressedTriggerRange = null

        if (!query.range.collapsed) {
            removeTextRange(query.range)
        }

        val insertIndex = query.range.min
        val trigger = findTrigger(triggerId) ?: return
        val triggerSpanStyle = trigger.style(config)

        val anchor = getRichSpanByTextIndex(insertIndex - 1)
        val targetParagraph = anchor?.paragraph
            ?: richParagraphList.firstOrNull()
            ?: return

        val tokenSpan = RichSpan(
            text = label,
            paragraph = targetParagraph,
            richSpanStyle = RichSpanStyle.Token(
                triggerId = triggerId,
                id = id,
                label = label,
            ),
            spanStyle = triggerSpanStyle,
        )
        val spaceSpan = RichSpan(
            text = " ",
            paragraph = targetParagraph,
        )

        addRichSpanAtPosition(tokenSpan, spaceSpan, index = insertIndex)

        val currentText = textFieldValue.text
        val before = currentText.substring(0, insertIndex)
        val after = currentText.substring(insertIndex)
        val newText = before + label + " " + after
        val newSelection = TextRange(insertIndex + label.length + 1)

        updateTextFieldValue(
            newTextFieldValue = textFieldValue.copy(
                text = newText,
                selection = newSelection,
            )
        )
    }

    internal var singleParagraphMode by mutableStateOf(false)

    internal var textLayoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

    private var lastPressPosition: Offset? by mutableStateOf(null)

    private var currentAppliedSpanStyle: SpanStyle by mutableStateOf(
        getRichSpanByTextIndex(textIndex = selection.min - 1)?.fullSpanStyle
            ?: RichSpanStyle.DefaultSpanStyle
    )

    private var currentAppliedRichSpanStyle: RichSpanStyle by mutableStateOf(
        getRichSpanByTextIndex(textIndex = selection.min - 1)?.richSpanStyle
            ?: RichSpanStyle.Default
    )

    /**
     * Returns whether the current selected text is a link.
     */
    public val isLink: Boolean get() = currentAppliedRichSpanStyle::class == RichSpanStyle.Link::class

    /**
     * Returns the selected link text.
     */
    public val selectedLinkText: String?
        get() =
            if (isLink)
                getRichSpanByTextIndex(textIndex = selection.min - 1)?.text
            else
                null

    /**
     * Returns the selected link URL.
     */
    public val selectedLinkUrl: String? get() = (currentAppliedRichSpanStyle as? RichSpanStyle.Link)?.url

    @Deprecated(
        message = "Use isCodeSpan instead",
        replaceWith = ReplaceWith("isCodeSpan"),
        level = DeprecationLevel.ERROR,
    )
    public val isCode: Boolean get() = isCodeSpan

    /**
     * Returns whether the current selected text is a code span.
     */
    public val isCodeSpan: Boolean get() = currentRichSpanStyle::class == RichSpanStyle.Code::class

    private var toAddSpanStyle: SpanStyle by mutableStateOf(SpanStyle())
    private var toRemoveSpanStyle: SpanStyle by mutableStateOf(SpanStyle())

    private var toAddRichSpanStyle: RichSpanStyle by mutableStateOf(RichSpanStyle.Default)
    private var toRemoveRichSpanStyleKClass: KClass<out RichSpanStyle> by mutableStateOf(
        RichSpanStyle.Default::class
    )

    @Deprecated(
        message = "Use isRichSpan with T or KClass instead",
        replaceWith = ReplaceWith("isRichSpan<RichSpanStyle>()"),
        level = DeprecationLevel.WARNING,
    )
    public fun isRichSpan(spanStyle: RichSpanStyle): Boolean =
        isRichSpan(spanStyle::class)

    public inline fun <reified T : RichSpanStyle> isRichSpan(): Boolean =
        isRichSpan(T::class)

    public fun isRichSpan(kClass: KClass<out RichSpanStyle>): Boolean {
        return currentRichSpanStyle::class == kClass
    }

    /**
     * The current span style.
     * If the selection is collapsed, the span style is the style of the character preceding the selection.
     * If the selection is not collapsed, the span style is the style of the selection.
     */
    public val currentSpanStyle: SpanStyle
        get() = currentAppliedSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

    /**
     * The current rich span style.
     * If the selection is collapsed, the rich span style is the style of the character preceding the selection.
     * If the selection is not collapsed, the rich span style is the style of the selection.
     */
    public val currentRichSpanStyle: RichSpanStyle
        get() = when {
            currentAppliedRichSpanStyle::class == RichSpanStyle.Default::class ->
                toAddRichSpanStyle

            currentAppliedRichSpanStyle::class == toRemoveRichSpanStyleKClass ->
                RichSpanStyle.Default

            else ->
                currentAppliedRichSpanStyle
        }

    internal var styledRichSpanList = mutableStateListOf<RichSpan>()
        private set

    private var currentAppliedParagraphStyle: ParagraphStyle by mutableStateOf(
        getRichParagraphByTextIndex(textIndex = selection.min - 1)?.paragraphStyle
            ?: richParagraphList.firstOrNull()?.paragraphStyle
            ?: RichParagraph.DefaultParagraphStyle
    )

    private var toAddParagraphStyle: ParagraphStyle by mutableStateOf(ParagraphStyle())
    private var toRemoveParagraphStyle: ParagraphStyle by mutableStateOf(ParagraphStyle())

    /**
     * The current paragraph style.
     * If the selection is collapsed, the paragraph style is the style of the paragraph containing the selection.
     * If the selection is not collapsed, the paragraph style is the style of the selection.
     */
    public val currentParagraphStyle: ParagraphStyle
        get() = currentAppliedParagraphStyle
            .merge(toAddParagraphStyle)
            .unmerge(toRemoveParagraphStyle)

    private var currentRichParagraphType: ParagraphType by mutableStateOf(
        getRichParagraphByTextIndex(textIndex = selection.min - 1)?.type
            ?: DefaultParagraph()
    )

    public var isUnorderedList: Boolean by mutableStateOf(currentRichParagraphType is UnorderedList)
        private set
    public var isOrderedList: Boolean by mutableStateOf(currentRichParagraphType is OrderedList)
        private set
    public var isList: Boolean by mutableStateOf(isUnorderedList || isOrderedList)
        private set
    public var canIncreaseListLevel: Boolean by mutableStateOf(false)
        private set
    public var canDecreaseListLevel: Boolean by mutableStateOf(false)
        private set

    public val config: RichTextConfig = RichTextConfig(
        updateText = {
            // Config changes can alter paragraph prefix widths (e.g. switching
            // orderedListStyleType from Decimal to LowerRoman changes "10. " to
            // "x. "). Rebuild from the RichParagraph tree so the raw text and
            // the rendered prefixes stay in sync; `updateAnnotatedString` relies
            // on substring(...) against the current raw text and would read out
            // of bounds when the prefix width has shifted.
            updateRichParagraphList()
        }
    )

    init {
        updateRichParagraphList(initialRichParagraphList)
    }

    /**
     * Public methods
     */

    @Deprecated(
        message = "Use config instead",
        replaceWith = ReplaceWith("config"),
        level = DeprecationLevel.WARNING,
    )
    public fun setConfig(
        linkColor: Color = Color.Unspecified,
        linkTextDecoration: TextDecoration? = null,
        codeColor: Color = Color.Unspecified,
        codeBackgroundColor: Color = Color.Unspecified,
        codeStrokeColor: Color = Color.Unspecified,
        listIndent: Int = -1
    ) {
        history.onProgrammaticReplace()
        if (linkColor.isSpecified)
            config.linkColor = linkColor

        if (linkTextDecoration != null)
            config.linkTextDecoration = linkTextDecoration

        if (codeColor.isSpecified)
            config.codeSpanColor = codeColor

        if (codeBackgroundColor.isSpecified)
            config.codeSpanBackgroundColor = codeBackgroundColor

        if (codeStrokeColor.isSpecified)
            config.codeSpanStrokeColor = codeStrokeColor

        if (listIndent > -1)
            config.listIndent = listIndent

        updateTextFieldValue(textFieldValue)
    }

    // Text

    /**
     * Removes the selected text from the current text input.
     *
     * This method removes the text specified by the `selection` from the current text input.
     *
     * @see removeTextRange
     */
    public fun removeSelectedText(): Unit =
        removeTextRange(selection)

    /**x
     * Removes the specified text range from the current text.
     *
     * @param textRange the range of text to be removed
     */
    public fun removeTextRange(
        textRange: TextRange
    ) {
        require(textRange.min >= 0) {
            "The start index must be non-negative."
        }

        require(textRange.max <= textFieldValue.text.length) {
            "The end index must be within the text bounds. " +
                    "The text length is ${textFieldValue.text.length}, " +
                    "but the end index is ${textRange.max}."
        }

        if (textRange.collapsed)
            return

        onTextFieldValueChange(
            newTextFieldValue = textFieldValue.copy(
                text = textFieldValue.text.removeRange(
                    startIndex = textRange.min,
                    endIndex = textRange.max,
                ),
                selection = TextRange(textRange.min),
            )
        )
    }

    /**
     * Replaces the currently selected text with the provided text.
     *
     * @param text The new text to be inserted
     */
    public fun replaceSelectedText(text: String): Unit =
        replaceTextRange(selection, text)

    /**
     * Replaces the text in the specified range with the provided text.
     *
     * @param textRange The range of text to be replaced
     * @param text The new text to be inserted
     */
    public fun replaceTextRange(
        textRange: TextRange,
        text: String
    ) {
        require(textRange.min >= 0) {
            "The start index must be non-negative."
        }

        require(textRange.max <= textFieldValue.text.length) {
            "The end index must be within the text bounds. " +
                    "The text length is ${textFieldValue.text.length}, " +
                    "but the end index is ${textRange.max}."
        }

        removeTextRange(textRange)
        addTextAfterSelection(text = text)
    }

    /**
     * Adds the provided text to the text field at the current selection.
     *
     * @param text The text to be added
     */
    public fun addTextAfterSelection(text: String): Unit =
        addTextAtIndex(
            index = if (selection.reversed) selection.start else selection.end,
            text = text
        )

    /**
     * Adds the provided text to the text field at the specified index.
     *
     * @param index The index at which the text should be added
     * @param text The text to be added
     */
    public fun addTextAtIndex(
        index: Int,
        text: String,
    ) {
        require(index >= 0) {
            "The index must be non-negative."
        }

        require(index <= textFieldValue.text.length) {
            "The index must be within the text bounds. " +
                    "The text length is ${textFieldValue.text.length}, " +
                    "but the index is $index."
        }

        val beforeText = textFieldValue.text.substring(0, index)
        val afterText = textFieldValue.text.substring(index, textFieldValue.text.length)
        val newText = "$beforeText$text$afterText"

        onTextFieldValueChange(
            newTextFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(index + text.length),
            )
        )
    }

    // SpanStyle

    /**
     * Returns the [SpanStyle] of the text at the specified text range.
     * If the text range is collapsed, the style of the character preceding the text range is returned.
     *
     * @param textRange the text range.
     * @return the [SpanStyle] of the text at the specified text range.
     */
    public fun getSpanStyle(textRange: TextRange): SpanStyle =
        if (textRange.collapsed) {
            val richSpan = getRichSpanByTextIndex(textIndex = textRange.min - 1)

            richSpan
                ?.fullSpanStyle
                ?: RichSpanStyle.DefaultSpanStyle
        } else {
            val richSpanList = getRichSpanListByTextRange(textRange)

            richSpanList
                .getCommonStyle()
                ?: RichSpanStyle.DefaultSpanStyle
        }

    /**
     * Returns the [RichSpanStyle] of the text at the specified text range.
     * If the text range is collapsed, the style of the character preceding the text range is returned.
     *
     * @param textRange the text range.
     * @return the [RichSpanStyle] of the text at the specified text range.
     */
    public fun getRichSpanStyle(textRange: TextRange): RichSpanStyle =
        if (textRange.collapsed) {
            val richSpan = getRichSpanByTextIndex(textIndex = textRange.min - 1)

            richSpan
                ?.fullStyle
                ?: RichSpanStyle.Default
        } else {
            val richSpanList = getRichSpanListByTextRange(textRange)

            richSpanList
                .getCommonRichStyle()
                ?: RichSpanStyle.Default
        }

    /**
     * Returns the [ParagraphStyle] of the text at the specified text range.
     * If the text range is collapsed, the style of the paragraph containing the text range is returned.
     *
     * @param textRange the text range.
     * @return the [ParagraphStyle] of the text at the specified text range.
     */
    public fun getParagraphStyle(textRange: TextRange): ParagraphStyle =
        if (textRange.collapsed) {
            val richParagraph = getRichParagraphByTextIndex(textIndex = textRange.min - 1)

            richParagraph
                ?.paragraphStyle
                ?: RichParagraph.DefaultParagraphStyle
        } else {
            val richParagraphList = getRichParagraphListByTextRange(textRange)

            richParagraphList
                .getCommonStyle()
                ?: RichParagraph.DefaultParagraphStyle
        }

    /**
     * Toggle the [SpanStyle]
     * If the passed span style doesn't exist in the [currentSpanStyle] it's going to be added.
     * If the passed span style already exists in the [currentSpanStyle] it's going to be removed.
     *
     * Example: You can toggle Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be toggled.
     * Only the specified params are going to be toggled, and the non specified ones are going to be ignored.
     * @see [addSpanStyle]
     * @see [removeSpanStyle]
     */
    public fun toggleSpanStyle(spanStyle: SpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        // Only record if the mutation actually applies to existing text; with a
        // collapsed caret it only updates the staged-style bag for future typing,
        // which undoes naturally via re-toggling.
        enabled = !selection.collapsed,
    ) {
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle))
            removeSpanStyle(spanStyle)
        else
            addSpanStyle(spanStyle)
    }

    /**
     * Add new [SpanStyle] to the [currentSpanStyle]
     *
     * Example: You can add Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be added to the [currentSpanStyle].
     * Only the specified params are going to be applied, and the non specified ones are going to be ignored.
     * @see [removeSpanStyle]
     * @see [toggleSpanStyle]
     */
    public fun addSpanStyle(spanStyle: SpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        if (!currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toAddSpanStyle = toAddSpanStyle.customMerge(spanStyle)
            toRemoveSpanStyle = toRemoveSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            applyRichSpanStyleToSelectedText()
    }

    /**
     * Add new [SpanStyle] for a specific [TextRange]
     *
     * Example: You can add Bold FontWeight to a specific range by passing:
     *
     * ```
     * state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 5))
     * ```
     *
     * @param spanStyle the span style that is going to be added to the rich span.
     * @param textRange the text range where the span style is going to be applied.
     */
    public fun addSpanStyle(spanStyle: SpanStyle, textRange: TextRange): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        val oldToRemoveSpanStyle = toRemoveSpanStyle
        val oldToAddSpanStyle = toAddSpanStyle

        toAddSpanStyle = spanStyle
        toRemoveSpanStyle = SpanStyle()

        applyRichSpanStyleToTextRange(textRange)

        toRemoveSpanStyle = oldToRemoveSpanStyle
        toAddSpanStyle = oldToAddSpanStyle
    }

    /**
     * Remove an existing [SpanStyle] from the [currentSpanStyle]
     *
     * Example: You can remove Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be removed from the [currentSpanStyle].
     * Only the specified params are going to be removed, and the non specified ones are going to be ignored.
     * @see [addSpanStyle]
     * @see [toggleSpanStyle]
     */
    public fun removeSpanStyle(spanStyle: SpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toRemoveSpanStyle = toRemoveSpanStyle.customMerge(spanStyle)
            toAddSpanStyle = toAddSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            applyRichSpanStyleToSelectedText()
    }

    /**
     * Remove an existing [SpanStyle] from a specific [TextRange]
     *
     * Example: You can remove Bold FontWeight from a specific range by passing:
     *
     * ```
     * state.removeSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 5))
     * ```
     *
     * @param spanStyle the span style that is going to be removed from the rich span.
     * @param textRange the text range where the span style is going to be removed.
     */
    public fun removeSpanStyle(spanStyle: SpanStyle, textRange: TextRange): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        val oldToRemoveSpanStyle = toRemoveSpanStyle
        val oldToAddSpanStyle = toAddSpanStyle

        toRemoveSpanStyle = spanStyle
        toAddSpanStyle = SpanStyle()

        applyRichSpanStyleToTextRange(textRange)

        toRemoveSpanStyle = oldToRemoveSpanStyle
        toAddSpanStyle = oldToAddSpanStyle
    }

    /**
     * Clear all [SpanStyle]s.
     */
    public fun clearSpanStyles(): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        removeSpanStyle(currentSpanStyle)
    }

    /**
     * Clear all [SpanStyle]s from a specific [TextRange].
     */
    public fun clearSpanStyles(textRange: TextRange): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        removeSpanStyle(currentSpanStyle, textRange)
    }

    /**
     * Sets the heading style for the currently selected paragraphs.
     *
     * This function applies the specified [headerParagraphStyle] to all paragraphs
     * that are fully or partially within the current [selection].
     *
     * If the specified style is [HeadingStyle.Normal], any existing heading
     * style (H1-H6) is removed from the selected paragraphs. Otherwise, the specified
     * heading style is applied, replacing any previous heading style on those paragraphs.
     * Heading styles are applied to the entire paragraph, if the selection is collapsed -
     * consistent with common rich text editor behavior. If the selection is not collapsed,
     * heading styles will be applied to each paragraph in the selection.
     */
    public fun setHeadingStyle(headerParagraphStyle: HeadingStyle) {
        val paragraphs = getRichParagraphListByTextRange(selection)
        if (paragraphs.isEmpty()) return

        paragraphs.forEach { paragraph ->
            paragraph.setHeadingStyle(headerParagraphStyle)
        }

        updateAnnotatedString()
        updateCurrentSpanStyle()
        updateCurrentParagraphStyle()
    }

    /**
     * Add a link to the text field.
     * The link is going to be added after the current selection.
     *
     * @param text the text of the link.
     * @param url the URL of the link.
     */
    public fun addLink(
        text: String,
        url: String,
    ): Unit = recordHistory(CommitTrigger.Formatting) {
        if (text.isEmpty()) return@recordHistory

        val paragraph = richParagraphList.firstOrNull() ?: return@recordHistory
        val linkStyle = RichSpanStyle.Link(
            url = url,
        )
        val linkRichSpan = RichSpan(
            text = text,
            richSpanStyle = linkStyle,
            paragraph = paragraph,
        )

        addRichSpanAtPosition(
            linkRichSpan,
            index = selection.min,
        )

        val beforeText = textFieldValue.text.substring(0, selection.min)
        val afterText = textFieldValue.text.substring(selection.min)
        val newText = "$beforeText$text$afterText"
        updateTextFieldValue(
            newTextFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(selection.min + text.length),
            )
        )
    }

    /**
     * Add a link to the selected text.
     *
     * @param url the URL of the link.
     */
    public fun addLinkToSelection(
        url: String,
    ): Unit = recordHistory(CommitTrigger.Formatting) {
        if (selection.collapsed) return@recordHistory

        val linkStyle = RichSpanStyle.Link(
            url = url,
        )

        toAddRichSpanStyle = linkStyle
        toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class

        addRichSpan(
            spanStyle = linkStyle
        )
    }

    /**
     * Add a link to a specific [TextRange].
     *
     * @param url the URL of the link.
     * @param textRange the text range where the link is going to be added.
     */
    public fun addLinkToTextRange(
        url: String,
        textRange: TextRange,
    ): Unit = recordHistory(CommitTrigger.Formatting) {
        if (textRange.collapsed) return@recordHistory

        val linkStyle = RichSpanStyle.Link(
            url = url,
        )

        toAddRichSpanStyle = linkStyle
        toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class

        addRichSpan(
            spanStyle = linkStyle,
            textRange = textRange
        )
    }

    /**
     * Update the link of the selected text.
     *
     * @param url the new URL of the link.
     */
    public fun updateLink(
        url: String,
    ): Unit = recordHistory(CommitTrigger.Formatting) {
        if (!isLink) return@recordHistory

        val linkStyle = RichSpanStyle.Link(
            url = url,
        )

        val richSpan = getSelectedLinkRichSpan() ?: return@recordHistory

        richSpan.richSpanStyle = linkStyle

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Remove the link from the selected text.
     */
    public fun removeLink(): Unit = recordHistory(CommitTrigger.Formatting) {
        if (!isLink) return@recordHistory

        val richSpan = getSelectedLinkRichSpan() ?: return@recordHistory

        richSpan.richSpanStyle = RichSpanStyle.Default

        updateTextFieldValue(textFieldValue)
    }

    @Deprecated(
        message = "Use toggleCodeSpan instead",
        replaceWith = ReplaceWith("toggleCodeSpan()"),
        level = DeprecationLevel.ERROR,
    )
    public fun toggleCode(): Unit = toggleCodeSpan()

    public fun toggleCodeSpan(): Unit = toggleRichSpan(RichSpanStyle.Code())

    @Deprecated(
        message = "Use addCodeSpan instead",
        replaceWith = ReplaceWith("addCodeSpan()"),
        level = DeprecationLevel.ERROR,
    )
    public fun addCode(): Unit = addCodeSpan()

    public fun addCodeSpan(): Unit = addRichSpan(RichSpanStyle.Code())

    @Deprecated(
        message = "Use removeCodeSpan instead",
        replaceWith = ReplaceWith("removeCodeSpan()"),
        level = DeprecationLevel.ERROR,
    )
    public fun removeCode(): Unit = removeCodeSpan()

    public fun removeCodeSpan(): Unit = removeRichSpan(RichSpanStyle.Code())

    public fun toggleRichSpan(spanStyle: RichSpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        if (isRichSpan(spanStyle::class))
            removeRichSpan(spanStyle)
        else
            addRichSpan(spanStyle)
    }

    /**
     * Add a new [RichSpanStyle] to the selected text or to the text
     * that is going to be typed if the selection is collapsed.
     *
     * @param spanStyle the rich span style that is going to be added.
     */
    public fun addRichSpan(spanStyle: RichSpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        if (toRemoveRichSpanStyleKClass == spanStyle::class)
            toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class
        toAddRichSpanStyle = spanStyle

        if (!selection.collapsed)
            applyRichSpanStyleToSelectedText()
    }

    /**
     * Add a new [RichSpanStyle] to a specific [TextRange].
     *
     * @param spanStyle the rich span style that is going to be added.
     * @param textRange the text range where the rich span style is going to be applied.
     */
    public fun addRichSpan(
        spanStyle: RichSpanStyle,
        textRange: TextRange,
    ): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        if (textRange.collapsed)
            return@recordHistory

        if (toRemoveRichSpanStyleKClass == spanStyle::class)
            toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class
        toAddRichSpanStyle = spanStyle

        applyRichSpanStyleToTextRange(textRange)
    }

    /**
     * Remove an existing [RichSpanStyle] from the selected text or from the text
     * that is going to be typed if the selection is collapsed.
     *
     * @param spanStyle the rich span style that is going to be removed.
     */
    public fun removeRichSpan(spanStyle: RichSpanStyle): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        if (toAddRichSpanStyle::class == spanStyle::class)
            toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyleKClass = spanStyle::class

        if (!selection.collapsed)
            applyRichSpanStyleToSelectedText()
    }

    /**
     * Remove an existing [RichSpanStyle] from a specific [TextRange].
     *
     * @param spanStyle the rich span style that is going to be removed.
     * @param textRange the text range where the rich span style is going to be removed.
     */
    public fun removeRichSpan(
        spanStyle: RichSpanStyle,
        textRange: TextRange
    ): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        if (textRange.collapsed)
            return@recordHistory

        if (toAddRichSpanStyle::class == spanStyle::class)
            toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyleKClass = spanStyle::class

        applyRichSpanStyleToTextRange(textRange)
    }

    /**
     * Clear all [RichSpanStyle]s.
     */
    public fun clearRichSpans(): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !selection.collapsed,
    ) {
        removeRichSpan(currentRichSpanStyle)
    }

    /**
     * Clear all [RichSpanStyle]s from a specific [TextRange].
     */
    public fun clearRichSpans(textRange: TextRange): Unit = recordHistory(
        trigger = CommitTrigger.Formatting,
        enabled = !textRange.collapsed,
    ) {
        removeRichSpan(currentRichSpanStyle, textRange)
    }

    /**
     * Toggle the [ParagraphStyle]
     * If the passed paragraph style doesn't exist in the [currentParagraphStyle] it's going to be added.
     * If the passed paragraph style already exists in the [currentParagraphStyle] it's going to be removed.
     *
     * Example: You can toggle TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be toggled.
     * Only the specified params are going to be toggled, and the non specified ones are going to be ignored.
     * @see [addParagraphStyle]
     * @see [removeParagraphStyle]
     */
    public fun toggleParagraphStyle(paragraphStyle: ParagraphStyle): Unit = recordHistory(CommitTrigger.Formatting) {
        if (currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle))
            removeParagraphStyle(paragraphStyle)
        else
            addParagraphStyle(paragraphStyle)
    }

    /**
     * Add new [ParagraphStyle] to the [currentParagraphStyle]
     *
     * Example: You can add TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be added to the [currentParagraphStyle].
     * Only the specified params are going to be applied, and the non specified ones are going to be ignored.
     * @see [removeParagraphStyle]
     * @see [toggleParagraphStyle]
     */
    public fun addParagraphStyle(paragraphStyle: ParagraphStyle): Unit = recordHistory(CommitTrigger.Formatting) {
        if (!currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle)) {
            // If the selection is collapsed, we add the paragraph style to the paragraph containing the selection
            if (selection.collapsed) {
                val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return@recordHistory
                paragraph.paragraphStyle = paragraph.paragraphStyle.merge(paragraphStyle)
                clearLineBreakContinuations(paragraph)
            }
            // If the selection is not collapsed, we add the paragraph style to all the paragraphs in the selection
            else {
                val paragraphs = getRichParagraphListByTextRange(selection)
                if (paragraphs.isEmpty()) return@recordHistory
                paragraphs.fastForEach {
                    it.paragraphStyle = it.paragraphStyle.merge(paragraphStyle)
                    clearLineBreakContinuations(it)
                }
            }
            // We update the annotated string to reflect the changes
            updateAnnotatedString()
            // We update the current paragraph style to reflect the changes
            updateCurrentParagraphStyle()
        }
    }

    /**
     * Remove an existing [ParagraphStyle] from the [currentParagraphStyle]
     *
     * Example: You can remove TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be removed from the [currentParagraphStyle].
     * Only the specified params are going to be removed, and the non specified ones are going to be ignored.
     * @see [addParagraphStyle]
     * @see [toggleParagraphStyle]
     */
    public fun removeParagraphStyle(paragraphStyle: ParagraphStyle): Unit = recordHistory(CommitTrigger.Formatting) {
        if (currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle)) {
            // If the selection is collapsed, we remove the paragraph style from the paragraph containing the selection
            if (selection.collapsed) {
                val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return@recordHistory
                paragraph.paragraphStyle = paragraph.paragraphStyle.unmerge(paragraphStyle)
                clearLineBreakContinuations(paragraph)
            }
            // If the selection is not collapsed, we remove the paragraph style from all the paragraphs in the selection
            else {
                val paragraphs = getRichParagraphListByTextRange(selection)
                if (paragraphs.isEmpty()) return@recordHistory
                paragraphs.fastForEach {
                    it.paragraphStyle = it.paragraphStyle.unmerge(paragraphStyle)
                    clearLineBreakContinuations(it)
                }
            }
            // We update the annotated string to reflect the changes
            updateAnnotatedString()
            // We update the current paragraph style to reflect the changes
            updateCurrentParagraphStyle()
        }
    }

    public fun toggleUnorderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)
        if (paragraphs.isEmpty())
            return@recordHistory
        val isFirstParagraphUnorderedList = paragraphs.first().type is UnorderedList
        paragraphs.fastForEach { paragraph ->
            if (isFirstParagraphUnorderedList)
                removeUnorderedList(paragraph)
            else
                addUnorderedList(paragraph)
        }
    }

    public fun addUnorderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)

        paragraphs.fastForEach { paragraph ->
            addUnorderedList(paragraph)
        }
    }

    public fun removeUnorderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)

        paragraphs.fastForEach { paragraph ->
            removeUnorderedList(paragraph)
        }
    }

    public fun toggleOrderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)
        if (paragraphs.isEmpty())
            return@recordHistory
        val isFirstParagraphOrderedList = paragraphs.first().type is OrderedList
        paragraphs.fastForEach { paragraph ->
            if (isFirstParagraphOrderedList) {
                removeOrderedList(paragraph)
            } else {
                addOrderedList(paragraph)
            }
        }
    }

    public fun addOrderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)

        paragraphs.fastForEach { paragraph ->
            addOrderedList(paragraph)
        }
    }

    public fun removeOrderedList(): Unit = recordHistory(CommitTrigger.Structural) {
        val paragraphs = getRichParagraphListByTextRange(selection)

        paragraphs.fastForEach { paragraph ->
            removeOrderedList(paragraph)
        }
    }

    /**
     * Increase the level of the current selected lists.
     *
     * If the current selection is not a list, this method does nothing.
     *
     * If multiple paragraphs are selected, they all must be lists.
     */
    public fun increaseListLevel(): Unit = recordHistory(CommitTrigger.Structural) {
        if (!isList)
            return@recordHistory

        val paragraphs = getRichParagraphListByTextRange(selection)

        if (paragraphs.isEmpty())
            return@recordHistory

        if (!canIncreaseListLevel(paragraphs))
            return@recordHistory

        // Increase list level
        val levelNumberMap = mutableMapOf<Int, Int>()
        var minParagraphLevel = Int.MAX_VALUE
        var minParagraphLevelOrderedListNumber = -1
        var startParagraphIndex = -1
        var startParagraphLevel = -1
        var endParagraphIndex = -1
        var processedParagraphCount = 0

        val firstSelectedParagraph = paragraphs.first()

        for (i in richParagraphList.indices) {
            val paragraph = richParagraphList[i]
            val type = paragraph.type

            // Skip paragraphs before the selected paragraphs
            if (startParagraphIndex == -1) {
                if (paragraph == firstSelectedParagraph) {
                    startParagraphIndex = i
                    startParagraphLevel =
                        if (type is ConfigurableListLevel)
                            type.level
                        else
                            0
                } else {
                    if (type is ConfigurableListLevel) {
                        levelNumberMap.keys.toList().fastForEach { level ->
                            if (level > type.level)
                                levelNumberMap.remove(level)
                        }

                        if (type is OrderedList)
                            levelNumberMap[type.level] = type.number

                        if (type is UnorderedList)
                            levelNumberMap.remove(type.level)
                    } else {
                        levelNumberMap.clear()
                    }

                    continue
                }
            }

            if (processedParagraphCount >= paragraphs.size) {
                if (
                    type !is ConfigurableListLevel ||
                    type.level <= minParagraphLevel
                ) {
                    endParagraphIndex = i - 1
                    break
                }
            }

            if (type is ConfigurableListLevel) {
                if (type.level <= minParagraphLevel) {
                    minParagraphLevel = type.level
                    minParagraphLevelOrderedListNumber =
                        if (type is OrderedList)
                            type.number - 1
                        else
                            -1
                }

                type.level++
            } else {
                if (minParagraphLevel != Int.MAX_VALUE && minParagraphLevelOrderedListNumber != -1)
                    levelNumberMap[minParagraphLevel] = minParagraphLevelOrderedListNumber

                minParagraphLevel = Int.MAX_VALUE
                minParagraphLevelOrderedListNumber = -1
            }

            processedParagraphCount++
        }

        if (minParagraphLevel != Int.MAX_VALUE && minParagraphLevelOrderedListNumber != -1)
            levelNumberMap[minParagraphLevel] = minParagraphLevelOrderedListNumber

        // Adjust ordered list numbers
        val newTextFieldValue = adjustOrderedListsNumbers(
            startParagraphIndex = startParagraphIndex,
            startNumber = levelNumberMap[startParagraphLevel + 1]?.plus(1) ?: 1,
            textFieldValue = textFieldValue,
            initialLevelNumberMap = levelNumberMap,
        )

        updateTextFieldValue(
            newTextFieldValue = newTextFieldValue,
        )
    }

    /**
     * Decrease the level of the current selected lists.
     *
     * If the current selection is not a list, this method does nothing.
     *
     * If multiple paragraphs are selected, they all must be lists.
     */
    public fun decreaseListLevel(): Unit = recordHistory(CommitTrigger.Structural) {
        if (!isList)
            return@recordHistory

        val paragraphs = getRichParagraphListByTextRange(selection)

        if (paragraphs.isEmpty())
            return@recordHistory

        if (!canDecreaseListLevel(paragraphs))
            return@recordHistory

        // Decrease list level
        val levelNumberMap = mutableMapOf<Int, Int>()
        var minParagraphLevel = Int.MAX_VALUE
        var minParagraphLevelOrderedListNumber = -1
        var startParagraphIndex = -1
        var endParagraphIndex = -1
        var startParagraphLevel = -1
        var processedParagraphCount = 0

        val firstSelectedParagraph = paragraphs.first()

        for (i in richParagraphList.indices) {
            val paragraph = richParagraphList[i]
            val type = paragraph.type

            // Skip paragraphs before the selected paragraphs
            if (startParagraphIndex == -1) {
                if (paragraph == firstSelectedParagraph) {
                    startParagraphIndex = i
                    startParagraphLevel =
                        if (type is ConfigurableListLevel)
                            type.level
                        else
                            0
                } else {
                    if (type is ConfigurableListLevel) {
                        levelNumberMap.keys.toList().fastForEach { level ->
                            if (level > type.level)
                                levelNumberMap.remove(level)
                        }

                        if (type is OrderedList)
                            levelNumberMap[type.level] = type.number

                        if (type is UnorderedList)
                            levelNumberMap.remove(type.level)
                    } else {
                        levelNumberMap.clear()
                    }

                    continue
                }
            }

            if (processedParagraphCount >= paragraphs.size) {
                if (
                    type !is ConfigurableListLevel ||
                    type.level <= minParagraphLevel
                ) {
                    endParagraphIndex = i - 1
                    break
                }
            }

            if (type is ConfigurableListLevel) {
                if (type.level <= minParagraphLevel) {
                    minParagraphLevel = type.level
                    minParagraphLevelOrderedListNumber =
                        if (type is OrderedList)
                            type.number - 1
                        else
                            -1
                }

                type.level = (type.level - 1).coerceAtLeast(1)
            } else {
                minParagraphLevel = Int.MAX_VALUE
                minParagraphLevelOrderedListNumber = -1
            }

            processedParagraphCount++
        }

        // Adjust ordered list numbers
        val newTextFieldValue = adjustOrderedListsNumbers(
            startParagraphIndex = startParagraphIndex,
            startNumber = levelNumberMap[startParagraphLevel - 1]?.plus(1) ?: 1,
            textFieldValue = textFieldValue,
            initialLevelNumberMap = levelNumberMap,
        )

        updateTextFieldValue(
            newTextFieldValue = newTextFieldValue,
        )
    }

    /**
     * Private/Internal methods
     */

    /**
     * Returns the [ParagraphType] of the text at the specified text range.
     * If the text range is collapsed, the type of the paragraph containing the text range is returned.
     *
     * @param textRange the text range.
     * @return the [ParagraphType] of the text at the specified text range.
     */
    internal fun getParagraphType(textRange: TextRange): ParagraphType =
        if (textRange.collapsed) {
            val richParagraph = getRichParagraphByTextIndex(textIndex = textRange.min - 1)

            richParagraph
                ?.type
                ?: DefaultParagraph()
        } else {
            val richParagraphList = getRichParagraphListByTextRange(textRange)

            richParagraphList
                .getCommonType()
                ?: DefaultParagraph()
        }

    private fun getSelectedLinkRichSpan(): RichSpan? {
        val richSpan = getRichSpanByTextIndex(selection.min - 1)

        return getLinkRichSpan(richSpan)
    }

    private fun addUnorderedList(paragraph: RichParagraph) {
        val paragraphType = paragraph.type
        if (paragraphType is UnorderedList)
            return

        val index = richParagraphList.indexOf(paragraph)

        if (index == -1)
            return

        val listLevel =
            if (paragraphType is ConfigurableListLevel)
                paragraphType.level
            else
                1

        val newType = UnorderedList(
            config = config,
            initialLevel = listLevel,
        )

        val newTextFieldValue = adjustOrderedListsNumbers(
            startParagraphIndex = index,
            startNumber = 1,
            textFieldValue = updateParagraphType(
                paragraph = paragraph,
                newType = newType,
                textFieldValue = textFieldValue,
            ),
        )

        updateTextFieldValue(
            newTextFieldValue = newTextFieldValue
        )
    }

    private fun removeUnorderedList(paragraph: RichParagraph) {
        if (paragraph.type !is UnorderedList)
            return

        resetParagraphType(paragraph = paragraph)
    }

    private fun addOrderedList(paragraph: RichParagraph) {
        val paragraphType = paragraph.type

        if (paragraphType is OrderedList)
            return

        val index = richParagraphList.indexOf(paragraph)

        if (index == -1)
            return

        var orderedListNumber = 1

        val listLevel =
            if (paragraphType is ConfigurableListLevel)
                paragraphType.level
            else
                1

        for (i in index - 1 downTo 0) {
            val prevParagraph = richParagraphList[i]
            val prevParagraphType = prevParagraph.type

            if (prevParagraphType is ConfigurableListLevel && prevParagraphType.level < listLevel)
                break

            if (prevParagraphType is ConfigurableListLevel && prevParagraphType !is OrderedList)
                continue

            if (prevParagraphType !is OrderedList)
                break

            if (prevParagraphType.level > listLevel)
                continue

            orderedListNumber = prevParagraphType.number + 1

            break
        }

        val newType = OrderedList(
            number = orderedListNumber,
            config = config,
            initialLevel = listLevel,
        )

        val newTextFieldValue = adjustOrderedListsNumbers(
            startParagraphIndex = index,
            startNumber = orderedListNumber,
            textFieldValue = updateParagraphType(
                paragraph = paragraph,
                newType = newType,
                textFieldValue = textFieldValue,
            ),
        )

        updateTextFieldValue(
            newTextFieldValue = newTextFieldValue,
        )
    }

    private fun removeOrderedList(paragraph: RichParagraph) {
        if (paragraph.type !is OrderedList) return
        val index = richParagraphList.indexOf(paragraph)
        if (index == -1) return

        for (i in (index + 1)..richParagraphList.lastIndex) {
            val currentParagraphType = richParagraphList[i].type
            if (currentParagraphType !is OrderedList) break
            currentParagraphType.number = i - index
        }

        resetParagraphType(paragraph = paragraph)
    }

    /**
     * Increases and decreases the list level of the current selected lists when the Tab key is pressed.
     *
     * @param event the key event.
     * @return true if the list level was increased or decreased, false otherwise.
     */
    internal fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        // Undo/redo shortcuts - intercepted before BasicTextField's built-in handler
        // so rich-model snapshots rewind instead of plain-text TextFieldValue state.
        if (!suppressUndoShortcuts && event.type == KeyEventType.KeyDown && !event.isAltPressed) {
            val modifier = event.isMetaPressed || event.isCtrlPressed
            if (modifier) {
                when (event.key) {
                    Key.Z if !event.isShiftPressed -> {
                        history.undo()
                        return true
                    }
                    Key.Z if event.isShiftPressed -> {
                        history.redo()
                        return true
                    }
                }
            }
        }

        // Give trigger-suggestions popup first refusal on key events while a query is active.
        // Popup handlers return true when they consume the event (↑/↓/Enter/Esc navigation).
        triggerKeyHandler?.invoke(event)?.let { handled ->
            if (handled) return true
        }

        if (event.type != KeyEventType.KeyDown)
            return false

        if (event.key != Key.Tab)
            return false

        if (
            event.isMetaPressed ||
            event.isCtrlPressed ||
            event.isAltPressed
        )
            return false

        if (!isList)
            return false

        if (event.isShiftPressed && canDecreaseListLevel())
            decreaseListLevel()
        else if (!event.isShiftPressed && canIncreaseListLevel())
            increaseListLevel()
        else
            return false

        return true
    }

    /**
     * Checks weather the list level can be increased or not.
     *
     * @param paragraphs the list of paragraphs to check.
     * @return true if the list level can be increased, false otherwise.
     */
    internal fun canIncreaseListLevel(
        paragraphs: List<RichParagraph> = getRichParagraphListByTextRange(selection),
    ): Boolean {
        if (paragraphs.isEmpty())
            return false

        val firstParagraph = paragraphs.first()
        val firstParagraphType = firstParagraph.type
        val firstParagraphIndex = richParagraphList.indexOf(firstParagraph)

        if (firstParagraphIndex == -1 || firstParagraphType !is ConfigurableListLevel)
            return false

        val previousParagraph = richParagraphList.getOrNull(firstParagraphIndex - 1)
        val previousParagraphType = previousParagraph?.type

        // The previous paragraph must be a list, otherwise we can't increase the list level
        if (previousParagraph == null || previousParagraphType !is ConfigurableListLevel)
            return false

        // The first paragraph must have the same or lower list level than the previous one
        if (firstParagraphType.level > previousParagraphType.level)
            return false

        paragraphs.fastForEach { paragraph ->
            val paragraphType = paragraph.type

            // All paragraphs must be ConfigurableListLevel
            if (paragraphType !is ConfigurableListLevel)
                return false

            // TODO: Maybe in the future we can remove this condition
            // The paragraph must have the same or higher list level than the first paragraph
            if (paragraphType.level < firstParagraphType.level)
                return false
        }

        return true
    }

    /**
     * Checks weather the list level can be decreased or not.
     *
     * @param paragraphs the list of paragraphs to check.
     * @return true if the list level can be decreased, false otherwise.
     */
    internal fun canDecreaseListLevel(
        paragraphs: List<RichParagraph> = getRichParagraphListByTextRange(selection),
    ): Boolean {
        if (paragraphs.isEmpty())
            return false

        paragraphs.fastForEach { paragraph ->
            val paragraphType = paragraph.type

            // All paragraphs must be ConfigurableListLevel
            if (paragraphType !is ConfigurableListLevel)
                return false

            // The paragraph list level must be at least 2
            if (paragraphType.level < 2)
                return false
        }

        return true
    }

    private fun updateParagraphType(
        paragraph: RichParagraph,
        newType: ParagraphType,
    ) {
        updateTextFieldValue(
            newTextFieldValue = updateParagraphType(
                paragraph = paragraph,
                newType = newType,
                textFieldValue = this.textFieldValue,
            )
        )
    }

    private fun updateParagraphType(
        paragraph: RichParagraph,
        newType: ParagraphType,
        textFieldValue: TextFieldValue,
    ): TextFieldValue {
        val selection = textFieldValue.selection
        val paragraphOldStartTextLength = paragraph.type.startText.length
        val textFieldValueDiff = this.textFieldValue.text.length - textFieldValue.text.length
        val firstNonEmptyChildIndex = paragraph.getFirstNonEmptyChild()?.textRange?.min?.let {
            if (it >= selection.min)
                it - textFieldValueDiff
            else
                it
        }
        val paragraphFirstChildStartIndex =
            (firstNonEmptyChildIndex ?: selection.min).coerceAtLeast(0)

        paragraph.type = newType

        // A paragraph change means this and its trailing <br> continuations are independent now
        clearLineBreakContinuations(paragraph)

        // If the paragraph type start text length didn't change, we don't need to update the text field value
        if (paragraphOldStartTextLength == newType.startText.length)
            return textFieldValue

        val beforeText = textFieldValue.text.substring(
            0,
            (paragraphFirstChildStartIndex - paragraphOldStartTextLength)
                .fastCoerceAtLeast(0)
        )
        val afterText = textFieldValue.text.substring(paragraphFirstChildStartIndex)

        val newSelectionMin =
            if (selection.min > paragraphFirstChildStartIndex) selection.min + newType.startText.length - paragraphOldStartTextLength
            else if (selection.min == paragraphFirstChildStartIndex) paragraphFirstChildStartIndex + newType.startText.length - paragraphOldStartTextLength
            else selection.min
        val newSelectionMax =
            if (selection.max > paragraphFirstChildStartIndex) selection.max + newType.startText.length - paragraphOldStartTextLength
            else if (selection.max == paragraphFirstChildStartIndex) paragraphFirstChildStartIndex + newType.startText.length - paragraphOldStartTextLength
            else selection.max

        return textFieldValue.copy(
            text = beforeText + newType.startText + afterText,
            selection = TextRange(
                newSelectionMin.coerceAtLeast(0),
                newSelectionMax.coerceAtLeast(0),
            ),
        )
    }

    private fun resetParagraphType(paragraph: RichParagraph) {
        updateParagraphType(paragraph, DefaultParagraph())
    }

    /**
     * Temporarily stores the new text field value, before it is validated.
     */
    private var tempTextFieldValue = textFieldValue

    /**
     * Set to true when the IME revert is detected (#640), so [checkForParagraphs]
     * uses a threshold of 0 to scan all newlines.
     */
    private var forceCheckAllNewlines = false

    /**
     * Handles the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun classifyTextChange(newTextFieldValue: TextFieldValue): CommitTrigger? {
        val oldText = textFieldValue.text
        val newText = newTextFieldValue.text
        return when {
            newText.length > oldText.length -> {
                val delta = newText.length - oldText.length
                val caret = newTextFieldValue.selection.min
                val start = (caret - delta).coerceAtLeast(0)
                val added = newText.substring(start, caret.coerceAtMost(newText.length))
                if (added.contains('\n')) CommitTrigger.LineBreak
                else CommitTrigger.Typing(addedText = added, caret = caret)
            }
            newText.length < oldText.length ->
                CommitTrigger.Delete(caret = newTextFieldValue.selection.min)
            newText == oldText && newTextFieldValue.selection != textFieldValue.selection ->
                CommitTrigger.SelectionJump
            else -> null
        }
    }

    internal fun onTextFieldValueChange(newTextFieldValue: TextFieldValue) {
        // Classify the change for history before any mutation happens.
        val pendingHtml = pendingClipboardHtml
        val isPaste = pendingHtml != null &&
            newTextFieldValue.text.length > textFieldValue.text.length
        val trigger: CommitTrigger? = when {
            isPaste -> CommitTrigger.Paste
            else -> classifyTextChange(newTextFieldValue)
        }
        val before = if (trigger != null) beginHistoryRecord() else null

        try {
            onTextFieldValueChangeInner(newTextFieldValue, isPaste, pendingHtml)
        } finally {
            if (trigger != null) finishHistoryRecord(trigger, before)
        }
    }

    private fun onTextFieldValueChangeInner(
        newTextFieldValue: TextFieldValue,
        isPaste: Boolean,
        pendingHtml: String?,
    ) {
        if (isPaste) {
            pendingClipboardHtml = null
            val position = selection.min
            // Suppress nested history captures during the remove+insert so the entire
            // paste is a single undo group attributable to the top-level trigger.
            val wasSuppressed = suppressHistoryRecording
            suppressHistoryRecording = true
            try {
                removeSelectedText()
                insertHtml(html = pendingHtml!!, position = position)
            } finally {
                suppressHistoryRecording = wasSuppressed
            }
            return
        }
        pendingClipboardHtml = null

        tempTextFieldValue = newTextFieldValue

        if (tempTextFieldValue.text.length > textFieldValue.text.length)
            handleAddingCharacters()
        else if (tempTextFieldValue.text.length < textFieldValue.text.length) {
            val newNewlineCount = tempTextFieldValue.text.count { it == '\n' }
            val oldNewlineCount = textFieldValue.text.count { it == '\n' }
            val isImeRevert = newNewlineCount > oldNewlineCount
                && !singleParagraphMode
                && richParagraphList.size > 1
                && textFieldValue.selection.collapsed

            // Selection replacement: old selection was a range and the new text fits the
            // pattern of "selection removed + optional replacement chars at selection start".
            // Split into two steps: remove the old selection, then add any replacement chars.
            val selMin = textFieldValue.selection.min
            val selMax = textFieldValue.selection.max
            val pureRemovalText = textFieldValue.text.substring(0, selMin) +
                textFieldValue.text.substring(selMax)
            val newTextStartsWithPrefix = tempTextFieldValue.text.length >= selMin &&
                tempTextFieldValue.text.substring(0, selMin) == textFieldValue.text.substring(0, selMin)
            val replacementCount = tempTextFieldValue.text.length - pureRemovalText.length
            val isSelectionReplacement = !textFieldValue.selection.collapsed &&
                !isImeRevert &&
                newTextStartsWithPrefix &&
                replacementCount >= 0 &&
                tempTextFieldValue.text.length >= selMin + replacementCount &&
                tempTextFieldValue.text.substring(selMin + replacementCount) ==
                    textFieldValue.text.substring(selMax)

            if (isImeRevert) {
                // IME revert: merge last paragraph back, let checkForParagraphs rebuild. See #640.
                val lastParagraph = richParagraphList.removeAt(richParagraphList.lastIndex)
                val precedingParagraph = richParagraphList.last()
                lastParagraph.updateChildrenParagraph(precedingParagraph)
                precedingParagraph.children.addAll(lastParagraph.children)
                forceCheckAllNewlines = true
            } else if (isSelectionReplacement) {
                // Step 1: remove the old selection as a pure deletion
                val actualNewTextFieldValue = tempTextFieldValue
                tempTextFieldValue = textFieldValue.copy(
                    text = pureRemovalText,
                    selection = TextRange(selMin),
                )
                handleRemovingCharacters()

                // Step 2: if there are replacement chars, add them
                if (replacementCount > 0) {
                    updateTextFieldValue()
                    tempTextFieldValue = actualNewTextFieldValue
                    if (actualNewTextFieldValue.text.length > textFieldValue.text.length) {
                        handleAddingCharacters()
                    }
                }
            } else {
                handleRemovingCharacters()
            }
        }
        else if (
            tempTextFieldValue.text == textFieldValue.text &&
            tempTextFieldValue.selection != textFieldValue.selection
        ) {
            val lastPressPosition = this.lastPressPosition
            if (lastPressPosition != null) {
                adjustSelection(lastPressPosition, newTextFieldValue.selection)
                return
            }
        }

        // Update text field value
        updateTextFieldValue()
    }

    /**
     * Handles updating the text field value and all the related states such as the [annotatedString] and [visualTransformation] to reflect the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun updateTextFieldValue(newTextFieldValue: TextFieldValue = tempTextFieldValue) {
        tempTextFieldValue = newTextFieldValue

        if (!singleParagraphMode) {
            // Check for paragraphs
            checkForParagraphs()
        }

        // Track the last non-collapsed selection for clipboard operations
        if (!textFieldValue.selection.collapsed) {
            lastNonCollapsedSelection = textFieldValue.selection
        }

        if (
            tempTextFieldValue.text == textFieldValue.text &&
            tempTextFieldValue.selection != textFieldValue.selection
        ) {
            // Pure selection change: normally we only reassign textFieldValue and skip
            // rebuilding annotatedString. But the annotatedString carries a
            // selection-dependent mask that drops background colors underneath the live
            // selection (see AnnotatedStringExt.append — prevents an opaque span
            // background from hiding the system selection highlight). If either the
            // previous or the new selection is non-collapsed, the mask set differs and
            // the cached annotatedString is stale — so force a rebuild. See #635.
            val maskAffected =
                !textFieldValue.selection.collapsed || !tempTextFieldValue.selection.collapsed
            if (maskAffected) {
                updateAnnotatedString(tempTextFieldValue)
            } else {
                textFieldValue = tempTextFieldValue
            }
        } else {
            // Update the annotatedString and the textFieldValue with the new values
            updateAnnotatedString(tempTextFieldValue)
        }

        // Clear un-applied styles
        toAddSpanStyle = SpanStyle()
        toRemoveSpanStyle = SpanStyle()
        toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class

        // Update current span style
        updateCurrentSpanStyle()

        // Update current paragraph style
        updateCurrentParagraphStyle()

        // Re-detect active trigger query after every edit / selection change
        refreshActiveTriggerQuery()

        // Clear [tempTextFieldValue]
        tempTextFieldValue = TextFieldValue()
    }

    private inner class HistoryHostImpl : RichTextHistoryHost {
        override fun captureState(timestampMs: Long): RichTextSnapshot =
            RichTextSnapshot.capture(
                paragraphs = richParagraphList,
                selection = textFieldValue.selection,
                composition = textFieldValue.composition,
                toAddSpanStyle = toAddSpanStyle,
                toAddRichSpanStyle = toAddRichSpanStyle,
                timestampMs = timestampMs,
            )

        override fun restoreState(snapshot: RichTextSnapshot) {
            restoreSnapshot(snapshot)
        }
    }

    private fun restoreSnapshot(snapshot: RichTextSnapshot) {
        suppressHistoryRecording = true
        try {
            val copied = snapshot.paragraphs.map { it.deepCopy() }

            // Delegate to the canonical rebuild path, which recomputes annotatedString
            // and textFieldValue.text honoring each paragraph's startText (list prefix,
            // etc.) and inter-paragraph separators. Hand-rolling this here previously
            // caused text-length mismatches crashing [updateAnnotatedString].
            updateRichParagraphList(copied)

            // updateRichParagraphList resets selection to a single caret; restore the
            // snapshot's selection, clamped to the rebuilt text.
            val textLen = textFieldValue.text.length
            val clampedSelection = TextRange(
                snapshot.selection.start.coerceIn(0, textLen),
                snapshot.selection.end.coerceIn(0, textLen),
            )
            textFieldValue = textFieldValue.copy(selection = clampedSelection)
            tempTextFieldValue = textFieldValue

            // Re-apply staged styles from the snapshot. updateRichParagraphList clears
            // these to defaults; we want the caret to remember the user's staged formatting.
            toAddSpanStyle = snapshot.toAddSpanStyle
            toAddRichSpanStyle = snapshot.toAddRichSpanStyle

            updateCurrentSpanStyle()
            updateCurrentParagraphStyle()
            refreshActiveTriggerQuery()

            tempTextFieldValue = TextFieldValue()
        } finally {
            suppressHistoryRecording = false
        }
    }

    /**
     * Captures the pre-mutation snapshot if the caller is an outer public-API entry.
     * Returns `null` if recording is suppressed or we are already inside a recording
     * call (nested mutation, should be attributed to the outer commit).
     */
    private fun beginHistoryRecord(): RichTextSnapshot? {
        if (suppressHistoryRecording || historyRecordingDepth > 0) return null
        historyRecordingDepth++
        return history.captureForCommit(timestampMs = currentMonotonicMs())
    }

    /**
     * Completes a recording started by [beginHistoryRecord]. If [before] is `null`
     * the call was nested / suppressed and nothing is committed.
     */
    private fun finishHistoryRecord(trigger: CommitTrigger, before: RichTextSnapshot?) {
        if (before == null) return
        historyRecordingDepth--
        history.onCommit(trigger, before)
        history.onAfterCommit(trigger)
    }

    private inline fun <T> recordHistory(
        trigger: CommitTrigger,
        enabled: Boolean = true,
        block: () -> T,
    ): T {
        // When [enabled] is false the mutation is known a priori to only touch
        // staged state (e.g. span styles bags) without changing the paragraph tree -
        // recording it would produce a phantom undo step that confuses users. We
        // still seal the pending coalesced typing group so the toggle acts as a
        // natural break between typing bursts, matching how most editors behave.
        val before = if (enabled) beginHistoryRecord() else null
        return try {
            block()
        } finally {
            if (before != null) {
                finishHistoryRecord(trigger, before)
            } else if (enabled.not() && !suppressHistoryRecording && historyRecordingDepth == 0) {
                history.sealPendingGroup()
            }
        }
    }

    private fun currentMonotonicMs(): Long =
        RichTextStateHistoryClockStart.elapsedNow().inWholeMilliseconds

    /**
     * Recompute [activeTriggerQuery] from the current text + caret + registered triggers.
     * Called from [updateTextFieldValue] after every edit.
     */
    private fun refreshActiveTriggerQuery() {
        if (_triggers.isEmpty()) {
            _activeTriggerQuery = null
            suppressedTriggerRange = null
            return
        }

        val selection = textFieldValue.selection
        if (!selection.collapsed) {
            _activeTriggerQuery = null
            return
        }

        // Clear suppression if the caret has left the suppressed range.
        val suppress = suppressedTriggerRange
        if (suppress != null) {
            val caret = selection.min
            val outside = caret < suppress.min || caret > suppress.max
            if (outside) {
                suppressedTriggerRange = null
            }
        }

        val caret = selection.min
        val text = textFieldValue.text

        // Guard against detection inside an existing atomic Token span - tokens
        // are atomic units and can't host a nested active trigger.
        val spanAtCaret = getRichSpanByTextIndex(caret - 1)
        if (spanAtCaret?.richSpanStyle is RichSpanStyle.Token) {
            _activeTriggerQuery = null
            return
        }

        _activeTriggerQuery = detectActiveTrigger(
            text = text,
            caretOffset = caret,
            triggers = _triggers,
            textLayoutResult = textLayoutResult,
            suppressedRange = suppressedTriggerRange,
        )
    }

    /**
     * Update the [annotatedString] to reflect the new changes on the [richParagraphList].
     * This method will update the [annotatedString] and the [textFieldValue] to reflect the new changes.
     * If no [newTextFieldValue] is passed, the [textFieldValue] will be used instead.
     *
     * @param newTextFieldValue the new text field value.
     * @see [textFieldValue]
     * @see [annotatedString]
     */
    internal fun updateAnnotatedString(newTextFieldValue: TextFieldValue = textFieldValue) {
        val newText =
            if (singleParagraphMode)
                newTextFieldValue.text
            else
                newTextFieldValue.text.replace('\n', ' ')

        val newStyledRichSpanList = mutableListOf<RichSpan>()

        usedInlineContentMapKeys.clear()

        annotatedString = buildAnnotatedString {
            var index = 0
            richParagraphList.fastForEachIndexed { i, richParagraph ->
                if (index > newText.length) {
                    richParagraphList.removeAt(i)
                    return@fastForEachIndexed
                }

                withStyle(richParagraph.paragraphStyle.merge(richParagraph.type.getStyle(config))) {
                    withStyle(richParagraph.getListMarkerSpanStyle(config.listMarkerStyleBehavior)) {
                        append(richParagraph.type.startText)
                    }
                    val richParagraphStartTextLength = richParagraph.type.startText.length
                    richParagraph.type.startRichSpan.textRange =
                        TextRange(index, index + richParagraphStartTextLength)
                    index += richParagraphStartTextLength
                    withStyle(RichSpanStyle.DefaultSpanStyle) {
                        index = append(
                            state = this@RichTextState,
                            richSpanList = richParagraph.children,
                            startIndex = index,
                            text = newText,
                            selection = newTextFieldValue.selection,
                            onStyledRichSpan = {
                                newStyledRichSpanList.add(it)
                            },
                        )

                        if (!singleParagraphMode) {
                            // Add empty space in the end of each paragraph to fix an issue with Compose TextField
                            // that makes that last char non-selectable when having multiple paragraphs
                            if (i != richParagraphList.lastIndex && index < newText.length) {
                                append(' ')
                                index++
                            }
                        }
                    }
                }
            }
        }

        inlineContentMap.keys.forEach { key ->
            if (key !in usedInlineContentMapKeys) {
                inlineContentMap.remove(key)
            }
        }

        styledRichSpanList.clear()
        val newTextLength = annotatedString.text.length
        textFieldValue = newTextFieldValue.copy(
            text = annotatedString.text,
            selection = TextRange(
                newTextFieldValue.selection.start.coerceIn(0, newTextLength),
                newTextFieldValue.selection.end.coerceIn(0, newTextLength),
            ),
        )
        visualTransformation = VisualTransformation { _ ->
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }
        styledRichSpanList.addAll(newStyledRichSpanList)
    }

    /**
     * Handles adding characters to the text field.
     * This method will update the [richParagraphList] to reflect the new changes.
     * This method will use the [tempTextFieldValue] to get the new characters.
     */
    private fun handleAddingCharacters() {
        val typedCharsCount = tempTextFieldValue.text.length - textFieldValue.text.length
        var startTypeIndex = textFieldValue.selection.min
        val typedText = tempTextFieldValue.text.substring(
            startIndex = startTypeIndex,
            endIndex = startTypeIndex + typedCharsCount,
        )
        val previousIndex = startTypeIndex - 1

        val candidateRichSpan = getOrCreateRichSpanByTextIndex(previousIndex)

        // Atomic spans (Image, Token, ...) must not absorb adjacent typing;
        // characters next to them go into a new sibling span instead. See #466.
        val activeRichSpan =
            if (candidateRichSpan?.richSpanStyle?.isAtomic == true)
                null
            else
                candidateRichSpan

        if (activeRichSpan != null) {
            val isAndroidSuggestion =
                activeRichSpan.isLastInParagraph &&
                        activeRichSpan.textRange.max == startTypeIndex &&
                        tempTextFieldValue.selection.max == startTypeIndex + typedCharsCount + 1

            val typedText =
                if (isAndroidSuggestion)
                    "$typedText "
                else
                    typedText

            if (isAndroidSuggestion) {
                val beforeText =
                    tempTextFieldValue.text.substring(0, startTypeIndex + typedCharsCount)

                val afterText =
                    tempTextFieldValue.text.substring(startTypeIndex + typedCharsCount)

                tempTextFieldValue = tempTextFieldValue.copy(
                    text = "$beforeText $afterText",
                )
            }

            if (startTypeIndex < activeRichSpan.textRange.min) {
                val indexDiff = activeRichSpan.textRange.min - startTypeIndex
                val beforeTypedText = tempTextFieldValue.text.substring(
                    startIndex = 0,
                    endIndex = startTypeIndex,
                )
                val paragraphStartText = tempTextFieldValue.text.substring(
                    startIndex = startTypeIndex + typedCharsCount,
                    endIndex = activeRichSpan.textRange.min + typedCharsCount,
                )
                val afterTypedText = tempTextFieldValue.text.substring(
                    startIndex = activeRichSpan.textRange.min + typedCharsCount,
                    endIndex = tempTextFieldValue.text.length,
                )
                val newTypedText = beforeTypedText + paragraphStartText + typedText + afterTypedText
                tempTextFieldValue = tempTextFieldValue.copy(
                    text = newTypedText,
                    selection = TextRange(
                        (tempTextFieldValue.selection.min + indexDiff).coerceAtLeast(0),
                        (tempTextFieldValue.selection.max + indexDiff).coerceAtMost(newTypedText.length),
                    ),
                )
            }

            startTypeIndex = max(startTypeIndex, activeRichSpan.textRange.min)
            val startIndex = max(0, startTypeIndex - activeRichSpan.textRange.min)
            val beforeText =
                if (activeRichSpan.text.isEmpty())
                    ""
                else
                    activeRichSpan.text.substring(0, startIndex)

            val afterText =
                if (activeRichSpan.text.isEmpty())
                    ""
                else
                    activeRichSpan.text.substring(startIndex)

            val activeRichSpanFullSpanStyle = activeRichSpan.fullSpanStyle
            val newSpanStyle =
                activeRichSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)
            val newRichSpanStyle =
                when {
                    toAddRichSpanStyle !is RichSpanStyle.Default ->
                        toAddRichSpanStyle

                    toRemoveRichSpanStyleKClass == activeRichSpan.richSpanStyle::class ->
                        RichSpanStyle.Default

                    else ->
                        activeRichSpan.richSpanStyle
                }

            val isToAddRemoveSpanStyleEmpty =
                toAddSpanStyle == SpanStyle() && toRemoveSpanStyle == SpanStyle()

            val isToAddRemoveRichSpanStyleEmpty =
                toAddRichSpanStyle is RichSpanStyle.Default && toRemoveRichSpanStyleKClass == RichSpanStyle.Default::class

            if (
                (isToAddRemoveSpanStyleEmpty && isToAddRemoveRichSpanStyleEmpty) ||
                (newSpanStyle == activeRichSpanFullSpanStyle && newRichSpanStyle::class == activeRichSpan.richSpanStyle::class)
            ) {
                activeRichSpan.text = beforeText + typedText + afterText

                checkListStart(richSpan = activeRichSpan)
            } else {
                handleUpdatingRichSpan(
                    richSpan = activeRichSpan,
                    beforeText = beforeText,
                    middleText = typedText,
                    afterText = afterText,
                    startIndex = startTypeIndex,
                    richSpanFullSpanStyle = activeRichSpanFullSpanStyle,
                    newSpanStyle = newSpanStyle,
                )
            }
        } else {
            if (richParagraphList.isEmpty()) {
                richParagraphList.add(RichParagraph())
            }

            val imageSibling =
                candidateRichSpan?.takeIf { it.richSpanStyle.isAtomic }
            val paragraph = imageSibling?.paragraph ?: richParagraphList.last()
            val newRichSpan = RichSpan(
                paragraph = paragraph,
                text = typedText,
                textRange = TextRange(startTypeIndex, startTypeIndex + typedText.length),
                spanStyle = toAddSpanStyle,
                richSpanStyle = toAddRichSpanStyle,
            )

            if (imageSibling != null) {
                // Insert on the correct side of the image placeholder so span
                // ordering matches the raw-text ordering when the tree is
                // re-serialized by updateAnnotatedString. See #466.
                val imageIndex = paragraph.children.indexOf(imageSibling)
                val insertIndex =
                    if (startTypeIndex <= imageSibling.textRange.min)
                        imageIndex
                    else
                        imageIndex + 1
                paragraph.children.add(insertIndex, newRichSpan)
            } else {
                paragraph.children.add(newRichSpan)
            }
        }
    }

    /**
     * Handles removing characters from the text field value.
     * This method will update the [richParagraphList] to reflect the new changes.
     * This method will use the [tempTextFieldValue] to get the removed characters.
     */
    private fun handleRemovingCharacters() {
        val removedCharsCount = textFieldValue.text.length - tempTextFieldValue.text.length

        val minRemoveIndex =
            tempTextFieldValue.selection.min
                .coerceAtLeast(0)

        val maxRemoveIndex =
            (minRemoveIndex + removedCharsCount)
                .coerceAtMost(textFieldValue.text.length)

        val removeRange = TextRange(minRemoveIndex, maxRemoveIndex)

        val minRichSpan = getRichSpanByTextIndex(textIndex = minRemoveIndex, true) ?: return
        val maxRichSpan = getRichSpanByTextIndex(textIndex = maxRemoveIndex - 1, true) ?: return

        // Check deleted paragraphs
        val minParagraphIndex = richParagraphList.indexOf(minRichSpan.paragraph)
        val maxParagraphIndex = richParagraphList.indexOf(maxRichSpan.paragraph)

        // Remove paragraphs between the min and max paragraphs
        if (minParagraphIndex < maxParagraphIndex - 1 && !singleParagraphMode) {
            richParagraphList.removeRange(minParagraphIndex + 1, maxParagraphIndex)
        }

        // Get the first non-empty child of the min paragraph
        val minFirstNonEmptyChild = minRichSpan.paragraph.getFirstNonEmptyChild()
        val minParagraphStartTextLength = minRichSpan.paragraph.type.startRichSpan.text.length
        val minParagraphFirstChildMinIndex =
            minFirstNonEmptyChild?.textRange?.min ?: minParagraphStartTextLength

        // Get the first non-empty child of the max paragraph
        val maxFirstNonEmptyChild = maxRichSpan.paragraph.getFirstNonEmptyChild()
        val maxParagraphStartTextLength = maxRichSpan.paragraph.type.startRichSpan.text.length
        val maxParagraphFirstChildMinIndex =
            maxFirstNonEmptyChild?.textRange?.min ?: maxParagraphStartTextLength

        // TODO:
        //  Check if we can remove this condition since we are already checking below
        //  if the paragraph needs to be removed
        if (minParagraphIndex == maxParagraphIndex && !singleParagraphMode) {
            if (
                (minFirstNonEmptyChild == null || minFirstNonEmptyChild.text.isEmpty()) &&
                minRichSpan.paragraph.type.startText.isEmpty()
            ) {
                // Remove the min paragraph if it's empty (and the max paragraph is the same)
                richParagraphList.removeAt(minParagraphIndex)
            }
        }

        // Handle Remove the min paragraph custom text
        if (minRemoveIndex < minParagraphFirstChildMinIndex) {
            if (minRichSpan.paragraph.type.startText.isEmpty() && minParagraphIndex != maxParagraphIndex) {
                minRichSpan.paragraph.children.clear()
                richParagraphList.remove(minRichSpan.paragraph)
            } else {
                handleRemoveMinParagraphStartText(
                    removeIndex = minRemoveIndex,
                    paragraphStartTextLength = minParagraphStartTextLength,
                    paragraphFirstChildMinIndex = minParagraphFirstChildMinIndex,
                )

                // Save the old paragraph type
                val minParagraphOldType = minRichSpan.paragraph.type

                // Set the paragraph type to DefaultParagraph
                minRichSpan.paragraph.type = DefaultParagraph()

                // Check if it's a list and handle level appropriately
                if (
                    maxRemoveIndex - minRemoveIndex == 1 &&
                    minParagraphOldType is ConfigurableListLevel &&
                    minParagraphOldType.level > 1
                ) {
                    // Decrease level instead of exiting list
                    minParagraphOldType.level -= 1
                    tempTextFieldValue = updateParagraphType(
                        paragraph = minRichSpan.paragraph,
                        newType = minParagraphOldType,
                        textFieldValue = tempTextFieldValue,
                    )
                }
            }
        }

        // Handle Remove the max paragraph custom text
        if (
            (minParagraphIndex == maxParagraphIndex || minRemoveIndex >= minParagraphFirstChildMinIndex) &&
            maxRemoveIndex < maxParagraphFirstChildMinIndex
        ) {
            handleRemoveMaxParagraphStartText(
                minRemoveIndex = minRemoveIndex,
                maxRemoveIndex = maxRemoveIndex,
                paragraphStartTextLength = maxParagraphStartTextLength,
                paragraphFirstChildMinIndex = maxParagraphFirstChildMinIndex,
            )

            tempTextFieldValue = adjustOrderedListsNumbers(
                startParagraphIndex = maxParagraphIndex + 1,
                startNumber = 1,
                textFieldValue = tempTextFieldValue,
            )
        } else if (
            minParagraphIndex != maxParagraphIndex &&
            minRemoveIndex < minParagraphFirstChildMinIndex &&
            maxRemoveIndex < maxParagraphFirstChildMinIndex &&
            maxParagraphStartTextLength > 0
        ) {
            // Cross-paragraph removal: both min and max paragraph prefixes are partially cut.
            // Leftover max prefix chars remain in tempTextFieldValue.text; preserve them by
            // prepending to the max paragraph's first non-empty child and demoting the
            // paragraph to DefaultParagraph so rendering aligns with the kept chars.
            val leftoverPrefixLength = maxParagraphFirstChildMinIndex - maxRemoveIndex
            if (leftoverPrefixLength > 0) {
                val leftoverChars = maxRichSpan.paragraph.type.startText
                    .takeLast(leftoverPrefixLength)
                val firstChild = maxRichSpan.paragraph.getFirstNonEmptyChild()
                if (firstChild != null) {
                    firstChild.text = leftoverChars + firstChild.text
                }
            }
            maxRichSpan.paragraph.type = DefaultParagraph()

            tempTextFieldValue = adjustOrderedListsNumbers(
                startParagraphIndex = maxParagraphIndex + 1,
                startNumber = 1,
                textFieldValue = tempTextFieldValue,
            )
        }

        // Remove spans from the max paragraph
        val isMaxParagraphEmpty =
            maxRichSpan.paragraph.removeTextRange(
                removeRange,
                maxParagraphFirstChildMinIndex
            ) == null

        if (!singleParagraphMode) {
            if (maxParagraphIndex != minParagraphIndex) {
                // Remove spans from the min paragraph
                val isMinParagraphEmpty =
                    minRichSpan.paragraph.removeTextRange(
                        removeRange,
                        minParagraphFirstChildMinIndex
                    ) == null

                if (isMaxParagraphEmpty) {
                    // Remove the max paragraph if it's empty
                    richParagraphList.remove(maxRichSpan.paragraph)
                }

                if (isMinParagraphEmpty) {
                    // Set the min paragraph type to the max paragraph type
                    // Since the max paragraph is going to take the min paragraph's place
//                    maxRichSpan.paragraph.type = minRichSpan.paragraph.type

                    // Remove the min paragraph if it's empty
                    richParagraphList.remove(minRichSpan.paragraph)
                }

                if (!isMinParagraphEmpty && !isMaxParagraphEmpty) {
                    // Merge the two paragraphs if they are not empty
                    mergeTwoRichParagraphs(
                        firstParagraph = minRichSpan.paragraph,
                        secondParagraph = maxRichSpan.paragraph,
                    )
                }
            }

            if (minRemoveIndex == minParagraphFirstChildMinIndex - minParagraphStartTextLength - 1) {
                if (minParagraphStartTextLength > 0) {
                    val beforeText = tempTextFieldValue.text.substring(
                        startIndex = 0,
                        endIndex = minRemoveIndex
                            .coerceAtMost(tempTextFieldValue.text.length),
                    )
                    val afterText =
                        if (minRemoveIndex + 1 > tempTextFieldValue.text.lastIndex)
                            ""
                        else
                            tempTextFieldValue.text.substring(
                                startIndex = minRemoveIndex + 1,
                            )

                    tempTextFieldValue = tempTextFieldValue.copy(
                        text = beforeText + afterText,
                        selection = TextRange(tempTextFieldValue.selection.min),
                    )
                }

                richParagraphList.getOrNull(minParagraphIndex - 1)?.let { previousParagraph ->
                    // Merge the two paragraphs if the line break is removed
                    mergeTwoRichParagraphs(
                        firstParagraph = previousParagraph,
                        secondParagraph = maxRichSpan.paragraph,
                    )
                }
            }
        }

        // Set current applied style to min rich span if the paragraph is empty
        if (
            config.preserveStyleOnEmptyLine &&
            minRichSpan.paragraph.isEmpty()
        ) {
            val minParagraphFirstRichSpan =
                minRichSpan.paragraph.getFirstNonEmptyChild() ?: run {
                    val richSpan =
                        RichSpan(
                            paragraph = minRichSpan.paragraph,
                            text = "",
                            textRange = TextRange(minRemoveIndex, minRemoveIndex),
                        )

                    minRichSpan.paragraph.children.add(richSpan)

                    richSpan
                }

            minParagraphFirstRichSpan.spanStyle = currentAppliedSpanStyle
            minParagraphFirstRichSpan.richSpanStyle = currentAppliedRichSpanStyle
        }

        checkOrderedListsNumbers(
            startParagraphIndex = minParagraphIndex - 1,
            endParagraphIndex = minParagraphIndex + 1,
        )
    }

    private fun handleRemoveMinParagraphStartText(
        removeIndex: Int,
        paragraphStartTextLength: Int,
        paragraphFirstChildMinIndex: Int,
    ) {
        if (removeIndex >= paragraphFirstChildMinIndex || paragraphStartTextLength <= 0)
            return

        val indexDiff = (paragraphStartTextLength - (paragraphFirstChildMinIndex - removeIndex))
            .coerceAtLeast(0)
        val beforeTextEndIndex =
            (paragraphFirstChildMinIndex - paragraphStartTextLength)
                .coerceAtMost(tempTextFieldValue.text.length)

        val beforeText =
            if (beforeTextEndIndex <= 0)
                ""
            else
                tempTextFieldValue.text.substring(
                    startIndex = 0,
                    endIndex = beforeTextEndIndex,
                )
        val afterText =
            if (tempTextFieldValue.text.length <= removeIndex)
                ""
            else
                tempTextFieldValue.text.substring(
                    startIndex = removeIndex,
                    endIndex = tempTextFieldValue.text.length,
                )
        val newText = beforeText + afterText
        val newSelection = TextRange(removeIndex - indexDiff)

        tempTextFieldValue = tempTextFieldValue.copy(
            text = newText,
            selection = newSelection,
        )
    }

    private fun handleRemoveMaxParagraphStartText(
        minRemoveIndex: Int,
        maxRemoveIndex: Int,
        paragraphStartTextLength: Int,
        paragraphFirstChildMinIndex: Int,
    ) {
        if (
            maxRemoveIndex < paragraphFirstChildMinIndex &&
//            maxRemoveIndex > paragraphFirstChildMinIndex - paragraphStartTextLength &&
            paragraphStartTextLength > 0
        ) {
            paragraphStartTextLength - (paragraphFirstChildMinIndex - maxRemoveIndex)

            val beforeText =
                if (minRemoveIndex <= 0)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = 0,
                        endIndex = minRemoveIndex,
                    )

            val afterTextStartIndex =
                minRemoveIndex + (paragraphFirstChildMinIndex - maxRemoveIndex)

            val afterText =
                if (tempTextFieldValue.text.length <= afterTextStartIndex)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = afterTextStartIndex,
                        endIndex = tempTextFieldValue.text.length,
                    )

            val newText = beforeText + afterText

            tempTextFieldValue = tempTextFieldValue.copy(
                text = newText,
            )
        }
    }

    private fun checkListStart(richSpan: RichSpan) {
        if (richSpan.paragraph.type !is DefaultParagraph)
            return

        if (!richSpan.isFirstInParagraph)
            return

        if (richSpan.text == "- " || richSpan.text == "* ") {
            richSpan.paragraph.type = UnorderedList(
                config = config,
            )
            richSpan.text = ""
        } else if (richSpan.text.matches(Regex("^\\d+\\. "))) {
            val dotIndex = richSpan.text.indexOf('.')
            if (dotIndex != -1) {
                val number = richSpan.text.substring(0, dotIndex).toIntOrNull() ?: 1
                richSpan.paragraph.type = OrderedList(
                    number = number,
                    config = config,
                )
                richSpan.text = ""
            }
        }
    }

    /**
     * Checks the ordered lists numbers and adjusts them if needed.
     *
     * @param startParagraphIndex the start paragraph index to start checking from.
     * @param startNumber the start number to start from.
     * @param textFieldValue the text field value to update.
     * @return the updated text field value.
     */
    private fun adjustOrderedListsNumbers(
        startParagraphIndex: Int,
        startNumber: Int,
        textFieldValue: TextFieldValue,
        initialLevelNumberMap: Map<Int, Int> = emptyMap(),
    ): TextFieldValue {
        var newTextFieldValue = textFieldValue
        // The map to store the list number of each list level, level -> number
        val levelNumberMap = mutableMapOf<Int, Int>()
        levelNumberMap.putAll(initialLevelNumberMap)

        // Update the paragraph type of the paragraphs after the new paragraph
        for (i in (startParagraphIndex)..(richParagraphList.lastIndex)) {
            val currentParagraph = richParagraphList[i]
            val currentParagraphType = currentParagraph.type

            if (currentParagraphType !is ConfigurableListLevel)
                break

            levelNumberMap.keys.toList().fastForEach { level ->
                if (level > currentParagraphType.level)
                    levelNumberMap.remove(level)
            }

            if (currentParagraphType is UnorderedList) {
                levelNumberMap[currentParagraphType.level] = 0
                continue
            }

            if (currentParagraphType !is OrderedList)
                break

            val currentNumber =
                if (i == startParagraphIndex)
                    startNumber
                else
                    levelNumberMap[currentParagraphType.level]
                        ?.plus(1)
                        ?: run {
                            if (levelNumberMap.containsKey(currentParagraphType.level - 1))
                                1
                            else
                                currentParagraphType.number
                        }

            levelNumberMap[currentParagraphType.level] = currentNumber

            newTextFieldValue = updateParagraphType(
                paragraph = currentParagraph,
                newType = OrderedList(
                    number = currentNumber,
                    config = config,
                    startTextWidth = currentParagraphType.startTextWidth,
                    initialLevel = currentParagraphType.level
                ),
                textFieldValue = newTextFieldValue,
            )
        }

        return newTextFieldValue
    }

    private fun checkOrderedListsNumbers(
        startParagraphIndex: Int,
        endParagraphIndex: Int,
    ) {
        // The map to store the list number of each list level, level -> number
        val levelNumberMap = mutableMapOf<Int, Int>()
        val startParagraph = richParagraphList.getOrNull(startParagraphIndex)
        val startParagraphType = startParagraph?.type
        if (startParagraphType is OrderedList)
            levelNumberMap[startParagraphType.level] = startParagraphType.number

        if (startParagraphIndex == -1)
            levelNumberMap[1] = 0

        // Update the paragraph type of the paragraphs after the new paragraph
        for (i in (startParagraphIndex + 1)..richParagraphList.lastIndex) {
            val currentParagraph = richParagraphList[i]
            val currentParagraphType = currentParagraph.type

            if (currentParagraphType is ConfigurableListLevel) {
                // Clear the completed list levels
                levelNumberMap.filterKeys { level ->
                    level <= currentParagraphType.level
                }
            } else {
                // Clear the map if the current paragraph is not a list
                levelNumberMap.clear()
                levelNumberMap[1] = 0
            }

            // Remove current list level from map if the current paragraph is an unordered list
            if (currentParagraphType is UnorderedList)
                levelNumberMap.remove(currentParagraphType.level)

            if (currentParagraphType is OrderedList) {
                val number =
                    levelNumberMap[currentParagraphType.level]
                        ?.plus(1)
                        ?: currentParagraphType.number

                levelNumberMap[currentParagraphType.level] = number

                tempTextFieldValue = updateParagraphType(
                    paragraph = currentParagraph,
                    newType = OrderedList(
                        number = number,
                        config = config,
                        startTextWidth = currentParagraphType.startTextWidth,
                        initialLevel = currentParagraphType.level
                    ),
                    textFieldValue = tempTextFieldValue,
                )
            }

            if (
                currentParagraphType !is ConfigurableListLevel ||
                (currentParagraphType is UnorderedList && currentParagraphType.level == 1)
            ) {
                // Break if we reach the end paragraph index
                if (i >= endParagraphIndex)
                    break
            }
        }
    }

    private fun checkForParagraphs() {
        var index = tempTextFieldValue.text.lastIndex

        // Count newlines vs paragraph breaks to detect unprocessed newlines.
        // This handles the case where IME sends a text update that removes our
        // paragraph prefix (e.g. "2. ") but keeps the newline, the newline
        // position ends up before the old selection, so the normal threshold
        // would skip it. See #640.
        // Lower the threshold to scan all newlines when:
        // - forceCheckAllNewlines: set by IME revert detection in onTextFieldValueChange
        // - There's a single paragraph but the text has newlines: autocorrect shortened
        //   text + added newline in one onValueChange call (the newline is before the
        //   old cursor, so the normal threshold would skip it). See #640.
        val actualNewlines = tempTextFieldValue.text.count { it == '\n' }
        val breakThreshold =
            if (forceCheckAllNewlines || (actualNewlines > 0 && richParagraphList.size == 1)) 0
            else textFieldValue.selection.min
        forceCheckAllNewlines = false

        while (true) {
            // Search for the next paragraph
            index = tempTextFieldValue.text.lastIndexOf('\n', index)

            // If there are no more paragraphs, break
            if (index < breakThreshold) break

            // Get the rich span style at the index to split it between two paragraphs
            var richSpan = getRichSpanByTextIndex(index)

            // If the newline is at the end of the text (past all spans) during an IME revert
            // rebuild, use the last span of the last paragraph. See #640.
            if (richSpan == null && index == tempTextFieldValue.text.lastIndex && breakThreshold == 0) {
                richSpan = richParagraphList.lastOrNull()?.getLastNonEmptyChild()
            }

            // If there is no rich span style at the index, continue (this should not happen)
            if (richSpan == null) {
                index--
                continue
            }

            // Get the paragraph style index of the rich span style
            val paragraphIndex = richParagraphList.indexOf(richSpan.paragraph)
            // If the paragraph index is -1, continue (this should not happen)
            if (paragraphIndex == -1) {
                index--
                continue
            }

            // Make sure the index is not less than the minimum text range of the rich span style
            // This is to make sure that the index is not in paragraph custom start text
            val sliceIndex = max(index, richSpan.textRange.min)

            // Create a new paragraph style
            val newParagraph = richSpan.paragraph.slice(
                startIndex = sliceIndex,
                richSpan = richSpan,
                removeSliceIndex = true,
            )

            // If the new paragraph is empty apply style depending on the config
            if (tempTextFieldValue.selection.collapsed && newParagraph.isEmpty()) {
                val newParagraphFirstRichSpan = newParagraph.getFirstNonEmptyChild()

                val isSelectionAtNewRichSpan =
                    newParagraphFirstRichSpan?.textRange?.min == tempTextFieldValue.selection.min - 1

                // Check if the cursor is at the new paragraph and if it's an empty list item
                if (
                    config.exitListOnEmptyItem &&
                    isSelectionAtNewRichSpan &&
                    richSpan.paragraph.isEmpty() &&
                    richSpan.paragraph.type is ConfigurableListLevel
                ) {
                    // Exit list by removing list formatting
                    tempTextFieldValue = updateParagraphType(
                        paragraph = richSpan.paragraph,
                        newType = DefaultParagraph(),
                        textFieldValue = tempTextFieldValue,
                    )
                    newParagraphFirstRichSpan?.spanStyle = SpanStyle()
                    newParagraphFirstRichSpan?.richSpanStyle = RichSpanStyle.Default

                    // Ignore adding the new paragraph
                    index--
                    continue
                } else if (
                    (!config.preserveStyleOnEmptyLine || richSpan.paragraph.isEmpty()) &&
                    isSelectionAtNewRichSpan
                ) {
                    newParagraphFirstRichSpan?.spanStyle = SpanStyle()
                    newParagraphFirstRichSpan?.richSpanStyle = RichSpanStyle.Default
                } else if (
                    config.preserveStyleOnEmptyLine &&
                    isSelectionAtNewRichSpan
                ) {
                    newParagraphFirstRichSpan?.spanStyle = currentSpanStyle
                    newParagraphFirstRichSpan?.richSpanStyle = currentRichSpanStyle
                }
            }

            // Get the text before and after the slice index
            val beforeText = tempTextFieldValue.text.substring(0, sliceIndex + 1)
            val afterText = tempTextFieldValue.text.substring(sliceIndex + 1)

            // Update the text field value to include the new paragraph custom start text
            tempTextFieldValue = tempTextFieldValue.copy(
                text = beforeText + newParagraph.type.startText + afterText,
                selection = TextRange(
                    start = tempTextFieldValue.selection.start + newParagraph.type.startText.length,
                    end = tempTextFieldValue.selection.end + newParagraph.type.startText.length,
                ),
            )

            // Add the new paragraph
            richParagraphList.add(paragraphIndex + 1, newParagraph)

            // Update the paragraph type of the paragraphs after the new paragraph
            val newParagraphType = newParagraph.type

            if (newParagraphType is OrderedList) {
                tempTextFieldValue = adjustOrderedListsNumbers(
                    startParagraphIndex = paragraphIndex + 1,
                    startNumber = newParagraphType.number,
                    textFieldValue = tempTextFieldValue,
                )
            }

            // Remove one from the index to continue searching for paragraphs
            index--
        }
    }

    /**
     * Handles adding or removing the style in [toAddSpanStyle] and [toRemoveSpanStyle] from the selected text.
     */
    private fun applyRichSpanStyleToSelectedText() {
        applyRichSpanStyleToTextRange(selection)
    }

    /**
     * Handles adding or removing the style in [toAddSpanStyle] and [toRemoveSpanStyle] from a given [TextRange].
     *
     * @param textRange The [TextRange] to apply the styles to.
     */
    private fun applyRichSpanStyleToTextRange(
        textRange: TextRange
    ) {
        // Get the rich span list of the selected text
        val selectedRichSpanList = getRichSpanListByTextRange(textRange)

        val startSelectionIndex = textRange.min
        val endSelectionIndex = textRange.max

        // Loop through the rich span list
        for (i in selectedRichSpanList.lastIndex downTo 0) {
            val richSpan = selectedRichSpanList[i]

            // Get the text before, during, and after the selected text
            val beforeText =
                if (startSelectionIndex in richSpan.textRange)
                    richSpan.text.substring(0, startSelectionIndex - richSpan.textRange.start)
                else
                    ""

            val middleText =
                richSpan.text.substring(
                    maxOf(startSelectionIndex - richSpan.textRange.start, 0),
                    minOf(endSelectionIndex - richSpan.textRange.start, richSpan.text.length)
                )

            val afterText =
                if (endSelectionIndex - 1 in richSpan.textRange)
                    richSpan.text.substring(endSelectionIndex - richSpan.textRange.start)
                else
                    ""

            val richSpanFullSpanStyle = richSpan.fullSpanStyle
            val newSpanStyle =
                richSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

            val startApplyStyleIndex = maxOf(startSelectionIndex, richSpan.textRange.start)

            handleUpdatingRichSpan(
                richSpan = richSpan,
                startIndex = startApplyStyleIndex,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                newSpanStyle = newSpanStyle,
            )
        }

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Apply [toAddSpanStyle] and [toRemoveSpanStyle] to a [RichSpan].
     *
     * @param richSpan The [RichSpan] to apply the styles to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     * @param richSpanFullSpanStyle The [SpanStyle] of the [RichSpan].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpan].
     */
    private fun handleUpdatingRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanFullSpanStyle: SpanStyle = richSpan.fullSpanStyle,
        newSpanStyle: SpanStyle = richSpanFullSpanStyle.customMerge(toAddSpanStyle)
            .unmerge(toRemoveSpanStyle),
        newRichSpanStyle: RichSpanStyle =
            when {
                toAddRichSpanStyle !is RichSpanStyle.Default ->
                    toAddRichSpanStyle

                toRemoveRichSpanStyleKClass == richSpan.richSpanStyle::class ->
                    RichSpanStyle.Default

                else ->
                    richSpan.richSpanStyle
            },
    ) {
        if (richSpanFullSpanStyle == newSpanStyle && newRichSpanStyle::class == richSpan.richSpanStyle::class) return

        if (
            (toRemoveSpanStyle == SpanStyle() || !richSpanFullSpanStyle.isSpecifiedFieldsEquals(
                toRemoveSpanStyle
            )) &&
            (toRemoveRichSpanStyleKClass == RichSpanStyle.Default::class || newRichSpanStyle::class == richSpan.richSpanStyle::class)
        ) {
            applyStyleToRichSpan(
                richSpan = richSpan,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
            )
        } else {
            handleRemovingStyleFromRichSpan(
                richSpan = richSpan,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
                richSpanFullSpanStyle = richSpanFullSpanStyle,
                newSpanStyle = newSpanStyle,
                newRichSpanStyle = newRichSpanStyle,
            )
        }
    }

    /**
     * Handles applying a new [SpanStyle] and a new [RichSpanStyle] to a [RichSpan].
     *
     * @param richSpan The [RichSpan] to apply the new [SpanStyle] to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     */
    private fun applyStyleToRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
    ) {
        val fullSpanStyle = richSpan.fullSpanStyle

        // Simplify the richSpan tree if possible, by avoiding creating a new RichSpan.
        if (
            beforeText.isEmpty() &&
            afterText.isEmpty() &&
            richSpan.children.isEmpty()
        ) {
            richSpan.text = middleText
            richSpan.spanStyle = richSpan.spanStyle
                .copy(textDecoration = fullSpanStyle.textDecoration)
                .customMerge(toAddSpanStyle)
            richSpan.richSpanStyle =
                if (toAddRichSpanStyle !is RichSpanStyle.Default)
                    toAddRichSpanStyle
                else
                    richSpan.richSpanStyle

            return
        }

        richSpan.text = beforeText
        val newRichSpan =
            RichSpan(
                paragraph = richSpan.paragraph,
                parent = richSpan,
                text = middleText,
                textRange = TextRange(
                    startIndex,
                    startIndex + middleText.length
                ),
                spanStyle = SpanStyle(textDecoration = fullSpanStyle.textDecoration).customMerge(
                    toAddSpanStyle
                ),
                richSpanStyle =
                    if (toAddRichSpanStyle !is RichSpanStyle.Default)
                        toAddRichSpanStyle
                    else
                        richSpan.richSpanStyle,
            )

        val parent = richSpan.parent
        val index =
            parent?.children?.indexOf(richSpan) ?: richSpan.paragraph.children.indexOf(richSpan)
        var isRemoved = false

        val isRichSpanStylingEmpty =
            richSpan.spanStyle == SpanStyle() && richSpan.richSpanStyle is RichSpanStyle.Default

        if (middleText.isNotEmpty()) {
            if (
                (isRichSpanStylingEmpty || richSpan.text.isEmpty()) &&
                index != -1 &&
                richSpan.children.isEmpty()
            ) {
                newRichSpan.parent = richSpan.parent

                if (!isRichSpanStylingEmpty) {
                    newRichSpan.spanStyle = richSpan.spanStyle.customMerge(newRichSpan.spanStyle)
                    if (richSpan.richSpanStyle !is RichSpanStyle.Default && newRichSpan.richSpanStyle is RichSpanStyle.Default)
                        newRichSpan.richSpanStyle = richSpan.richSpanStyle
                }

                if (parent != null) {
                    parent.children.add(index + 1, newRichSpan)

                    if (richSpan.text.isEmpty()) {
                        parent.children.removeAt(index)
                        isRemoved = true
                    }
                } else {
                    richSpan.paragraph.children.add(index + 1, newRichSpan)

                    if (richSpan.text.isEmpty()) {
                        richSpan.paragraph.children.removeAt(index)
                        isRemoved = true
                    }
                }
            } else {
                richSpan.children.add(0, newRichSpan)
                newRichSpan.parent = richSpan
            }
        }

        if (afterText.isNotEmpty()) {
            val afterRichSpan =
                RichSpan(
                    paragraph = richSpan.paragraph,
                    parent = richSpan,
                    text = afterText,
                    textRange = TextRange(
                        startIndex + middleText.length,
                        startIndex + middleText.length + afterText.length
                    ),
                )

            if (
                (isRichSpanStylingEmpty || richSpan.text.isEmpty()) &&
                index != -1 &&
                richSpan.children.isEmpty()
            ) {
                afterRichSpan.parent = richSpan.parent

                if (!isRichSpanStylingEmpty) {
                    afterRichSpan.spanStyle =
                        richSpan.spanStyle.customMerge(afterRichSpan.spanStyle)
                    if (richSpan.richSpanStyle !is RichSpanStyle.Default && afterRichSpan.richSpanStyle is RichSpanStyle.Default)
                        afterRichSpan.richSpanStyle = richSpan.richSpanStyle
                }

                val addIndex =
                    if (isRemoved || middleText.isEmpty())
                        index + 1
                    else
                        index + 2

                if (parent != null) {
                    parent.children.add(addIndex, afterRichSpan)

                    if (richSpan.text.isEmpty() && !isRemoved)
                        parent.children.removeAt(index)
                } else {
                    richSpan.paragraph.children.add(addIndex, afterRichSpan)

                    if (richSpan.text.isEmpty() && !isRemoved)
                        richSpan.paragraph.children.removeAt(index)
                }
            } else {
                richSpan.children.add(1, afterRichSpan)
                afterRichSpan.parent = richSpan
            }
        } else {
            val firstRichSpan = richSpan.children.firstOrNull()
            val secondRichSpan = richSpan.children.getOrNull(1)

            if (
                firstRichSpan != null &&
                secondRichSpan != null &&
                firstRichSpan.spanStyle == secondRichSpan.spanStyle &&
                firstRichSpan.richSpanStyle == secondRichSpan.richSpanStyle
            ) {
                firstRichSpan.text += secondRichSpan.text
                firstRichSpan.children.addAll(secondRichSpan.children)
                richSpan.children.removeAt(1)
            }

            if (
                firstRichSpan != null &&
                richSpan.text.isEmpty() &&
                richSpan.children.size == 1
            ) {
                richSpan.text = firstRichSpan.text
                richSpan.spanStyle = richSpan.spanStyle.customMerge(firstRichSpan.spanStyle)
                richSpan.children.clear()
                richSpan.children.addAll(firstRichSpan.children)
            }
        }
    }

    /**
     * Handles removing a [SpanStyle] from a [RichSpan].
     *
     * @param richSpan The [RichSpan] to remove the [SpanStyle] from.
     * @param beforeText The text before removing the styles.
     * @param middleText The text to remove the styles from.
     * @param afterText The text after removing the styles.
     * @param startIndex The start index of the text to remove the styles from.
     * @param richSpanFullSpanStyle The [SpanStyle] of the [RichSpan].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpan].
     * @param newRichSpanStyle The new [RichSpanStyle] to apply to the [RichSpan].
     */
    private fun handleRemovingStyleFromRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanFullSpanStyle: SpanStyle,
        newSpanStyle: SpanStyle,
        newRichSpanStyle: RichSpanStyle,
    ) {
        richSpan.text = beforeText
        val parentRichSpan = richSpan.getClosestRichSpan(newSpanStyle, newRichSpanStyle)
        val newRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = parentRichSpan,
            text = middleText,
            textRange = TextRange(
                startIndex,
                startIndex + middleText.length
            ),
            spanStyle = newSpanStyle.unmerge(parentRichSpan?.spanStyle),
            richSpanStyle = newRichSpanStyle,
        )
        val afterRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = parentRichSpan,
            text = afterText,
            textRange = TextRange(
                startIndex + middleText.length,
                startIndex + middleText.length + afterText.length
            ),
            spanStyle = richSpanFullSpanStyle,
            richSpanStyle = richSpan.richSpanStyle,
        )

        val toShiftRichSpanList: MutableList<RichSpan> = mutableListOf()
        var previousRichSpan: RichSpan?
        var currentRichSpan: RichSpan? = richSpan

        toShiftRichSpanList.add(newRichSpan)
        if (afterRichSpan.text.isNotEmpty())
            toShiftRichSpanList.add(afterRichSpan)

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan?.parent

            if (currentRichSpan == null || currentRichSpan == parentRichSpan) {
                break
            } else {
                val index = currentRichSpan.children.indexOf(previousRichSpan)
                if (index in 0 until currentRichSpan.children.lastIndex) {
                    val currentRichSpanFullSpanStyle = currentRichSpan.fullSpanStyle
                    ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                        val childRichSpan = currentRichSpan.children[it]

                        // Ignore shifting empty RichSpan.
                        if (childRichSpan.text.isEmpty() && childRichSpan.children.isEmpty()) {
                            return@forEach
                        }

                        // Merge RichSpan span style with parent RichSpan span style.
                        childRichSpan.spanStyle =
                            currentRichSpanFullSpanStyle.merge(childRichSpan.spanStyle)

                        // Lookup for RichSpan with the same span style and merge them to optimize the RichSpan tree.
                        val lastChild = toShiftRichSpanList.lastOrNull()
                        if (lastChild != null && lastChild.spanStyle == childRichSpan.spanStyle) {
                            if (lastChild.children.isEmpty()) {
                                lastChild.text += childRichSpan.text
                                lastChild.children.addAll(childRichSpan.children)
                            } else {
                                lastChild.children.add(childRichSpan)
                                childRichSpan.parent = lastChild
                                childRichSpan.spanStyle = RichSpanStyle.DefaultSpanStyle
                                for (i in childRichSpan.children.lastIndex downTo 0) {
                                    val child = childRichSpan.children[i]
                                    child.parent = lastChild
                                    childRichSpan.children.removeAt(i)
                                    lastChild.children.add(child)
                                }
                            }
                        } else {
                            childRichSpan.parent = parentRichSpan
                            toShiftRichSpanList.add(childRichSpan)
                        }
                    }

                    // Remove shifted RichSpan from parent RichSpan.
                    currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)

                    // Remove empty RichSpan.
                    if (previousRichSpan?.isEmpty() == true) {
                        currentRichSpan.children.removeAt(index)
                    }
                }
            }
        }

        if (parentRichSpan == null || currentRichSpan == null) {
            val index = richSpan.paragraph.children.indexOf(previousRichSpan)
            if (index in 0..richSpan.paragraph.children.lastIndex) {
                richSpan.paragraph.children.addAll(
                    index + 1,
                    toShiftRichSpanList
                )
            }

            // Remove empty RichSpan.
            if (previousRichSpan?.isEmpty() == true) {
                richSpan.paragraph.children.removeAt(index)
            }
        } else {
            val index = parentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0..parentRichSpan.children.lastIndex) {
                parentRichSpan.children.addAll(
                    index + 1,
                    toShiftRichSpanList
                )
            }

            // Remove empty RichSpan.
            if (previousRichSpan?.isEmpty() == true) {
                parentRichSpan.children.removeAt(index)
            }
        }

        if (richSpan.text.isEmpty() && richSpan.children.isEmpty()) {
            val parent = richSpan.parent
            if (parent != null) {
                parent.children.remove(richSpan)
            } else {
                richSpan.paragraph.children.remove(richSpan)
            }
        }
    }

    private fun addRichSpanAtPosition(
        vararg richSpan: RichSpan,
        index: Int,
    ) {
        val previousIndex = index - 1

        val activeRichSpan = getRichSpanByTextIndex(previousIndex)

        // If there is no active rich span, add the rich span to the last paragraph
        if (activeRichSpan == null) {
            richParagraphList.last().children.addAll(richSpan)
            return
        }

        val startIndex = max(0, index - activeRichSpan.textRange.min)
        val beforeText = activeRichSpan.text.substring(0, startIndex)
        val afterText = activeRichSpan.text.substring(startIndex)

        // Simplify the richSpan tree if possible, by avoiding creating a new RichSpan.
        if (
            beforeText.isEmpty() &&
            afterText.isEmpty() &&
            activeRichSpan.children.isEmpty() &&
            richSpan.size == 1
        ) {
            activeRichSpan.text = richSpan.first().text
            activeRichSpan.richSpanStyle = richSpan.first().richSpanStyle
            return
        }

        activeRichSpan.text = beforeText

        var addedTextLength = 0
        for (i in richSpan.lastIndex downTo 0) {
            val newRichSpan = richSpan[i]
            newRichSpan.paragraph = activeRichSpan.paragraph
            newRichSpan.parent = activeRichSpan
            activeRichSpan.children.add(
                0,
                newRichSpan
            )
            addedTextLength += newRichSpan.text.length
        }
        if (afterText.isNotEmpty()) {
            activeRichSpan.children.add(
                1,
                RichSpan(
                    paragraph = activeRichSpan.paragraph,
                    parent = activeRichSpan,
                    text = afterText,
                    textRange = TextRange(
                        index + addedTextLength,
                        index + addedTextLength + afterText.length
                    ),
                )
            )
        } else {
            val firstRichSpan = activeRichSpan.children.firstOrNull()
            val secondRichSpan = activeRichSpan.children.getOrNull(1)

            if (
                firstRichSpan != null &&
                secondRichSpan != null &&
                firstRichSpan.spanStyle == secondRichSpan.spanStyle
            ) {
                firstRichSpan.text += secondRichSpan.text
                firstRichSpan.children.addAll(secondRichSpan.children)
                activeRichSpan.children.removeAt(1)
            }

            if (
                firstRichSpan != null &&
                activeRichSpan.text.isEmpty() &&
                activeRichSpan.children.size == 1 &&
                richSpan.size == 1
            ) {
                activeRichSpan.text = firstRichSpan.text
                activeRichSpan.spanStyle =
                    richSpan.first().spanStyle.customMerge(firstRichSpan.spanStyle)
                activeRichSpan.children.clear()
                activeRichSpan.children.addAll(firstRichSpan.children)
            }
        }
    }

    private fun getToShiftRichSpanList(
        startRichSpan: RichSpan,
        endRichSpan: RichSpan?,
    ): List<RichSpan> {
        val toShiftRichSpanList: MutableList<RichSpan> = mutableListOf()
        var previousRichSpan: RichSpan?
        var currentRichSpan: RichSpan? = startRichSpan

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan?.parent

            if (currentRichSpan == null || currentRichSpan == endRichSpan) {
                break
            } else {
                val index = currentRichSpan.children.indexOf(previousRichSpan)
                if (index in 0 until currentRichSpan.children.lastIndex) {
                    ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                        val richSpan = currentRichSpan.children[it]
                        richSpan.spanStyle = richSpan.fullSpanStyle
                        richSpan.parent = endRichSpan
                        toShiftRichSpanList.add(richSpan)
                    }
                    currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
                }
            }
        }

        return toShiftRichSpanList
    }

    /**
     * Slice [RichParagraph] by [startIndex] and [richSpan] that contains [startIndex].
     * The passed [RichParagraph] will be modified, containing only the text before [startIndex].
     * And the new [RichParagraph] will be returned, containing the text after [startIndex].
     *
     * @param startIndex The start index of the slice.
     * @param richSpan The [RichSpan] that contains [startIndex].
     * @return The new [RichParagraph].
     */
    private fun RichParagraph.slice(
        startIndex: Int,
        richSpan: RichSpan,
        removeSliceIndex: Boolean,
    ): RichParagraph {
        val newRichParagraph = RichParagraph(
            paragraphStyle = paragraphStyle,
            type = type.getNextParagraphType(),
        )

        var previousRichSpan: RichSpan
        var currentRichSpan: RichSpan = richSpan

        val textStartIndex =
            if (startIndex == type.startRichSpan.textRange.min)
                startIndex - richSpan.textRange.min + type.startRichSpan.text.length
            else
                startIndex - richSpan.textRange.min

        newRichParagraph.type.startRichSpan.paragraph = newRichParagraph
        newRichParagraph.type.startRichSpan.textRange = TextRange(
            0,
            newRichParagraph.type.startRichSpan.text.length
        )

        val beforeText =
            richSpan.text.substring(
                startIndex = 0,
                endIndex = textStartIndex
                    .coerceIn(0, richSpan.text.length)
            )
        val afterTextStartIndex =
            if (removeSliceIndex)
                textStartIndex + 1
            else
                textStartIndex
        val afterText =
            richSpan.text.substring(
                startIndex = afterTextStartIndex
                    .coerceIn(0, richSpan.text.length)
            )

        richSpan.text = beforeText
        richSpan.textRange = TextRange(
            richSpan.textRange.min,
            richSpan.textRange.min + beforeText.length
        )

        // We don't copy the current rich span style to the new rich span
        val newRichSpan = RichSpan(
            paragraph = newRichParagraph,
            parent = null,
            text = afterText,
            textRange = TextRange(
                startIndex,
                startIndex + afterText.length
            ),
            spanStyle = richSpan.fullSpanStyle,
        )

        newRichParagraph.children.add(newRichSpan)

        for (i in richSpan.children.lastIndex downTo 0) {
            val childRichSpan = richSpan.children[i]
            richSpan.children.removeAt(i)
            childRichSpan.parent = newRichSpan
            childRichSpan.paragraph = newRichParagraph
            newRichSpan.children.add(childRichSpan)
        }

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan.parent ?: break

            val index = currentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0 until currentRichSpan.children.lastIndex) {
                ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                    val childRichSpan = currentRichSpan.children[it]
                    childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                    childRichSpan.parent = null
                    childRichSpan.paragraph = newRichParagraph
                    newRichParagraph.children.add(childRichSpan)
                }
                currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
            }
        }

        val index = richSpan.paragraph.children.indexOf(previousRichSpan)
        if (index in 0 until richSpan.paragraph.children.lastIndex) {
            ((index + 1)..richSpan.paragraph.children.lastIndex).forEach {
                val childRichSpan = richSpan.paragraph.children[it]
                childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                childRichSpan.parent = null
                childRichSpan.paragraph = newRichParagraph
                newRichParagraph.children.add(childRichSpan)
            }
            richSpan.paragraph.children.removeRange(index + 1, richSpan.paragraph.children.size)
        }

        return newRichParagraph
    }

    /**
     * Slice [RichSpan] by [startIndex] and [richSpan] that contains [startIndex].
     * The passed [RichSpan] will be modified, containing only the text before [startIndex].
     * And the new [RichSpan] will be returned, containing the text after [startIndex].
     *
     * @param startIndex The start index of the slice.
     * @param richSpan The [RichSpan] that contains [startIndex].
     * @return The new [RichSpan].
     */
    private fun RichSpan.slice(
        startIndex: Int,
        richSpan: RichSpan,
    ): RichSpan {
        val newRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
        )

        var previousRichSpan: RichSpan
        var currentRichSpan: RichSpan = richSpan

        val textStartIndex = startIndex - richSpan.textRange.min
        val beforeText =
            if (textStartIndex > 0) richSpan.text.substring(0, textStartIndex) else "" // + ' '
        val afterText = richSpan.text.substring(textStartIndex + 1)

        richSpan.text = beforeText
        richSpan.textRange = TextRange(
            richSpan.textRange.min,
            richSpan.textRange.min + beforeText.length
        )

        val afterRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = newRichSpan,
            text = afterText,
            textRange = TextRange(
                startIndex,
                startIndex + afterText.length
            ),
            spanStyle = richSpan.fullSpanStyle,
        )

        newRichSpan.children.add(afterRichSpan)

        for (i in richSpan.children.lastIndex downTo 0) {
            val childRichSpan = richSpan.children[i]
            richSpan.children.removeAt(i)
            childRichSpan.parent = afterRichSpan
            afterRichSpan.children.add(childRichSpan)
        }

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan.parent ?: break

            val index = currentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0 until currentRichSpan.children.lastIndex) {
                ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                    val childRichSpan = currentRichSpan.children[it]
                    childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                    childRichSpan.parent = null
                    newRichSpan.children.add(childRichSpan)
                }
                currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
            }
        }

        val index = richSpan.paragraph.children.indexOf(previousRichSpan)
        if (index in 0 until richSpan.paragraph.children.lastIndex) {
            ((index + 1)..richSpan.paragraph.children.lastIndex).forEach {
                val childRichSpan = richSpan.paragraph.children[it]
                childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                childRichSpan.parent = null
                newRichSpan.children.add(childRichSpan)
            }
            richSpan.paragraph.children.removeRange(index + 1, richSpan.paragraph.children.size)
        }

        return newRichSpan
    }

    /**
     * Merges two [RichParagraph]s into one.
     * The [firstParagraph] will be modified, containing the text of both [firstParagraph] and [secondParagraph].
     * And the [secondParagraph] will be removed.
     *
     * @param firstParagraph The first [RichParagraph].
     * @param secondParagraph The second [RichParagraph].
     */
    private fun mergeTwoRichParagraphs(
        firstParagraph: RichParagraph,
        secondParagraph: RichParagraph,
    ) {
        // Update the children paragraph of the second paragraph to the first paragraph.
        secondParagraph.updateChildrenParagraph(firstParagraph)

        // Add the children of the second paragraph to the first paragraph.
        firstParagraph.children.addAll(secondParagraph.children)

        // Remove the second paragraph from the rich paragraph list.
        richParagraphList.remove(secondParagraph)
    }

    /**
     * Updates the [currentAppliedSpanStyle] to the [SpanStyle] that should be applied to the current selection.
     */
    private fun updateCurrentSpanStyle() {
        if (selection.collapsed) {
            val richSpan = getRichSpanByTextIndex(textIndex = selection.min - 1)

            currentAppliedRichSpanStyle = richSpan
                ?.fullStyle
                ?: RichSpanStyle.Default
            currentAppliedSpanStyle = richSpan
                ?.fullSpanStyle
                ?: RichSpanStyle.DefaultSpanStyle

//            if (
//                config.preserveStyleOnEmptyLine &&
//                (richSpan == null || (richSpan.isFirstInParagraph && richSpan.paragraph.isEmpty()))
//            ) {
//                val paragraphBefore =
//                    if (selection.min - 2 < 0)
//                        null
//                    else
//                        getRichParagraphByTextIndex(selection.min - 2)
//
//                if (paragraphBefore == null || paragraphBefore.isNotEmpty()) {
//                    toAddRichSpanStyle = currentRichSpanStyle
//                    toAddSpanStyle = currentSpanStyle
//                }
//            }
        } else {
            val richSpanList = getRichSpanListByTextRange(selection)

            currentAppliedRichSpanStyle = richSpanList
                .getCommonRichStyle()
                ?: RichSpanStyle.Default
            currentAppliedSpanStyle = richSpanList
                .getCommonStyle()
                ?: RichSpanStyle.DefaultSpanStyle
        }
    }

    /**
     * Gets the common [RichSpanStyle] of the [RichSpan]s in the [textRange].
     *
     * @param textRange The [TextRange] to get the common [RichSpanStyle] from.
     * @return The common [RichSpanStyle] of the [RichSpan]s in the [textRange].
     */
    private fun getCommonRichSpanStyleByTextRange(
        textRange: TextRange,
    ): RichSpanStyle {
        val richSpanList = getRichSpanListByTextRange(textRange)

        return richSpanList.getCommonRichStyle() ?: RichSpanStyle.Default
    }

    /**
     * Gets the common [SpanStyle] of the [RichSpan]s in the [textRange].
     *
     * @param textRange The [TextRange] to get the common [SpanStyle] from.
     * @return The common [SpanStyle] of the [RichSpan]s in the [textRange].
     */
    private fun getCommonSpanStyleByTextRange(
        textRange: TextRange,
    ): SpanStyle {
        val richSpanList = getRichSpanListByTextRange(textRange)

        return richSpanList.getCommonStyle() ?: RichSpanStyle.DefaultSpanStyle
    }

    /**
     * Clears [RichParagraph.isFromLineBreak] on the given paragraph and all
     * consecutive trailing paragraphs that have `isFromLineBreak = true`.
     *
     * This ensures that when a paragraph's style or type changes, its `<br>`
     * continuations become independent paragraphs in the HTML output.
     */
    private fun clearLineBreakContinuations(paragraph: RichParagraph) {
        paragraph.isFromLineBreak = false
        val index = richParagraphList.indexOf(paragraph)
        if (index < 0) return
        for (i in (index + 1)..richParagraphList.lastIndex) {
            if (!richParagraphList[i].isFromLineBreak) break
            richParagraphList[i].isFromLineBreak = false
        }
    }

    /**
     * Updates the [currentAppliedParagraphStyle] to the [ParagraphStyle] that should be applied to the current selection.
     */
    private fun updateCurrentParagraphStyle() {
        if (selection.collapsed) {
            val richParagraph = getRichParagraphByTextIndex(selection.min - 1)

            currentRichParagraphType = richParagraph?.type
                ?: richParagraphList.firstOrNull()?.type
                        ?: DefaultParagraph()
            currentAppliedParagraphStyle = richParagraph?.paragraphStyle
                ?: richParagraphList.firstOrNull()?.paragraphStyle
                        ?: RichParagraph.DefaultParagraphStyle
            isUnorderedList = richParagraph?.type is UnorderedList
            isOrderedList = richParagraph?.type is OrderedList
            isList = isUnorderedList || isOrderedList
            canIncreaseListLevel = richParagraph?.let { canIncreaseListLevel(listOf(it)) } == true
            canDecreaseListLevel = richParagraph?.let { canDecreaseListLevel(listOf(it)) } == true
        } else {
            val richParagraphList = getRichParagraphListByTextRange(selection)

            currentRichParagraphType = richParagraphList
                .getCommonType()
                ?: DefaultParagraph()
            currentAppliedParagraphStyle = richParagraphList
                .getCommonStyle()
                ?: ParagraphStyle()

            isUnorderedList = richParagraphList.all { it.type is UnorderedList }
            isOrderedList = richParagraphList.all { it.type is OrderedList }
            isList = richParagraphList.all { it.type is UnorderedList || it.type is OrderedList }
            canIncreaseListLevel = canIncreaseListLevel(richParagraphList)
            canDecreaseListLevel = canDecreaseListLevel(richParagraphList)
        }
    }

    internal fun onTextLayout(
        textLayoutResult: TextLayoutResult,
        density: Density,
    ) {
        this.textLayoutResult = textLayoutResult
        adjustRichParagraphLayout(
            density = density,
        )
        // When the user adds characters, refreshActiveTriggerQuery runs against the
        // OLD text layout (the new caret position is past the old layout's valid range),
        // so caretRect ends up stale. Re-resolve it now that we have the fresh layout.
        refreshActiveTriggerCaretRect()
    }

    /**
     * Update the active query's [TriggerQuery.caretRect] using the current [textLayoutResult].
     * No-op when there is no active query or no layout yet.
     */
    private fun refreshActiveTriggerCaretRect() {
        val query = _activeTriggerQuery ?: return
        val layout = textLayoutResult ?: return
        val caret = textFieldValue.selection.min
        val fresh = runCatching { layout.getCursorRect(caret) }.getOrNull()
        if (fresh != query.caretRect) {
            _activeTriggerQuery = query.copy(caretRect = fresh)
        }
    }

    private fun adjustRichParagraphLayout(
        density: Density,
    ) {
        var isParagraphUpdated = false

        textLayoutResult?.let { textLayoutResult ->
            val layoutTextLength = textLayoutResult.layoutInput.text.text.length

            // Skip if the layout result is stale (text changed since layout was computed)
            if (layoutTextLength != annotatedString.text.length) return

            val multiParagraph = textLayoutResult.multiParagraph
            val offsetLimit =
                if (
                    multiParagraph.didExceedMaxLines &&
                    multiParagraph.maxLines > 0 &&
                    multiParagraph.maxLines != Int.MAX_VALUE &&
                    multiParagraph.lineCount > 0
                )
                    textLayoutResult.getLineEnd(
                        minOf(multiParagraph.maxLines, multiParagraph.lineCount) - 1
                    )
                else
                    layoutTextLength

            // Snapshot the list to avoid ConcurrentModificationException on SnapshotStateList
            val paragraphs = richParagraphList.toList()

            paragraphs.forEachIndexed { index, richParagraph ->
                val paragraphType = richParagraph.type

                if (
                    paragraphType is ConfigurableStartTextWidth &&
                    paragraphType.startText.isNotEmpty() &&
                    paragraphType.startRichSpan.textRange.min >= 0 &&
                    paragraphType.startRichSpan.textRange.max <= offsetLimit
                ) {
                    val start =
                        textLayoutResult.getHorizontalPosition(
                            offset = paragraphType.startRichSpan.textRange.min,
                            usePrimaryDirection = true
                        )
                    val end =
                        textLayoutResult.getHorizontalPosition(
                            offset = paragraphType.startRichSpan.textRange.max,
                            usePrimaryDirection = true
                        )
                    val distanceSp =
                        with(density) {
                            (end - start).absoluteValue.toSp()
                        }

                    if (paragraphType.startTextWidth != distanceSp) {
                        paragraphType.startTextWidth = distanceSp
                        isParagraphUpdated = true
                    }
                }
            }
        }

        if (isParagraphUpdated)
            updateTextFieldValue(textFieldValue)
    }

    internal fun getLinkByOffset(offset: Offset): String? {
        val richSpan = getRichSpanByOffset(offset)
        val linkRichSpan = getLinkRichSpan(richSpan)

        return (linkRichSpan?.richSpanStyle as? RichSpanStyle.Link)?.url
    }

    internal fun isLink(offset: Offset): Boolean {
        val richSpan = getRichSpanByOffset(offset)
        val linkRichSpan = getLinkRichSpan(richSpan)

        return linkRichSpan != null
    }

    private fun getLinkRichSpan(initialRichSpan: RichSpan?): RichSpan? {
        var richSpan = initialRichSpan

        while (richSpan != null && richSpan.richSpanStyle !is RichSpanStyle.Link) {
            richSpan = richSpan.parent
        }

        return richSpan
    }

    private fun getRichSpanByOffset(offset: Offset): RichSpan? {
        this.textLayoutResult?.let { textLayoutResult ->
            val position = textLayoutResult.getOffsetForPosition(offset)
            return getRichSpanByTextIndex(position, true)
        }
        return null
    }

    /**
     * Adjusts the [selection] to the [pressPosition].
     * This is a workaround for the [TextField] that the [selection] is not always correct when you have multiple lines.
     *
     * @param pressPosition The press position.
     */
    internal suspend fun adjustSelectionAndRegisterPressPosition(
        pressPosition: Offset,
    ) {
        adjustSelection(pressPosition)
        registerLastPressPosition(pressPosition)
    }

    /**
     * Adjusts the [selection] to the [pressPosition].
     * This is a workaround for the [TextField] that the [selection] is not always correct when you have multiple lines.
     *
     * @param pressPosition The press position.
     * @param newSelection The new selection.
     */
    private fun adjustSelection(
        pressPosition: Offset,
        newSelection: TextRange? = null,
    ) {
        val selection = newSelection ?: this.selection
        var pressX = pressPosition.x
        var pressY = pressPosition.y
        val textLayoutResult = this.textLayoutResult ?: return
        var index = 0
        var lastIndex = 0

        // Get the length of the text
        val textLength = textLayoutResult.layoutInput.text.length

        // Ensure pressY is within valid bounds
        pressY = pressY.coerceIn(0f, textLayoutResult.size.height.toFloat())

        for (i in 0 until textLayoutResult.lineCount) {
            val start = textLayoutResult.getLineStart(i)
            val top = textLayoutResult.getLineTop(i)

            if (i == 0) {
                if (start > 0f) {
                    pressX += start
                }

                if (top > 0f) {
                    pressY += top
                }
            }

            // Make sure pressY is within the current line's top position
            if (i == 0 && top > pressY) {
                break
            }

            if (top > pressY) {
                index = lastIndex
                break
            }

            lastIndex = index

            if (textLayoutResult.layoutInput.text.text.lastIndex == -1)
                break

            richParagraphList.getOrNull(index)?.let { paragraph ->
                val textRange = paragraph.getTextRange().coerceIn(
                    0, textLayoutResult.layoutInput.text.text.lastIndex
                )

                val pStartTop = textLayoutResult.getBoundingBox(textRange.min).top
                val pEndTop = textLayoutResult.getBoundingBox(textRange.max).top

                val pStartEndTopDiff = (pStartTop - pEndTop).absoluteValue
                val pEndTopLTopDiff = (pEndTop - top).absoluteValue

                if (pStartEndTopDiff < 2f || pEndTopLTopDiff < 2f || pEndTop < top) {
                    index++
                }
            }
        }

        if (index > richParagraphList.lastIndex)
            index = richParagraphList.lastIndex

        val selectedParagraph = richParagraphList.getOrNull(index) ?: return
        val nextParagraph = richParagraphList.getOrNull(index + 1)
        val nextParagraphStart =
            if (nextParagraph == null)
                null
            else
                (nextParagraph.getFirstNonEmptyChild() ?: nextParagraph.type.startRichSpan)
                    .textRange.min.minus(nextParagraph.type.startText.length)

        // Handle selection adjustments
        if (
            selection.collapsed &&
            selection.min == nextParagraphStart
        ) {
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = TextRange(
                        (selection.min - 1).coerceAtLeast(0),
                        (selection.min - 1).coerceAtLeast(0)
                    )
                )
            )
        } else if (
            selection.collapsed &&
            index == richParagraphList.lastIndex &&
            selectedParagraph.isEmpty() &&
            selection.min == selectedParagraph.getFirstNonEmptyChild()?.textRange?.min?.minus(1)
        ) {
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = TextRange(
                        (selection.min + 1).coerceAtMost(textLength - 1),
                        (selection.min + 1).coerceAtMost(textLength - 1)
                    )
                )
            )
        } else if (newSelection != null) {
            // Ensure newSelection is within valid bounds
            val adjustedSelection = TextRange(
                newSelection.start.coerceIn(0, textLength),
                newSelection.end.coerceIn(0, textLength)
            )
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = adjustedSelection
                )
            )
        }
    }

    private var registerLastPressPositionJob: Job? = null
    private suspend fun registerLastPressPosition(pressPosition: Offset): Unit = coroutineScope {
        registerLastPressPositionJob?.cancel()
        registerLastPressPositionJob = launch {
            lastPressPosition = pressPosition
            delay(300)
            lastPressPosition = null
        }
    }

    /**
     * Returns the [RichParagraph] that contains the given [textIndex].
     * If no [RichParagraph] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichParagraph] that contains the given [textIndex], or null if no such [RichParagraph] exists.
     */
    private fun getRichParagraphByTextIndex(
        textIndex: Int,
    ): RichParagraph? {
        if (singleParagraphMode || textIndex < 0)
            return richParagraphList.firstOrNull()

        var index = 0
        var paragraphIndex = -1
        return richParagraphList.fastFirstOrNull { richParagraphStyle ->
            paragraphIndex++
            val result = richParagraphStyle.getRichSpanByTextIndex(
                paragraphIndex = paragraphIndex,
                textIndex = textIndex,
                offset = index,
            )
            index = result.first
            result.second != null
        }
    }

    /**
     * Returns a list of [RichParagraph]s that contains at least a part of the given [searchTextRange].
     * If no [RichParagraph] contains at least a part of the given [searchTextRange], an empty list is returned.
     *
     * @param searchTextRange The [TextRange] to search for.
     * @return A list of [RichParagraph]s that contains a part of the given [searchTextRange],
     * or an empty list if no such [RichParagraph] exists.
     */
    internal fun getRichParagraphListByTextRange(searchTextRange: TextRange): List<RichParagraph> {
        if (singleParagraphMode)
            return richParagraphList.toList()

        var index = 0
        val richParagraphList = mutableListOf<RichParagraph>()
        this.richParagraphList.fastForEachIndexed { paragraphIndex, richParagraphStyle ->
            val result = richParagraphStyle.getRichSpanListByTextRange(
                paragraphIndex = paragraphIndex,
                searchTextRange = searchTextRange,
                offset = index,
            )

            val paragraphStartIndex =
                if (paragraphIndex == 0)
                    0
                else if (searchTextRange.collapsed)
                    index + 1
                // If the search text range is not collapsed, we need to ignore the first index of the paragraph.
                // Because the first index of the paragraph is the last index of the previous paragraph.
                else
                    index + 2

            val isCursorInParagraph =
                searchTextRange.min in paragraphStartIndex..result.first ||
                        searchTextRange.max in paragraphStartIndex..result.first

            if (result.second.isNotEmpty() || isCursorInParagraph)
                richParagraphList.add(richParagraphStyle)

            index = result.first
        }
        return richParagraphList
    }

    private fun getOrCreateRichSpanByTextIndex(
        textIndex: Int,
        ignoreCustomFiltering: Boolean = false,
    ): RichSpan? {
        val richSpan =
            getRichSpanByTextIndex(
                textIndex = textIndex,
                ignoreCustomFiltering = ignoreCustomFiltering,
            )

        if (richSpan == null && textIndex < 0) {
            val firstParagraph = richParagraphList.firstOrNull() ?: return null
            val newRichSpan = RichSpan(
                paragraph = firstParagraph,
                text = "",
            )
            firstParagraph.children.add(0, newRichSpan)
            return newRichSpan
        }

        return richSpan
    }

    /**
     * Returns the [RichSpan] that contains the given [textIndex].
     * If no [RichSpan] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichSpan] that contains the given [textIndex], or null if no such [RichSpan] exists.
     */
    internal fun getRichSpanByTextIndex(
        textIndex: Int,
        ignoreCustomFiltering: Boolean = false,
    ): RichSpan? {
        // If the text index is equal or less than 0, we can return the first non-empty child of the first paragraph.
        if (textIndex < 0) {
            val firstParagraph = richParagraphList.firstOrNull() ?: return null
            return firstParagraph.getFirstNonEmptyChild(firstParagraph.type.startText.length)
        }

        var index = 0
        richParagraphList.fastForEachIndexed { paragraphIndex, richParagraph ->
            val result = richParagraph.getRichSpanByTextIndex(
                paragraphIndex = paragraphIndex,
                textIndex = textIndex,
                offset = index,
                ignoreCustomFiltering = ignoreCustomFiltering,
            )
            if (result.second != null)
                return result.second
            else
                index = result.first
        }
        return null
    }

    /**
     * Returns a list of [RichSpan]s that contains at least a part of the given [searchTextRange].
     * If no [RichSpan] contains at least a part of the given [searchTextRange], an empty list is returned.
     *
     * @param searchTextRange The [TextRange] to search for.
     * @return A list of [RichSpan]s that contains a part of the given [searchTextRange], or an empty list if no such [RichSpan] exists.
     */
    private fun getRichSpanListByTextRange(searchTextRange: TextRange): List<RichSpan> {
        var index = 0
        val richSpanList = mutableListOf<RichSpan>()
        richParagraphList.fastForEachIndexed { paragraphIndex, richParagraphStyle ->
            val result = richParagraphStyle.getRichSpanListByTextRange(
                paragraphIndex = paragraphIndex,
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanList.addAll(result.second)
            index = result.first
        }
        return richSpanList
    }

    /**
     * Internal helper for testing
     */
    internal fun printParagraphs() {
        richParagraphList.fastForEachIndexed { i, richParagraph ->
            println("Paragraph $i: $richParagraph")
        }
    }

    /**
     * Returns a copy of this [RichTextState].
     * It can be used to create a snapshot of the current state.
     *
     * @return A copy of this [RichTextState].
     */
    public fun copy(): RichTextState {
        val richParagraphList = richParagraphList.map { it.copy() }
        val richTextState = RichTextState(richParagraphList)
        richTextState.updateTextFieldValue(textFieldValue)
        richTextState.config.linkColor = config.linkColor
        richTextState.config.linkTextDecoration = config.linkTextDecoration
        richTextState.config.codeSpanColor = config.codeSpanColor
        richTextState.config.codeSpanBackgroundColor = config.codeSpanBackgroundColor
        richTextState.config.codeSpanStrokeColor = config.codeSpanStrokeColor
        richTextState.config.listIndent = config.listIndent
        richTextState.config.orderedListIndent = config.orderedListIndent
        richTextState.config.unorderedListIndent = config.unorderedListIndent
        richTextState.config.unorderedListStyleType = config.unorderedListStyleType
        richTextState.config.orderedListStyleType = config.orderedListStyleType
        richTextState.config.preserveStyleOnEmptyLine = config.preserveStyleOnEmptyLine
        richTextState.config.exitListOnEmptyItem = config.exitListOnEmptyItem

        return richTextState
    }

    /**
     * Updates the [RichTextState] with the given [text].
     *
     * @param text The text to update the [RichTextState] with.
     */
    public fun setText(
        text: String,
        selection: TextRange = TextRange(text.length),
    ): RichTextState {
        val textFieldValue =
            TextFieldValue(
                text = text,
                selection = selection,
            )

        onTextFieldValueChange(
            newTextFieldValue = textFieldValue
        )
        return this
    }

    /**
     * Updates the [RichTextState] with the given [html].
     *
     * @param html The html to update the [RichTextState] with.
     */
    public fun setHtml(html: String): RichTextState {
        history.onProgrammaticReplace()
        val richParagraphList = RichTextStateHtmlParser.encode(html).richParagraphList
        updateRichParagraphList(richParagraphList)
        return this
    }

    /**
     * Inserts the given [html] content after selection in the [RichTextState].
     *
     * @param html The html content to insert.
     */
    public fun insertHtmlAfterSelection(html: String) {
        val newParagraphs = RichTextStateHtmlParser.encode(html).richParagraphList
        val position = selection.max

        selection = TextRange(selection.max)

        insertParagraphs(
            newParagraphs = newParagraphs,
            position = position,
        )
    }

    /**
     * Inserts the given [html] content at the specified [position] in the [RichTextState].
     *
     * The insertion behavior depends on the HTML content and the insertion position:
     * 1. If the HTML contains a single paragraph:
     *    - The content is inserted at the exact position within the existing paragraph
     *    - All styles (both span and rich span styles) are preserved
     * 2. If the HTML contains multiple paragraphs:
     *    - The current paragraph is split at the insertion point
     *    - The new paragraphs are inserted between the split parts
     *    - All styles from the original paragraph are preserved in both split parts
     *
     * Special cases:
     * - If position is 0, the content is inserted at the start
     * - If position equals text length, the content is appended at the end
     * - If the HTML is empty, no changes are made
     *
     * @param html The html content to insert.
     * @param position The position at which to insert the html content.
     */
    public fun insertHtml(html: String, position: Int) {
        val newParagraphs = RichTextStateHtmlParser.encode(html).richParagraphList

        insertParagraphs(
            newParagraphs = newParagraphs,
            position = position,
        )
    }

    /**
     * Updates the [RichTextState] with the given [markdown].
     *
     * @param markdown The markdown to update the [RichTextState] with.
     */
    public fun setMarkdown(markdown: String): RichTextState {
        history.onProgrammaticReplace()
        val richParagraphList = RichTextStateMarkdownParser.encode(markdown).richParagraphList
        updateRichParagraphList(richParagraphList)
        return this
    }

    /**
     * Inserts the given [markdown] content after selection in the [RichTextState].
     *
     * @param markdown The markdown content to insert.
     */
    public fun insertMarkdownAfterSelection(markdown: String) {
        val newParagraphs = RichTextStateMarkdownParser.encode(markdown).richParagraphList
        val position = selection.max

        selection = TextRange(selection.max)

        insertParagraphs(
            newParagraphs = newParagraphs,
            position = position,
        )
    }

    /**
     * Inserts the given [markdown] content at the specified [position] in the [RichTextState].
     *
     * The insertion behavior depends on the Markdown content and the insertion position:
     * 1. If the Markdown contains a single paragraph:
     *    - The content is inserted at the exact position within the existing paragraph
     *    - All styles (both span and rich span styles) are preserved
     * 2. If the Markdown contains multiple paragraphs:
     *    - The current paragraph is split at the insertion point
     *    - The new paragraphs are inserted between the split parts
     *    - All styles from the original paragraph are preserved in both split parts
     *
     * Special cases:
     * - If position is 0, the content is inserted at the start
     * - If position equals text length, the content is appended at the end
     * - If the Markdown is empty, no changes are made
     *
     * @param markdown The markdown content to insert.
     * @param position The position at which to insert the markdown content.
     */
    public fun insertMarkdown(markdown: String, position: Int) {
        val newParagraphs = RichTextStateMarkdownParser.encode(markdown).richParagraphList

        insertParagraphs(
            newParagraphs = newParagraphs,
            position = position,
        )
    }

    /**
     * Inserts the given [newParagraphs] at the specified [position] in the [RichTextState].
     *
     * The insertion behavior depends on the paragraphs and the insertion position:
     * 1. If the list contains a single paragraph:
     *    - The content is inserted at the exact position within the existing paragraph
     *    - All styles (both span and rich span styles) are preserved
     * 2. If the list contains multiple paragraphs:
     *    - The current paragraph is split at the insertion point
     *    - The new paragraphs are inserted between the split parts
     *    - All styles from the original paragraph are preserved in both split parts
     *
     * Special cases:
     * - If position is 0, the content is inserted at the start
     * - If position equals text length, the content is appended at the end
     * - If the list is empty, no changes are made
     *
     * @param newParagraphs The new paragraphs to insert.
     * @param position The position at which to insert the new paragraphs.
     */
    internal fun insertParagraphs(
        newParagraphs: List<RichParagraph>,
        position: Int
    ) {
        val position = position
            .coerceIn(0, annotatedString.text.length)

        if (newParagraphs.isEmpty())
            return

        if (richParagraphList.isEmpty())
            richParagraphList.add(RichParagraph())

        richParagraphList.first().let { p ->
            if (p.children.isEmpty())
                p.children.add(RichSpan(paragraph = p))
        }

        val firstNewParagraph = newParagraphs.first()

        val richSpan = getRichSpanByTextIndex(
            textIndex = position - 1,
            ignoreCustomFiltering = true,
        )
            ?: return

        val targetParagraph = richSpan.paragraph
        val paragraphIndex = richParagraphList.indexOf(targetParagraph)

        val sliceIndex = max(position, richSpan.textRange.min)

        val targetParagraphFirstHalf = targetParagraph
        val targetParagraphSecondHalf = targetParagraph.slice(
            richSpan = richSpan,
            startIndex = sliceIndex,
            removeSliceIndex = false,
        )

        if (targetParagraphFirstHalf.isEmpty() && firstNewParagraph.isNotEmpty()) {
            targetParagraphFirstHalf.paragraphStyle = firstNewParagraph.paragraphStyle
            targetParagraphFirstHalf.type = firstNewParagraph.type
        }

        if (newParagraphs.size == 1) {
            // Before position + Pasted Content + After Position

            firstNewParagraph.updateChildrenParagraph(targetParagraphFirstHalf)
            targetParagraphSecondHalf.updateChildrenParagraph(targetParagraphFirstHalf)
            targetParagraphFirstHalf.children.addAll(firstNewParagraph.children)
            targetParagraphFirstHalf.children.addAll(targetParagraphSecondHalf.children)
            targetParagraphFirstHalf.removeEmptyChildren()
        } else {
            // Before position + First pasted paragraph
            // Pasted paragraphs between first and last
            // Last pasted paragraph + After position

            val lastNewParagraph = newParagraphs.last()

            // Before position + First pasted paragraph
            firstNewParagraph.updateChildrenParagraph(targetParagraphFirstHalf)
            targetParagraphFirstHalf.children.addAll(firstNewParagraph.children)
            targetParagraphFirstHalf.removeEmptyChildren()

            // Pasted paragraphs between first and last
            if (newParagraphs.size >= 3) {
                val middleParagraphs = newParagraphs.subList(1, newParagraphs.size - 1)
                richParagraphList.addAll(paragraphIndex + 1, middleParagraphs)
            }

            // Last pasted paragraph + After position
            targetParagraphSecondHalf.updateChildrenParagraph(lastNewParagraph)
            lastNewParagraph.children.addAll(targetParagraphSecondHalf.children)
            lastNewParagraph.removeEmptyChildren()
            richParagraphList.add(paragraphIndex + newParagraphs.size - 1, lastNewParagraph)
        }

        // Update the state
        updateRichParagraphList()
    }

    /**
     * Updates the [RichTextState] with the given [newRichParagraphList].
     * The [RichTextState] will be updated with the given [newRichParagraphList] and the [annotatedString] will be updated.
     *
     * @param newRichParagraphList The [RichParagraph]s to update the [RichTextState] with.
     */
    internal fun updateRichParagraphList(newRichParagraphList: List<RichParagraph>) {
        richParagraphList.clear()
        richParagraphList.addAll(newRichParagraphList)
        updateRichParagraphList()
    }

    internal fun updateRichParagraphList() {
        if (richParagraphList.isEmpty())
            richParagraphList.add(RichParagraph())

        val beforeTextLength = annotatedString.text.length

        val newStyledRichSpanList = mutableListOf<RichSpan>()

        usedInlineContentMapKeys.clear()

        annotatedString = buildAnnotatedString {
            var index = 0
            richParagraphList.fastForEachIndexed { i, richParagraph ->
                withStyle(richParagraph.paragraphStyle.merge(richParagraph.type.getStyle(config))) {
                    withStyle(
                        richParagraph.getListMarkerSpanStyle(config.listMarkerStyleBehavior)
                    ) {
                        append(richParagraph.type.startText)
                    }

                    val richParagraphStartTextLength = richParagraph.type.startText.length
                    richParagraph.type.startRichSpan.textRange =
                        TextRange(index, index + richParagraphStartTextLength)
                    index += richParagraphStartTextLength
                    withStyle(RichSpanStyle.DefaultSpanStyle) {
                        index = append(
                            state = this@RichTextState,
                            richSpanList = richParagraph.children,
                            startIndex = index,
                            onStyledRichSpan = {
                                newStyledRichSpanList.add(it)
                            },
                        )

                        if (!singleParagraphMode) {
                            if (i != richParagraphList.lastIndex) {
                                append(' ')
                                index++
                            }
                        }
                    }
                }
            }
        }

        inlineContentMap.keys.forEach { key ->
            if (key !in usedInlineContentMapKeys) {
                inlineContentMap.remove(key)
            }
        }

        val selectionIndex =
            (textFieldValue.selection.min + (annotatedString.text.length - beforeTextLength))
                .coerceIn(0, annotatedString.text.length)

        styledRichSpanList.clear()
        textFieldValue = TextFieldValue(
            text = annotatedString.text,
            selection = TextRange(selectionIndex),
        )
        visualTransformation = VisualTransformation { _ ->
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }
        styledRichSpanList.addAll(newStyledRichSpanList)

        // Clear un-applied styles
        toAddSpanStyle = SpanStyle()
        toRemoveSpanStyle = SpanStyle()
        toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyleKClass = RichSpanStyle.Default::class

        // Update current span style
        updateCurrentSpanStyle()

        // Update current paragraph style
        updateCurrentParagraphStyle()

        // Check paragraphs type
        checkParagraphsType()
    }

    private fun checkParagraphsType() {
        tempTextFieldValue = textFieldValue

        // Todo: It's not the best way to set start text span style, try to set it from parser
        var orderedListStartTextSpanStyle = SpanStyle()

        val levelNumberMap = hashMapOf<Int, Int>()

        richParagraphList.fastForEachIndexed { index, richParagraph ->
            val type = richParagraph.type

            if (type is ConfigurableListLevel) {
                // Clear the completed list levels
                levelNumberMap.keys.toList().fastForEach { level ->
                    if (level > type.level)
                        levelNumberMap.remove(level)
                }
            } else {
                // Clear the map if the current paragraph is not a list
                levelNumberMap.clear()
            }

            // Remove current list level from map if the current paragraph is an unordered list
            if (type is UnorderedList)
                levelNumberMap.remove(type.level)

            if (type is OrderedList) {
                // Use startFrom for the first item if explicitly set (from <ol start="N">),
                // otherwise default to 1
                val isFirstAtLevel = levelNumberMap[type.level] == null
                val orderedListNumber =
                    levelNumberMap[type.level]
                        ?.plus(1)
                        ?: type.startFrom

                levelNumberMap[type.level] = orderedListNumber

                if (orderedListNumber == 1)
                    orderedListStartTextSpanStyle =
                        richParagraph.getFirstNonEmptyChild()?.spanStyle ?: SpanStyle()

                // Preserve startFrom on the first item so it survives re-runs
                val preservedStartFrom = if (isFirstAtLevel) type.startFrom else 1

                tempTextFieldValue = updateParagraphType(
                    paragraph = richParagraph,
                    newType = OrderedList(
                        number = orderedListNumber,
                        config = config,
                        startTextWidth = type.startTextWidth,
                        initialLevel = type.level,
                        startFrom = preservedStartFrom,
                    ),
                    textFieldValue = tempTextFieldValue,
                )

                type.number = orderedListNumber
            } else {
                orderedListStartTextSpanStyle = SpanStyle()
            }
        }

        updateTextFieldValue()
    }

    /**
     * Extracts a new [RichTextState] containing only the content within the given [range].
     * This method directly copies and trims paragraphs without going through the editing
     * pipeline, preserving paragraph types (lists, etc.) and avoiding side effects like
     * list-exit behavior that [removeTextRange] would trigger.
     *
     * @param range The [TextRange] to extract.
     * @return A new [RichTextState] with only the content in the range.
     */
    private fun extractRangeState(range: TextRange): RichTextState {
        val textLength = annotatedString.text.length
        val rangeStart = range.min.coerceIn(0, textLength)
        val rangeEnd = range.max.coerceIn(0, textLength)

        if (rangeStart >= rangeEnd) {
            return RichTextState(listOf(RichParagraph()))
        }

        val resultParagraphs = mutableListOf<RichParagraph>()

        richParagraphList.fastForEachIndexed { i, paragraph ->
            val startTextLen = paragraph.type.startText.length
            val paragraphStart = paragraph.type.startRichSpan.textRange.min
            val contentStart = paragraphStart + startTextLen

            // Compute paragraph content end from children
            val contentEnd = paragraph.children.lastOrNull()?.fullTextRange?.max ?: contentStart

            // Each paragraph (except the last) has a trailing separator character
            val hasSeparator = i < richParagraphList.lastIndex
            val paragraphEndWithSep = contentEnd + if (hasSeparator) 1 else 0

            // Skip paragraphs that are entirely outside the range (including separator)
            if (paragraphEndWithSep <= rangeStart || contentStart >= rangeEnd) {
                return@fastForEachIndexed
            }

            // If only the trailing separator is in range (content itself is before range),
            // include an empty paragraph to represent the line break
            if (rangeStart in contentEnd..<paragraphEndWithSep) {
                resultParagraphs.add(RichParagraph())
                return@fastForEachIndexed
            }

            // This paragraph has content within the range, copy and trim
            val newParagraph = paragraph.copy()

            // Trim children spans to only include text within [rangeStart, rangeEnd)
            trimSpanList(newParagraph.children, rangeStart, rangeEnd)
            newParagraph.removeEmptyChildren()

            resultParagraphs.add(newParagraph)
        }

        if (resultParagraphs.isEmpty()) {
            resultParagraphs.add(RichParagraph())
        }

        return RichTextState(resultParagraphs)
    }

    /**
     * Recursively trims a list of [RichSpan]s so that only text within
     * [rangeStart, rangeEnd) (in terms of the original annotated string positions) is kept.
     * Spans completely outside the range are emptied; partially overlapping spans are substring-trimmed.
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun trimSpanList(
        spans: MutableList<RichSpan>,
        rangeStart: Int,
        rangeEnd: Int,
    ) {
        val toRemove = mutableListOf<Int>()

        spans.fastForEachIndexed { i, span ->
            val spanTextStart = span.textRange.min
            val spanTextEnd = span.textRange.max

            // Trim this span's own text
            if (spanTextEnd <= rangeStart || spanTextStart >= rangeEnd) {
                // Completely outside, clear text
                span.text = ""
            } else if (spanTextStart < rangeStart || spanTextEnd > rangeEnd) {
                // Partially overlapping, trim
                val trimStart = (rangeStart - spanTextStart).coerceAtLeast(0)
                val trimEnd = (rangeEnd - spanTextStart).coerceAtMost(span.text.length)
                span.text = span.text.substring(trimStart, trimEnd)
            }
            // else: fully inside, keep as-is

            // Recursively trim children
            trimSpanList(span.children, rangeStart, rangeEnd)

            // Mark empty spans for removal (atomic spans like Image/Token keep their placeholder)
            if (span.text.isEmpty() && span.children.isEmpty() && !span.richSpanStyle.isAtomic) {
                toRemove.add(i)
            }
        }

        toRemove.fastForEachReversed { i ->
            spans.removeAt(i)
        }
    }

    /**
     * Returns the [RichTextState] as a text string.
     *
     * @return The text string.
     */
    public fun toText(): String =
        toText(richParagraphList = richParagraphList)

    /**
     * Returns a specific range of the [RichTextState] as a text string.
     *
     * @param range The [TextRange] to convert to text.
     * @return The text string for the specified range.
     */
    public fun toText(range: TextRange): String {
        val state = extractRangeState(range)
        return state.toText()
    }

    /**
     * Decodes the [RichTextState] to a html string.
     *
     * @return The html string.
     */
    public fun toHtml(): String {
        return RichTextStateHtmlParser.decode(this)
    }

    /**
     * Decodes a specific range of the [RichTextState] to a html string.
     *
     * @param range The [TextRange] to convert to HTML.
     * @return The html string for the specified range.
     */
    public fun toHtml(range: TextRange): String {
        val state = extractRangeState(range)
        return RichTextStateHtmlParser.decode(state)
    }

    /**
     * Decodes the [RichTextState] to a markdown string.
     *
     * @return The markdown string.
     */
    public fun toMarkdown(): String {
        return RichTextStateMarkdownParser.decode(this)
    }

    /**
     * Decodes a specific range of the [RichTextState] to a markdown string.
     *
     * @param range The [TextRange] to convert to markdown.
     * @return The markdown string for the specified range.
     */
    public fun toMarkdown(range: TextRange): String {
        val state = extractRangeState(range)
        return RichTextStateMarkdownParser.decode(state)
    }

    /**
     * Clears the [RichTextState] and sets the [TextFieldValue] to an empty value.
     */
    public fun clear() {
        richParagraphList.clear()
        richParagraphList.add(RichParagraph())
        updateTextFieldValue(TextFieldValue())
    }

    public companion object {
        public val Saver: Saver<RichTextState, *> = listSaver(
            save = {
                listOf(
                    it.toHtml(),
                    it.selection.start.toString(),
                    it.selection.end.toString(),
                )
            },
            restore = {
                val html = it[0]
                val selectionStart = it[1].toInt()
                val selectionEnd = it[2].toInt()
                val selection = TextRange(selectionStart, selectionEnd)
                val richTextState = RichTextState()
                richTextState.setHtml(html)
                richTextState.updateTextFieldValue(
                    richTextState.textFieldValue.copy(
                        selection = selection
                    )
                )
                richTextState
            }
        )
    }
}
