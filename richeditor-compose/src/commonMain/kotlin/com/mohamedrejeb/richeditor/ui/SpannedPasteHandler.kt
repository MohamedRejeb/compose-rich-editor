package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Platform-specific handler that intercepts paste actions and attempts to paste styled
 * (span-aware / HTML) content from the platform clipboard instead of plain text.
 *
 * On Android this reads [ClipData.Item.htmlText] (set by Chrome, Docs, etc.) or converts
 * an [android.text.Spanned] value (set by AppCompatEditText copies) via [android.text.Html].
 * On other platforms it is a no-op and returns `false` so the default paste flow is used.
 */
internal interface SpannedPasteHandler {
    /**
     * Reads the platform clipboard and, if it contains styled text (HTML or Android Spans),
     * deletes any active selection and inserts that styled content at the cursor position.
     *
     * @return `true` if styled content was consumed; `false` to fall back to plain-text paste.
     */
    fun tryPasteSpanned(): Boolean
}

@Composable
internal expect fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler
