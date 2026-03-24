package com.mohamedrejeb.richeditor.ui

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
internal actual fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler {
    val context = LocalContext.current
    val androidClipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    return remember(state, androidClipboard) {
        AndroidSpannedPasteHandler(state, androidClipboard)
    }
}

private class AndroidSpannedPasteHandler(
    private val state: RichTextState,
    private val clipboardManager: ClipboardManager,
) : SpannedPasteHandler {

    // ------------------------------------------------------------------
    // Read-only: just returns the HTML string, never touches RichTextState
    // ------------------------------------------------------------------
    override fun readHtml(): String? {
        val item = clipboardManager.primaryClip?.getItemAt(0) ?: return null
        // Prefer explicit HTML provided by the source app (Chrome, Docs, Gmail, …)
        return item.htmlText
            ?: run {
                // Fall back: if the clipboard text is a Spanned (copied from an
                // AppCompatEditText), convert Android spans → HTML.
                val text = item.text ?: return null
                if (text is Spanned) spannedToHtml(text) else null
            }
    }

    // ------------------------------------------------------------------
    // TextToolbar / Ctrl+V path: delete selection, insert HTML directly.
    // The IME path is handled via RichTextState.pendingHtmlPaste instead.
    // ------------------------------------------------------------------
    override fun tryPasteSpanned(): Boolean {
        val html = readHtml() ?: return false

        if (!state.selection.collapsed) {
            val selMin = state.selection.min
            val trimmed = state.textFieldValue.text.removeRange(selMin, state.selection.max)
            state.onTextFieldValueChange(
                TextFieldValue(text = trimmed, selection = TextRange(selMin))
            )
        }

        state.insertHtmlAfterSelection(html)
        return true
    }

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private fun spannedToHtml(spanned: Spanned): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
        } else {
            Html.toHtml(spanned)
        }
}
