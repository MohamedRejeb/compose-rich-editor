package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichParagraph
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState

private fun RichSpanStyle.encapsulateText(text: String): String {
    return when (this) {
        is RichSpanStyle.Code -> {
            "`$text`"
        }

        is RichSpanStyle.Link -> {
            "[$text](${this.url})"
        }

        is RichSpanStyle.Default -> text

        else -> {
            throw IllegalStateException("unknown rich span style")
        }
    }
}

internal fun RichSpan.toMarkdownText(): String {
    if (this.isEmpty()) return ""
    return if (this.children.isEmpty()) {
        this.style.encapsulateText(
            text = startMarkdown(this.spanStyle) + this.text + endMarkdown(this.spanStyle)
        )
    } else {
        var markdownText = ""
        markdownText += startMarkdown(this.spanStyle)
        markdownText += this.text
        for (child in this.children) {
            markdownText += child.toMarkdownText()
        }
        markdownText += endMarkdown(this.spanStyle)
        this.style.encapsulateText(markdownText)
    }
}

internal fun RichParagraph.toMarkdownAsUnorderedList(): String {
    var markdownText = ""
    for (child in this.children) {
        markdownText += "- ${child.toMarkdownText()}\n"
    }
    return markdownText
}

internal fun RichParagraph.toMarkdownAsOrderedList(type: RichParagraph.Type.OrderedList): String {
    var current = type.number
    var markdownText = ""
    for (child in this.children) {
        markdownText += "${current}. ${child.toMarkdownText()}\n"
        current++
    }
    return markdownText
}

internal fun RichParagraph.toMarkdownAsNormal(): String {
    var normalText = ""
    for (child in this.children) {
        normalText += child.toMarkdownText()
    }
    return normalText
}

fun RichTextState.toMarkdown(): String {
    var markdownText = ""
    for (paragraph in this.richParagraphList) {
        when (paragraph.type) {
            is RichParagraph.Type.UnorderedList -> {
                markdownText += paragraph.toMarkdownAsUnorderedList()
            }

            is RichParagraph.Type.OrderedList -> {
                markdownText += paragraph.toMarkdownAsOrderedList(paragraph.type as RichParagraph.Type.OrderedList)
            }

            is RichParagraph.Type.Default -> {
                markdownText += paragraph.toMarkdownAsNormal() + "\n"
            }
        }
    }
    return markdownText
}

/** appends the starting markdown for given [style] **/
internal fun startMarkdown(style: SpanStyle): String = buildString {
    with(style) {
        if (fontWeight != null && fontWeight!!.weight > 400) append("**")
        if (fontStyle == FontStyle.Italic) append("*")
        when(textDecoration){
            // there's no equivalent of underline in markdown apparently
//            TextDecoration.Underline -> append("__")
            TextDecoration.LineThrough -> append("~~")
            TextDecoration.LineThrough + TextDecoration.Underline -> append("~~")
        }
    }
}

/** appends the ending markdown for given [style] **/
internal fun endMarkdown(style: SpanStyle): String = startMarkdown(style)