package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.isForModelText
import com.mohamedrejeb.richeditor.model.linesOf
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalRichTextApi::class)
internal fun Modifier.drawRichSpanStyle(
    richTextState: RichTextState,
    topPadding: Float = 0f,
    startPadding: Float = 0f,
    endPadding: Float = 0f,
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

            // A block is as wide as the text area, and one scrolled partly out of an editor
            // must not paint over the padding around that area.
            clipRect(
                left = startPadding,
                top = topPadding,
                right = size.width - endPadding,
                bottom = size.height - bottomPadding,
            ) {
                translate(top = -richTextState.scrollState.value.toFloat()) {
                    richTextState.textLayoutResult?.let { textLayoutResult ->
                        if (textLayoutResult.isForModelText(richTextState.annotatedString.length)) {
                            richTextState.renderedCodeBlocks.fastForEach { block ->
                                val lines = textLayoutResult.linesOf(block) ?: return@fastForEach
                                val gap = CodeBlockVerticalGap.toPx()
                                val top = textLayoutResult.getLineTop(lines.first) + gap
                                drawRoundRect(
                                    color = richTextState.config.codeBlockBackgroundColor,
                                    topLeft = Offset(startPadding, top + topPadding),
                                    size = Size(
                                        width = size.width - startPadding - endPadding,
                                        height = textLayoutResult.getLineBottom(lines.last) - gap - top,
                                    ),
                                    cornerRadius = CornerRadius(CodeBlockCornerRadius.toPx()),
                                )
                            }
                        }
                    }
                }
            }

            translate(top = -richTextState.scrollState.value.toFloat()) {
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

/** Left clear above and below a block's background, inside the room its first and last line make. */
internal val CodeBlockVerticalGap = 2.dp
