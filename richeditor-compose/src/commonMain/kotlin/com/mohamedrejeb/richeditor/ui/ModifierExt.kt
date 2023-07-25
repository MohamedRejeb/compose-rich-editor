package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.utils.fastForEach

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

                if (
                    lastAddedItem != null &&
                    lastAddedItem.first::class == richSpan.style::class &&
                    lastAddedItem.second.end == richSpan.textRange.start
                ) {
                    styledRichSpanList[styledRichSpanList.lastIndex] = Pair(
                        lastAddedItem.first,
                        TextRange(lastAddedItem.second.start, richSpan.textRange.end)
                    )
                } else {
                    styledRichSpanList.add(Pair(richSpan.style, richSpan.textRange))
                }
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
                                topPadding = topPadding,
                                startPadding = startPadding,
                                richTextConfig = richTextState.richTextConfig,
                            )
                        }
                    }
                }
            }
        }
}