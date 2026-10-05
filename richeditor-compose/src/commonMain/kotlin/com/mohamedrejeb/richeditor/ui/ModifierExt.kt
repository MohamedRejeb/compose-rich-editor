package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.isForModelText
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalRichTextApi::class)
internal fun Modifier.drawRichSpanStyle(
    richTextState: RichTextState,
    topPadding: Float = 0f,
    startPadding: Float = 0f,
    bottomPadding: Float = 0f,
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

            // This node is the editor's outer box and does not clip, while the text clips at the
            // text area inside the padding. Clip where content is scrolled past an edge (#601),
            // and nowhere else: a span's padding and stroke reach a little outside its line, and
            // must stay whole on the first and last line of text that is at rest.
            val scrollState = richTextState.scrollState
            val clipTop = if (scrollState.value > 0) topPadding else -UnclippedExtent
            val clipBottom =
                if (scrollState.value < scrollState.maxValue) size.height - bottomPadding
                else size.height + UnclippedExtent

            clipRect(
                left = -UnclippedExtent,
                top = clipTop,
                right = size.width + UnclippedExtent,
                bottom = clipBottom,
            ) {
            translate(top = -scrollState.value.toFloat()) {
                styledRichSpanList.fastForEach { (style, textRange) ->
                    richTextState.textLayoutResult?.let { textLayoutResult ->
                        with(style) {
                            if (textLayoutResult.isForModelText(richTextState.annotatedString.length)) {
                                drawCustomStyle(
                                    layoutResult = textLayoutResult,
                                    textRange = textRange,
                                    config = richTextState.config,
                                    topPadding = topPadding,
                                    startPadding = startPadding
                                )
                            }
                        }
                    }
                }
            }
            }
        }
}
// Far enough that an edge using it never cuts anything; finite, since a clip rectangle with an
// infinite edge is not applied.
private const val UnclippedExtent = 100_000f
