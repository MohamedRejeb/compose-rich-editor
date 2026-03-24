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

    override fun readHtml(): String? {
        val clip = clipboardManager.primaryClip
        if (clip == null) {
            pasteLog(PASTE_TAG, "readHtml: primaryClip is null — nothing on clipboard")
            return null
        }
        pasteLog(PASTE_TAG, "readHtml: clip has ${clip.itemCount} item(s), " +
                "mimeTypes=${List(clip.description.mimeTypeCount) { clip.description.getMimeType(it) }}")

        val item = clip.getItemAt(0)
        val htmlText = item.htmlText
        if (htmlText != null) {
            pasteLog(PASTE_TAG, "readHtml: htmlText found (${htmlText.length} chars) — using directly")
            return htmlText
        }

        val text = item.text
        pasteLog(PASTE_TAG, "readHtml: htmlText=null, text type=${text?.javaClass?.simpleName ?: "null"}")
        if (text is Spanned) {
            val converted = spannedToHtml(text)
            pasteLog(PASTE_TAG, "readHtml: Spanned converted to HTML (${converted.length} chars): $converted")
            return converted
        }

        pasteLog(PASTE_TAG, "readHtml: no HTML or Spanned found — plain text paste will proceed normally")
        return null
    }

    override fun tryPasteSpanned(): Boolean {
        pasteLog(PASTE_TAG, "tryPasteSpanned: called (TextToolbar / Ctrl+V path)")
        val html = readHtml() ?: run {
            pasteLog(PASTE_TAG, "tryPasteSpanned: readHtml returned null — falling back to plain paste")
            return false
        }

        if (!state.selection.collapsed) {
            pasteLog(PASTE_TAG, "tryPasteSpanned: deleting selection ${state.selection.min}..${state.selection.max} before insert")
            val selMin = state.selection.min
            val trimmed = state.textFieldValue.text.removeRange(selMin, state.selection.max)
            state.onTextFieldValueChange(
                TextFieldValue(text = trimmed, selection = TextRange(selMin))
            )
        }

        pasteLog(PASTE_TAG, "tryPasteSpanned: calling insertHtmlAfterSelection at cursor=${state.selection.max}")
        state.insertHtmlAfterSelection(html)
        pasteLog(PASTE_TAG, "tryPasteSpanned: done")
        return true
    }

    override fun getHtmlIfMatch(addedText: String): String? {
        pasteLog(PASTE_TAG, "getHtmlIfMatch: checking clipboard for addedText=\"${addedText.take(80)}\"")
        val clip = clipboardManager.primaryClip ?: run {
            pasteLog(PASTE_TAG, "getHtmlIfMatch: primaryClip is null")
            return null
        }
        val item = clip.getItemAt(0)
        val clipPlain = item.text?.toString() ?: run {
            pasteLog(PASTE_TAG, "getHtmlIfMatch: clip has no text item")
            return null
        }
        // Trim trailing newline that Android sometimes appends
        if (clipPlain.trimEnd() != addedText.trimEnd()) {
            pasteLog(PASTE_TAG, "getHtmlIfMatch: no match — clip=\"${clipPlain.take(80)}\" vs added=\"${addedText.take(80)}\"")
            return null
        }
        val html = item.htmlText ?: (item.text as? Spanned)?.let { spannedToHtml(it) }
        if (html != null) {
            pasteLog(PASTE_TAG, "getHtmlIfMatch: match + HTML found (${html.length} chars) — IME paste will be styled")
        } else {
            pasteLog(PASTE_TAG, "getHtmlIfMatch: match but no HTML/Spanned — plain paste")
        }
        return html
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
