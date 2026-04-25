package com.mohamedrejeb.richeditor.ui.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.platform.currentPlatform

/**
 * Material 3 suggestions popup that renders while a matching [Trigger] is active.
 *
 * Reads [RichTextState.activeTriggerQuery]; shows nothing when no query is active
 * or the trigger id does not match [triggerId]. Otherwise, anchors a [Popup] just
 * below the caret with one row per item returned by [suggestions].
 *
 * Supports keyboard navigation while visible:
 *  - `ArrowDown` / `ArrowUp` move the highlighted row;
 *  - `Enter` commits the highlight;
 *  - `Esc` cancels the active query (leaves the typed text in place).
 *
 * On commit (click or Enter), [onSelect] returns a [RichSpanStyle.Token]; the
 * library then calls [RichTextState.insertToken] to splice it into the document
 * as an atomic span followed by a trailing space.
 *
 * @param state The editor's rich text state.
 * @param triggerId Id of the trigger this popup serves (e.g. `"mention"`).
 * @param suggestions Pure function mapping the current query text to a list of candidates.
 * @param onSelect Invoked when a row is committed; must return a [RichSpanStyle.Token]
 * whose [RichSpanStyle.Token.triggerId] equals [triggerId] and whose
 * [RichSpanStyle.Token.label] starts with the trigger's char.
 * @param modifier Modifier applied to the popup's content container.
 * @param verticalOffset Gap between the caret bottom and the popup top. Tune per decoration
 * (e.g. `OutlinedRichTextEditor` needs more space than `BasicRichTextEditor`).
 * @param containerColor Popup background. Defaults to [MaterialTheme.colorScheme.surface].
 * @param contentColor Text color for rows. Defaults to [MaterialTheme.colorScheme.onSurface].
 * @param highlightColor Background of the currently-highlighted row. Defaults to a translucent
 * `surfaceVariant`.
 * @param shape Shape of the popup container.
 * @param maxVisibleItems Soft cap on rendered rows; content scrolls past this count.
 * @param item Row composable for a single suggestion.
 */
