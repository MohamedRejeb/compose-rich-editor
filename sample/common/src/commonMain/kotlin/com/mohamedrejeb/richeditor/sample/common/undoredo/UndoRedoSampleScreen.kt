package com.mohamedrejeb.richeditor.sample.common.undoredo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UndoRedoSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()

    SampleScaffold(
        title = "Undo & redo",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            HelpCard()

            // Prevent the editor from losing focus when clicking any toolbar button
            // (same workaround used by the other sample screens).
            val noFocusModifier = Modifier.focusProperties { canFocus = false }

            ToolbarCard(
                title = "History",
                accent = SampleAccents.Teal,
            ) {
                ChipAction(
                    label = "Undo",
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    enabled = state.history.canUndo,
                    onClick = { state.history.undo() },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Redo",
                    icon = Icons.AutoMirrored.Outlined.Redo,
                    enabled = state.history.canRedo,
                    onClick = { state.history.redo() },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Clear history",
                    icon = Icons.Outlined.DeleteSweep,
                    enabled = true,
                    onClick = { state.history.clear() },
                    modifier = noFocusModifier,
                )
            }

            ToolbarCard(
                title = "Formatting",
                accent = SampleAccents.Indigo,
            ) {
                ChipAction(
                    label = "Bold",
                    icon = Icons.Outlined.FormatBold,
                    enabled = true,
                    onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Italic",
                    icon = Icons.Outlined.FormatItalic,
                    enabled = true,
                    onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Underline",
                    icon = Icons.Outlined.FormatUnderlined,
                    enabled = true,
                    onClick = {
                        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Ordered list",
                    icon = Icons.Outlined.FormatListNumbered,
                    enabled = true,
                    onClick = { state.toggleOrderedList() },
                    modifier = noFocusModifier,
                )
                ChipAction(
                    label = "Bulleted list",
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    enabled = true,
                    onClick = { state.toggleUnorderedList() },
                    modifier = noFocusModifier,
                )
            }

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "Keyboard shortcuts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Teal,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Type and format text, then press Ctrl/Cmd+Z to undo or Ctrl/Cmd+Shift+Z to redo. " +
                "The buttons below drive the same history stack.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ToolbarCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
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
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
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
