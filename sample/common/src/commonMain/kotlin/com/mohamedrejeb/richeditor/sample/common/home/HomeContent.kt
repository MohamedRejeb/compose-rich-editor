package com.mohamedrejeb.richeditor.sample.common.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.moriatsushi.insetsx.ExperimentalSoftwareKeyboardApi
import com.moriatsushi.insetsx.safeDrawingPadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSoftwareKeyboardApi::class)
@Composable
fun HomeContent() {
    val navigator = LocalNavigator.currentOrThrow

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Rich Editor") },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { paddingValue ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            val richTextState = rememberRichTextState()

            LaunchedEffect(Unit) {
                richTextState.setHtml("<u>Welcome</u> to <b>Compose Rich Editor Demo</b>")
            }

            RichText(
                state = richTextState,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    navigator.push(RichEditorScreen)
                }
            ) {
                Text("Rich Text Editor Demo")
            }

            Button(
                onClick = {
                    navigator.push(HtmlEditorScreen)
                },
            ) {
                Text("HTML Editor Demo")
            }

            Button(
                onClick = {
                    navigator.push(MarkdownEditorScreen)
                },
            ) {
                Text("Markdown Editor Demo")
            }

            Button(
                onClick = {
                    navigator.push(SlackDemoScreen)
                },
            ) {
                Text("Slack Clone Demo")
            }

        }
    }
}