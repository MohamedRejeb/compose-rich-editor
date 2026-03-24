package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

private object NoOpSpannedPasteHandler : SpannedPasteHandler {
    override fun tryPasteSpanned(): Boolean = false
}

@Composable
internal actual fun rememberSpannedPasteHandler(state: RichTextState): SpannedPasteHandler =
    NoOpSpannedPasteHandler
