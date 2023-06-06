package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.parser.annotatedstring.RichTextAnnotatedStringParser
import com.mohamedrejeb.richeditor.parser.html.RichTextHtmlParser
import com.mohamedrejeb.richeditor.utils.RichTextValueBuilder

/**
 * A value that represents the text of a RichTextEditor
 * @param textFieldValue the [TextFieldValue] of the text field
 * @param currentStyles the current styles applied to the text
 * @param parts the parts of the text that have different styles
 * @see RichTextStyle
 * @see RichTextPart
 */
@Immutable
data class RichTextValue internal constructor(
    internal val textFieldValue: TextFieldValue,
    val currentStyles: Set<RichTextStyle> = emptySet(),
    internal val parts: List<RichTextPart> = emptyList(),
) {

    /**
     * The [VisualTransformation] to apply to the text field
     */
    internal val visualTransformation
        get() = VisualTransformation {
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }

    /**
     * The [AnnotatedString] representation of the text
     */
    internal val annotatedString: AnnotatedString
        get() = buildAnnotatedString {
            var lastToIndex = 0
            parts.forEach { part ->
                if (part.fromIndex > lastToIndex) {
                    // Append the text between the last part's end and the current part's start
                    append(textFieldValue.text.substring(lastToIndex, part.fromIndex))
                }

                val spanStyle = part.styles.fold(SpanStyle()) { acc, style ->
                    style.applyStyle(acc)
                }
                // Check if this part has a ListItem or OrderedListItem style
                val listItem =
                    part.styles.firstOrNull { it is RichTextStyle.UnorderedListItem || it is RichTextStyle.OrderedListItem }

                withStyle(spanStyle) {
                    // If this part is a list item, append a bullet point at the start
                    // If it's an ordered list item, append the position instead
                    when (listItem) {
                        is RichTextStyle.UnorderedListItem -> append("• ")
                        is RichTextStyle.OrderedListItem -> append("${listItem.position}. ")
                    }
                    append(textFieldValue.text.substring(part.fromIndex, part.toIndex + 1))
                }
                part.styles.filterIsInstance<RichTextStyle.Hyperlink>().forEach { hyperlink ->
                    addStringAnnotation(
                        tag = "URL",
                        annotation = hyperlink.url,
                        start = part.fromIndex,
                        end = part.toIndex + 1
                    )
                }

                lastToIndex = part.toIndex + 1
            }

            if (lastToIndex < textFieldValue.text.length) {
                // Append the remaining text after the last part
                append(textFieldValue.text.substring(lastToIndex))
            }
        }


    constructor(
        text: String = "",
        currentStyles: Set<RichTextStyle> = emptySet(),
    ) : this(
        textFieldValue = TextFieldValue(text = text),
        currentStyles = currentStyles,
        parts = emptyList(),
    )

    /**
     * Toggle a style
     * @param style the style to toggle
     * @return a new [RichTextValue] with the style toggled
     * @see RichTextStyle
     */
    fun toggleStyle(style: RichTextStyle): RichTextValue {
        return if (currentStyles.contains(style)) {
            removeStyle(style)
        } else {
            addStyle(style)
        }
    }

    /**
     * Add a style to the current styles
     * @param style the style to add
     * @return a new [RichTextValue] with the new style added
     * @see RichTextStyle
     */
    fun addStyle(vararg style: RichTextStyle): RichTextValue {
        return RichTextValueBuilder
            .from(this)
            .addStyle(*style)
            .build()
    }

    /**
     * Remove a style from the current styles
     * @param style the style to remove
     * @return a new [RichTextValue] with the style removed
     * @see RichTextStyle
     */
    fun removeStyle(vararg style: RichTextStyle): RichTextValue {
        return RichTextValueBuilder
            .from(this)
            .removeStyle(*style)
            .build()
    }

    /**
     * Update the current styles
     * @param newStyles the new styles
     * @return a new [RichTextValue] with the new styles
     * @see RichTextStyle
     */
    fun updateStyles(newStyles: Set<RichTextStyle>): RichTextValue {
        return RichTextValueBuilder
            .from(this)
            .updateStyles(newStyles)
            .build()
    }

    /**
     * Update the text field value and update the rich text parts accordingly to the new text field value
     * @param newTextFieldValue the new text field value
     * @return a new [RichTextValue] with the new text field value
     */
    internal fun updateTextFieldValue(newTextFieldValue: TextFieldValue): RichTextValue {
        return RichTextValueBuilder
            .from(this)
            .updateTextFieldValue(newTextFieldValue)
            .build()
    }

    /**
     * Create an HTML string from the [RichTextValue]
     *
     * @return an HTML string from the [RichTextValue]
     */
    fun toHtml(): String {
        return RichTextHtmlParser.decode(this)
    }

    /**
     * Create an [AnnotatedString] from the [RichTextValue]
     *
     * @return an [AnnotatedString] from the [RichTextValue]
     */
    fun toAnnotatedString(): AnnotatedString {
        return RichTextAnnotatedStringParser.decode(this)
    }

    companion object {
        /**
         * Create a [RichTextValue] from an HTML string
         *
         * @param html the HTML string
         * @return a [RichTextValue] from the HTML string
         */
        fun from(html: String): RichTextValue {
            return RichTextHtmlParser.encode(html)
        }

        /**
         * Create a [RichTextValue] from an [AnnotatedString]
         *
         * @param annotatedString the [AnnotatedString]
         * @return a [RichTextValue] from the [AnnotatedString]
         */
        @ExperimentalRichTextApi
        fun from(annotatedString: AnnotatedString): RichTextValue {
            return RichTextAnnotatedStringParser.encode(annotatedString)
        }

        /**
         * The default [Saver] implementation for [RichTextValue].
         */
        val Saver = Saver<RichTextValue, Any>(
            save = {
                arrayListOf(
                    RichTextHtmlParser.decode(it),
                )
            },
            restore = {
                val list = it as? List<*> ?: return@Saver null
                val htmlString = list[0] as? String ?: return@Saver null

                RichTextHtmlParser.encode(htmlString)
            }
        )
    }
}