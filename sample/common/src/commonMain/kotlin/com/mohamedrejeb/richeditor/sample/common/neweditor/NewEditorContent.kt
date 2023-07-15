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

@OptIn(ExperimentalMaterial3Api::class)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // BasicRichTextEditor
                Text(
                    text = "BasicRichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = richTextState,
                )

                RichTextEditor(
                    state = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp),
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
}