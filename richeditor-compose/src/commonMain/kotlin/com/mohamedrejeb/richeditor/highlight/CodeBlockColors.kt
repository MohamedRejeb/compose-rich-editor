package com.mohamedrejeb.richeditor.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** The style of each kind of token. A style, not just a colour, so a comment can be italic. */
@ExperimentalRichTextApi
@Immutable
public class CodeBlockColors(
    public val keyword: SpanStyle,
    public val string: SpanStyle,
    public val number: SpanStyle,
    public val comment: SpanStyle,
    public val annotation: SpanStyle,
) {
    public fun styleOf(kind: CodeTokenKind): SpanStyle = when (kind) {
        CodeTokenKind.Keyword -> keyword
        CodeTokenKind.String -> string
        CodeTokenKind.Number -> number
        CodeTokenKind.Comment -> comment
        CodeTokenKind.Annotation -> annotation
    }

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is CodeBlockColors && keyword == other.keyword && string == other.string &&
                number == other.number && comment == other.comment && annotation == other.annotation
            )

    override fun hashCode(): Int =
        listOf(keyword, string, number, comment, annotation).fold(0) { hash, style -> 31 * hash + style.hashCode() }

    public companion object {
        /** Mid-tone colours that stay readable on both light and dark backgrounds. */
        public val Default: CodeBlockColors = CodeBlockColors(
            keyword = SpanStyle(color = Color(0xFFC2619D)),
            string = SpanStyle(color = Color(0xFF4E9A51)),
            number = SpanStyle(color = Color(0xFFC77B2B)),
            comment = SpanStyle(color = Color(0xFF8A8F98), fontStyle = FontStyle.Italic),
            annotation = SpanStyle(color = Color(0xFFA9882F)),
        )
    }
}
