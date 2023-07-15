package com.mohamedrejeb.richeditor.sample.common.neweditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.test.DebugRichTextEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    val richTextState = rememberRichTextState()

    ComposeRichEditorTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Rich Editor") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValue ->
            DebugRichTextEditor(
                richTextState = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .consumeWindowInsets(paddingValue)
            ) {
                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = richTextState,
                )
            }
        }
    }
}