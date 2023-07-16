package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.model.RichParagraph.Type.Companion.startText
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.utils.append
import com.mohamedrejeb.richeditor.utils.fastForEachIndexed
import kotlin.math.max
import kotlin.math.min

/**
 * A [ClipboardManager] that can handle [RichTextState].
 * It will convert the [RichTextState] to [AnnotatedString] and delegate the [ClipboardManager] to handle the rest.
 *
 * @param richTextState The [RichTextState] to be handled.
 * @param clipboardManager The [ClipboardManager] to delegate the rest of the work to.
 */
internal class RichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboardManager: ClipboardManager
): ClipboardManager {
    override fun getText(): AnnotatedString? {
        return clipboardManager.getText()
    }

    override fun setText(annotatedString: AnnotatedString) {
        val selection = richTextState.selection
        val richTextAnnotatedString = buildAnnotatedString {
            var index = 0
            richTextState.richParagraphList.fastForEachIndexed { i, richParagraphStyle ->
                withStyle(richParagraphStyle.paragraphStyle.merge(richParagraphStyle.type.style)) {
                    if (
                        !selection.collapsed &&
                        selection.min < index + richParagraphStyle.type.startText.length &&
                        selection.max > index
                    ) {
                        val selectedText = richParagraphStyle.type.startText.substring(
                            max(0, selection.min - index),
                            min(selection.max - index, richParagraphStyle.type.startText.length)
                        )
                        append(selectedText)
                    }
                    index += richParagraphStyle.type.startText.length
                    withStyle(RichSpanStyle.DefaultSpanStyle) {
                        index = append(
                            richSpanList = richParagraphStyle.children,
                            startIndex = index,
                            selection = selection,
                            richTextConfig = richTextState.richTextConfig,
                        )
                        if (!richTextState.singleParagraphMode) {
                            if (i != richTextState.richParagraphList.lastIndex) {
                                if (
                                    !selection.collapsed &&
                                    selection.min < index + 1 &&
                                    selection.max > index
                                ) append("\n")
                                index++
                            }
                        }
                    }
                }
            }
        }
        clipboardManager.setText(richTextAnnotatedString)
    }
}