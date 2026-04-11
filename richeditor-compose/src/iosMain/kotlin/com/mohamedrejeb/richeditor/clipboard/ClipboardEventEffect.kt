package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
internal actual fun ClipboardEventEffect(richTextState: RichTextState) {
    // No-op: iOS routes clipboard operations through the Compose framework
}
