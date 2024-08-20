package com.mohamedrejeb.richeditor.sample.common.htmleditor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.coil3.Coil3ImageLoader
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun HtmlToRichText(
    html: TextFieldValue,
    onHtmlChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val richTextState = rememberRichTextState()

    LaunchedEffect(html.text) {
        richTextState.setHtml(html.text)
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
                text = "HTML code:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                value = html,
                onValueChange = {
                    onHtmlChange(it)
                },
            )
        }

        Spacer(Modifier.width(8.dp))

        HorizontalDivider(
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
                    SelectionContainer {
                        RichText(
                            state = richTextState,
                            imageLoader = Coil3ImageLoader,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}