package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.utils.fastForEach

@Composable
fun BasicRichText(
    state: RichTextState,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf()
) {
    val uriHandler = LocalUriHandler.current
    val pointerIcon = remember {
        mutableStateOf(PointerIcon.Default)
    }

    BasicText(
        text = state.visualTransformation.filter(state.annotatedString).text,
        modifier = modifier
            .drawRichSpanStyle(state)
            .pointerHoverIcon(pointerIcon.value)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        val isLink = state.isLink(position)

                        if (isLink) pointerIcon.value = PointerIcon.Hand
                        else pointerIcon.value = PointerIcon.Default
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        state.getLinkByOffset(offset)?.let { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            },
        style = style,
        onTextLayout = {
            state.onTextLayout(it)
            onTextLayout(it)
        },
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent
    )
}