@ExperimentalRichTextApi
@Composable
public fun <T> TriggerSuggestions(
    state: RichTextState,
    triggerId: String,
    suggestions: (query: String) -> List<T>,
    onSelect: (T) -> RichSpanStyle.Token,
    modifier: Modifier = Modifier,
    verticalOffset: Dp = 4.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    shape: Shape = RoundedCornerShape(8.dp),
    maxVisibleItems: Int = 5,
    item: @Composable (T) -> Unit,
) {
    val query = state.activeTriggerQuery
    if (query == null || query.triggerId != triggerId) return

    // Hold rendering until the caret rect has been resolved against the fresh text
    // layout. Without this, the first frame after query activation renders the popup
    // at (0,0) - the old layout's `getCursorRect` answer - before `onTextLayout`
    // patches it one frame later. That produces a visible flicker.
    //
    // NOTE: a cursor rect has zero width (left == right), so we can't use Rect.isEmpty.
    // A valid caret rect always has a positive `bottom`; Rect.Zero's bottom is 0.
    val caretRect = query.caretRect ?: return
    if (caretRect.bottom <= 0f) return

    val items = remember(query.query) { suggestions(query.query) }
    if (items.isEmpty()) return

    // `highlighted` state is read inside the key handler - we pass the delegate reference
    // via a separate ref so the handler always sees the latest value, never a stale closure.
    val highlightedState = remember(query.query) { mutableStateOf(0) }
    val safeHighlighted = highlightedState.value.coerceIn(0, items.lastIndex)

    val density = LocalDensity.current
    val verticalOffsetPx = remember(verticalOffset, density) { with(density) { verticalOffset.roundToPx() } }

    // iOS only: the soft keyboard is an overlay, so the scene's reported size
    // does not shrink when the IME shows; we must subtract the IME bottom inset
    // ourselves to keep the popup in the visible area. On Android the keyboard
    // resizes the window (adjustResize), so the scene size already excludes the
    // keyboard - subtracting again would double-count and push the popup
    // incorrectly. Desktop / Web have no soft keyboard.
    val imeBottomPx = if (currentPlatform.isIOS) {
        WindowInsets.ime.getBottom(density)
    } else {
        0
    }

    // Resolve the caret line's true baseline and top from the layout. The cursor
    // rect returned by getCursorRect spans the full LINE BOX (with line-leading
    // padding above and below the visible glyphs) - using its bottom as the
    // popup anchor produces a visible gap that grows with the editor's line
    // height. The baseline is the typographic invariant: positioning a popup at
    // baseline + verticalOffset puts it directly under where letters "sit",
    // independent of font size, line leading, or platform line-height policy.
    val layout = state.textLayoutResult
    val (caretLineTopY, caretBaselineY) = remember(query.range, caretRect, layout) {
        if (layout != null) {
            val safeEnd = query.range.end.coerceIn(0, layout.layoutInput.text.text.length)
            runCatching {
                val line = layout.getLineForOffset(safeEnd)
                layout.getLineTop(line) to layout.getLineBaseline(line)
            }.getOrDefault(caretRect.top to caretRect.bottom)
        } else {
            caretRect.top to caretRect.bottom
        }
    }

    val positionProvider = remember(
        caretRect,
        caretLineTopY,
        caretBaselineY,
        state.textFieldWindowPosition,
        imeBottomPx,
        verticalOffsetPx,
    ) {
        CaretWindowPositionProvider(
            textFieldWindowX = state.textFieldWindowPosition.x.toInt(),
            textFieldWindowY = state.textFieldWindowPosition.y.toInt(),
            caretX = caretRect.left.toInt(),
            caretLineTop = caretLineTopY.toInt(),
            caretBaseline = caretBaselineY.toInt(),
            verticalOffset = verticalOffsetPx,
            imeBottom = imeBottomPx,
        )
    }

    val scrollState = rememberScrollState()
    // Row y-positions (within the scrolling Column) and heights, populated via
    // onPlaced as each row lays out. Used to scroll the highlighted
    // row into view when the user navigates with arrow keys.
    val rowYs = remember(items) { mutableStateListOf<Int>().apply { repeat(items.size) { add(0) } } }
    val rowHeights = remember(items) { mutableStateListOf<Int>().apply { repeat(items.size) { add(0) } } }

    // Install key handler while popup is visible. Consumes ↑/↓/Enter/Esc before they
    // reach the text field. The lambda reads `highlightedState.value` afresh each call,
    // so Enter always commits the CURRENTLY-highlighted row, not the one at install time.
    DisposableEffect(state, triggerId, items) {
        state.triggerKeyHandler = { event: KeyEvent ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else when (event.key) {
                Key.DirectionDown -> {
                    if (items.isNotEmpty()) {
                        highlightedState.value = (highlightedState.value + 1).mod(items.size)
                    }
                    true
                }
                Key.DirectionUp -> {
                    if (items.isNotEmpty()) {
                        highlightedState.value = ((highlightedState.value - 1) + items.size).mod(items.size)
                    }
                    true
                }
                Key.Enter, Key.NumPadEnter -> {
                    val currentIndex = highlightedState.value.coerceIn(0, items.lastIndex)
                    items.getOrNull(currentIndex)?.let { value ->
                        commitSelection(state, triggerId, value, onSelect)
                    }
                    true
                }
                Key.Escape -> {
                    state.cancelActiveTrigger()
                    true
                }
                else -> false
            }
        }
        onDispose {
            if (state.triggerKeyHandler != null) {
                state.triggerKeyHandler = null
            }
        }
    }

    // Keep the highlighted row visible. Scrolls only when the row is outside the
    // current viewport; otherwise leaves the offset alone so the list stays stable.
    LaunchedEffect(safeHighlighted) {
        val rowY = rowYs.getOrNull(safeHighlighted) ?: return@LaunchedEffect
        val rowH = rowHeights.getOrNull(safeHighlighted) ?: return@LaunchedEffect
        val viewportH = scrollState.viewportSize.takeIf { it > 0 } ?: return@LaunchedEffect
        val scrollOffset = scrollState.value
        when {
            rowY < scrollOffset -> scrollState.animateScrollTo(rowY)
            rowY + rowH > scrollOffset + viewportH ->
                scrollState.animateScrollTo(rowY + rowH - viewportH)
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { state.cancelActiveTrigger() },
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            modifier = modifier
                .width(IntrinsicSize.Max)
                .heightIn(max = 40.dp * maxVisibleItems),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .verticalScroll(scrollState),
            ) {
                items.forEachIndexed { index, value ->
                    val isHighlighted = index == safeHighlighted
                    val interaction = remember(index) { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPlaced { coords ->
                                if (index < rowYs.size) {
                                    rowYs[index] = coords.positionInParent().y.toInt()
                                    rowHeights[index] = coords.size.height
                                }
                            }
                            .background(if (isHighlighted) highlightColor else Color.Transparent)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                            ) {
                                commitSelection(state, triggerId, value, onSelect)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        item(value)
                    }
                }
            }
        }
    }
}

