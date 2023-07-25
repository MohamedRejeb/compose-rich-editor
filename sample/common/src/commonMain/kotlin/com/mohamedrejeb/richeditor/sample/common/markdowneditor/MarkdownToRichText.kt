package com.mohamedrejeb.richeditor.sample.common.markdowneditor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownToRichText(
    markdown: TextFieldValue,
    onMarkdownChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val richTextState = rememberRichTextState()

    LaunchedEffect(markdown.text) {
        richTextState.setMarkdown(markdown.text)
    }

    Row(
        modifier = modifier
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                text = "Markdown code:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                value = markdown,
                onValueChange = {
                    onMarkdownChange(it)
                },
            )
        }

        Spacer(Modifier.width(8.dp))

        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                text = "Rich Text:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                item {
                    RichText(
                        state = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}