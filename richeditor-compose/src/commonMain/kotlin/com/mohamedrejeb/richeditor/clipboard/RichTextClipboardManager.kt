package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Creates a new instance of [RichTextClipboardManager]
 * @param richTextState The [RichTextState] to be used for clipboard operations
 * @param clipboard The Compose [Clipboard] for handling clipboard operations
 * @return A new instance of [RichTextClipboardManager]
 */
internal expect fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard,
): RichTextClipboardManager

/**
 * Platform-specific clipboard manager that intercepts [Clipboard] operations
 * to support rich text (HTML) copy/paste.
 *
 * Each platform implementation reads HTML from the native clipboard in [getClipEntry]
 * and writes HTML + plain text in [setClipEntry].
 */
internal interface RichTextClipboardManager : Clipboard
