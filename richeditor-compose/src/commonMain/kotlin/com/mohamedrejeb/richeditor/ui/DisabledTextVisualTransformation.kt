package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Default alpha applied to text when the editor is rendered in the disabled
 * state. Matches the `ContentAlpha.disabled` convention used by Material and
 * Material3 disabled text colors.
 */
internal const val DisabledStateAlpha: Float = 0.38f

/**
 * Wraps another [VisualTransformation] and multiplies the alpha of every
 * explicitly-colored span by [disabledAlpha].
 *
 * The Material wrappers around the rich text editor compute a disabled text
 * color from the field colors and merge it into the default text style. That
 * default only applies to spans that did not set their own color, which means
 * spans with an explicit `SpanStyle(color = ...)` keep full opacity in the
 * disabled state. This transformation closes that gap by also dimming the
 * explicitly-colored spans, so the whole text fades together.
 *
 * Spans without a specified color, paragraph styles, the underlying text, and
 * the delegate's [androidx.compose.ui.text.input.OffsetMapping] are passed
 * through unchanged.
 */
internal class DisabledTextVisualTransformation(
    private val delegate: VisualTransformation,
    private val disabledAlpha: Float,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = delegate.filter(text)
        val source = transformed.text

        val dimmedSpans = source.spanStyles.map { range ->
            val originalColor = range.item.color
            if (originalColor.isSpecified) {
                AnnotatedString.Range(
                    item = range.item.copy(
                        color = originalColor.dimmedBy(disabledAlpha),
                    ),
                    start = range.start,
                    end = range.end,
                )
            } else {
                range
            }
        }

        return TransformedText(
            text = AnnotatedString(
                text = source.text,
                spanStyles = dimmedSpans,
                paragraphStyles = source.paragraphStyles,
            ),
            offsetMapping = transformed.offsetMapping,
        )
    }
}

private fun Color.dimmedBy(alphaFactor: Float): Color =
    copy(alpha = (alpha * alphaFactor).coerceIn(0f, 1f))
