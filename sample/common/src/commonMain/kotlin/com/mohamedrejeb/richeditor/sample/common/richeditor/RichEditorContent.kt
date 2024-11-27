package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.InteractionType
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.fillMaxSize()
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
                    Box {
                        var spellCheckExpanded by remember { mutableStateOf<Offset?>(null) }

                        BasicRichTextEditor(
                            modifier = Modifier.fillMaxWidth(),
                            state = basicRichTextState,
                            onRichSpanClick = { span, range, offset, type ->
                                println("clicked: $type")
                                if (type == InteractionType.PrimaryClick || type == InteractionType.DoubleTap) {
                                    if (span is SpellCheck) {
                                        println("Spell check clicked")
                                        println("Range: $range")
                                        println("Offset: $offset")
                                        spellCheckExpanded = offset
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = spellCheckExpanded != null,
                            onDismissRequest = { spellCheckExpanded = null },
                            offset = DpOffset(
                                x = spellCheckExpanded?.x?.dp ?: 0.dp,
                                y = spellCheckExpanded?.y?.dp ?: 0.dp,
                            ),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Spelling") },
                                onClick = {}
                            )

                            DropdownMenuItem(
                                text = { Text("Spelling") },
                                onClick = {}
                            )
                        }

                    }
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
                        onRichSpanClick = { span, range, offset, type ->
                            println("clicked: $type")
                            if (span is SpellCheck) {
                                println("Spell check clicked")
                                println("Range: $range")
                                println("Offset: $offset")
                                true
                            } else {
                                false
                            }
                        }
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