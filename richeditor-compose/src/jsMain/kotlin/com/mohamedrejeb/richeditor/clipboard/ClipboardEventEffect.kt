@file:OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.richPasteEnabled
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@Composable
internal actual fun ClipboardEventEffect(
    richTextState: RichTextState,
    readOnly: Boolean,
) {
    val isReadOnly by rememberUpdatedState(readOnly)
    DisposableEffect(richTextState) {
        // A dialog or popup is its own layer with its own focus, so the editor underneath
        // stays focused while a field in that layer is used. Key events only reach the top
        // layer though: a clipboard event that follows a shortcut the editor never received
        // belongs to that layer. One with no shortcut (browser menu, touch) falls back to focus.
        var shortcutPressed = false

        val keyDownHandler: (Event) -> Unit = { event ->
            if (isClipboardShortcut(event as KeyboardEvent)) {
                shortcutPressed = true
                richTextState.sawClipboardShortcutKey = false
            }
        }

        val keyUpHandler: (Event) -> Unit = { shortcutPressed = false }

        fun ownsClipboardEvents() =
            richTextState.isFocused && (!shortcutPressed || richTextState.sawClipboardShortcutKey)

        val pasteHandler: (Event) -> Unit = { event ->
            if (ownsClipboardEvents() && !isReadOnly) richTextState.pasteFrom(event)
        }

        val copyHandler: (Event) -> Unit = { event ->
            if (ownsClipboardEvents()) richTextState.copySelectionTo(event)
        }

        val cutHandler: (Event) -> Unit = { event ->
            if (ownsClipboardEvents() && !isReadOnly && richTextState.copySelectionTo(event)) {
                richTextState.removeSelectedText()
            }
        }

        // Use capture phase (true) to intercept before the browser's default handler
        document.addEventListener("paste", pasteHandler, true)
        document.addEventListener("copy", copyHandler, true)
        document.addEventListener("cut", cutHandler, true)
        // Capture phase so the reset in keyDownHandler runs before Compose delivers the key.
        document.addEventListener("keydown", keyDownHandler, true)
        document.addEventListener("keyup", keyUpHandler, true)

        onDispose {
            document.removeEventListener("paste", pasteHandler, true)
            document.removeEventListener("copy", copyHandler, true)
            document.removeEventListener("cut", cutHandler, true)
            document.removeEventListener("keydown", keyDownHandler, true)
            document.removeEventListener("keyup", keyUpHandler, true)
        }
    }
}

private fun isClipboardShortcut(event: KeyboardEvent): Boolean {
    val command = event.ctrlKey || event.metaKey
    return when (event.key.lowercase()) {
        "c", "x", "v" -> command
        "insert" -> command || event.shiftKey
        "delete" -> event.shiftKey
        else -> false
    }
}

private fun RichTextState.pasteFrom(event: Event) {
    val html =
        if (config.richPasteEnabled) getClipboardDataHtml(event)
        else null
    val text = if (html.isNullOrBlank()) getClipboardDataText(event) else null
    if (html.isNullOrBlank() && text.isNullOrBlank()) return

    event.preventDefault()
    event.stopPropagation()
    val position = selection.min
    removeSelectedText()
    if (!html.isNullOrBlank()) insertHtml(html = html, position = position)
    else if (text != null) addTextAtIndex(index = position, text = text)
}

/** Returns false, leaving the event to the browser, when there is nothing to copy. */
private fun RichTextState.copySelectionTo(event: Event): Boolean {
    val selection = domClipboardSelection() ?: return false
    event.preventDefault()
    event.stopPropagation()
    if (config.richClipboardEnabled) {
        setClipboardData(event, "text/html", toHtml(selection))
    }
    setClipboardData(event, "text/plain", toText(selection))
    return true
}

@Suppress("UNUSED_PARAMETER")
private fun getClipboardDataHtml(event: Event): String? =
    js("event.clipboardData && event.clipboardData.getData('text/html')") as? String

@Suppress("UNUSED_PARAMETER")
private fun getClipboardDataText(event: Event): String? =
    js("event.clipboardData && event.clipboardData.getData('text/plain')") as? String

@Suppress("UNUSED_PARAMETER")
private fun setClipboardData(event: Event, format: String, data: String) {
    js("event.clipboardData && event.clipboardData.setData(format, data)")
}
