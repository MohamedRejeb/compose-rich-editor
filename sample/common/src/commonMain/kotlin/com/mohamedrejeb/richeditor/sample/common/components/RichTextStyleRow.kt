package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.sample.common.model.CustomStyle

@Composable
fun RichTextStyleRow(
    modifier: Modifier = Modifier,
    value: RichTextValue,
    onValueChanged: (RichTextValue) -> Unit,
) {
    LazyRow(
        modifier = modifier
    ) {
        item {
            RichTextStyleButton(
                style = RichTextStyle.Bold,
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatBold
            )
        }

        item {
            RichTextStyleButton(
                style = RichTextStyle.Italic,
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatItalic
            )
        }

        item {
            RichTextStyleButton(
                style = RichTextStyle.Underline,
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatUnderlined
            )
        }

        item {
            RichTextStyleButton(
                style = RichTextStyle.Strikethrough,
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatStrikethrough
            )
        }

        item {
            RichTextStyleButton(
                style = RichTextStyle.FontSize(28.sp),
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatSize
            )
        }

        item {
            RichTextStyleButton(
                style = RichTextStyle.TextColor(Color.Red),
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Filled.Circle,
                tint = Color.Red
            )
        }

        item {
            RichTextStyleButton(
                style = CustomStyle(color = Color.Blue, background = Color.Green),
                value = value,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.Circle,
                tint = Color.Green
            )
        }
    }
}