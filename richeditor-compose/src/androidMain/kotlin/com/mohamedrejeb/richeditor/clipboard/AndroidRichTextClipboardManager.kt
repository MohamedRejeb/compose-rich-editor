@file:OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.AndroidClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.richPasteEnabled

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
) : RichTextClipboardManager, AndroidClipboard, Clipboard by clipboard {

    /**
     * Compose foundation requires the [androidx.compose.ui.platform.LocalClipboard]
     * instance to be an [AndroidClipboard] and reaches the platform
     * [ClipboardManager] through it (#744). Delegate to the wrapped platform
     * clipboard's manager. Diagnosis credit: @ErnestoOlalla, PR #745.
     */
    override val clipboardManager: ClipboardManager
        get() = (clipboard as? AndroidClipboard)?.clipboardManager
            ?: error("AndroidRichTextClipboardManager requires the platform AndroidClipboard")

    override suspend fun getClipEntry(): ClipEntry? {
        if (!richTextState.config.richPasteEnabled)
            return clipboard.getClipEntry()

        try {
            val entry = clipboard.getClipEntry() ?: return null
            val clipData = entry.clipData
            if (clipData.itemCount > 0) {
                val htmlText = clipData.getItemAt(0).htmlText
                if (htmlText != null) {
                    richTextState.pendingClipboardHtml = htmlText
                    richTextState.pendingClipboardPlainText =
                        clipData.getItemAt(0).text?.toString()
                }
            }
            return entry
        } catch (e: Exception) {
            e.printStackTrace()
            return clipboard.getClipEntry()
        }
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (!richTextState.config.richClipboardEnabled) {
            val content = richTextState.takeClipboardContent()
            if (clipEntry == null || content == null) {
                clipboard.setClipEntry(clipEntry)
                return
            }
            // The raw ClipEntry carries the editor's internal rendering (paragraphs joined
            // by spaces, list prefixes included); a plain-text copy must use toText.
            val text = content.toText()
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
            return
        }

        if (clipEntry == null) {
            clipboard.setClipEntry(null)
            return
        }

        try {
            val content = richTextState.takeClipboardContent()

            if (content == null) {
                clipboard.setClipEntry(null)
                return
            }

            val html = content.toHtml()
            val text = content.toText()
            val newClipData = ClipData.newHtmlText("rich text", text, html)
            clipboard.setClipEntry(ClipEntry(newClipData))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
