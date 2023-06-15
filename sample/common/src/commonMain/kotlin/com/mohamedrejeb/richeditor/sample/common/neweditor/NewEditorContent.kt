package com.mohamedrejeb.richeditor.sample.common.neweditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.NewRichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.NewRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    var basicRichTextValue by remember { mutableStateOf(RichTextValue()) }
    var richTextValue by remember { mutableStateOf(
        RichTextValue.from(
            """
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            """.trimIndent()
        )
    ) }
    var outlinedRichTextValue by remember { mutableStateOf(RichTextValue()) }

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

                NewRichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    richTextState = richTextState,
                )

                NewRichTextEditor(
                    richTextState = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp),
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                Text(
                    text = "tree representation:",
                )

                key(
                   richTextState.annotatedString
                ) {
                    richTextState.richParagraphStyleList.forEachIndexed { index, richParagraphStyle ->
                        Text("Paragraph $index: ${richParagraphStyle.children.size} children")
                        richParagraphStyle.children.forEachIndexed { index, richTextStyle ->
                            RichTextStyleTreeRepresentation(index, richTextStyle, " -")
                        }

                        Divider(modifier = Modifier.padding(vertical = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RichTextStyleTreeRepresentation(index: Int, richSpanStyle: RichSpanStyle, startText: String) {
    Text("${startText}Text $index `$richSpanStyle`: ${richSpanStyle.children.size} children")
    richSpanStyle.children.forEachIndexed { index, richSpanStyle ->
        RichTextStyleTreeRepresentation(index, richSpanStyle, "$startText-")
    }
}