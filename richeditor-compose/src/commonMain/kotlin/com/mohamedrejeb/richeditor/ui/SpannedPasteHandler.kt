package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Platform-specific handler for span-aware / HTML clipboard content.
 *
 * On Android this reads [ClipData.Item.htmlText] (set by Chrome, Docs, etc.) or converts
 * an [android.text.Spanned] value (set by AppCompatEditText copies) via [android.text.Html].
 * On other platforms both methods are no-ops.
 */
internal interface SpannedPasteHandler {
    /**
     * Returns the HTML string currently on the platform clipboard, or `null` if the clipboard
     * holds only unstyled plain text.  Does **not** modify any state.
     */
    fun readHtml(): String?

    /**
     * Convenience: reads HTML, deletes any active selection, then inserts the styled content at
     * the cursor.  Used for TextToolbar "Paste" and hardware-keyboard Ctrl/Cmd+V intercepts.
     *
     * @return `true` if styled content was applied; `false` to fall back to plain-text paste.
     */
    fun tryPasteSpanned(): Boolean
}

@Composable
internal expect fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler
