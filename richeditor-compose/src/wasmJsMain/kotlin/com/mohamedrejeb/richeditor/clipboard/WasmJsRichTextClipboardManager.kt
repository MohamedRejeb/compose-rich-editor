@file:OptIn(ExperimentalWasmJsInterop::class)

package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardItem
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.await
import kotlin.js.Promise

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    WasmJsRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * WebAssembly JavaScript implementation of [RichTextClipboardManager].
 * Handles rich text clipboard operations using the Web Clipboard API with text/html MIME type.
 *
 * On paste, [getClipEntry] extracts HTML from the clipboard and stores it in
 * [RichTextState.pendingClipboardHtml]. The actual HTML insertion happens in
 * [RichTextState.onTextFieldValueChange] when text is added.
 *
 * Note: Keyboard shortcuts (Ctrl+C/V/X) bypass this manager entirely on web and are
 * handled by [ClipboardEventEffect] via DOM event listeners instead.
 *
 * @property richTextState The [RichTextState] to be used for clipboard operations
 * @property clipboard The Compose [Clipboard] for handling clipboard operations
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class WasmJsRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        try {
            val items = clipboard.nativeClipboard.read().await<JsArray<ClipboardItem>>()

            for (i in 0 until items.length) {
                val item = items[i] ?: continue
                val types = item.types
                val hasHtml = (0 until types.length).any { types[it].toString() == "text/html" }

                if (hasHtml) {
                    val blob: JsAny = item.getType("text/html".toJsString()).await<JsAny>()
                    val html = getBlobText(blob).await<JsString>().toString()
                    richTextState.pendingClipboardHtml = html
                    break
                }
            }
        } catch (e: Exception) {
            // Clipboard API may fail (permissions, insecure context)
        }

        return clipboard.getClipEntry()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            clipboard.setClipEntry(null)
            return
        }

        try {
            val copySelection = richTextState.copySelection

            if (copySelection == null || copySelection.collapsed) {
                clipboard.setClipEntry(null)
                return
            }

            val html = richTextState.toHtml(copySelection)
            val text = richTextState.toText(copySelection)

            val clipboardItem = createHtmlClipboardItem(html.toJsString(), text.toJsString())
            clipboard.nativeClipboard.write(clipboardItem).await<Nothing>()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fall back to default clipboard behavior
            clipboard.setClipEntry(clipEntry)
        }
    }
}

// Wasm JS interop helpers

private fun getBlobText(blob: JsAny): Promise<JsString> =
    js("blob.text()")

@OptIn(ExperimentalComposeUiApi::class)
private fun createHtmlClipboardItem(html: JsString, text: JsString): JsArray<ClipboardItem> = js(
    """
    [new ClipboardItem({
        'text/html': new Blob([html], { type: 'text/html' }),
        'text/plain': new Blob([text], { type: 'text/plain' })
    })]
    """
)
