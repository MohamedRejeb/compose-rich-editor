package com.mocoding.richeditor.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mocoding.richeditor.model.RichTextValue

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

@Composable
fun defaultRichTextStyle() = RichTextStyle(
    textStyle = MaterialTheme.typography.body1,
    textColor = MaterialTheme.colors.onPrimary,
)

data class RichTextStyle(
    val textStyle: TextStyle,
    val textColor: Color,
)
