package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.ui.material3.RichText

private val AvatarSize = 36.dp
private val AvatarColumnWidth = 52.dp

@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun SlackMessageRow(
    message: SlackMessage,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .background(if (isHovered) SlackColors.SurfaceHover else Color.Transparent),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (showHeader) 8.dp else 2.dp),
        ) {
            AvatarSlot(message = message, showHeader = showHeader, isHovered = isHovered)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showHeader) {
                    MessageHeader(message.author, message.timestamp)
                    Spacer(Modifier.height(2.dp))
                }
                MessageBody(message = message)
                if (message.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ReactionsRow(message.reactions)
                }
                message.thread?.let { hint ->
                    Spacer(Modifier.height(6.dp))
                    ThreadRow(hint)
                }
            }
        }

        if (isHovered) {
            MessageActionBar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun AvatarSlot(
    message: SlackMessage,
    showHeader: Boolean,
    isHovered: Boolean,
) {
    Box(
        modifier = Modifier.width(AvatarColumnWidth),
        contentAlignment = Alignment.TopStart,
    ) {
        if (showHeader) {
            UserAvatar(message.author)
        } else if (isHovered) {
            // Slack hides the avatar for grouped messages and shows a compact
            // timestamp in the same slot on hover.
            Text(
                text = compactTime(message.timestamp),
                color = SlackColors.TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
internal fun UserAvatar(user: SlackUser, size: androidx.compose.ui.unit.Dp = AvatarSize) {
    val baseColor = user.avatarColor()
    val initial = user.name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(baseColor, baseColor.copy(alpha = 0.75f)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
private fun MessageHeader(author: SlackUser, timestamp: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = author.name,
            color = SlackColors.TextStrong,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = timestamp,
            color = SlackColors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun MessageBody(message: SlackMessage) {
    Row(verticalAlignment = Alignment.Bottom) {
        RichText(
            state = message.body,
            color = SlackColors.TextPrimary,
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 22.sp,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (message.edited) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "(edited)",
                color = SlackColors.TextMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionsRow(reactions: List<SlackReaction>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        reactions.forEach { reaction ->
            ReactionPill(reaction)
        }
        AddReactionPill()
    }
}

@Composable
private fun ReactionPill(reaction: SlackReaction) {
    val bg = if (reaction.reactedByMe) SlackColors.ReactionPillSelected else SlackColors.ReactionPill
    val stroke = if (reaction.reactedByMe) SlackColors.ReactionStrokeSelected else SlackColors.ReactionStroke
    val countColor = if (reaction.reactedByMe) SlackColors.AccentBlue else SlackColors.TextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {}
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = reaction.emoji, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = reaction.count.toString(),
            color = countColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AddReactionPill() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlackColors.ReactionPill)
            .border(1.dp, SlackColors.ReactionStroke, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {}
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AddReaction,
            contentDescription = "Add reaction",
            tint = SlackColors.TextMuted,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ThreadRow(hint: ThreadHint) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) {}
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        hint.avatars.take(3).forEach { user ->
            UserAvatar(user, size = 20.dp)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = "${hint.replies} replies",
            color = SlackColors.ThreadHint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Last reply ${relativeTime(hint.lastReply)}",
            color = SlackColors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MessageActionBar(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SlackColors.Surface)
            .border(1.dp, SlackColors.Border, RoundedCornerShape(6.dp)),
    ) {
        ActionIcon(Icons.Outlined.AddReaction, "React")
        ActionIcon(Icons.AutoMirrored.Outlined.Reply, "Reply")
        ActionIcon(Icons.Outlined.Share, "Share")
        ActionIcon(Icons.Outlined.BookmarkBorder, "Bookmark")
        ActionIcon(Icons.Outlined.MoreHoriz, "More")
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clickable(role = Role.Button) {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SlackColors.TextPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun SlackUnreadDivider(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        androidx.compose.material3.HorizontalDivider(
            color = SlackColors.UnreadDividerLine,
            thickness = 1.dp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, SlackColors.UnreadDividerLine, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count new",
                color = SlackColors.UnreadDividerText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Extracts just the "9:14 AM" portion from "Today at 9:14 AM" for compact grouped-message display. */
private fun compactTime(timestamp: String): String {
    val atIndex = timestamp.indexOf("at ")
    return if (atIndex >= 0) timestamp.substring(atIndex + 3) else timestamp
}

private fun relativeTime(timestamp: String): String {
    val atIndex = timestamp.indexOf("at ")
    return if (atIndex >= 0) timestamp.substring(atIndex + 3).lowercase() else timestamp
}
