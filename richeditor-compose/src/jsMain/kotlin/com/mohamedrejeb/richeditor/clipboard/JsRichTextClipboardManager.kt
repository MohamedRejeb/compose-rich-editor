package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardItem
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.w3c.files.Blob

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    JsRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * JavaScript implementation of [RichTextClipboardManager].
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
internal class JsRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        try {
            val items: Array<ClipboardItem> = clipboard.nativeClipboard.read().await()

            for (item in items) {
                if ("text/html" in item.types) {
                    val blob = item.getType("text/html").await<Blob>()
                    val html = getBlobText(blob).await() as? String
                    if (html != null) {
                        richTextState.pendingClipboardHtml = html
                    }
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

            val clipboardItem = createHtmlClipboardItem(html, text)
            clipboard.nativeClipboard.write(clipboardItem).await<Nothing>()
        } catch (e: Exception) {
            // Fall back to default clipboard behavior
            clipboard.setClipEntry(clipEntry)
        }
    }
}

// JS interop helpers

@Suppress("UNUSED_PARAMETER")
private fun getBlobText(blob: Blob): Promise<dynamic> =
    js("blob.text()")

@OptIn(ExperimentalComposeUiApi::class)
private fun createHtmlClipboardItem(html: String, text: String): Array<ClipboardItem> = js(
    """
    [new ClipboardItem({
        'text/html': new Blob([html], { type: 'text/html' }),
        'text/plain': new Blob([text], { type: 'text/plain' })
    })]
    """
)
