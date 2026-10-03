@file:OptIn(ExperimentalWasmJsInterop::class, com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
internal actual fun ClipboardEventEffect(
    richTextState: RichTextState,
    readOnly: Boolean,
) {
    val isReadOnly by rememberUpdatedState(readOnly)
    DisposableEffect(richTextState) {
        val pasteHandler: (Event) -> Unit = { event ->
            if (richTextState.isFocused && !isReadOnly) richTextState.pasteFrom(event)
        }

        val copyHandler: (Event) -> Unit = { event ->
            if (richTextState.isFocused) richTextState.copySelectionTo(event)
        }

        val cutHandler: (Event) -> Unit = { event ->
            if (richTextState.isFocused && !isReadOnly && richTextState.copySelectionTo(event)) {
                richTextState.removeSelectedText()
            }
        }

        // Use capture phase (true) to intercept before the browser's default handler
        document.addEventListener("paste", pasteHandler, true)
        document.addEventListener("copy", copyHandler, true)
        document.addEventListener("cut", cutHandler, true)

        onDispose {
            document.removeEventListener("paste", pasteHandler, true)
            document.removeEventListener("copy", copyHandler, true)
            document.removeEventListener("cut", cutHandler, true)
        }
    }
}

private fun RichTextState.pasteFrom(event: Event) {
    val html =
        if (config.richClipboardEnabled) getClipboardDataHtml(event)?.toString()
        else null
    val text = if (html.isNullOrBlank()) getClipboardDataText(event)?.toString() else null
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
        setClipboardData(event, "text/html".toJsString(), toHtml(selection).toJsString())
    }
    setClipboardData(event, "text/plain".toJsString(), toText(selection).toJsString())
    return true
}

private fun getClipboardDataHtml(event: Event): JsString? =
    getClipboardData(event, "text/html".toJsString())

private fun getClipboardDataText(event: Event): JsString? =
    getClipboardData(event, "text/plain".toJsString())

@Suppress("UNUSED_PARAMETER")
private fun getClipboardData(event: Event, format: JsString): JsString? =
    js("event.clipboardData && event.clipboardData.getData(format)")

@Suppress("UNUSED_PARAMETER")
private fun setClipboardData(event: Event, format: JsString, data: JsString) {
    js("event.clipboardData && event.clipboardData.setData(format, data)")
}
