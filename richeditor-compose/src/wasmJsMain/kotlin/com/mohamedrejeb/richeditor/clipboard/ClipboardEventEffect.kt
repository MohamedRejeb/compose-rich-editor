@file:OptIn(ExperimentalWasmJsInterop::class)

package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
internal actual fun ClipboardEventEffect(richTextState: RichTextState) {
    DisposableEffect(richTextState) {
        val pasteHandler: (Event) -> Unit = handler@{ event ->
            if (!richTextState.isFocused) return@handler

            val html = getClipboardDataHtml(event)?.toString()
            if (!html.isNullOrBlank()) {
                event.preventDefault()
                event.stopPropagation()
                val position = richTextState.selection.min
                richTextState.removeSelectedText()
                richTextState.insertHtml(html = html, position = position)
                return@handler
            }

            val text = getClipboardDataText(event)?.toString()
            if (!text.isNullOrBlank()) {
                event.preventDefault()
                event.stopPropagation()
                val position = richTextState.selection.min
                richTextState.removeSelectedText()
                richTextState.addTextAtIndex(index = position, text = text)
            }
        }

        val copyHandler: (Event) -> Unit = handler@{ event ->
            if (!richTextState.isFocused) return@handler

            val selection = richTextState.copySelection ?: return@handler
            event.preventDefault()
            event.stopPropagation()
            val html = richTextState.toHtml(selection)
            val text = richTextState.toText(selection)
            setClipboardData(event, "text/html".toJsString(), html.toJsString())
            setClipboardData(event, "text/plain".toJsString(), text.toJsString())
        }

        val cutHandler: (Event) -> Unit = handler@{ event ->
            if (!richTextState.isFocused) return@handler

            val selection = richTextState.copySelection ?: return@handler
            event.preventDefault()
            event.stopPropagation()
            val html = richTextState.toHtml(selection)
            val text = richTextState.toText(selection)
            setClipboardData(event, "text/html".toJsString(), html.toJsString())
            setClipboardData(event, "text/plain".toJsString(), text.toJsString())
            richTextState.removeSelectedText()
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
