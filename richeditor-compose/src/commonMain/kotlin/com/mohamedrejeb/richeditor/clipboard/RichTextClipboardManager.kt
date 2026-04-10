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
 * Interface for managing clipboard operations with rich text content.
 * This interface provides methods for copying and pasting rich text while preserving formatting.
 */
internal interface RichTextClipboardManager: Clipboard {
    /**
     * Gets the current content from the clipboard.
     * @return [RichTextContent] containing the clipboard content, or null if clipboard is empty
     */
    fun getRichTextContent()

    /**
     * Sets the rich text content to the clipboard.
     * @param content The [RichTextContent] to be set to the clipboard
     */
    fun setRichTextContent()

    /**
     * Checks if the clipboard has content that can be converted to rich text.
     * @return true if clipboard has compatible content, false otherwise
     */
    fun hasRichTextContent(): Boolean
}
