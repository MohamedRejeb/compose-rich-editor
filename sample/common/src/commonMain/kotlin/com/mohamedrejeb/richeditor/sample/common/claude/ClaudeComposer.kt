package com.mohamedrejeb.richeditor.sample.common.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

@OptIn(ExperimentalRichTextApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ClaudeComposer(
    state: RichTextState,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend by remember {
        derivedStateOf { !isStreaming && state.annotatedString.text.isNotBlank() }
    }
    val openLinkDialog = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ClaudeColors.Surface)
            .border(1.dp, ClaudeColors.Border, RoundedCornerShape(20.dp)),
    ) {
        ClaudeFormattingPanel(
            state = state,
            openLinkDialog = openLinkDialog,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        HorizontalDivider(color = ClaudeColors.Divider, thickness = 1.dp)

        Box(modifier = Modifier.fillMaxWidth()) {
            RichTextEditor(
                state = state,
                placeholder = {
                    Text(
                        text = "Reply to Claude... (try @ or /)",
                        color = ClaudeColors.TextPlaceholder,
                    )
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    textColor = ClaudeColors.TextPrimary,
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    placeholderColor = ClaudeColors.TextPlaceholder,
                    cursorColor = ClaudeColors.AccentOrange,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Default,
                    fontSize = 15.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )

            TriggerSuggestions(
                state = state,
                triggerId = MENTION_TRIGGER_ID,
                suggestions = { query ->
                    claudeMentionContacts.filter {
                        query.isEmpty() ||
                            it.handle.contains(query, ignoreCase = true) ||
                            it.name.contains(query, ignoreCase = true)
                    }
                },
                onSelect = { contact ->
                    RichSpanStyle.Token(
                        triggerId = MENTION_TRIGGER_ID,
                        id = contact.id,
                        label = contact.handle,
                    )
                },
                containerColor = ClaudeColors.MenuSurface,
                contentColor = ClaudeColors.TextPrimary,
                highlightColor = ClaudeColors.MenuHighlight,
                item = { contact -> MentionRow(contact) },
            )

            TriggerSuggestions(
                state = state,
                triggerId = SLASH_TRIGGER_ID,
                suggestions = { query ->
                    claudeSlashCommands.filter {
                        query.isEmpty() || it.keyword.contains(query, ignoreCase = true)
                    }
                },
                onSelect = { command ->
                    RichSpanStyle.Token(
                        triggerId = SLASH_TRIGGER_ID,
                        id = command.id,
                        label = "/${command.keyword}",
                    )
                },
                containerColor = ClaudeColors.MenuSurface,
                contentColor = ClaudeColors.TextPrimary,
                highlightColor = ClaudeColors.MenuHighlight,
                item = { command -> SlashRow(command) },
            )
        }

        BottomBar(
            canSend = canSend,
            isStreaming = isStreaming,
            onSend = onSend,
            onStop = onStop,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }

    if (openLinkDialog.value) {
        Dialog(onDismissRequest = { openLinkDialog.value = false }) {
            ClaudeLinkDialog(
                state = state,
                openLinkDialog = openLinkDialog,
            )
        }
    }
}

@Composable
private fun BottomBar(
    canSend: Boolean,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Markdown supported",
            color = ClaudeColors.TextMuted,
            fontSize = 11.sp,
        )

        Spacer(Modifier.weight(1f))

        if (isStreaming) {
            StopButton(onClick = onStop)
        } else {
            SendButton(enabled = canSend, onClick = onSend)
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (enabled) ClaudeColors.AccentOrange else ClaudeColors.SurfaceHover
    val tint = if (enabled) Color.White else ClaudeColors.TextMuted
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .focusProperties { canFocus = false }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Send",
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ClaudeColors.SurfaceHover)
            .border(1.dp, ClaudeColors.AccentOrangeMuted, RoundedCornerShape(10.dp))
            .focusProperties { canFocus = false }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Stop,
            contentDescription = "Stop streaming",
            tint = ClaudeColors.AccentOrange,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MentionRow(contact: ClaudeContact) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ClaudeColors.AccentOrangeMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.handle,
                    color = ClaudeColors.MentionAccent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = contact.name,
                    color = ClaudeColors.TextStrong,
                    fontSize = 13.sp,
                )
            }
            Text(
                text = contact.description,
                color = ClaudeColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SlashRow(command: ClaudeSlashCommand) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ClaudeColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "/",
                color = ClaudeColors.SlashAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Column {
            Text(
                text = command.keyword,
                color = ClaudeColors.TextStrong,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = command.description,
                color = ClaudeColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaudeLinkDialog(
    state: RichTextState,
    openLinkDialog: MutableState<Boolean>,
) {
    val initialText = remember {
        when {
            !state.selection.collapsed ->
                state.annotatedString.text.substring(state.selection.min, state.selection.max)
            state.isLink -> state.selectedLinkText.orEmpty()
            else -> ""
        }
    }
    var text by remember { mutableStateOf(TextFieldValue(initialText)) }
    var url by remember { mutableStateOf(TextFieldValue(state.selectedLinkUrl.orEmpty())) }

    AlertDialog(
        onDismissRequest = { openLinkDialog.value = false },
        title = {
            Text(
                text = if (state.isLink) "Edit link" else "Add link",
                color = ClaudeColors.TextStrong,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    singleLine = true,
                    enabled = state.selection.collapsed && !state.isLink,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClaudeColors.AccentOrange,
                        cursorColor = ClaudeColors.AccentOrange,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitLink(state, text.text, url.text)
                            openLinkDialog.value = false
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClaudeColors.AccentOrange,
                        cursorColor = ClaudeColors.AccentOrange,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    commitLink(state, text.text, url.text)
                    openLinkDialog.value = false
                },
                enabled = url.text.isNotBlank() &&
                    (text.text.isNotBlank() || !state.selection.collapsed || state.isLink),
            ) {
                Text(
                    text = if (state.isLink) "Update" else "Insert",
                    color = ClaudeColors.AccentOrange,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { openLinkDialog.value = false }) {
                Text("Cancel", color = ClaudeColors.TextSecondary)
            }
        },
        containerColor = ClaudeColors.Surface,
        shape = RoundedCornerShape(16.dp),
    )
}

private fun commitLink(state: RichTextState, text: String, url: String) {
    val trimmedUrl = url.trim()
    if (trimmedUrl.isEmpty()) return
    when {
        state.isLink -> state.updateLink(url = trimmedUrl)
        state.selection.collapsed -> state.addLink(text = text.ifBlank { trimmedUrl }, url = trimmedUrl)
        else -> state.addLinkToSelection(url = trimmedUrl)
    }
}
