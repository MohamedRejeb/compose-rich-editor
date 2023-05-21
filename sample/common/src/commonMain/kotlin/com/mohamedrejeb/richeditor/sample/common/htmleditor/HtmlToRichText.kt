package com.mohamedrejeb.richeditor.sample.common.htmleditor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlToRichText(
    modifier: Modifier = Modifier,
) {
    var html by remember {
        mutableStateOf(TextFieldValue())
    }
    val richTextValue = RichTextValue.from(html.text)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                text = "HTML Code:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                value = html,
                onValueChange = {
                    html = it
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
                text = "HTML Preview:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
            )

            Spacer(Modifier.height(8.dp))

            RichText(
                richText = richTextValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            )
        }
    }
}