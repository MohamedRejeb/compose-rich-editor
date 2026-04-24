package com.mohamedrejeb.richeditor.sample.common.notion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState

private const val MENU_WIDTH_DP = 320
private const val MENU_MAX_HEIGHT_DP = 340
private const val ROW_HEIGHT_DP = 52
private const val CARET_GAP_DP = 8

/**
 * Notion-style command palette that opens when the user types `/` inside the editor.
 *
 * Implementation note: Uses a [Popup] (same family as the library's
 * [com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions]) so the menu
 * escapes parent clipping and paints above any sibling content below the editor.
 * Unlike `TriggerSuggestions`, which commits a [com.mohamedrejeb.richeditor.model.RichSpanStyle.Token],
 * this menu replaces the `/query` text with a paragraph-level block transformation
 * (see [applyNotionBlockCommand]).
 *
 * Anchor the composable inside the editor's wrapping [Box]: the position provider
 * takes the anchor's window coordinates and adds the caret's editor-local
 * coordinates to place the menu right under the caret.
 *
 * Keyboard handling lives in [rememberNotionSlashKeyHandler] which the screen
 * attaches to the editor via `Modifier.onPreviewKeyEvent` so ↑/↓/Enter/Escape
 * are intercepted before the text field consumes them.
 *
 * @param state Editor state containing the active trigger query.
 * @param highlightedIndex Index of the currently-highlighted row. Owned by the
 * caller (so the key handler and the menu stay in sync).
 * @param onHighlightChange Callback when a row is hovered or clicked.
 * @param onCommit Invoked when a command is committed (click or Enter).
 */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun NotionSlashMenu(
    state: RichTextState,
    highlightedIndex: Int,
    onHighlightChange: (Int) -> Unit,
    onCommit: (NotionBlockCommand) -> Unit,
) {
    val query = state.activeTriggerQuery
    if (query == null || query.triggerId != NOTION_BLOCK_TRIGGER_ID) return

    val caret = query.caretRect ?: return
    if (caret.bottom <= 0f) return

    val commands = remember(query.query) {
        NotionBlockCommand.entries.filter { it.matches(query.query) }
    }
    if (commands.isEmpty()) return

    val safeHighlighted = highlightedIndex.coerceIn(0, commands.lastIndex)
    val density = LocalDensity.current
    val verticalOffsetPx = remember(density) {
        with(density) { CARET_GAP_DP.dp.roundToPx() }
    }

    val positionProvider = remember(caret, verticalOffsetPx) {
        CaretAnchorPopupPositionProvider(
            caretX = caret.left.toInt(),
            caretBottom = caret.bottom.toInt(),
            verticalOffset = verticalOffsetPx,
        )
    }

    val listState = rememberLazyListState()
    LaunchedEffect(safeHighlighted) {
        listState.animateScrollToItem(safeHighlighted)
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { state.cancelActiveTrigger() },
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NotionColors.MenuSurface,
            contentColor = NotionColors.TextPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, NotionColors.Border),
            modifier = Modifier.width(MENU_WIDTH_DP.dp),
        ) {
            Column {
                MenuHeader(query.query)

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = MENU_MAX_HEIGHT_DP.dp),
                ) {
                    itemsIndexed(commands, key = { _, cmd -> cmd.id }) { index, command ->
                        val isHighlighted = index == safeHighlighted
                        val interaction = remember(command.id) { MutableInteractionSource() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ROW_HEIGHT_DP.dp)
                                .focusProperties { canFocus = false }
                                .background(if (isHighlighted) NotionColors.MenuHighlight else Color.Transparent)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                ) {
                                    onHighlightChange(index)
                                    onCommit(command)
                                }
                                .padding(horizontal = 10.dp),
                        ) {
                            CommandIcon(command)
                            Spacer(Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = command.label,
                                    color = NotionColors.TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = command.description,
                                    color = NotionColors.TextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(query: String) {
    val label = if (query.isEmpty()) "Basic blocks" else "Results for \"$query\""
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = NotionColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CommandIcon(command: NotionBlockCommand) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(NotionColors.SidebarSurface)
            .border(1.dp, NotionColors.Border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = command.iconLabel,
            color = NotionColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

/**
 * Positions the popup so its top-left anchors at the editor-local caret rect,
 * translated into window coordinates via [PopupPositionProvider.calculatePosition]'s
 * `anchorBounds` (which is the wrapping composable's window bounds).
 *
 * This mirrors the library's internal `CaretWindowPositionProvider` without
 * needing the internal `textFieldWindowPosition` accessor.
 */
private class CaretAnchorPopupPositionProvider(
    private val caretX: Int,
    private val caretBottom: Int,
    private val verticalOffset: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + caretX)
            .coerceAtMost(windowSize.width - popupContentSize.width)
            .coerceAtLeast(0)
        val y = (anchorBounds.top + caretBottom + verticalOffset)
            .coerceAtMost(windowSize.height - popupContentSize.height)
            .coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * Trigger id for the `/`-slash menu. Kept in a single constant so the trigger
 * registration on the screen and the popup guard here can't drift apart.
 */
internal const val NOTION_BLOCK_TRIGGER_ID = "block"

/**
 * Creates a key handler for the editor that drives [NotionSlashMenu].
 *
 * Attach to the editor via `Modifier.onPreviewKeyEvent(handler)` so ↑/↓/Enter/Escape
 * are intercepted while the menu is visible. Returns false when the menu is
 * closed so the text field handles the key normally.
 */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun rememberNotionSlashKeyHandler(
    state: RichTextState,
    highlightedIndex: Int,
    onHighlightChange: (Int) -> Unit,
): (KeyEvent) -> Boolean {
    return remember(state, highlightedIndex, onHighlightChange) {
        handler@{ event ->
            val query = state.activeTriggerQuery
            if (query == null || query.triggerId != NOTION_BLOCK_TRIGGER_ID) return@handler false
            if (event.type != KeyEventType.KeyDown) return@handler false

            val commands = NotionBlockCommand.entries.filter { it.matches(query.query) }
            if (commands.isEmpty()) return@handler false

            when (event.key) {
                Key.DirectionDown -> {
                    onHighlightChange((highlightedIndex + 1).mod(commands.size))
                    true
                }
                Key.DirectionUp -> {
                    onHighlightChange(((highlightedIndex - 1) + commands.size).mod(commands.size))
                    true
                }
                Key.Enter, Key.NumPadEnter -> {
                    val index = highlightedIndex.coerceIn(0, commands.lastIndex)
                    commands.getOrNull(index)?.let { command ->
                        commitNotionCommand(state, command)
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
    }
}

/**
 * Deletes the active `/query` range and applies the block transformation for [command].
 *
 * Call from both the menu's click handler and the key handler's Enter branch so
 * the commit path is identical regardless of how the user selected the command.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun commitNotionCommand(
    state: RichTextState,
    command: NotionBlockCommand,
) {
    val query = state.activeTriggerQuery ?: return
    if (query.triggerId != NOTION_BLOCK_TRIGGER_ID) return

    if (!query.range.collapsed) {
        state.removeTextRange(query.range)
    }
    state.cancelActiveTrigger()

    applyNotionBlockCommand(state, command)
}
