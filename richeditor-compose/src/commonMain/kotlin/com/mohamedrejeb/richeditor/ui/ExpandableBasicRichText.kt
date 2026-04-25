package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageLoader
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.model.LocalRichTextMaxImageWidthProvider
import com.mohamedrejeb.richeditor.model.RichTextMaxImageWidthProvider
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Read-only rich text that collapses to [collapsedMaxLines] with an inline `… See more` toggle, and
 * expands inline with a trailing ` See less` toggle. The toggle text shares the baseline of the
 * surrounding content (LinkedIn / X / Reddit style) and is wired through a Compose [LinkAnnotation],
 * so taps fire [onExpandedChange] without any extra pointer-input plumbing.
 *
 * v1 limitations: this composable renders through [BasicText] (not [BasicRichText]), so it
 * preserves span-level styling - bold, italic, color, underline, font size, hyperlinks, inline
 * content - but does not render code-span pill backgrounds, list-bullet glyphs, paragraph
 * backgrounds, mention/token pointer interactions, or other paragraph-level decoration that the
 * rich state draws via its own modifier overlays. If your content needs those, prefer
 * [BasicRichText] with manual maxLines until v2 of this composable.
 *
 * @param state The [RichTextState] to render.
 * @param expanded Whether the content is fully shown. Required hoisted state - the caller owns the
 * boolean and must update it from [onExpandedChange].
 * @param onExpandedChange Invoked when the user taps `See more` (with `true`) or `See less` (with
 * `false`).
 * @param collapsedMaxLines Maximum number of lines shown when collapsed. Must be >= 1.
 * @param seeMoreLabel Inline label appended at the end of the last visible line when collapsed.
 * Default `"… See more"` includes the leading horizontal ellipsis and a space so it sits naturally
 * after the trimmed prefix.
 * @param seeLessLabel Inline label appended at the end of the content when expanded. Default
 * `" See less"` includes a leading space so it doesn't run into the final word.
 * @param seeMoreStyle Span style applied to both labels via Compose's [TextLinkStyles] so the
 * styling reacts to focus / hover / press states. Default underlines the label and inherits the
 * surrounding text color.
 *
 * @see BasicRichText for the non-expanding read-only equivalent.
 */
@ExperimentalRichTextApi
@Composable
public fun ExpandableBasicRichText(
    state: RichTextState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    collapsedMaxLines: Int = 3,
    seeMoreLabel: String = "… See more",
    seeLessLabel: String = " See less",
    seeMoreStyle: SpanStyle = SpanStyle(
        color = Color.Unspecified,
        textDecoration = TextDecoration.Underline,
    ),
    softWrap: Boolean = true,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    imageLoader: ImageLoader = LocalImageLoader.current,
) {
    require(collapsedMaxLines >= 1) { "collapsedMaxLines must be >= 1, was $collapsedMaxLines" }

    val measurer = rememberTextMeasurer()
    val maxImageWidthProvider = remember { RichTextMaxImageWidthProvider() }

    // Stable callback ref: a fresh lambda from the caller per recomposition shouldn't invalidate
    // the AnnotatedString built below.
    val expandedListenerState = rememberUpdatedState(onExpandedChange)
    val seeMoreListener = remember {
        LinkInteractionListener { expandedListenerState.value(true) }
    }
    val seeLessListener = remember {
        LinkInteractionListener { expandedListenerState.value(false) }
    }

    val visualString = remember(state.visualTransformation, state.annotatedString) {
        // Strip paragraph styles so concatenating the See more / See less suffix doesn't push it
        // into a separate paragraph (which would render on a new line). Span styles and link
        // annotations on the content are preserved.
        state.visualTransformation.filter(state.annotatedString).text.flattenToInlineParagraph()
    }

    val seeMoreSuffix = remember(seeMoreLabel, seeMoreStyle, seeMoreListener) {
        buildLinkSuffix(seeMoreLabel, seeMoreStyle, "see-more", seeMoreListener)
    }
    val seeLessSuffix = remember(seeLessLabel, seeMoreStyle, seeLessListener) {
        buildLinkSuffix(seeLessLabel, seeMoreStyle, "see-less", seeLessListener)
    }

    // State machine driven by onTextLayout.
    var phase by remember(
        visualString, collapsedMaxLines, style, seeMoreLabel, seeLessLabel, seeMoreStyle,
    ) {
        mutableStateOf<TruncationPhase>(TruncationPhase.NeedsMeasure)
    }

    val displayText = when (val current = phase) {
        TruncationPhase.NeedsMeasure, TruncationPhase.Fits ->
            if (expanded) visualString + seeLessSuffix else visualString
        is TruncationPhase.Truncated ->
            if (expanded) visualString + seeLessSuffix else current.text
    }

    val effectiveMaxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines

    CompositionLocalProvider(
        LocalImageLoader provides imageLoader,
        LocalRichTextMaxImageWidthProvider provides maxImageWidthProvider,
    ) {
        BasicText(
            text = displayText,
            modifier = modifier,
            style = style,
            onTextLayout = { result ->
                if (expanded) {
                    // Don't track truncation while expanded - the next collapse will re-measure.
                    return@BasicText
                }
                val width = result.size.width
                when (val current = phase) {
                    TruncationPhase.NeedsMeasure -> {
                        phase = if (!result.hasVisualOverflow) {
                            TruncationPhase.Fits
                        } else {
                            val lineEnd = result.getLineEnd(
                                lineIndex = collapsedMaxLines - 1,
                                visibleEnd = true,
                            )
                            TruncationPhase.Truncated(
                                width = width,
                                text = computeCollapsedText(
                                    full = visualString,
                                    suffix = seeMoreSuffix,
                                    lineEnd = lineEnd,
                                    width = width,
                                    style = style,
                                    maxLines = collapsedMaxLines,
                                    measurer = measurer,
                                ),
                            )
                        }
                    }
                    TruncationPhase.Fits -> {
                        // Width may have shrunk and made content overflow that previously fit.
                        if (result.hasVisualOverflow) {
                            phase = TruncationPhase.NeedsMeasure
                        }
                    }
                    is TruncationPhase.Truncated -> {
                        // Width changed: re-measure against the full text. The next layout pass
                        // either confirms truncation under the new width or marks it as fitting.
                        if (current.width != width) {
                            phase = TruncationPhase.NeedsMeasure
                        }
                    }
                }
            },
            overflow = TextOverflow.Clip,
            softWrap = softWrap,
            maxLines = effectiveMaxLines,
            inlineContent = remember(inlineContent, state.inlineContentMap.toMap()) {
                inlineContent + state.inlineContentMap
            },
        )
    }
}

