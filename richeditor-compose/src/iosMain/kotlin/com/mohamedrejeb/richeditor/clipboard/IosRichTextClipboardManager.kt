package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    IosRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * iOS implementation of [RichTextClipboardManager].
 * Handles rich text clipboard operations using iOS's UIPasteboard with public.html UTI.
 *
 * On paste, [getClipEntry] extracts HTML from the pasteboard and stores it in
 * [RichTextState.pendingClipboardHtml]. The actual HTML insertion happens in
 * [RichTextState.onTextFieldValueChange] when text is added.
 *
 * @property richTextState The [RichTextState] to be used for clipboard operations
 * @property clipboard The Compose [Clipboard] for handling clipboard operations
 */
@OptIn(BetaInteropApi::class, ExperimentalComposeUiApi::class)
internal class IosRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        try {
            val pasteboard = clipboard.nativeClipboard
            val htmlData = pasteboard.dataForPasteboardType(HTML_UTI)
            if (htmlData != null) {
                val html = NSString.create(data = htmlData, encoding = NSUTF8StringEncoding)
                    ?.toString()
                if (html != null) {
                    richTextState.pendingClipboardHtml = html
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            val pasteboard = clipboard.nativeClipboard

            val htmlData = NSString.create(string = html)
                .dataUsingEncoding(NSUTF8StringEncoding)
            val textData = NSString.create(string = text)
                .dataUsingEncoding(NSUTF8StringEncoding)

            if (htmlData != null && textData != null) {
                pasteboard.items = listOf(
                    mapOf(
                        HTML_UTI to htmlData,
                        PLAIN_TEXT_UTI to textData,
                    )
                )
            } else {
                pasteboard.string = text
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val HTML_UTI = "public.html"
        const val PLAIN_TEXT_UTI = "public.utf8-plain-text"
    }
}
