package com.mohamedrejeb.richeditor.sample.common.htmleditor

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
import com.moriatsushi.insetsx.ExperimentalSoftwareKeyboardApi
import com.moriatsushi.insetsx.safeDrawingPadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSoftwareKeyboardApi::class)
@Composable
fun HtmlEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    var isHtmlToRichText by remember { mutableStateOf(false) }

    var html by remember {
        mutableStateOf(TextFieldValue(""))
    }
    val richTextState = rememberRichTextState()

    LaunchedEffect(richTextState.annotatedString, isHtmlToRichText) {
        if (!isHtmlToRichText) {
            html = TextFieldValue(richTextState.toHtml())
        }
    }

    ComposeRichEditorTheme(false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Html Editor") },
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
                .safeDrawingPadding()
        ) { paddingValue ->
            Column(
                modifier = Modifier
                    .padding(paddingValue)
                    .fillMaxSize()
            ) {
                if (isHtmlToRichText) {
                    HtmlToRichText(
                        html = html,
                        onHtmlChange = {
                            html = it
                            richTextState.setHtml(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    RichTextToHtml(
                        richTextState = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}