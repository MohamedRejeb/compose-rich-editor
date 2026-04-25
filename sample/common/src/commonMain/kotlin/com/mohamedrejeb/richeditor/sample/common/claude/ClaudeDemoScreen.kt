package com.mohamedrejeb.richeditor.sample.common.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class ClaudeChatMessage(
    val id: Long,
    val isUser: Boolean,
    val body: RichTextState,
    val streaming: androidx.compose.runtime.MutableState<Boolean>,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
fun ClaudeDemoScreen(
    navigateBack: () -> Unit,
) {
    val composerState = rememberRichTextState()
    val messages = remember { mutableStateListOf<ClaudeChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var nextMessageId by remember { mutableStateOf(0L) }
    var streamingJob by remember { mutableStateOf<Job?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var replyIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        composerState.config.linkColor = ClaudeColors.LinkAccent
        composerState.config.codeSpanColor = ClaudeColors.CodeText
        composerState.config.codeSpanBackgroundColor = ClaudeColors.CodeBackground
        composerState.config.codeSpanStrokeColor = ClaudeColors.CodeStroke

        composerState.registerTrigger(
            Trigger(
                id = MENTION_TRIGGER_ID,
                char = '@',
                style = { SpanStyle(color = ClaudeColors.MentionAccent, fontWeight = FontWeight.Medium) },
            )
        )
        composerState.registerTrigger(
            Trigger(
                id = SLASH_TRIGGER_ID,
                char = '/',
                style = { SpanStyle(color = ClaudeColors.SlashAccent, fontWeight = FontWeight.Medium) },
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    // While the current Claude reply is streaming, its body grows but `messages.size`
    // doesn't change, so the effect above never re-fires. Observe the last message's
    // length and keep the list pinned to the bottom-pad item on every chunk.
    LaunchedEffect(isStreaming, messages.size) {
        if (!isStreaming || messages.isEmpty()) return@LaunchedEffect
        val lastMessage = messages.last()
        snapshotFlow { lastMessage.body.annotatedString.text.length }
            .collect {
                listState.scrollToItem(messages.size + 1)
            }
    }

    val send: () -> Unit = send@{
        if (composerState.annotatedString.text.isBlank()) return@send
        if (isStreaming) return@send

        val userBody = composerState.copy()
        messages.add(
            ClaudeChatMessage(
                id = nextMessageId++,
                isUser = true,
                body = userBody,
                streaming = mutableStateOf(false),
            )
        )
        composerState.clear()

        val replyMarkdown = claudeCannedReplies[replyIndex.mod(claudeCannedReplies.size)]
        replyIndex++

        val replyBody = RichTextState().apply {
            this.config.linkColor = ClaudeColors.LinkAccent
            this.config.codeSpanColor = ClaudeColors.CodeText
            this.config.codeSpanBackgroundColor = ClaudeColors.CodeBackground
            this.config.codeSpanStrokeColor = ClaudeColors.CodeStroke
        }
        val streamingState = mutableStateOf(true)
        messages.add(
            ClaudeChatMessage(
                id = nextMessageId++,
                isUser = false,
                body = replyBody,
                streaming = streamingState,
            )
        )

        isStreaming = true
        streamingJob = scope.launch {
            try {
                streamMarkdownInto(replyBody, replyMarkdown)
            } finally {
                streamingState.value = false
                isStreaming = false
            }
        }
    }

    val stop: () -> Unit = {
        streamingJob?.cancel()
        streamingJob = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ClaudeTitle() },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClaudeColors.TextStrong,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClaudeColors.Background,
                    scrolledContainerColor = ClaudeColors.Background,
                    titleContentColor = ClaudeColors.TextStrong,
                ),
            )
        },
        containerColor = ClaudeColors.Background,
        contentColor = ClaudeColors.TextPrimary,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .background(ClaudeColors.Background),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                item(key = "intro") {
                    if (messages.isEmpty()) {
                        ClaudeIntro()
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                items(
                    count = messages.size,
                    key = { messages[it].id },
                ) { index ->
                    val message = messages[index]
                    if (message.isUser) {
                        UserMessageRow(body = message.body)
                    } else {
                        ClaudeMessageRow(
                            body = message.body,
                            isStreaming = message.streaming.value,
                            isEmpty = message.body.annotatedString.text.isEmpty(),
                        )
                    }
                }

                item(key = "bottom-pad") {
                    Spacer(Modifier.height(8.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                ) {
                    ClaudeComposer(
                        state = composerState,
                        isStreaming = isStreaming,
                        onSend = send,
                        onStop = stop,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaudeTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
        Column {
            Text(
                text = "Claude",
                color = ClaudeColors.TextStrong,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                text = "Streaming demo",
                color = ClaudeColors.TextMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ClaudeIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ClaudeColors.AccentOrange),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "C",
                color = ClaudeColors.TextStrong,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = "How can I help today?",
            color = ClaudeColors.TextStrong,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
        )
        Text(
            text = "Type a message and watch the response stream as live markdown. " +
                "Try @ to mention or / to run a command.",
            color = ClaudeColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.widthIn(max = 480.dp),
        )
    }
}
