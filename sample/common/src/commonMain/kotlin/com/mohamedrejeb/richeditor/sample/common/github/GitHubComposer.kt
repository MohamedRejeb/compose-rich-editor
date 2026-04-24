package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

private enum class ComposerTab(val label: String) {
    Write("Write"),
    Preview("Preview"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
internal fun GitHubComposer(
    onSubmit: (RichTextState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberRichTextState()
    var tab by remember { mutableStateOf(ComposerTab.Write) }
    var showLinkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state.config.linkColor = GitHubColors.Link
        state.config.codeSpanColor = GitHubColors.Text
        state.config.codeSpanBackgroundColor = GitHubColors.CodeBackground
        state.config.codeSpanStrokeColor = Color.Transparent

        state.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { SpanStyle(color = GitHubColors.Mention, fontWeight = FontWeight.Medium) },
            )
        )
        state.registerTrigger(
            Trigger(
                id = "issueRef",
                char = '#',
                style = { SpanStyle(color = GitHubColors.IssueRef, fontWeight = FontWeight.Medium) },
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GitHubColors.Border, RoundedCornerShape(8.dp))
            .background(GitHubColors.Background),
    ) {
        TabRow(selected = tab, onSelect = { tab = it })

        if (tab == ComposerTab.Write) {
            GitHubComposerToolbar(
                state = state,
                onLinkClick = { showLinkDialog = true },
                onMentionTrigger = { state.replaceSelectedText("@") },
                onIssueRefTrigger = { state.replaceSelectedText("#") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GitHubColors.Surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .padding(12.dp),
        ) {
            when (tab) {
                ComposerTab.Write -> {
                    OutlinedRichTextEditor(
                        state = state,
                        placeholder = {
                            Text(
                                "Leave a comment",
                                color = GitHubColors.TextSubtle,
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = GitHubColors.Text,
                            fontFamily = FontFamily.SansSerif,
                        ),
                        colors = RichTextEditorDefaults.richTextEditorColors(
                            textColor = GitHubColors.Text,
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            placeholderColor = GitHubColors.TextSubtle,
                            cursorColor = GitHubColors.Link,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp),
                    )

                    TriggerSuggestions(
                        state = state,
                        triggerId = "mention",
                        suggestions = { query ->
                            sampleUsers.filter {
                                query.isEmpty() ||
                                    it.handle.contains(query, ignoreCase = true) ||
                                    it.displayName.contains(query, ignoreCase = true)
                            }
                        },
                        onSelect = { user ->
                            RichSpanStyle.Token(
                                triggerId = "mention",
                                id = user.id,
                                label = user.handle,
                            )
                        },
                        item = { user -> SuggestionRow(user) },
                    )

                    TriggerSuggestions(
                        state = state,
                        triggerId = "issueRef",
                        suggestions = { query ->
                            sampleIssueRefs.filter {
                                query.isEmpty() ||
                                    it.number.toString().contains(query) ||
                                    it.title.contains(query, ignoreCase = true)
                            }
                        },
                        onSelect = { ref ->
                            RichSpanStyle.Token(
                                triggerId = "issueRef",
                                id = ref.id,
                                label = "#${ref.number}",
                            )
                        },
                        item = { ref -> IssueRefRow(ref) },
                    )
                }
                ComposerTab.Preview -> {
                    if (state.annotatedString.text.isBlank()) {
                        Text(
                            text = "Nothing to preview",
                            color = GitHubColors.TextSubtle,
                            fontSize = 14.sp,
                        )
                    } else {
                        GitHubTokenInteractionBox(
                            modifier = Modifier.fillMaxWidth(),
                        ) { onTokenClick, onTokenHover ->
                            RichText(
                                state = state,
                                color = GitHubColors.Text,
                                modifier = Modifier.fillMaxWidth(),
                                onTokenClick = onTokenClick,
                                onTokenHover = onTokenHover,
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onSubmit(state.copy())
                    state.setHtml("")
                    tab = ComposerTab.Write
                },
                enabled = state.annotatedString.text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GitHubColors.Success,
                    contentColor = Color.White,
                    disabledContainerColor = GitHubColors.Success.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text("Comment", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLinkDialog) {
        GitHubLinkDialog(
            state = state,
            onDismiss = { showLinkDialog = false },
        )
    }
}

@Composable
private fun TabRow(
    selected: ComposerTab,
    onSelect: (ComposerTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GitHubColors.Surface)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp),
    ) {
        ComposerTab.entries.forEach { entry ->
            Tab(
                label = entry.label,
                isSelected = entry == selected,
                onClick = { onSelect(entry) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Tab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) GitHubColors.Border else Color.Transparent
    val background = if (isSelected) GitHubColors.Background else Color.Transparent
    val textColor = if (isSelected) GitHubColors.Text else GitHubColors.TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            )
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SuggestionRow(user: GitHubUser) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Avatar(user, size = 24)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = user.handle,
                color = GitHubColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = user.displayName,
                color = GitHubColors.TextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun IssueRefRow(ref: GitHubIssueRef) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GitHubColors.IssueRef.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = "#${ref.number}",
                color = GitHubColors.IssueRef,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = ref.title,
            color = GitHubColors.Text,
            fontSize = 13.sp,
        )
    }
}

