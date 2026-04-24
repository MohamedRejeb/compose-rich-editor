package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.common.generated.resources.Res
import com.mohamedrejeb.richeditor.common.generated.resources.slack_logo
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class SlackUser(val id: String, val name: String, val handle: String)
private data class SlackChannel(val id: String, val name: String)

private val slackUsers = listOf(
    SlackUser("u-mohamed", "Mohamed Rejeb", "@mohamed"),
    SlackUser("u-alice", "Alice Johnson", "@alice"),
    SlackUser("u-bob", "Bob Smith", "@bob"),
    SlackUser("u-carol", "Carol Diaz", "@carol"),
    SlackUser("u-david", "David Lee", "@david"),
    SlackUser("u-elena", "Elena Park", "@elena"),
)

private val slackChannels = listOf(
    SlackChannel("c-general", "general"),
    SlackChannel("c-compose-rich-text-editor", "compose-rich-text-editor"),
    SlackChannel("c-kmp", "kotlin-multiplatform"),
    SlackChannel("c-android", "android"),
    SlackChannel("c-design", "design"),
    SlackChannel("c-random", "random"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class, ExperimentalResourceApi::class)
@Composable
fun SlackDemoScreen(
    navigateBack: () -> Unit
) {
    val richTextState = rememberRichTextState()

    val messages = remember {
        mutableStateListOf<RichTextState>()
    }

    val openLinkDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        richTextState.config.linkColor = Color(0xFF1d9bd1)
        richTextState.config.linkTextDecoration = TextDecoration.None
        richTextState.config.codeSpanColor = Color(0xFFd7882d)
        richTextState.config.codeSpanBackgroundColor = Color.Transparent
        richTextState.config.codeSpanStrokeColor = Color(0xFF494b4d)
        richTextState.config.unorderedListIndent = 40
        richTextState.config.orderedListIndent = 50

        // Register Slack-style triggers so users can @-mention teammates and link
        // #channels. The popup UI is wired up below, over the RichTextEditor.
        richTextState.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { SpanStyle(color = Color(0xFFECB22E), fontWeight = FontWeight.Medium) },
            )
        )
        richTextState.registerTrigger(
            Trigger(
                id = "channel",
                char = '#',
                style = { SpanStyle(color = Color(0xFF1d9bd1), fontWeight = FontWeight.Medium) },
            )
        )
    }

    Box(
        modifier = Modifier
            .background(Color(0xFF1a1d21))
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                ) {
                    TopAppBar(
                        title = { Text("Slack Demo") },
                        navigationIcon = {
                            IconButton(
                                onClick = navigateBack
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.mediumTopAppBarColors(
                            containerColor = Color(0xFF1a1d21),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                        )
                    )

                    HorizontalDivider(color = Color(0xFFCBCCCD))
                }
            },
            containerColor = Color(0xFF1a1d21),
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.ime)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        items(messages) { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                ) {
                                    Image(
                                        painterResource(Res.drawable.slack_logo),
                                        contentDescription = "Slack Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = "Mohamed Rejeb",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    RichText(
                                        state = message,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222528))
                            .border(1.dp, Color(0xFFCBCCCD), RoundedCornerShape(10.dp))
                    ) {
                        SlackDemoPanel(
                            state = richTextState,
                            openLinkDialog = openLinkDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .padding(horizontal = 20.dp)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RichTextEditor(
                                state = richTextState,
                                placeholder = {
                                    Text(
                                        text = "Message #compose-rich-text-editor",
                                    )
                                },
                                colors = RichTextEditorDefaults.richTextEditorColors(
                                    textColor = Color(0xFFCBCCCD),
                                    containerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    placeholderColor = Color.White.copy(alpha = .6f),
                                ),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )

                            // @-mention popup
                            TriggerSuggestions(
                                state = richTextState,
                                triggerId = "mention",
                                suggestions = { query ->
                                    slackUsers.filter {
                                        query.isEmpty() ||
                                            it.handle.contains(query, ignoreCase = true) ||
                                            it.name.contains(query, ignoreCase = true)
                                    }
                                },
                                onSelect = { user ->
                                    RichSpanStyle.Token(
                                        triggerId = "mention",
                                        id = user.id,
                                        label = user.handle,
                                    )
                                },
                                item = { user ->
                                    Column {
                                        Text(
                                            text = user.handle,
                                            color = Color(0xFFECB22E),
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = user.name,
                                            color = Color(0xFFCBCCCD),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                            )

                            // #-channel popup
                            TriggerSuggestions(
                                state = richTextState,
                                triggerId = "channel",
                                suggestions = { query ->
                                    slackChannels.filter {
                                        query.isEmpty() || it.name.contains(query, ignoreCase = true)
                                    }
                                },
                                onSelect = { channel ->
                                    RichSpanStyle.Token(
                                        triggerId = "channel",
                                        id = channel.id,
                                        label = "#${channel.name}",
                                    )
                                },
                                item = { channel ->
                                    Text(
                                        text = "#${channel.name}",
                                        color = Color(0xFF1d9bd1),
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    onClick = {
                                        messages.add(richTextState.copy())
                                        richTextState.clear()
                                    },
                                    enabled = true,
                                    role = Role.Button,
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF007a5a))
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                if (openLinkDialog.value)
                    Dialog(
                        onDismissRequest = {
                            openLinkDialog.value = false
                        }
                    ) {
                        SlackDemoLinkDialog(
                            state = richTextState,
                            openLinkDialog = openLinkDialog
                        )
                    }
            }
        }
    }
}