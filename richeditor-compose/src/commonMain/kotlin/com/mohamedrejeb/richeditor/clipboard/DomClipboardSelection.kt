package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * The range a DOM copy or cut event acts on: the current selection, or nothing.
 *
 * The browser fires these events for a collapsed caret too. [RichTextState.copySelection]
 * falls back to the last non-collapsed selection, which exists for the touch toolbar's copy
 * (it collapses the selection before the clipboard is written). A DOM event never arrives
 * that way, so the fallback would only ever copy content the user no longer has selected,
 * possibly from text that has changed since. With a caret the handler leaves the event to
 * the browser, which copies nothing from a canvas.
 */
internal fun RichTextState.domClipboardSelection(): TextRange? =
    selection.takeUnless { it.collapsed }
