package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.util.DebugLogger
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RichEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    val basicRichTextState = rememberRichTextState()
    val richTextState = rememberRichTextState()
    val outlinedRichTextState = rememberRichTextState()

    LaunchedEffect(Unit) {
        richTextState.setHtml(
            """
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            """.trimIndent()
        )
    }

    LaunchedEffect(Unit) {
        basicRichTextState.setMarkdown(
            """
                <br>
                <br>
                some text
            """.trimIndent()
        )
    }

    LaunchedEffect(basicRichTextState.annotatedString) {
        val markdown = basicRichTextState.toMarkdown()
        DebugLogger().log(
            "RichEditorContent",
            coil3.util.Logger.Level.Debug,
            "BasicRichTextEditor markdown: $markdown",
            null
        )
    }

    ComposeRichEditorTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Rich Editor") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValue ->
            LazyColumn(
                contentPadding = paddingValue,
                modifier = Modifier
                    .padding(paddingValue)
                    .windowInsetsPadding(WindowInsets.ime)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // BasicRichTextEditor
                item {
                    Text(
                        text = "BasicRichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = basicRichTextState,
                    )
                }

                item {
                    BasicRichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = basicRichTextState,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // RichTextEditor
                item {
                    Text(
                        text = "RichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                    )
                }

                item {
                    RichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                        readOnly = true,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // OutlinedRichTextEditor
                item {
                    Text(
                        text = "OutlinedRichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = outlinedRichTextState,
                    )
                }

                item {
                    OutlinedRichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = outlinedRichTextState,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // RichText
                item {
                    Text(
                        text = "RichText:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichText(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                    )
                }
            }
        }
    }
}