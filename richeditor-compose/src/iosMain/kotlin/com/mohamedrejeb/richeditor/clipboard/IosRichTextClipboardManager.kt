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
                    richTextState.pendingClipboardHtml = stripSourceBackgroundColor(html)
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

    /**
     * iOS clipboard HTML carries the source document's background-color (typically white
     * or transparent) on every span. Strip those so they don't render as white boxes
     * behind the text inside the editor.
     *
     * Only white, near-white, and transparent values are removed. Intentional highlight
     * colors (yellow, blue, etc.) are left intact.
     */
    private fun stripSourceBackgroundColor(html: String): String =
        html.replace(
            Regex(
                """background-color\s*:\s*""" +
                """(?:white|transparent""" +
                """|#[fF]{3,6}""" +
                """|rgb\(\s*255\s*,\s*255\s*,\s*255\s*\)""" +
                """|rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*(?:0|1|1\.0|0\.0)\s*\)""" +
                """)\s*;?""",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
}
