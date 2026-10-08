package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
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

            translate(top = -richTextState.scrollState.value.toFloat()) {
                richTextState.textLayoutResult?.let { textLayoutResult ->
                    val textLength = textLayoutResult.layoutInput.text.length
                    if (
                        textLength > 0 &&
                        textLayoutResult.lineCount > 0 &&
                        textLayoutResult.isForModelText(richTextState.annotatedString.length)
                    ) {
                        // maxLines and overflow stop the layout early, and asking it for a line
                        // past what it laid out throws.
                        val laidOutEnd = textLayoutResult.getLineEnd(textLayoutResult.lineCount - 1)
                        val isCutOff = laidOutEnd < textLength
                        val lastOffset = if (isCutOff) laidOutEnd - 1 else laidOutEnd
                        richTextState.renderedCodeBlocks.fastForEach { block ->
                            if (block.range.min > lastOffset) return@fastForEach
                            val firstLine = textLayoutResult.getLineForOffset(block.range.min.coerceIn(0, lastOffset))
                            val lastLine = textLayoutResult.getLineForOffset(block.lastLineOffset.coerceIn(0, lastOffset))
                            val top = textLayoutResult.getLineTop(firstLine)
                            drawRoundRect(
                                color = richTextState.config.codeBlockBackgroundColor,
                                topLeft = Offset(0f, top + topPadding),
                                size = Size(size.width, textLayoutResult.getLineBottom(lastLine) - top),
                                cornerRadius = CornerRadius(CodeBlockCornerRadius.toPx()),
                            )
                        }
                    }
                }
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

private val CodeBlockCornerRadius = 8.dp
