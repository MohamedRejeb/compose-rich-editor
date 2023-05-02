package com.mohamedrejeb.richeditor.sample.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue

@Composable
fun RichTextStyleRow(
    modifier: Modifier = Modifier,
    value: RichTextValue,
    onValueChanged: (RichTextValue) -> Unit,
) {
    Row(
        modifier = modifier
    ) {
        RichTextStyleButton(
            style = RichTextStyle.Bold,
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.FormatBold
        )

        RichTextStyleButton(
            style = RichTextStyle.Italic,
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.FormatItalic
        )

        RichTextStyleButton(
            style = RichTextStyle.Underline,
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.FormatUnderlined
        )

        RichTextStyleButton(
            style = RichTextStyle.Strikethrough,
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.FormatStrikethrough
        )

        RichTextStyleButton(
            style = RichTextStyle.FontSize(28.sp),
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.FormatSize
        )

        RichTextStyleButton(
            style = RichTextStyle.TextColor(Color.Red),
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Filled.Circle,
            tint = Color.Red
        )

        RichTextStyleButton(
            style = CustomStyle(color = Color.White, background = Color.Green),
            value = value,
            onValueChanged = onValueChanged,
            icon = Icons.Outlined.Circle,
            tint = Color.Green
        )
    }
}