package com.mocoding.richeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import com.mocoding.richeditor.model.RichTextValue
import com.mocoding.richeditor.model.RichTextStyle
import com.mocoding.richeditor.ui.OutlinedRichTextEditor
import com.mocoding.richeditor.ui.RichText

@Composable
fun App() {
    var richTextValue by remember { mutableStateOf(RichTextValue()) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
    ) {
        val focusManager = LocalFocusManager.current
        Column {
            Row {
                IconToggleButton(
                    checked = richTextValue.currentStyles.any { it is RichTextStyle.Bold },
                    onCheckedChange = {
                        richTextValue = richTextValue.toggleStyle(RichTextStyle.Bold)
                    },
                    modifier = Modifier
                        .onFocusChanged {
                            println("Focus changed")
                            println(it.isFocused)
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        Icons.Outlined.FormatBold,
                        contentDescription = "Bold"
                    )
                }

                IconToggleButton(
                    checked = richTextValue.currentStyles.any { it is RichTextStyle.Italic },
                    onCheckedChange = {
                        richTextValue = richTextValue.toggleStyle(RichTextStyle.Italic)
                    },
                ) {
                    Icon(
                        Icons.Outlined.FormatItalic,
                        contentDescription = "Italic"
                    )
                }

                IconToggleButton(
                    checked = richTextValue.currentStyles.any { it is RichTextStyle.Underline },
                    onCheckedChange = {
                        richTextValue = richTextValue.toggleStyle(RichTextStyle.Underline)
                    },
                ) {
                    Icon(
                        Icons.Outlined.FormatUnderlined,
                        contentDescription = "Underline"
                    )
                }

                IconToggleButton(
                    checked = richTextValue.currentStyles.any { it is RichTextStyle.Strikethrough },
                    onCheckedChange = {
                        richTextValue = richTextValue.toggleStyle(RichTextStyle.Strikethrough)
                    },
                ) {
                    Icon(
                        Icons.Outlined.FormatStrikethrough,
                        contentDescription = "Strikethrough"
                    )
                }

                IconToggleButton(
                    checked = richTextValue.currentStyles.any { it is RichTextStyle.Red },
                    onCheckedChange = {
                        richTextValue = richTextValue.toggleStyle(RichTextStyle.Red)
                    },
                ) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = "Red",
                        tint = Color.Red
                    )
                }
            }

            Divider()

            OutlinedRichTextEditor(
                modifier = Modifier.fillMaxWidth(),
                value = richTextValue,
                onValueChange = {
                    richTextValue = it
                },
            )

            RichText(
                value = richTextValue,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}