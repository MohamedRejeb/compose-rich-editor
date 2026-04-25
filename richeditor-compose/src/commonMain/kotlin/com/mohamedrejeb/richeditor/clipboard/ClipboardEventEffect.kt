package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Registers platform-specific clipboard event handlers for keyboard shortcuts (Ctrl+C/V/X).
 *
 * On web platforms (JS/WasmJS), the browser handles Ctrl+C/V/X at the DOM level,
 * bypassing Compose's [Clipboard] interface. This effect intercepts those DOM clipboard
 * events to handle rich text copy/paste with HTML formatting.
 *
 * On native platforms (Android/iOS/Desktop), this is a no-op since the OS routes clipboard
 * operations through the Compose framework.
 */
@Composable
internal expect fun ClipboardEventEffect(richTextState: RichTextState)