@ExperimentalRichTextApi
private fun <T> commitSelection(
    state: RichTextState,
    triggerId: String,
    value: T,
    onSelect: (T) -> RichSpanStyle.Token,
) {
    val token = onSelect(value)
    require(token.triggerId == triggerId) {
        "onSelect returned Token(triggerId='${token.triggerId}'), expected '$triggerId'"
    }
    state.insertToken(
        triggerId = token.triggerId,
        id = token.id,
        label = token.label,
    )
}

/**
 * Anchors the popup in WINDOW coordinates by using the text field's captured
 * window position plus the caret's typographic anchors within the text layout:
 *  - [caretBaseline] for below-placement (popup top sits at baseline + offset,
 *    so it visually attaches to where letters "sit" without being pushed away
 *    by line-box bottom leading).
 *  - [caretLineTop] for above-placement (popup bottom sits at lineTop - offset,
 *    safely clearing the ascent-most glyph and any top leading).
 *
 * Honors the IME bottom inset so the popup doesn't slide behind the soft
 * keyboard, and flips above the caret when there's no room below (preferring
 * above the popup's anchor bounds - the editor's outer rect - when that fits,
 * otherwise above the caret line itself).
 */
private class CaretWindowPositionProvider(
    private val textFieldWindowX: Int,
    private val textFieldWindowY: Int,
    private val caretX: Int,
    private val caretLineTop: Int,
    private val caretBaseline: Int,
    private val verticalOffset: Int,
    private val imeBottom: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val effectiveBottom = (windowSize.height - imeBottom).coerceAtLeast(0)

        val baselineWindowY = textFieldWindowY + caretBaseline
        val lineTopWindowY = textFieldWindowY + caretLineTop

        val belowY = baselineWindowY + verticalOffset
        val fitsBelow = belowY + popupContentSize.height <= effectiveBottom

        // Two flip targets, in priority order:
        //  1. Above the popup's anchor (editor outer bounds) - clears decoration padding.
        //  2. Above the caret line top - falls back when the editor is too high
        //     on screen for option 1 to fit (would push the popup off the top
        //     of the window).
        val flipAboveAnchorY = anchorBounds.top - verticalOffset - popupContentSize.height
        val flipAboveCaretY = lineTopWindowY - verticalOffset - popupContentSize.height
        val flipAboveAnchorFits =
            anchorBounds.top in 1 until lineTopWindowY && flipAboveAnchorY >= 0

        val rawY = when {
            fitsBelow -> belowY
            flipAboveAnchorFits -> flipAboveAnchorY
            else -> flipAboveCaretY
        }

        val rawX = textFieldWindowX + caretX
        val x = rawX
            .coerceAtMost(windowSize.width - popupContentSize.width)
            .coerceAtLeast(0)
        // Clamp Y into [0, effectiveBottom - popupHeight] so even when flipping
        // the popup never lands above the top of the screen or behind the IME.
        val maxY = (effectiveBottom - popupContentSize.height).coerceAtLeast(0)
        val y = rawY.coerceAtMost(maxY).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
