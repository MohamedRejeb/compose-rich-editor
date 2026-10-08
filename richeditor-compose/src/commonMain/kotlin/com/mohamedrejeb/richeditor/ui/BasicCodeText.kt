package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import com.mohamedrejeb.richeditor.highlight.CodeLanguage
import com.mohamedrejeb.richeditor.highlight.highlightCode

/**
 * Read-only code, coloured by [language]. It draws the text and nothing else: give it a
 * background, padding and shape through [modifier].
 *
 * @param language the language to colour by, or null to draw the code plain.
 * @param softWrap when false, a long line scrolls sideways instead of wrapping.
 */
@ExperimentalRichTextApi
@Composable
public fun BasicCodeText(
    code: String,
    language: CodeLanguage?,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontFamily = FontFamily.Monospace),
    colors: CodeBlockColors = CodeBlockColors.Default,
    softWrap: Boolean = false,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val text = remember(code, language, colors) { highlightCode(code, language, colors) }
    BasicText(
        text = text,
        modifier = if (softWrap) modifier else modifier.horizontalScroll(rememberScrollState()),
        style = style,
        softWrap = softWrap,
        onTextLayout = onTextLayout,
    )
}
