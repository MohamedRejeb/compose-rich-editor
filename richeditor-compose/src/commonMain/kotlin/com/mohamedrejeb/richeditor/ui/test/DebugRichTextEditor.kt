package com.mohamedrejeb.richeditor.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.utils.fastForEachIndexed

@Composable
internal fun DebugRichTextEditor(
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
            text = "Annotation Length: ${richTextState.annotatedString.text.length}",
        )

        SelectionContainer {
            Text(
                text = "Annotation Text: ${richTextState.annotatedString.text}",
            )
        }

        Text(
            text = "Text Length: ${richTextState.textFieldValue.text.length}",
        )

        SelectionContainer {
            Text(
                text = "Text Length: ${richTextState.textFieldValue.text}",
            )
        }

        Text(
            text = "Selection: ${richTextState.selection}",
        )

        Text(
            text = "tree representation:",
        )

        key(
            richTextState.annotatedString
        ) {
            richTextState.richParagraphList.fastForEachIndexed { index, richParagraphStyle ->
                Text("Paragraph $index: ${richParagraphStyle.children.size} children")
                Text(" - Start Text: ${richParagraphStyle.type.startRichSpan}")
                richParagraphStyle.children.fastForEachIndexed { childIndex, richTextStyle ->
                    RichTextStyleTreeRepresentation(childIndex, richTextStyle, " -")
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
}

@Composable
private fun RichTextStyleTreeRepresentation(index: Int, richSpan: RichSpan, startText: String) {
    Text("${startText}Text $index `$richSpan`: ${richSpan.children.size} children")
    richSpan.children.fastForEachIndexed { childIndex, childRichSpan ->
        RichTextStyleTreeRepresentation(childIndex, childRichSpan, "$startText-")
    }
}

internal fun getRichTextStyleTreeRepresentation(
    stringBuilder: StringBuilder,
    index: Int,
    richSpan: RichSpan,
    startText: String
) {
    stringBuilder.append("${startText}Text $index `$richSpan`: ${richSpan.children.size} children")
    stringBuilder.appendLine()
    richSpan.children.fastForEachIndexed { childIndex, childRichSpan ->
        getRichTextStyleTreeRepresentation(stringBuilder, childIndex, childRichSpan, "$startText-")
    }
}