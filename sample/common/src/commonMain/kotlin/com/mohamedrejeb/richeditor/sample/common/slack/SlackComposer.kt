package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

internal const val MENTION_TRIGGER_ID = "mention"
internal const val CHANNEL_TRIGGER_ID = "channel"

@OptIn(ExperimentalRichTextApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SlackComposer(
    state: RichTextState,
    channel: SlackChannel,
    openLinkDialog: MutableState<Boolean>,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend by remember { derivedStateOf { state.annotatedString.text.isNotBlank() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlackColors.Surface)
            .border(1.dp, SlackColors.BorderStrong, RoundedCornerShape(10.dp)),
    ) {
        SlackDemoPanel(
            state = state,
            openLinkDialog = openLinkDialog,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )

        HorizontalDivider(color = SlackColors.Divider, thickness = 1.dp)

        Box(modifier = Modifier.fillMaxWidth()) {
            RichTextEditor(
                state = state,
                placeholder = {
                    Text(
                        text = "Message #${channel.name}",
                        color = SlackColors.TextPlaceholder,
                    )
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    textColor = SlackColors.TextPrimary,
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    placeholderColor = SlackColors.TextPlaceholder,
                    cursorColor = SlackColors.TextStrong,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )

            TriggerSuggestions(
                state = state,
                triggerId = MENTION_TRIGGER_ID,
                suggestions = { query ->
                    slackWorkspaceUsers.filter {
                        query.isEmpty() ||
                            it.handle.contains(query, ignoreCase = true) ||
                            it.name.contains(query, ignoreCase = true)
                    }
                },
                onSelect = { user ->
                    RichSpanStyle.Token(
                        triggerId = MENTION_TRIGGER_ID,
                        id = user.id,
                        label = user.handle,
                    )
                },
                containerColor = SlackColors.SurfaceElevated,
                contentColor = SlackColors.TextPrimary,
                highlightColor = SlackColors.SurfaceHover,
                item = { user -> MentionItem(user) },
            )

            TriggerSuggestions(
                state = state,
                triggerId = CHANNEL_TRIGGER_ID,
                suggestions = { query ->
                    slackChannels.filter {
                        query.isEmpty() || it.name.contains(query, ignoreCase = true)
                    }
                },
                onSelect = { ch ->
                    RichSpanStyle.Token(
                        triggerId = CHANNEL_TRIGGER_ID,
                        id = ch.id,
                        label = "#${ch.name}",
                    )
                },
                containerColor = SlackColors.SurfaceElevated,
                contentColor = SlackColors.TextPrimary,
                highlightColor = SlackColors.SurfaceHover,
                item = { ch -> ChannelItem(ch) },
            )
        }

        BottomActionRow(
            canSend = canSend,
            onSend = onSend,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun BottomActionRow(
    canSend: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        ActionButton(Icons.Outlined.Add, "Add")
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(1.dp)
                .background(SlackColors.Divider),
        )
        Spacer(Modifier.width(2.dp))
        ActionButton(Icons.Outlined.AlternateEmail, "Mention")
        ActionButton(Icons.Outlined.EmojiEmotions, "Emoji")
        ActionButton(Icons.Outlined.Videocam, "Record video")
        ActionButton(Icons.Outlined.AttachFile, "Attach")
        ActionButton(Icons.Outlined.Mic, "Record audio")

        Spacer(Modifier.weight(1f))

        SendButton(enabled = canSend, onClick = onSend)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .focusProperties { canFocus = false }
            .clickable(role = Role.Button) {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SlackColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) SlackColors.AccentGreen else SlackColors.Surface
    val tint = if (enabled) Color.White else SlackColors.TextMuted
    val stroke = if (enabled) Color.Transparent else SlackColors.Border
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(6.dp))
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
private fun MentionItem(user: SlackUser) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(user, size = 24.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.handle,
                    color = SlackColors.MentionYellow,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = user.name,
                    color = SlackColors.TextStrong,
                    fontSize = 13.sp,
                )
            }
            Text(
                text = user.title,
                color = SlackColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ChannelItem(channel: SlackChannel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#",
                color = SlackColors.ChannelBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = channel.name,
                color = SlackColors.ChannelBlue,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${channel.memberCount} members",
                color = SlackColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
        Text(
            text = channel.topic,
            color = SlackColors.TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

