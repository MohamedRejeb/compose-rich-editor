package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mohamedrejeb.richeditor.gesture.detectTapGestures
import com.mohamedrejeb.richeditor.model.ImageLoader
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
public fun BasicRichText(
    state: RichTextState,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    imageLoader: ImageLoader = LocalImageLoader.current,
) {
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val pointerIcon = remember {
        mutableStateOf(PointerIcon.Default)
    }

    val text = remember(
        state.visualTransformation,
        state.annotatedString,
    ) {
        state.visualTransformation.filter(state.annotatedString).text
    }

    CompositionLocalProvider(
        LocalImageLoader provides imageLoader
    ) {
        BasicText(
            text = text,
            modifier = modifier
                .drawRichSpanStyle(state)
                .pointerHoverIcon(pointerIcon.value)
                .pointerInput(state) {
                    try {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            val isLink = state.isLink(position)

                            pointerIcon.value =
                                if (isLink)
                                    PointerIcon.Hand
                                else
                                    PointerIcon.Default
                        }
                    } catch (_: Exception) {

                    }
                }
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = { offset ->
                            state.getLinkByOffset(offset)?.let { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        consumeDown = { offset ->
                            state.isLink(offset)
                        },
                    )
                },
            style = style,
            onTextLayout = {
                state.onTextLayout(
                    textLayoutResult = it,
                    density = density,
                )
                onTextLayout(it)
            },
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            inlineContent = remember(inlineContent, state.inlineContentMap.toMap()) {
                inlineContent + state.inlineContentMap
            }
        )
    }
}