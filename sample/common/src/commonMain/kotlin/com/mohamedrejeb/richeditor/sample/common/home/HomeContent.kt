package com.mohamedrejeb.richeditor.sample.common.home

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorScreen
import com.mohamedrejeb.richeditor.sample.common.markdowneditor.MarkdownEditorScreen
import com.mohamedrejeb.richeditor.sample.common.richeditor.RichEditorScreen
import com.mohamedrejeb.richeditor.sample.common.slack.SlackDemoScreen
import com.mohamedrejeb.richeditor.sample.common.spellcheck.SpellCheckScreen
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeContent() {
    val navigator = LocalNavigator.currentOrThrow

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
                    onClick = {
                        navigator.push(RichEditorScreen)
                    }
                ) {
                    Text("Rich Text Editor Demo")
                }
            }

            item {
                Button(
                    onClick = {
                        navigator.push(HtmlEditorScreen)
                    },
                ) {
                    Text("HTML Editor Demo")
                }
            }

            item {
                Button(
                    onClick = {
                        navigator.push(MarkdownEditorScreen)
                    },
                ) {
                    Text("Markdown Editor Demo")
                }
            }

            item {
                Button(
                    onClick = {
                        navigator.push(SlackDemoScreen)
                    },
                ) {
                    Text("Slack Clone Demo")
                }
            }

            item {
                Button(
                    onClick = {
                        navigator.push(SpellCheckScreen)
                    }
                ) {
                    Text("Spell Check Demo")
                }
            }
        }
    }
}