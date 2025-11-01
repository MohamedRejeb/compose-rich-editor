package com.mohamedrejeb.richeditor.model

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import org.intellij.markdown.MarkdownElementTypes

/**
 * Represents the different heading levels (H1 to H6) and a normal paragraph style
 * that can be applied to a paragraph in the Rich Editor.
 *
 * Each heading level is associated with a specific Markdown element (e.g., "# ", "## ")
 * and HTML tag (e.g., "h1", "h2").
 *
 * These styles are typically applied to an entire paragraph, influencing its appearance
 * and semantic meaning in both the editor and when converted to formats like Markdown or HTML.
 */
public enum class HeadingStyle(
    public val markdownElement: String,
    public val htmlTag: String? = null,
) {
    /**
     * Represents a standard, non-heading paragraph.
     */
    Normal(""),

    /**
     * Represents a Heading Level 1.
     */
    H1("# ", "h1"),

    /**
     * Represents a Heading Level 2.
     */
    H2("## ", "h2"),

    /**
     * Represents a Heading Level 3.
     */
    H3("### ", "h3"),

    /**
     * Represents a Heading Level 4.
     */
    H4("#### ", "h4"),

    /**
     * Represents a Heading Level 5.
     */
    H5("##### ", "h5"),

    /**
     * Represents a Heading Level 6.
     */
    H6("###### ", "h6");

    // Using Material 3 Typography for default heading styles
    // Instantiation here allows use to use Typography without a composable
    private val typography = Typography()

    /**
     * Retrieves the base [SpanStyle] associated with this heading level.
     *
     * This function converts the [TextStyle] obtained from [getTextStyle] to a [SpanStyle].
     *
     * Setting [FontWeight] to `null` here prevents the base heading's font weight
     * ([FontWeight.Normal] in typography for each heading) from interfering with user-applied font weights
     * like [FontWeight.Bold] when identifying or diffing styles.
     *
     * @return The base [SpanStyle] for this heading level, with [FontWeight] set to `null`.
     */
    public fun getSpanStyle(): SpanStyle {
        return this.getTextStyle().toSpanStyle().copy(fontWeight = null)
    }

    /**
     * Retrieves the base [ParagraphStyle] associated with this heading level.
     *
     * This function converts the [TextStyle] obtained from [getTextStyle] to a [ParagraphStyle].
     * This style includes paragraph-level properties like line height, text alignment, etc.,
     * as defined by the Material 3 Typography for the corresponding text style.
     *
     * @return The base [ParagraphStyle] for this heading level.
     */
    public fun getParagraphStyle() : ParagraphStyle {
        return this.getTextStyle().toParagraphStyle()
    }

    /**
     * Retrieves the base [TextStyle] associated with this heading level from the
     * Material 3 Typography.
     *
     * This maps each heading level (H1-H6) to a specific Material 3 display or
     * headline text style. [Normal] maps to [TextStyle.Default].
     *
     * @return The base [TextStyle] for this heading level.
     * @see <a href="https://developer.android.com/develop/ui/compose/designsystems/material2-material3#typography">Material 3 Typography Mapping</a>
     */
    public fun getTextStyle() : TextStyle {
        return when (this) {
            Normal -> TextStyle.Default
            H1 -> typography.displayLarge
            H2 -> typography.displayMedium
            H3 -> typography.displaySmall
            H4 -> typography.headlineMedium
            H5 -> typography.headlineSmall
            H6 -> typography.titleLarge
        }
    }

    public companion object {
        /**
         * Identifies the [HeadingStyle] based on a given [SpanStyle].
         *
         * This function compares the provided [spanStyle] with the base [SpanStyle]
         * of each heading level defined in [HeadingStyle.getTextStyle].
         * It primarily matches based on properties like font size, font family,
         * and letter spacing, as these are strong indicators of a heading style
         * derived from typography.
         *
         * Special handling for [FontWeight.Normal]: If a heading's base style has
         * [FontWeight.Normal] (which is common in typography but explicitly set to
         * `null` by [getSpanStyle]), this property is effectively ignored during
         * comparison. This allows user-applied non-normal font weights (like Bold)
         * to coexist with the identified heading style without preventing a match.
         *
         * @param spanStyle The [SpanStyle] to compare against heading styles.
         * @return The matching [HeadingStyle], or [HeadingStyle.Normal] if no match is found.
         */
        public fun fromSpanStyle(spanStyle: SpanStyle): HeadingStyle {
            return entries.find {
                val entrySpanStyle = it.getSpanStyle()
                entrySpanStyle.fontSize == spanStyle.fontSize
                        // Ignore fontWeight comparison because getSpanStyle makes it null
                        && entrySpanStyle.fontFamily == spanStyle.fontFamily
                        && entrySpanStyle.letterSpacing == spanStyle.letterSpacing
            } ?: Normal
        }

        /**
         * Identifies the [HeadingStyle] based on the [SpanStyle] of a given [RichSpan].
         *
         * This function is a convenience wrapper around [fromSpanStyle], extracting the
         * [SpanStyle] from the provided [richSpan] and passing it to [fromSpanStyle]
         * for comparison against heading styles.
         *
         * Special handling for [FontWeight.Normal] is inherited from [fromSpanStyle].
         *
         * @param richSpan The [RichSpan] whose style is compared against heading styles.
         * @return The matching [HeadingStyle], or [HeadingStyle.Normal] if no match is found.
         */
        internal fun fromRichSpan(richSpanStyle: RichSpan): HeadingStyle {
            return fromSpanStyle(richSpanStyle.spanStyle)
        }

        /**
         * Identifies the [HeadingStyle] based on a given [ParagraphStyle].
         *
         * This function compares the provided [paragraphStyle] with the base [ParagraphStyle]
         * of each heading level defined in [HeadingStyle.getTextStyle].
         * It primarily matches based on properties like line height, text alignment,
         * text direction, line break, and hyphens, as these are strong indicators
         * of a paragraph style derived from typography.
         *
         * @param paragraphStyle The [ParagraphStyle] to compare against heading styles.
         * @return The matching [HeadingStyle], or [HeadingStyle.Normal] if no match is found.
         */
        public fun fromParagraphStyle(paragraphStyle: ParagraphStyle): HeadingStyle {
            return entries.find {
                val entryParagraphStyle = it.getParagraphStyle()
                entryParagraphStyle.lineHeight == paragraphStyle.lineHeight
                        && entryParagraphStyle.textAlign == paragraphStyle.textAlign
                        && entryParagraphStyle.textDirection == paragraphStyle.textDirection
                        && entryParagraphStyle.lineBreak == paragraphStyle.lineBreak
                        && entryParagraphStyle.hyphens == paragraphStyle.hyphens
            } ?: Normal
        }

        /**
         * HTML heading tags.
         *
         * @see <a href="https://www.w3schools.com/html/html_headings.asp">HTML headings</a>
         */
        internal val headingTags = setOf("h1", "h2", "h3", "h4", "h5", "h6")

        /**
         * Markdown heading nodes.
         *
         * @see <a href="https://www.w3schools.com/html/html_headings.asp">Markdown headings</a>
         */
        internal val markdownHeadingNodes = setOf(
            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6,
        )

    }
}
