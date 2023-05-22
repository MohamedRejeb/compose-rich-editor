package com.mohamedrejeb.richeditor.parser.annotatedstring

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.parser.RichTextParser

internal object RichTextAnnotatedStringParser : RichTextParser<AnnotatedString> {

    override fun encode(input: AnnotatedString): RichTextValue {
        val text = input.text
        val spanStyles = input.spanStyles
        val currentStyles = mutableSetOf<RichTextStyle>()

        val parts = spanStyles.map { (style, start, end) ->
            val part = RichTextPart(
                fromIndex = start,
                toIndex = end - 1,
                styles = setOf(
                    object : RichTextStyle {
                        override fun applyStyle(spanStyle: SpanStyle) = spanStyle.merge(style)
                    }
                )
            )

            if (part.toIndex == text.lastIndex) currentStyles.addAll(part.styles)

            part
        }

        return RichTextValue(
            textFieldValue = TextFieldValue(text),
            currentStyles = currentStyles,
            parts = parts
        )
    }

    override fun decode(richTextValue: RichTextValue): AnnotatedString {
        val text = richTextValue.textFieldValue.text
        val parts = richTextValue.parts

        val spanStyles = parts.map { (fromIndex, toIndex, styles) ->
            AnnotatedString.Range(
                start = fromIndex,
                end = toIndex + 1,
                item = styles.fold(SpanStyle()) { acc, style ->
                    style.applyStyle(acc)
                }
            )
        }

        return AnnotatedString(
            text = text,
            spanStyles = spanStyles
        )
    }

}