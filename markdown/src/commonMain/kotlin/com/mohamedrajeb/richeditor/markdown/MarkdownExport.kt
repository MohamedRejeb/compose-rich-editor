package com.mohamedrajeb.richeditor.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration

//fun EditorState.exportToMarkdown(): String {
//    return blocks.joinToString("\n") {
//        it.exportMarkdown(this)
//    }
//}

/** converts annotated string to markdown **/
//fun AnnotatedString.toMarkdown(): String {
//    var markdown = ""
//
//    val sortedStyles = toSortedStyleContainers(
//        addLineBreaks = false,
//        descending = true
//    )
//
//    if (sortedStyles.isEmpty()) {
//        // Adding complete text is there are no styles
//        return text
//    }
//
//    // Adding text before first style , if it does not start at 0
//    val firstStyle = sortedStyles.first()
//    if (firstStyle.index > 0) {
//        markdown += text.substring(0, firstStyle.index)
//    }
//
//    for (i in sortedStyles.size - 1 downTo 0) {
//        val it = sortedStyles[i]
//
//        val mark = when (it) {
//            is EditorStyle.SpanStyleContainer -> {
//                if (!it.isStarting) endMarkdown(it.spanStyle) else startMarkdown(it.spanStyle)
//            }
//            is EditorStyle.LinkStyle -> {
//                if (it.isStarting) "[" else "](${it.link})"
//            }
//            is EditorStyle.LineBreak -> "\n"
//        }
//
//        markdown = mark + markdown
//
//        // Adding text in between this ending style and next starting style
////        if (i < sortedStyles.size - 2) {
////            val next = sortedStyles[i + 1]
////            if (next.index > it.index && next.index < text.length - 1) {
////                markdown += text.substring(it.index, next.index)
////            }
////        }
//    }
//
//    return markdown
//
//}

/** converts annotated string to markdown **/
//fun AnnotatedString.toMarkdown(): String {
//
//    var markdown = ""
//
//    val sortedStyles = toSortedStyleContainers(
//        addLineBreaks = false
//    )
//
//    var index = text.length
//
//    for(it in sortedStyles){
//        val mark = when (it) {
//            is EditorStyle.SpanStyleContainer -> {
//                if (!it.isStarting) endMarkdown(it.spanStyle) else startMarkdown(it.spanStyle)
//            }
//            is EditorStyle.LinkStyle -> {
//                if (it.isStarting) "[" else "](${it.link})"
//            }
//            is EditorStyle.LineBreak -> "\n"
//        }
//        markdown = mark + text.substring(it.index, index) + markdown
//        index = it.index
//    }
//
//    // Appending text before first style
//    markdown = text.substring(0, index) + markdown
//
//    return markdown
//}
//
///** appends the starting markdown for given [style] **/
//private fun startMarkdown(style: SpanStyle): String = buildString {
//    with(style) {
//        if (fontWeight != null && fontWeight!!.weight > 400) append("**")
//        if (fontStyle == FontStyle.Italic) append("*")
//        if (textDecoration == TextDecoration.LineThrough) append("~~")
//    }
//}
//
///** appends the ending markdown for given [style] **/
//private fun endMarkdown(style: SpanStyle): String = startMarkdown(style)