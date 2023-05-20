package com.mohamedrejeb.richeditor.sample.common.htmleditor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlEditorDemo() {
    var isHtmlToRichText by remember { mutableStateOf(false) }

    ComposeRichEditorTheme(false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Html Editor") },
                    actions = {
                        IconButton(
                            onClick = {
                                isHtmlToRichText = !isHtmlToRichText
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
            if (isHtmlToRichText) {
                HtmlToRichText(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                )
            } else {
                RichTextToHtml(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                )
            }
        }
    }
}