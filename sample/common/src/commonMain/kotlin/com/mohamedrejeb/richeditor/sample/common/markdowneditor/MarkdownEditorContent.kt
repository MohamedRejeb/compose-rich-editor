package com.mohamedrejeb.richeditor.sample.common.markdowneditor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    var isMarkdownToRichText by remember { mutableStateOf(false) }

    var markdown by remember {
        mutableStateOf(TextFieldValue(""))
    }
    val richTextState = rememberRichTextState()

    LaunchedEffect(richTextState.annotatedString, isMarkdownToRichText) {
        if (!isMarkdownToRichText) {
            markdown = TextFieldValue(richTextState.toMarkdown())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markdown Editor") },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.pop() }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isMarkdownToRichText = !isMarkdownToRichText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "Swap",
                        )
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
        ) {
            if (isMarkdownToRichText) {
                MarkdownToRichText(
                    markdown = markdown,
                    onMarkdownChange = {
                        markdown = it
                        richTextState.setMarkdown(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                RichTextToMarkdown(
                    richTextState = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}