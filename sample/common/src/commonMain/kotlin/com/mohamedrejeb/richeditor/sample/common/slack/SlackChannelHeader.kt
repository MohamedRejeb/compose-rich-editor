package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SlackChannelHeader(
    channel: SlackChannel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Material3 `TopAppBar` consumes status-bar inset automatically; this is a custom
    // header so we apply it ourselves, otherwise the bar draws under the system status
    // bar on Android / iOS.
    Column(
        modifier = modifier
            .background(SlackColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        ) {
            BackButton(onBack)
            Spacer(Modifier.width(4.dp))

            WorkspaceBadge()
            Spacer(Modifier.width(10.dp))

            ChannelIdentity(channel = channel, modifier = Modifier.weight(1f))

            HeaderIconButton(Icons.Outlined.HeadsetMic, "Huddle")
            HeaderIconButton(Icons.Outlined.Videocam, "Call")
            Spacer(Modifier.width(4.dp))
            MembersPill(count = channel.memberCount)
            Spacer(Modifier.width(4.dp))
            HeaderIconButton(Icons.Outlined.Info, "Details")
        }

        HorizontalDivider(color = SlackColors.Divider, thickness = 1.dp)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = SlackColors.TextStrong,
        )
    }
}

@Composable
private fun WorkspaceBadge() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(SlackColors.Aubergine),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "C",
            color = SlackColors.TextStrong,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ChannelIdentity(
    channel: SlackChannel,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) {}
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#",
                    color = SlackColors.TextStrong,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = channel.name,
                    color = SlackColors.TextStrong,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SlackColors.TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = channel.topic,
                color = SlackColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SlackColors.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MembersPill(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, SlackColors.Border, RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) {}
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.People,
            contentDescription = null,
            tint = SlackColors.TextPrimary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = SlackColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun SlackChannelIntro(
    channel: SlackChannel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SlackColors.Aubergine),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#",
                    color = SlackColors.TextStrong,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "This is the very beginning of the #${channel.name} channel.",
            color = SlackColors.TextStrong,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = channel.topic,
            color = SlackColors.TextSecondary,
            fontSize = 14.sp,
        )
    }
}
