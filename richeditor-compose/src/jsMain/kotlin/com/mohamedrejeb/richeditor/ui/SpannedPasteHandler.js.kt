package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

private object NoOpSpannedPasteHandler : SpannedPasteHandler {
    override fun readHtml(): String? = null
    override fun tryPasteSpanned(): Boolean = false
    override fun getHtmlIfMatch(addedText: String): String? = null
}

@Composable
internal actual fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler =
    NoOpSpannedPasteHandler
