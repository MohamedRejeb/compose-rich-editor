package com.mohamedrejeb.richeditor.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@Composable
fun DebugRichTextEditor(
    modifier: Modifier = Modifier,
    richTextState: RichTextState,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // BasicRichTextEditor
        Text(
            text = "BasicRichTextEditor:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        content()

        RichTextEditor(
            state = richTextState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(8.dp),
        )

        Divider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "Annoation Length: ${richTextState.annotatedString.text.length}",
        )

        Text(
            text = "Text Length: ${richTextState.textFieldValue.text.length}",
        )

        Text(
            text = "Selection: ${richTextState.selection}",
        )

        Text(
            text = "tree representation:",
        )

        key(
            richTextState.annotatedString
        ) {
            richTextState.richParagraphList.forEachIndexed { index, richParagraphStyle ->
                Text("Paragraph $index: ${richParagraphStyle.children.size} children")
                Text(" - Start Text: ${richParagraphStyle.type.startRichSpan}")
                richParagraphStyle.children.forEachIndexed { index, richTextStyle ->
                    RichTextStyleTreeRepresentation(index, richTextStyle, " -")
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
}

@Composable
private fun RichTextStyleTreeRepresentation(index: Int, richSpan: RichSpan, startText: String) {
    Text("${startText}Text $index `$richSpan`: ${richSpan.children.size} children")
    richSpan.children.forEachIndexed { index, richSpan ->
        RichTextStyleTreeRepresentation(index, richSpan, "$startText-")
    }
}