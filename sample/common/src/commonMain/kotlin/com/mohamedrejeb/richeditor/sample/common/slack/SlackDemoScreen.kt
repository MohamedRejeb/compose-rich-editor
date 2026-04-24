package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun SlackDemoScreen(
    navigateBack: () -> Unit,
) {
    val composerState = rememberRichTextState()
    val seedMessages = rememberSeededMessages()
    val messages = remember(seedMessages) { mutableStateListOf<SlackMessage>().apply { addAll(seedMessages) } }
    val openLinkDialog = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        composerState.config.linkColor = SlackColors.LinkBlue
        composerState.config.linkTextDecoration = TextDecoration.None
        composerState.config.codeSpanColor = SlackColors.CodeSpan
        composerState.config.codeSpanBackgroundColor = Color.Transparent
        composerState.config.codeSpanStrokeColor = SlackColors.CodeSpanStroke
        composerState.config.unorderedListIndent = 40
        composerState.config.orderedListIndent = 50

        composerState.registerTrigger(
            Trigger(
                id = MENTION_TRIGGER_ID,
                char = '@',
                style = { SpanStyle(color = SlackColors.MentionYellow, fontWeight = FontWeight.Medium) },
            )
        )
        composerState.registerTrigger(
            Trigger(
                id = CHANNEL_TRIGGER_ID,
                char = '#',
                style = { SpanStyle(color = SlackColors.ChannelBlue, fontWeight = FontWeight.Medium) },
            )
        )
    }

    // Keep the newest message in view - matches Slack's default "pinned to bottom"
    // behaviour. Triggers once on open (to land at the latest seeded message) and
    // every time a new message is sent.
    LaunchedEffect(messages.size) {
        val lastIndex = messages.size  // intro=0, messages at 1..size, pad at size+1
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        topBar = {
            SlackChannelHeader(
                channel = currentChannel,
                onBack = navigateBack,
            )
        },
        containerColor = SlackColors.Background,
        contentColor = SlackColors.TextPrimary,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .background(SlackColors.Background),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                item(key = "intro") {
                    SlackChannelIntro(channel = currentChannel)
                }

                items(messages.size) { index ->
                    val message = messages[index]
                    val prev = messages.getOrNull(index - 1)
                    val showHeader = prev == null ||
                        prev.author.id != message.author.id ||
                        !sameSendBucket(prev.timestamp, message.timestamp)

                    if (index == 1) {
                        // Surface an unread divider just below the channel owner's
                        // first message so the demo shows off the "new" affordance.
                        SlackUnreadDivider(count = messages.size - 1)
                    }

                    SlackMessageRow(
                        message = message,
                        showHeader = showHeader,
                    )
                }

                item(key = "bottom-pad") {
                    Spacer(Modifier.height(12.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                SlackComposer(
                    state = composerState,
                    channel = currentChannel,
                    openLinkDialog = openLinkDialog,
                    onSend = {
                        if (composerState.annotatedString.text.isNotBlank()) {
                            val snapshot = composerState.copy()
                            messages.add(
                                SlackMessage(
                                    author = currentUser,
                                    timestamp = "Now",
                                    body = snapshot,
                                ),
                            )
                            composerState.clear()
                        }
                    },
                )
            }
        }

        if (openLinkDialog.value) {
            Dialog(onDismissRequest = { openLinkDialog.value = false }) {
                SlackDemoLinkDialog(
                    state = composerState,
                    openLinkDialog = openLinkDialog,
                )
            }
        }
    }

}

/**
 * True when two timestamps land in the same "send bucket" so the second message
 * should group under the first (Slack uses ~3-minute windows). The demo's
 * timestamps are free-form strings, so this is a conservative "same 'at HH:'
 * prefix" match, which is good enough for seeded demo data.
 */
private fun sameSendBucket(a: String, b: String): Boolean {
    val hourA = a.substringAfter("at ", "").substringBefore(':', "")
    val hourB = b.substringAfter("at ", "").substringBefore(':', "")
    return hourA.isNotEmpty() && hourA == hourB
}
