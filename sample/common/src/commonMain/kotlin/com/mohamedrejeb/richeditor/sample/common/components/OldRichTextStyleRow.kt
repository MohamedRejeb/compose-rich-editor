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
fun OldRichTextStyleRow(
    modifier: Modifier = Modifier,
    state: RichTextValue,
    onValueChanged: (RichTextValue) -> Unit,
) {
    LazyRow(
        modifier = modifier
    ) {
        item {
            OldRichTextStyleButton(
                style = RichTextStyle.Bold,
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatBold
            )
        }

        item {
            OldRichTextStyleButton(
                style = RichTextStyle.Italic,
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatItalic
            )
        }

        item {
            OldRichTextStyleButton(
                style = RichTextStyle.Underline,
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatUnderlined
            )
        }

        item {
            OldRichTextStyleButton(
                style = RichTextStyle.Strikethrough,
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatStrikethrough
            )
        }

        item {
            OldRichTextStyleButton(
                style = RichTextStyle.FontSize(28.sp),
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.FormatSize
            )
        }

        item {
            OldRichTextStyleButton(
                style = RichTextStyle.TextColor(Color.Red),
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Filled.Circle,
                tint = Color.Red
            )
        }

        item {
            OldRichTextStyleButton(
                style = CustomStyle(color = Color.Blue, background = Color.Green),
                value = state,
                onValueChanged = onValueChanged,
                icon = Icons.Outlined.Circle,
                tint = Color.Green
            )
        }
    }
}