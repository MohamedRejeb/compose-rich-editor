package com.mohamedrejeb.richeditor.parser.utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.model.RichSpan
import org.intellij.markdown.MarkdownElementTypes

internal val htmlHeadingTags = setOf( "h1", "h2", "h3", "h4", "h5", "h6")

internal val markdownHeadingNodes = setOf(
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6
)

public enum class HeadingParagraphStyle(
    public val markdownElement: String
) {
    Normal(""),
    H1("# "),
    H2("## "),
    H3("### "),
    H4("#### "),
    H5("##### "),
    H6("###### ");

    private val typography = Typography()

    public fun getSpanStyle(): SpanStyle {
        return this.getTextStyle().toSpanStyle()
    }

    public fun getParagraphStyle() : ParagraphStyle {
        return this.getTextStyle().toParagraphStyle()
    }

    //See H1-H6 Conversion https://developer.android.com/develop/ui/compose/designsystems/material2-material3#typography
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
        public fun fromSpanStyle(spanStyle: SpanStyle): HeadingParagraphStyle {
            return entries.find {
                val entrySpanStyle = it.getSpanStyle()
                entrySpanStyle.fontSize == spanStyle.fontSize
                        && entrySpanStyle.fontWeight == spanStyle.fontWeight
                        && entrySpanStyle.fontFamily == spanStyle.fontFamily
                        && entrySpanStyle.letterSpacing == spanStyle.letterSpacing
            } ?: Normal
        }

        internal fun fromRichSpan(richSpanStyle: RichSpan): HeadingParagraphStyle {
            return entries.find {
                val entrySpanStyle = it.getSpanStyle()
                entrySpanStyle.fontSize == richSpanStyle.spanStyle.fontSize
                        && entrySpanStyle.fontWeight == richSpanStyle.spanStyle.fontWeight
                        && entrySpanStyle.fontFamily == richSpanStyle.spanStyle.fontFamily
                        && entrySpanStyle.letterSpacing == richSpanStyle.spanStyle.letterSpacing
            } ?: Normal
        }
    }
}