package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.utils.getBoundingBoxes

@ExperimentalRichTextApi
public interface RichSpanStyle {
    public val spanStyle: (RichTextConfig) -> SpanStyle

    /**
     * If true, the user can add new text in the edges of the span,
     * For example, if the span is "Hello" and the user adds "World" in the end, the span will be "Hello World"
     * If false, the user can't add new text in the edges of the span,
     * For example, if the span is a "Hello" link and the user adds "World" in the end, the "World" will be added in a separate a span,
     */
    public val acceptNewTextInTheEdges: Boolean

    public fun DrawScope.drawCustomStyle(
        layoutResult: TextLayoutResult,
        textRange: TextRange,
        richTextConfig: RichTextConfig,
        topPadding: Float = 0f,
        startPadding: Float = 0f,
    )

    public fun AnnotatedString.Builder.appendCustomContent(
        richTextState: RichTextState
    ): AnnotatedString.Builder = this

    public class Link(
        public val url: String,
    ) : RichSpanStyle {
        override val spanStyle: (RichTextConfig) -> SpanStyle = {
            SpanStyle(
                color = it.linkColor,
                textDecoration = it.linkTextDecoration,
            )
        }

        override fun DrawScope.drawCustomStyle(
            layoutResult: TextLayoutResult,
            textRange: TextRange,
            richTextConfig: RichTextConfig,
            topPadding: Float,
            startPadding: Float,
        ): Unit = Unit

        override val acceptNewTextInTheEdges: Boolean =
            false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Link) return false

            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            return url.hashCode()
        }
    }

    public class Code(
        private val cornerRadius: TextUnit = 8.sp,
        private val strokeWidth: TextUnit = 1.sp,
        private val padding: TextPaddingValues = TextPaddingValues(horizontal = 2.sp, vertical = 2.sp)
    ) : RichSpanStyle {
        override val spanStyle: (RichTextConfig) -> SpanStyle = {
            SpanStyle(
                color = it.codeSpanColor,
            )
        }

        override fun DrawScope.drawCustomStyle(
            layoutResult: TextLayoutResult,
            textRange: TextRange,
            richTextConfig: RichTextConfig,
            topPadding: Float,
            startPadding: Float,
        ) {
            val path = Path()
            val backgroundColor = richTextConfig.codeSpanBackgroundColor
            val strokeColor = richTextConfig.codeSpanStrokeColor
            val cornerRadius = CornerRadius(cornerRadius.toPx())
            val boxes = layoutResult.getBoundingBoxes(
                startOffset = textRange.start,
                endOffset = textRange.end,
                flattenForFullParagraphs = true
            )

            boxes.fastForEachIndexed { index, box ->
                path.addRoundRect(
                    RoundRect(
                        rect = box.copy(
                            left = box.left - padding.horizontal.toPx() + startPadding,
                            right = box.right + padding.horizontal.toPx() + startPadding,
                            top = box.top - padding.vertical.toPx() + topPadding,
                            bottom = box.bottom + padding.vertical.toPx() + topPadding,
                        ),
                        topLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
                        bottomLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
                        topRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero,
                        bottomRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero
                    )
                )
                drawPath(
                    path = path,
                    color = backgroundColor,
                    style = Fill
                )
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                    )
                )
            }
        }

        override val acceptNewTextInTheEdges: Boolean =
            true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Code) return false

            if (cornerRadius != other.cornerRadius) return false
            if (strokeWidth != other.strokeWidth) return false
            if (padding != other.padding) return false

            return true
        }

        override fun hashCode(): Int {
            var result = cornerRadius.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            result = 31 * result + padding.hashCode()
            return result
        }
    }

    public class Image(
        public val model: Any,
        width: TextUnit,
        height: TextUnit,
        public val contentDescription: String? = null,
    ) : RichSpanStyle {

        init {
            require(width.isSpecified || height.isSpecified) {
                "At least one of the width or height should be specified"
            }

            require(width.value >= 0 || height.value >= 0) {
                "The width and height should be greater than or equal to 0"
            }

            require(width.value.isFinite() || height.value.isFinite()) {
                "The width and height should be finite"
            }
        }

        public var width: TextUnit by mutableStateOf(width)
            private set

        public var height: TextUnit by mutableStateOf(height)
            private set

        private val id get() = "$model-${width.value}-${height.value}"

        override val spanStyle: (RichTextConfig) -> SpanStyle =
            { SpanStyle() }

        override fun DrawScope.drawCustomStyle(
            layoutResult: TextLayoutResult,
            textRange: TextRange,
            richTextConfig: RichTextConfig,
            topPadding: Float,
            startPadding: Float,
        ): Unit = Unit

        override fun AnnotatedString.Builder.appendCustomContent(
            richTextState: RichTextState
        ): AnnotatedString.Builder {
            if (id !in richTextState.inlineContentMap.keys) {
                richTextState.inlineContentMap[id] = createInlineTextContent(richTextState = richTextState)
            }

            richTextState.usedInlineContentMapKeys.add(id)

            appendInlineContent(id = id)

            return this
        }

        private fun createInlineTextContent(
            richTextState: RichTextState
        ): InlineTextContent =
            InlineTextContent(
                placeholder = Placeholder(
                    width = width.value.coerceAtLeast(0f).sp,
                    height = height.value.coerceAtLeast(0f).sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                ),
                children = {
                    val density = LocalDensity.current
                    val imageLoader = LocalImageLoader.current
                    val data = imageLoader.load(model) ?: return@InlineTextContent

                    LaunchedEffect(id, data) {
                        if (data.painter.intrinsicSize.isUnspecified)
                            return@LaunchedEffect

                        val newWidth = with(density) {
                            data.painter.intrinsicSize.width.coerceAtLeast(0f).toSp()
                        }
                        val newHeight = with(density) {
                            data.painter.intrinsicSize.height.coerceAtLeast(0f).toSp()
                        }

                        if (width == newWidth && height == newHeight)
                            return@LaunchedEffect

                        richTextState.inlineContentMap.remove(id)

                        if (width.isUnspecified || width.value <= 0)
                            width = newWidth

                        if (height.isUnspecified || height.value <= 0)
                            height = newHeight

                        richTextState.inlineContentMap[id] = createInlineTextContent(richTextState = richTextState)
                        richTextState.updateAnnotatedString()
                    }

                    Image(
                        painter = data.painter,
                        contentDescription = data.contentDescription ?: contentDescription,
                        alignment = data.alignment,
                        contentScale = data.contentScale,
                        modifier = data.modifier
                            .fillMaxSize()
                    )
                }
            )

        override val acceptNewTextInTheEdges: Boolean =
            false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false

            if (model != other.model) return false
            if (width != other.width) return false
            if (height != other.height) return false

            return true
        }

        override fun hashCode(): Int {
            var result = model.hashCode()
            result = 31 * result + width.hashCode()
            result = 31 * result + height.hashCode()
            return result
        }
    }

    public data object Default : RichSpanStyle {
        override val spanStyle: (RichTextConfig) -> SpanStyle =
            { SpanStyle() }

        override fun DrawScope.drawCustomStyle(
            layoutResult: TextLayoutResult,
            textRange: TextRange,
            richTextConfig: RichTextConfig,
            topPadding: Float,
            startPadding: Float,
        ): Unit = Unit

        override val acceptNewTextInTheEdges: Boolean =
            true
    }

    public companion object {
        internal val DefaultSpanStyle = SpanStyle()
    }
}