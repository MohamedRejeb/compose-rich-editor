package com.mohamedrejeb.richeditor.sample.common.claude

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun UserMessageRow(body: RichTextState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ClaudeColors.UserBubble)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RichText(
                state = body,
                color = ClaudeColors.TextStrong,
                style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
            )
        }
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun ClaudeMessageRow(
    body: RichTextState,
    isStreaming: Boolean,
    isEmpty: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ClaudeAvatar()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Claude",
                color = ClaudeColors.TextStrong,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.size(4.dp))
            if (isEmpty && isStreaming) {
                ThinkingIndicator()
            } else {
                RichText(
                    state = body,
                    color = ClaudeColors.TextPrimary,
                    style = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
                )
                if (isStreaming) {
                    Spacer(Modifier.size(4.dp))
                    StreamingCaret()
                }
            }
        }
    }
}

@Composable
private fun ClaudeAvatar() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ClaudeColors.AccentOrange),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "C",
            color = ClaudeColors.TextStrong,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Restart,
        ),
        label = "thinking-phase",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { index ->
            val active = phase.toInt() == index
            Box(
                modifier = Modifier
                    .size(if (active) 7.dp else 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) ClaudeColors.StreamingDot
                        else ClaudeColors.TextMuted,
                    ),
            )
        }
    }
}

@Composable
private fun StreamingCaret() {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caret-alpha",
    )
    Box(
        modifier = Modifier
            .size(width = 8.dp, height = 14.dp)
            .background(ClaudeColors.StreamingDot.copy(alpha = alpha)),
    )
}
