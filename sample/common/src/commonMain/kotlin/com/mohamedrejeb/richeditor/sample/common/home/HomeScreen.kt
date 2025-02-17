package com.mohamedrejeb.richeditor.sample.common.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    navigateToRichEditor: () -> Unit,
    navigateToHtmlEditor: () -> Unit,
    navigateToMarkdownEditor: () -> Unit,
    navigateToSlack: () -> Unit,
) {
    val richTextState = rememberRichTextState()

    LaunchedEffect(Unit) {
        richTextState.setHtml("<u>Welcome</u> to <b>Compose Rich Editor Demo</b>")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Rich Editor") },
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                RichText(
                    state = richTextState,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(20.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Button(
                    onClick = navigateToRichEditor,
                ) {
                    Text("Rich Text Editor Demo")
                }
            }

            item {
                Button(
                    onClick = navigateToHtmlEditor,
                ) {
                    Text("HTML Editor Demo")
                }
            }

            item {
                Button(
                    onClick = navigateToMarkdownEditor,
                ) {
                    Text("Markdown Editor Demo")
                }
            }

            item {
                Button(
                    onClick = navigateToSlack,
                ) {
                    Text("Slack Clone Demo")
                }
            }
        }
    }
}