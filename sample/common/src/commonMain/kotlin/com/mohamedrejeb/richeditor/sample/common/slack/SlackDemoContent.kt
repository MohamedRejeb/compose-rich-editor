package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.moriatsushi.insetsx.ExperimentalSoftwareKeyboardApi
import com.moriatsushi.insetsx.safeDrawingPadding
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class,
    ExperimentalSoftwareKeyboardApi::class
)
@Composable
fun SlackDemoContent() {
    val navigator = LocalNavigator.currentOrThrow

    val richTextState = rememberRichTextState()

    val messages = remember {
        mutableStateListOf<RichTextState>()
    }

    val openLinkDialog = remember { mutableStateOf(false) }
    val text = remember { mutableStateOf("") }
    val link = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        richTextState.setConfig(
            linkColor = Color(0xFF1d9bd1),
            linkTextDecoration = TextDecoration.None,
            codeColor = Color(0xFFd7882d),
            codeBackgroundColor = Color.Transparent,
            codeStrokeColor = Color(0xFF494b4d),
        )
    }

    ComposeRichEditorTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF1a1d21))
                .safeDrawingPadding()
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
                                    onClick = { navigator.pop() }
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.mediumTopAppBarColors(
                                containerColor = Color(0xFF1a1d21),
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                            )
                        )
                        Divider(color = Color(0xFFCBCCCD), thickness = 1.dp)
                    }
                },
                containerColor = Color(0xFF1a1d21),
                modifier = Modifier
                    .fillMaxSize()
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
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
                                            painterResource("slack-logo.png"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )

                            // 1d99ce
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
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple()
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Send,
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

                    if (openLinkDialog.value) {
                        SlackDemoLinkDialog(
                            state = richTextState,
                            text = text,
                            link = link,
                            openLinkDialog = openLinkDialog
                        )
                    }
                }
            }
        }
    }
}