private sealed interface TruncationPhase {
    data object NeedsMeasure : TruncationPhase
    data object Fits : TruncationPhase
    data class Truncated(val width: Int, val text: AnnotatedString) : TruncationPhase
}

private fun buildLinkSuffix(
    label: String,
    style: SpanStyle,
    tag: String,
    listener: LinkInteractionListener,
): AnnotatedString = buildAnnotatedString {
    withLink(
        LinkAnnotation.Clickable(
            tag = tag,
            styles = TextLinkStyles(style),
            linkInteractionListener = listener,
        )
    ) {
        append(label)
    }
}

private fun computeCollapsedText(
    full: AnnotatedString,
    suffix: AnnotatedString,
    lineEnd: Int,
    width: Int,
    style: TextStyle,
    maxLines: Int,
    measurer: TextMeasurer,
): AnnotatedString {
    if (width <= 0) return full
    val constraints = Constraints(maxWidth = width)
    val rawText = full.text

    // Word-boundary walk back: keep stepping past the previous whitespace until prefix + suffix
    // fits within maxLines. Trim trailing whitespace from the prefix so the suffix's leading "…"
    // sits flush against the last word.
    var cut = lineEnd.coerceAtMost(rawText.length)
    while (cut > 0) {
        val prevSpace = rawText.lastIndexOf(' ', cut - 1)
        if (prevSpace < 0) break
        cut = prevSpace
        val candidate = full.subSequence(0, cut).trimEndAnnotated() + suffix
        val measured = measurer.measure(
            text = candidate,
            style = style,
            constraints = constraints,
            maxLines = maxLines,
        )
        if (!measured.hasVisualOverflow) return candidate
    }

    // Char-boundary fallback for inputs with no whitespace (single very long word, etc.).
    cut = lineEnd.coerceAtMost(rawText.length)
    while (cut > 0) {
        val candidate = full.subSequence(0, cut) + suffix
        val measured = measurer.measure(
            text = candidate,
            style = style,
            constraints = constraints,
            maxLines = maxLines,
        )
        if (!measured.hasVisualOverflow) return candidate
        cut--
    }

    // Pathological: the affordance alone overflows. Rendering only the suffix is at least
    // recoverable - the user can still tap to expand.
    return suffix
}

/**
 * Returns this AnnotatedString with trailing whitespace (spaces, tabs, newlines, etc.) removed
 * so the appended See more / See less suffix sits flush against the last word. Span styles and
 * annotations on the surviving prefix are preserved by [AnnotatedString.subSequence].
 */
private fun AnnotatedString.trimEndAnnotated(): AnnotatedString {
    var end = length
    while (end > 0 && text[end - 1].isWhitespace()) end--
    return if (end == length) this else subSequence(0, end)
}

/**
 * Returns a copy of this AnnotatedString with all paragraph styles removed, preserving span
 * styles and link annotations. Without this, concatenating an inline suffix to a [RichTextState]'s
 * annotated string would push the suffix into a fresh default-styled paragraph (rendered on a new
 * line), because each [com.mohamedrejeb.richeditor.paragraph.RichParagraph] contributes its own
 * [androidx.compose.ui.text.ParagraphStyle] range - leaving the suffix range with no overlapping
 * paragraph style and therefore a paragraph boundary at the seam.
 *
 * Paragraph-level visual settings (lineHeight, textAlign, textIndent, etc.) on the original state
 * are dropped here. The surrounding [BasicText] call applies its [TextStyle.toParagraphStyle] as
 * the default for the whole text, which is the same behavior most read-only consumers want.
 */
private fun AnnotatedString.flattenToInlineParagraph(): AnnotatedString {
    if (paragraphStyles.isEmpty()) return this
    return buildAnnotatedString {
        append(text)
        spanStyles.fastForEach { range ->
            addStyle(range.item, range.start, range.end)
        }
        getLinkAnnotations(0, length).fastForEach { range ->
            when (val link = range.item) {
                is LinkAnnotation.Url -> addLink(link, range.start, range.end)
                is LinkAnnotation.Clickable -> addLink(link, range.start, range.end)
            }
        }
    }
}
