package com.mohamedrejeb.richeditor.sample.common.links

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.components.StatusBadge
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText

private data class QuickLink(val label: String, val url: String)

private val quickLinks = listOf(
    QuickLink("Compose Rich Editor", "https://github.com/MohamedRejeb/Compose-Rich-Editor"),
    QuickLink("Compose Multiplatform", "https://www.jetbrains.com/lp/compose-multiplatform/"),
    QuickLink("Material 3", "https://m3.material.io/"),
)

private const val SeedHtml = """
<p>Compose Rich Editor ships with first-class link support - try selecting any text and pressing
<b>Add link</b>, or click the <a href="https://github.com/MohamedRejeb/Compose-Rich-Editor">existing link</a>
to edit it. You can also <a href="https://m3.material.io/">style them</a> via RichTextConfig.</p>
"""

private enum class ViewMode(val label: String, val icon: ImageVector) {
    Editor("Editor", Icons.Outlined.Edit),
    Reader("RichText", Icons.Outlined.Visibility),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LinksSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()
    var showDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.Editor) }

    LaunchedEffect(Unit) {
        state.config.linkColor = SampleAccents.Sky
        state.config.linkTextDecoration = TextDecoration.Underline
        state.setHtml(SeedHtml)
    }

    val isLink = state.isLink
    val selectedText = state.selectedLinkText
    val selectedUrl = state.selectedLinkUrl
    val selectionCollapsed = state.selection.collapsed
    val isEditorMode = viewMode == ViewMode.Editor

    val noFocus = Modifier.focusProperties { canFocus = false }

    SampleScaffold(
        title = "Links",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(4.dp))

            IntroCard()

            ToolbarCard(enabled = isEditorMode) {
                ChipAction(
                    label = if (isLink) "Edit link" else "Add link",
                    icon = if (isLink) Icons.Outlined.Edit else Icons.Outlined.Link,
                    enabled = isEditorMode,
                    onClick = { showDialog = true },
                    modifier = noFocus,
                )
                ChipAction(
                    label = "Remove link",
                    icon = Icons.Outlined.LinkOff,
                    enabled = isEditorMode && isLink,
                    onClick = { state.removeLink() },
                    modifier = noFocus,
                )
                ChipAction(
                    label = "Reset",
                    icon = Icons.Outlined.Delete,
                    enabled = isEditorMode,
                    onClick = { state.setHtml(SeedHtml) },
                    modifier = noFocus,
                )
            }

            QuickInsertCard(
                enabled = isEditorMode,
                onInsert = { quickLink ->
                    if (selectionCollapsed) {
                        state.addLink(text = quickLink.label, url = quickLink.url)
                    } else {
                        state.addLinkToSelection(url = quickLink.url)
                    }
                },
            )

            StatusCard(
                isLink = isLink,
                selectedText = selectedText,
                selectedUrl = selectedUrl,
                selectionCollapsed = selectionCollapsed,
                isEditorMode = isEditorMode,
            )

            ViewModePicker(
                selected = viewMode,
                onSelect = { viewMode = it },
            )

            EditorOrReaderCard(state = state, viewMode = viewMode)

            ProgrammaticDemo(
                enabled = isEditorMode,
                onApply = {
                    val text = state.annotatedString.text
                    val needle = "RichTextConfig"
                    val start = text.indexOf(needle)
                    if (start >= 0) {
                        state.addLinkToTextRange(
                            url = "https://github.com/MohamedRejeb/Compose-Rich-Editor",
                            textRange = TextRange(start, start + needle.length),
                        )
                    }
                },
            )

            ExportCard(
                title = "HTML output",
                accent = SampleAccents.Coral,
                content = remember(state.annotatedString) { state.toHtml() },
            )

            ExportCard(
                title = "Markdown output",
                accent = SampleAccents.Amber,
                content = remember(state.annotatedString) { state.toMarkdown() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDialog) {
        LinkDialog(
            initialText = selectedText.orEmpty().ifEmpty {
                if (!selectionCollapsed) state.annotatedString.text.substring(
                    state.selection.min,
                    state.selection.max,
                ) else ""
            },
            initialUrl = selectedUrl.orEmpty(),
            isEditing = isLink,
            isSelectionCollapsed = selectionCollapsed,
            onSubmit = { text, url ->
                when {
                    isLink -> state.updateLink(url = url)
                    selectionCollapsed -> state.addLink(text = text, url = url)
                    else -> state.addLinkToSelection(url = url)
                }
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Sky,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Use the Editor view to add, edit and remove links. Switch to the RichText view to " +
                "render the same state as read-only content - links open via the platform UriHandler when clicked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolbarCard(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = if (enabled) "Actions" else "Actions (Editor view only)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Sky,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickInsertCard(
    enabled: Boolean,
    onInsert: (QuickLink) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "Quick insert",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Sky,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Click to insert as text - or select something first to wrap the selection.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickLinks.forEach { quickLink ->
                AssistChip(
                    onClick = { onInsert(quickLink) },
                    enabled = enabled,
                    label = { Text(quickLink.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isLink: Boolean,
    selectedText: String?,
    selectedUrl: String?,
    selectionCollapsed: Boolean,
    isEditorMode: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Caret status",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SampleAccents.Sky,
            )
            Spacer(Modifier.width(10.dp))
            when {
                !isEditorMode -> StatusBadge(
                    label = "Read-only",
                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                isLink -> StatusBadge(
                    label = "Inside link",
                    background = Color(0xFFDCFCE7),
                    contentColor = Color(0xFF166534),
                )
                !selectionCollapsed -> StatusBadge(
                    label = "Selection ready",
                    background = SampleAccents.Sky.copy(alpha = 0.18f),
                    contentColor = SampleAccents.Sky,
                )
                else -> StatusBadge(
                    label = "Plain text",
                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Always render two info rows so the card height never reflows when the
        // selection moves in or out of a link.
        Text(
            text = "Text: ${if (isLink) selectedText.orEmpty().ifEmpty { "(empty)" } else "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "URL:  ${if (isLink) selectedUrl.orEmpty().ifEmpty { "(none)" } else "-"}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                !isEditorMode ->
                    "Switch to Editor to inspect the caret. Tap a link in the RichText view below to open it."
                isLink -> "Move the caret out of the link to deselect."
                !selectionCollapsed -> "Selection is ready to be wrapped in a link."
                else -> "Place the caret anywhere - Add link will prompt for both text and URL."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ViewModePicker(
    selected: ViewMode,
    onSelect: (ViewMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "View",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Sky,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Editor lets you author and select text. RichText is the read-only renderer that " +
                "opens links on click via LocalUriHandler.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = { Text(mode.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SampleAccents.Sky.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = SampleAccents.Sky,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorOrReaderCard(
    state: com.mohamedrejeb.richeditor.model.RichTextState,
    viewMode: ViewMode,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = if (viewMode == ViewMode.Editor) "Editor" else "RichText (read-only)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Indigo,
        )
        Spacer(Modifier.height(12.dp))
        when (viewMode) {
            ViewMode.Editor -> OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
            )
            ViewMode.Reader -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(16.dp),
            ) {
                RichText(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProgrammaticDemo(
    enabled: Boolean,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "Programmatic API",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Magenta,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "addLinkToTextRange wraps any TextRange - useful for autolink heuristics. " +
                "Tap below to wrap the substring \"RichTextConfig\" in the editor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onApply, enabled = enabled) {
            Text("Linkify \"RichTextConfig\"")
        }
    }
}

@Composable
private fun ChipAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ExportCard(
    title: String,
    accent: Color,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LinkDialog(
    initialText: String,
    initialUrl: String,
    isEditing: Boolean,
    isSelectionCollapsed: Boolean,
    onSubmit: (text: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }

    val canSubmit = url.isNotBlank() && (isEditing || !isSelectionCollapsed || text.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit link" else "Add link",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Display text") },
                    singleLine = true,
                    enabled = !isEditing && isSelectionCollapsed,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text, url) },
                enabled = canSubmit,
            ) {
                Text(if (isEditing) "Save" else "Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}
