package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

/**
 * Used in [RichTextState.updateAnnotatedString]
*/
internal fun AnnotatedString.Builder.append(
    state: RichTextState,
    richSpanList: MutableList<RichSpan>,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
): Int {
    return appendRichSpan(
        state = state,
        richSpanList = richSpanList,
        startIndex = startIndex,
        text = text,
        selection = selection,
        onStyledRichSpan = onStyledRichSpan,
    )
}

/**
 * Used in [RichTextState.updateAnnotatedString]
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.appendRichSpan(
    state: RichTextState,
    parent: RichSpan? = null,
    richSpanList: MutableList<RichSpan>,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
): Int {
    var index = startIndex
    var previousRichSpan = parent
    val toRemoveRichSpanIndices = mutableListOf<Int>()

    richSpanList.fastForEachIndexed { i, richSpan ->
        index = append(
            state = state,
            richSpan = richSpan,
            startIndex = index,
            text = text,
            selection = selection,
            onStyledRichSpan = onStyledRichSpan,
        )

        if (
            previousRichSpan != null &&
            previousRichSpan.spanStyle == richSpan.spanStyle &&
            previousRichSpan.richSpanStyle == richSpan.richSpanStyle &&
            previousRichSpan.children.isEmpty() &&
            richSpan.children.isEmpty()
        ) {
            previousRichSpan.text += richSpan.text
            previousRichSpan.textRange = TextRange(previousRichSpan.textRange.min, richSpan.textRange.max)
            toRemoveRichSpanIndices.add(i)
        } else {
            previousRichSpan = richSpan
        }
    }

    toRemoveRichSpanIndices.reversed().forEach { i ->
        richSpanList.removeAt(i)
    }

    if (
        parent != null &&
        parent.text.isEmpty() &&
        richSpanList.size == 1 &&
        (parent.richSpanStyle is RichSpanStyle.Default || richSpanList.first().richSpanStyle is RichSpanStyle.Default)
    ) {
        val firstChild = richSpanList.first()

        val richSpanStyle =
            if (firstChild.richSpanStyle !is RichSpanStyle.Default)
                firstChild.richSpanStyle
            else
                parent.richSpanStyle

        parent.spanStyle = parent.spanStyle.merge(firstChild.spanStyle)
        parent.richSpanStyle = richSpanStyle
        parent.text = firstChild.text
        parent.textRange = firstChild.textRange
        parent.children.clear()
        parent.children.addAll(firstChild.children)
    }

    return index
}


/**
 * Used in [RichTextState.updateAnnotatedString]
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.append(
    state: RichTextState,
    richSpan: RichSpan,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle.merge(resolveRichSpanStyleStyle(state, richSpan.richSpanStyle))) {
        if (richSpan.richSpanStyle is RichSpanStyle.Image) {
            // Image owns a single placeholder char in the raw text;
            // appendCustomContent (via appendInlineContent) emits that
            // char plus the inline-content annotation. Skip the normal
            // append path so the placeholder isn't duplicated. See #466.
            richSpan.textRange = TextRange(index, index + richSpan.text.length)

            with(richSpan.richSpanStyle) {
                appendCustomContent(richTextState = state)
            }

            onStyledRichSpan(richSpan)

            index += richSpan.text.length
            return@withStyle
        }

        val newText = text.substring(index, index + richSpan.text.length)

        richSpan.text = newText
        richSpan.textRange = TextRange(index, index + richSpan.text.length)

        // Ignore setting the background color for the selected text to avoid the selection being hidden
        if (
            !selection.collapsed &&
            selection.min < index + richSpan.text.length &&
            selection.max > index
        ) {
            val beforeSelection =
                if (selection.min > index)
                    richSpan.text.substring(0, selection.min - index)
                else
                    ""

            val selectedText =
                richSpan.text.substring(
                    max(0, selection.min - index),
                    min(selection.max - index, richSpan.text.length)
                )

            val afterSelection =
                if (selection.max - index < richSpan.text.length)
                    richSpan.text.substring(selection.max - index)
                else
                    ""

            append(beforeSelection)
            withStyle(SpanStyle(background = Color.Transparent)) {
                append(selectedText)
            }
            append(afterSelection)
        } else {
            append(newText)
        }

        with(richSpan.richSpanStyle) {
            appendCustomContent(
                richTextState = state
            )
        }

        if (richSpan.richSpanStyle !is RichSpanStyle.Default) {
            onStyledRichSpan(richSpan)
        }

        index += richSpan.text.length

        index = appendRichSpan(
            state = state,
            parent = richSpan,
            richSpanList = richSpan.children,
            startIndex = index,
            text = text,
            selection = selection,
            onStyledRichSpan = onStyledRichSpan,
        )
    }
    return index
}

/**
 * Used in [RichTextState.updateRichParagraphList]
 */
internal fun AnnotatedString.Builder.append(
    state: RichTextState,
    richSpanList: List<RichSpan>,
    startIndex: Int,
    onStyledRichSpan: (RichSpan) -> Unit,
): Int {
    var index = startIndex
    richSpanList.fastForEach { richSpan ->
        index = append(
            state = state,
            richSpan = richSpan,
            startIndex = index,
            onStyledRichSpan = onStyledRichSpan,
        )
    }
    return index
}

/**
 * Used in [RichTextState.updateRichParagraphList]
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.append(
    state: RichTextState,
    richSpan: RichSpan,
    startIndex: Int,
    onStyledRichSpan: (RichSpan) -> Unit,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle.merge(resolveRichSpanStyleStyle(state, richSpan.richSpanStyle))) {
        richSpan.textRange = TextRange(index, index + richSpan.text.length)

        // Image owns a single placeholder char in the raw text;
        // appendCustomContent (via appendInlineContent) emits that
        // char plus the inline-content annotation. Skip append(text)
        // for images so the placeholder isn't duplicated. See #466.
        if (richSpan.richSpanStyle !is RichSpanStyle.Image) {
            append(richSpan.text)
        }

        with(richSpan.richSpanStyle) {
            appendCustomContent(
                richTextState = state,
            )
        }

        if (richSpan.richSpanStyle !is RichSpanStyle.Default) {
            onStyledRichSpan(richSpan)
        }

        index += richSpan.text.length
        richSpan.children.fastForEach { richSpan ->
            index = append(
                state = state,
                richSpan = richSpan,
                startIndex = index,
                onStyledRichSpan = onStyledRichSpan,
            )
        }
    }
    return index
}

/**
 * Resolve a span's render-time [SpanStyle], preferring the registered [Trigger]'s style
 * for [RichSpanStyle.Token] spans when the trigger is known. Falls back to the style
 * encoded on the [RichSpanStyle] itself (which, for Token, is a neutral default).
 */
@OptIn(ExperimentalRichTextApi::class)
private fun resolveRichSpanStyleStyle(
    state: RichTextState,
    richSpanStyle: RichSpanStyle,
): SpanStyle {
    if (richSpanStyle is RichSpanStyle.Token) {
        val trigger = state.findTrigger(richSpanStyle.triggerId)
        if (trigger != null) return trigger.style(state.config)
    }
    return richSpanStyle.spanStyle(state.config)
}