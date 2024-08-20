package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalRichTextApi::class)
internal fun Modifier.drawRichSpanStyle(
    richTextState: RichTextState,
    topPadding: Float = 0f,
    startPadding: Float = 0f,
): Modifier {
    return this
        .drawBehind {
            val styledRichSpanList = mutableListOf<Pair<RichSpanStyle, TextRange>>()

            richTextState.styledRichSpanList.fastForEach { richSpan ->
                val lastAddedItem = styledRichSpanList.lastOrNull()

                val end = richSpan.getLastNonEmptyChild()?.textRange?.end ?: richSpan.textRange.end

                if (
                    lastAddedItem != null &&
                    lastAddedItem.first::class == richSpan.richSpanStyle::class &&
                    lastAddedItem.second.end == richSpan.textRange.start
                )
                    styledRichSpanList[styledRichSpanList.lastIndex] =
                        lastAddedItem.first to TextRange(lastAddedItem.second.start, end)
                else
                    styledRichSpanList.add(richSpan.richSpanStyle to TextRange(richSpan.textRange.start, end))
            }

            styledRichSpanList.fastForEach { (style, textRange) ->
                richTextState.textLayoutResult?.let { textLayoutResult ->
                    with(style) {
                        val textLength = richTextState.annotatedString.length
                        val measuredTextLength = textLayoutResult.multiParagraph.intrinsics.annotatedString.length
                        if (textLength == measuredTextLength) {
                            drawCustomStyle(
                                layoutResult = textLayoutResult,
                                textRange = textRange,
                                richTextConfig = richTextState.config,
                                topPadding = topPadding,
                                startPadding = startPadding
                            )
                        }
                    }
                }
            }
        }
}