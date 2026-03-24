package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIPasteboard

private const val HTML_UTI = "public.html"

private class IosSpannedPasteHandler(
    private val state: RichTextState,
) : SpannedPasteHandler {

    /** Reads HTML from UIPasteboard, trying string then NSData (UTF-8). */
    private fun readHtmlFromPasteboard(): String? {
        val pasteboard = UIPasteboard.generalPasteboard
        val value = pasteboard.valueForPasteboardType(HTML_UTI) ?: return null
        val raw = if (value is String) value else {
            // iOS sometimes stores HTML as NSData
            val data = value as? NSData ?: pasteboard.dataForPasteboardType(HTML_UTI) ?: return null
            @Suppress("UNCHECKED_CAST")
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String ?: return null
        }
        return stripSourceBackgroundColor(raw)
    }

    /**
     * iOS clipboard HTML carries the source document's background-color (typically white
     * or transparent) on every span.  Strip those so they don't render as white boxes
     * behind the text inside the editor.
     *
     * Intentional highlight colors (yellow, blue, etc.) are left intact because only
     * white, near-white, and transparent values are removed.
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

    override fun readHtml(): String? {
        pasteLog(PASTE_TAG, "iOS readHtml: checking UIPasteboard")
        val html = readHtmlFromPasteboard()
        if (html != null) {
            pasteLog(PASTE_TAG, "iOS readHtml: found HTML (${html.length} chars)")
        } else {
            pasteLog(PASTE_TAG, "iOS readHtml: no HTML on clipboard")
        }
        return html
    }

    override fun tryPasteSpanned(): Boolean {
        pasteLog(PASTE_TAG, "iOS tryPasteSpanned: called")
        val html = readHtml() ?: run {
            pasteLog(PASTE_TAG, "iOS tryPasteSpanned: no HTML — falling back to plain")
            return false
        }

        if (!state.selection.collapsed) {
            val selMin = state.selection.min
            val trimmed = state.textFieldValue.text.removeRange(selMin, state.selection.max)
            state.onTextFieldValueChange(
                TextFieldValue(text = trimmed, selection = TextRange(selMin))
            )
        }

        pasteLog(PASTE_TAG, "iOS tryPasteSpanned: inserting HTML at cursor=${state.selection.max}")
        state.insertHtmlAfterSelection(html)
        pasteLog(PASTE_TAG, "iOS tryPasteSpanned: done")
        return true
    }

    override fun getHtmlIfMatch(addedText: String): String? {
        pasteLog(PASTE_TAG, "iOS getHtmlIfMatch: checking for \"${addedText.take(80)}\"")
        val pasteboard = UIPasteboard.generalPasteboard
        val clipPlain = pasteboard.string ?: run {
            pasteLog(PASTE_TAG, "iOS getHtmlIfMatch: no plain text on clipboard")
            return null
        }
        if (clipPlain.trimEnd() != addedText.trimEnd()) {
            pasteLog(PASTE_TAG, "iOS getHtmlIfMatch: no match — clip=\"${clipPlain.take(80)}\" vs added=\"${addedText.take(80)}\"")
            return null
        }
        val html = readHtmlFromPasteboard()
        if (html != null) {
            pasteLog(PASTE_TAG, "iOS getHtmlIfMatch: match + HTML found (${html.length} chars)")
        } else {
            pasteLog(PASTE_TAG, "iOS getHtmlIfMatch: match but no HTML — plain paste")
        }
        return html
    }
}

@Composable
internal actual fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler =
    remember(state) { IosSpannedPasteHandler(state) }
