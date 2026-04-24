package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.utils.getBoundingBoxes
import kotlin.random.Random

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

    /**
     * If true, the span is treated as a single atomic unit for editing:
     * backspace deletes the whole span, typing adjacent to it creates a sibling span
     * instead of appending into it, and selections that straddle the span snap to its edges.
     *
     * Defaults to false. Overridden to true by [Image] and atomic token spans (e.g. mentions).
     */
    public val isAtomic: Boolean get() = false

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

        /**
         * Initial `(width, height)` for this Image span.
         *
         * Consult [resolvedDimensionsCache] first: a previously-rendered
         * Image with the same [model] (same src) will have populated the
         * cache with its post-clamp size. Using that eliminates the
         * big-then-small flicker that otherwise happens when HTML attrs
         * overstate the display size (e.g. `<img width="600">` on a 2x
         * density screen would reserve a 600sp slot that later shrinks to
         * the 300sp intrinsic once the painter resolves).
         *
         * Falls back to the caller-supplied dimensions on the first-ever
         * render of a given model, where the cache is still empty.
         */
        private val initialDimensions: Pair<TextUnit, TextUnit> =
            resolvedDimensionsCache[model] ?: (width to height)

        public var width: TextUnit by mutableStateOf(initialDimensions.first)
            private set

        public var height: TextUnit by mutableStateOf(initialDimensions.second)
            private set

        /**
         * Stable, per-instance id used as the key into
         * [RichTextState.inlineContentMap]. Deliberately independent of
         * [width]/[height] so that updating dimensions (from intrinsic size,
         * from the container-width clamp, etc.) replaces the map entry in
         * place instead of routing through a different key, which would
         * briefly desync the annotated string's inline-content marker from
         * the map and cause the image to disappear for a frame.
         */
        private val id: String = "richtext-img-${Random.nextLong().toULong().toString(16)}"

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
                    val maxImageWidth = LocalRichTextMaxImageWidthProvider.current.maxWidth
                    val data = imageLoader.load(model) ?: return@InlineTextContent
                    // Read intrinsicSize in composable scope so we observe its
                    // state. Async painters (Coil, etc.) start with
                    // [Size.Unspecified] and flip to a real size once the
                    // image decodes. Including it in the effect key ensures
                    // the effect re-runs when the size becomes available,
                    // instead of exiting early on first run and leaving the
                    // Placeholder at 0x0 forever.
                    val intrinsicSize = data.painter.intrinsicSize

                    // [id] is included in the key so that each fresh Image
                    // instance (new id) retriggers the clamp. BasicText's
                    // inline-content subcomposition is sometimes reused across
                    // `setHtml(...)` calls even when the Image instance and
                    // inlineContentMap key change — the remembered [data] and
                    // the remembered `imageData` state in ImageLoaders that
                    // cache painters (e.g. Coil3) end up identical to the
                    // previous scope, so without a fresh key the effect would
                    // see unchanged (data, intrinsicSize, maxImageWidth) and
                    // skip its body, leaving the new Image's dimensions
                    // un-clamped until the next container resize.
                    LaunchedEffect(id, data, intrinsicSize, maxImageWidth) {
                        if (intrinsicSize.isUnspecified)
                            return@LaunchedEffect

                        val intrinsicWidth = with(density) {
                            intrinsicSize.width.coerceAtLeast(0f).toSp()
                        }
                        val intrinsicHeight = with(density) {
                            intrinsicSize.height.coerceAtLeast(0f).toSp()
                        }

                        val (clampedWidth, clampedHeight) = clampToMaxWidth(
                            width = intrinsicWidth,
                            height = intrinsicHeight,
                            maxWidth = maxImageWidth,
                        )

                        val shouldSetWidth = width.isUnspecified ||
                            width.value <= 0 ||
                            width != clampedWidth
                        val shouldSetHeight = height.isUnspecified ||
                            height.value <= 0 ||
                            height != clampedHeight

                        if (!shouldSetWidth && !shouldSetHeight)
                            return@LaunchedEffect

                        if (shouldSetWidth) width = clampedWidth
                        if (shouldSetHeight) height = clampedHeight

                        // Remember the resolved dimensions for this model so
                        // the next Image span with the same src (created by
                        // a later setHtml, e.g. every keystroke in an HTML
                        // source-editor) starts with the right Placeholder
                        // size instead of blinking through 0x0.
                        resolvedDimensionsCache[model] = width to height

                        // Overwrite the InlineTextContent at the same stable [id]
                        // so BasicText observes the new Placeholder dimensions on
                        // the next frame. The annotated string's inline-content
                        // marker does not change, so no rebuild is needed.
                        richTextState.inlineContentMap[id] = createInlineTextContent(
                            richTextState = richTextState,
                        )
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

        override val isAtomic: Boolean = true

        internal companion object {
            /**
             * Process-wide cache of resolved `(width, height)` per image
             * [model]. Populated when the painter's intrinsic size has been
             * clamped and applied; consulted in [Image.init] so that a fresh
             * Image constructed from the same src (e.g. a later `setHtml`)
             * starts at the already-known Placeholder size.
             *
             * Not thread-safe; Compose edits run on the main thread. Grows
             * unboundedly in pathological cases; acceptable for realistic
             * document sizes.
             */
            internal val resolvedDimensionsCache: MutableMap<Any, Pair<TextUnit, TextUnit>> =
                mutableMapOf()

            /**
             * Scale [width]/[height] down proportionally so [width] is at most
             * [maxWidth]. Returns the input unchanged when [maxWidth] is
             * unspecified, non-positive, or already wider than [width].
             */
            internal fun clampToMaxWidth(
                width: TextUnit,
                height: TextUnit,
                maxWidth: TextUnit,
            ): Pair<TextUnit, TextUnit> {
                if (!maxWidth.isSpecified || maxWidth.value <= 0f) return width to height
                if (!width.isSpecified || width.value <= maxWidth.value) return width to height

                val scale = maxWidth.value / width.value
                val clampedHeight = if (height.isSpecified)
                    (height.value * scale).sp
                else
                    height
                return maxWidth to clampedHeight
            }
        }

        // Image intentionally does not override equals/hashCode. It relies
        // on identity: two `<img>` tags are two distinct visual slots in
        // the document and must never be collapsed by the consecutive-span
        // merging that runs on `richSpanStyle ==` (see
        // AnnotatedStringExt.appendRichSpanList). Content-based equality
        // would also have been a footgun once [width]/[height] resolve from
        // intrinsic size, since those are mutable state.
    }

    /**
     * Atomic token produced by committing a trigger query (see [com.mohamedrejeb.richeditor.model.trigger.Trigger]).
     *
     * Tokens are single indivisible units for editing purposes:
     * backspace removes the whole [label], typing adjacent to a token creates
     * a sibling span, and selections that straddle a token snap to its edges.
     *
     * @property triggerId Id of the [com.mohamedrejeb.richeditor.model.trigger.Trigger] that produced this token.
     * Used at render time to look up the trigger's style and at serialization
     * time to round-trip the token through HTML/Markdown.
     * @property id Stable identity for the referenced entity (e.g. user id, tag slug, command name).
     * Preserved across HTML/Markdown round-trips.
     * @property label Display text of the token, including the trigger character
     * (e.g. "@mohamed", "#release", "/help"). This text becomes the raw text
     * of the span.
     */
    public class Token(
        public val triggerId: String,
        public val id: String,
        public val label: String,
    ) : RichSpanStyle {
        override val spanStyle: (RichTextConfig) -> SpanStyle = {
            SpanStyle(color = it.linkColor)
        }

        override fun DrawScope.drawCustomStyle(
            layoutResult: TextLayoutResult,
            textRange: TextRange,
            richTextConfig: RichTextConfig,
            topPadding: Float,
            startPadding: Float,
        ): Unit = Unit

        override val acceptNewTextInTheEdges: Boolean = false

        override val isAtomic: Boolean = true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Token) return false
            return triggerId == other.triggerId && id == other.id && label == other.label
        }

        override fun hashCode(): Int {
            var result = triggerId.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + label.hashCode()
            return result
        }

        override fun toString(): String =
            "Token(triggerId='$triggerId', id='$id', label='$label')"
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