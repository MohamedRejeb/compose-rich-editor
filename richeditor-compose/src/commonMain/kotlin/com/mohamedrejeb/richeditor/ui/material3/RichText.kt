package com.mohamedrejeb.richeditor.ui.material3

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.model.RichTextValue

@Composable
fun RichText(
    value: RichTextValue,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = value.visualTransformation.filter(value.annotatedString).text,
    )
}
