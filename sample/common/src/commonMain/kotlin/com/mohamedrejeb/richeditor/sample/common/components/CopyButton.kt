package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay

/**
 * Small chip that copies the provided string to the system clipboard and shows a brief "Copied"
 * confirmation. Used on the export panels of the Images, Links and Mentions samples.
 *
 * Uses [LocalClipboardManager] (deprecated in favour of suspend-based LocalClipboard) because
 * the synchronous API is simpler for a sample and works uniformly across every CMP target.
 */
@Suppress("DEPRECATION")
@Composable
fun CopyButton(
    content: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    AssistChip(
        onClick = {
            clipboard.setText(AnnotatedString(content))
            copied = true
        },
        label = { Text(if (copied) "Copied" else "Copy") },
        leadingIcon = {
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier,
    )
}
