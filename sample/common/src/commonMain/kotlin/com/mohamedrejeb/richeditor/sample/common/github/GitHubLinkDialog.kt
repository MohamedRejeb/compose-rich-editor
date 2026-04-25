package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mohamedrejeb.richeditor.model.RichTextState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GitHubLinkDialog(
    state: RichTextState,
    onDismiss: () -> Unit,
) {
    val isLink = state.isLink
    val selectionCollapsed = state.selection.collapsed

    var text by remember {
        mutableStateOf(
            state.selectedLinkText.orEmpty().ifEmpty {
                if (!selectionCollapsed) state.annotatedString.text.substring(
                    state.selection.min,
                    state.selection.max,
                ) else ""
            }
        )
    }
    var url by remember { mutableStateOf(state.selectedLinkUrl.orEmpty()) }

    val canSubmit = url.isNotBlank() && (isLink || !selectionCollapsed || text.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GitHubColors.Surface)
                .padding(20.dp),
        ) {
            Text(
                text = if (isLink) "Edit link" else "Add link",
                color = GitHubColors.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Display text", color = GitHubColors.TextMuted) },
                singleLine = true,
                enabled = !isLink && selectionCollapsed,
                colors = githubFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL", color = GitHubColors.TextMuted) },
                singleLine = true,
                colors = githubFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLink) {
                    OutlinedButton(
                        onClick = {
                            state.removeLink()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFF85149),
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF85149).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Remove")
                    }
                    Spacer(Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = GitHubColors.Text,
                    ),
                    border = BorderStroke(1.dp, GitHubColors.Border),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Cancel")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        when {
                            isLink -> state.updateLink(url = url)
                            selectionCollapsed -> state.addLink(text = text, url = url)
                            else -> state.addLinkToSelection(url = url)
                        }
                        onDismiss()
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GitHubColors.Success,
                        contentColor = Color.White,
                        disabledContainerColor = GitHubColors.Success.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f),
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (isLink) "Save" else "Insert")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun githubFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GitHubColors.Text,
    unfocusedTextColor = GitHubColors.Text,
    disabledTextColor = GitHubColors.TextMuted,
    focusedBorderColor = GitHubColors.Link,
    unfocusedBorderColor = GitHubColors.Border,
    cursorColor = GitHubColors.Link,
    focusedContainerColor = GitHubColors.Background,
    unfocusedContainerColor = GitHubColors.Background,
    disabledContainerColor = GitHubColors.Background,
)
