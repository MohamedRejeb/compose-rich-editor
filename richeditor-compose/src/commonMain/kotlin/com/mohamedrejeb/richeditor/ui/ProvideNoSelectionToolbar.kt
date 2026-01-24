package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.Composable

/**
 * Platform hook used to disable the system text selection toolbar (ActionMode: cut/copy/paste)
 * when [disableSelectionToolbar] is true.
 *
 * Implemented per-platform to avoid referencing Android-only APIs from common code.
 */
@Composable
internal expect fun ProvideNoSelectionToolbar(
    disableSelectionToolbar: Boolean,
    content: @Composable () -> Unit,
)
