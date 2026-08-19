@file:OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.awtClipboard
import com.mohamedrejeb.richeditor.model.RichTextState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    DesktopRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * Desktop implementation of [RichTextClipboardManager].
 * Handles rich text clipboard operations using AWT's clipboard functionality.
 *
 * On paste, [getClipEntry] extracts HTML from the AWT clipboard and stores it in
 * [RichTextState.pendingClipboardHtml]. The actual HTML insertion happens in
 * [RichTextState.onTextFieldValueChange] when text is added.
 *
 * @property richTextState The [RichTextState] to be used for clipboard operations
 * @property clipboard The Compose [Clipboard] for handling clipboard operations
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class DesktopRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        if (!richTextState.config.richClipboardEnabled)
            return clipboard.getClipEntry()

        try {
            val transferable = awtClipboard?.getContents(null)
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor)) {
                val rawHtmlText =
                    withContext(Dispatchers.IO) {
                        transferable.getTransferData(DataFlavor.fragmentHtmlFlavor)
                    } as String
                richTextState.pendingClipboardHtml = rawHtmlText
                if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    richTextState.pendingClipboardPlainText =
                        withContext(Dispatchers.IO) {
                            transferable.getTransferData(DataFlavor.stringFlavor)
                        } as? String
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return clipboard.getClipEntry()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (!richTextState.config.richClipboardEnabled) {
            val copySelection = richTextState.copySelection
            if (clipEntry == null || copySelection == null || copySelection.collapsed) {
                clipboard.setClipEntry(clipEntry)
                return
            }
            // The raw ClipEntry carries the editor's internal rendering (paragraphs joined
            // by spaces, list prefixes included); a plain-text copy must use toText.
            awtClipboard?.setContents(StringSelection(richTextState.toText(copySelection)), null)
            return
        }

        if (clipEntry == null) {
            clipboard.setClipEntry(null)
            return
        }

        val copySelection = richTextState.copySelection

        if (copySelection == null || copySelection.collapsed) {
            clipboard.setClipEntry(null)
            return
        }

        val html = richTextState.toHtml(copySelection)
        val text = richTextState.toText(copySelection)

        val htmlSelection = object : StringSelection(html), Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> =
                arrayOf(DataFlavor.fragmentHtmlFlavor, DataFlavor.stringFlavor)

            override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
                flavor in getTransferDataFlavors()

            override fun getTransferData(flavor: DataFlavor?): Any = when (flavor) {
                DataFlavor.fragmentHtmlFlavor -> html
                DataFlavor.stringFlavor -> text
                else -> throw UnsupportedOperationException("Unsupported flavor: $flavor")
            }
        }

        awtClipboard?.setContents(htmlSelection, null)
    }
}
