package com.mohamedrejeb.richeditor.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.AndroidClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    if (clipboard.isAndroidClipboard()) {
        AndroidClipboardRichTextClipboardManager(
            richTextState = richTextState,
            clipboard = clipboard
        )
    } else {
        AndroidRichTextClipboardManager(
            richTextState = richTextState,
            clipboard = clipboard
        )
    }

/**
 * True when the platform clipboard implements [AndroidClipboard].
 *
 * [AndroidClipboard] only exists in compose-ui 1.12+; on older Compose versions
 * resolving the type throws [LinkageError], in which case the plain wrapper is
 * safe to use (foundation's `Clipboard.nativeClipboardManager` type check does
 * not exist there either).
 */
private fun Clipboard.isAndroidClipboard(): Boolean =
    try {
        this is AndroidClipboard
    } catch (e: LinkageError) {
        false
    }

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
internal open class AndroidRichTextClipboardManager(
    private val richTextState: RichTextState,
    protected val clipboard: Clipboard,
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

/**
 * Compose 1.12+ variant of [AndroidRichTextClipboardManager] that is itself an
 * [AndroidClipboard].
 *
 * Since compose-ui 1.12, foundation's paste-availability path
 * (`ClipboardUtils.hasText` -> `Clipboard.nativeClipboardManager`) runs
 * `require(this is AndroidClipboard)` on `LocalClipboard.current`. Because
 * BasicRichTextEditor replaces `LocalClipboard` with this wrapper, the wrapper
 * must implement [AndroidClipboard] or any long-press inside the editor throws
 * `IllegalArgumentException`.
 *
 * Only instantiated when the wrapped clipboard is an [AndroidClipboard]
 * (see [isAndroidClipboard]), so this class is never loaded on Compose < 1.12.
 */
internal class AndroidClipboardRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard,
) : AndroidRichTextClipboardManager(richTextState, clipboard), AndroidClipboard {

    override val clipboardManager: android.content.ClipboardManager
        get() = (clipboard as AndroidClipboard).clipboardManager

    // Explicit override resolves the diamond between Clipboard-by-delegation
    // (inherited from the superclass) and AndroidClipboard's default.
    @Deprecated(
        message = "Use [nativeClipboardManager] extension instead",
        replaceWith = ReplaceWith("nativeClipboardManager"),
    )
    override val nativeClipboard: android.content.ClipboardManager
        get() = clipboardManager
}
