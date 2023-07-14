package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.sample.common.slack.SlackDemoPanelButton

@Composable
fun NewRichTextStyleRow(
    modifier: Modifier = Modifier,
    richTextState: RichTextState,
) {
    LazyRow(
        modifier = modifier
    ) {
        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Left,
                        )
                    )
                },
                isSelected = richTextState.currentParagraphStyle.textAlign == TextAlign.Left,
                icon = Icons.Outlined.FormatAlignLeft
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Center
                        )
                    )
                },
                isSelected = richTextState.currentParagraphStyle.textAlign == TextAlign.Center,
                icon = Icons.Outlined.FormatAlignCenter
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Right
                        )
                    )
                },
                isSelected = richTextState.currentParagraphStyle.textAlign == TextAlign.Right,
                icon = Icons.Outlined.FormatAlignRight
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                icon = Icons.Outlined.FormatBold
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                icon = Icons.Outlined.FormatItalic
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                icon = Icons.Outlined.FormatUnderlined
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                icon = Icons.Outlined.FormatStrikethrough
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            fontSize = 28.sp
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.fontSize == 28.sp,
                icon = Icons.Outlined.FormatSize
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            color = Color.Red
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.color == Color.Red,
                icon = Icons.Filled.Circle,
                tint = Color.Red
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleSpanStyle(
                        SpanStyle(
                            background = Color.Yellow
                        )
                    )
                },
                isSelected = richTextState.currentSpanStyle.background == Color.Yellow,
                icon = Icons.Outlined.Circle,
                tint = Color.Yellow
            )
        }

        item {
            Box(
                Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFF393B3D))
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleUnorderedList()
                },
                isSelected = richTextState.currentRichParagraphType is RichParagraph.Type.UnorderedList,
                icon = Icons.Outlined.FormatListBulleted,
            )
        }

        item {
            NewRichTextStyleButton(
                onClick = {
                    richTextState.toggleOrderedList()
                },
                isSelected = richTextState.currentRichParagraphType is RichParagraph.Type.OrderedList,
                icon = Icons.Outlined.FormatListNumbered,
            )
        }
    }
}