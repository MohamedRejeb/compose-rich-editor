package com.mohamedrejeb.richeditor.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    AndroidRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * Android implementation of [RichTextClipboardManager].
 * Handles rich text clipboard operations using Android's ClipData with text/html MIME type.
 *
 * On paste, [getClipEntry] extracts HTML from the clipboard and stores it in
 * [RichTextState.pendingClipboardHtml]. The actual HTML insertion happens in
 * [RichTextState.onTextFieldValueChange] when text is added. This avoids treating
 * non-paste calls to [getClipEntry] (e.g. clipboard availability checks) as paste.
 *
 * @property richTextState The [RichTextState] to be used for clipboard operations
 * @property clipboard The Compose [Clipboard] for handling clipboard operations
 */
internal class AndroidRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        try {
            val entry = clipboard.getClipEntry() ?: return null
            val clipData = entry.clipData
            if (clipData.itemCount > 0) {
                val htmlText = clipData.getItemAt(0).htmlText
                if (htmlText != null) {
                    richTextState.pendingClipboardHtml = htmlText
                }
            }
            return entry
        } catch (e: Exception) {
            e.printStackTrace()
            return clipboard.getClipEntry()
        }
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
            val newClipData = ClipData.newHtmlText("rich text", text, html)
            clipboard.setClipEntry(ClipEntry(newClipData))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
