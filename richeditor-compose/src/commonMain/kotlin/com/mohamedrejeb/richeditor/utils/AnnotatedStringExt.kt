package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextConfig
import kotlin.math.max
import kotlin.math.min

internal fun AnnotatedString.Builder.append(
    richSpanList: MutableList<RichSpan>,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
    richTextConfig: RichTextConfig,
): Int {
    return appendRichSpan(
        richSpanList = richSpanList,
        startIndex = startIndex,
        text = text,
        selection = selection,
        onStyledRichSpan = onStyledRichSpan,
        richTextConfig = richTextConfig,
    )
}

internal fun AnnotatedString.Builder.append(
    richSpanList: List<RichSpan>,
    startIndex: Int,
    selection: TextRange,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex
    richSpanList.fastForEach { richSpan ->
        index = append(
            richSpan = richSpan,
            startIndex = index,
            selection = selection,
            richTextConfig = richTextConfig,
        )
    }
    return index
}

internal fun AnnotatedString.Builder.append(
    richSpanList: List<RichSpan>,
    startIndex: Int,
    onStyledRichSpan: (RichSpan) -> Unit,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex
    richSpanList.fastForEach { richSpan ->
        index = append(
            richSpan = richSpan,
            startIndex = index,
            onStyledRichSpan = onStyledRichSpan,
            richTextConfig = richTextConfig,
        )
    }
    return index
}

@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.append(
    richSpan: RichSpan,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle.merge(richSpan.style.spanStyle(richTextConfig))) {
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

        if (richSpan.style !is RichSpanStyle.Default) {
            onStyledRichSpan(richSpan)
        }

        index += richSpan.text.length

        index = appendRichSpan(
            parent = richSpan,
            richSpanList = richSpan.children,
            startIndex = index,
            text = text,
            selection = selection,
            onStyledRichSpan = onStyledRichSpan,
            richTextConfig = richTextConfig,
        )
    }
    return index
}

@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.appendRichSpan(
    parent: RichSpan? = null,
    richSpanList: MutableList<RichSpan>,
    startIndex: Int,
    text: String,
    selection: TextRange,
    onStyledRichSpan: (RichSpan) -> Unit,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex
    var previousRichSpan = parent
    val toRemoveRichSpanIndices = mutableListOf<Int>()

    richSpanList.fastForEachIndexed { i, richSpan ->
        index = append(
            richSpan = richSpan,
            startIndex = index,
            text = text,
            selection = selection,
            onStyledRichSpan = onStyledRichSpan,
            richTextConfig = richTextConfig,
        )

        if (
            previousRichSpan != null &&
            previousRichSpan!!.spanStyle == richSpan.spanStyle &&
            previousRichSpan!!.style == richSpan.style &&
            previousRichSpan!!.children.isEmpty() &&
            richSpan.children.isEmpty()
        ) {
            previousRichSpan!!.text += richSpan.text
            previousRichSpan!!.textRange = TextRange(previousRichSpan!!.textRange.min, richSpan.textRange.max)
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
        richSpanList.size == 1
    ) {
        val firstChild = richSpanList.first()
        parent.spanStyle = parent.spanStyle.merge(firstChild.spanStyle)
        parent.style = firstChild.style
        parent.text = firstChild.text
        parent.textRange = firstChild.textRange
        parent.children.clear()
        parent.children.addAll(firstChild.children)
    }

    return index
}

@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.append(
    richSpan: RichSpan,
    startIndex: Int,
    selection: TextRange,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle.merge(richSpan.style.spanStyle(richTextConfig))) {
        richSpan.textRange = TextRange(index, index + richSpan.text.length)
        if (
            !selection.collapsed &&
            selection.min < index + richSpan.text.length &&
            selection.max > index
        ) {
            val selectedText = richSpan.text.substring(
                max(0, selection.min - index),
                min(selection.max - index, richSpan.text.length)
            )
            append(selectedText)
        }
        index += richSpan.text.length
        richSpan.children.fastForEach { richSpan ->
            index = append(
                richSpan = richSpan,
                startIndex = index,
                selection = selection,
                richTextConfig = richTextConfig,
            )
        }
    }
    return index
}

@OptIn(ExperimentalRichTextApi::class)
internal fun AnnotatedString.Builder.append(
    richSpan: RichSpan,
    startIndex: Int,
    onStyledRichSpan: (RichSpan) -> Unit,
    richTextConfig: RichTextConfig,
): Int {
    var index = startIndex

    withStyle(richSpan.spanStyle.merge(richSpan.style.spanStyle(richTextConfig))) {
        richSpan.textRange = TextRange(index, index + richSpan.text.length)
        append(richSpan.text)

        if (richSpan.style !is RichSpanStyle.Default) {
            onStyledRichSpan(richSpan)
        }

        index += richSpan.text.length
        richSpan.children.fastForEach { richSpan ->
            index = append(
                richSpan = richSpan,
                startIndex = index,
                onStyledRichSpan = onStyledRichSpan,
                richTextConfig = richTextConfig,
            )
        }
    }
    return index
}