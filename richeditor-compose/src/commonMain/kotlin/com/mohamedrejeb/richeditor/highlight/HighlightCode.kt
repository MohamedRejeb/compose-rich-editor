package com.mohamedrejeb.richeditor.highlight

import androidx.compose.ui.text.AnnotatedString
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * [code] with the style of each token applied, for a `Text` of your own. A null [language]
 * returns the code unstyled. For a ready composable see `BasicCodeText`.
 */
@ExperimentalRichTextApi
public fun highlightCode(
    code: String,
    language: CodeLanguage?,
    colors: CodeBlockColors = CodeBlockColors.Default,
): AnnotatedString {
    if (language == null) return AnnotatedString(code)
    return AnnotatedString.Builder(code).apply {
        CodeHighlighter.tokenize(code, language).forEach { addStyle(colors.styleOf(it.kind), it.start, it.end) }
    }.toAnnotatedString()
}
