package com.mohamedrejeb.richeditor.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class HighlightCodeTest {

    private val colors = CodeBlockColors(
        keyword = SpanStyle(color = Color.Red),
        string = SpanStyle(color = Color.Green),
        number = SpanStyle(color = Color.Blue),
        comment = SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic),
        annotation = SpanStyle(color = Color.Yellow),
    )

    @Test
    fun `each token gets the style of its kind`() {
        val text = highlightCode("val a = 1 // c", CodeLanguage.Kotlin, colors)
        assertEquals("val a = 1 // c", text.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(colors.keyword, 0, 3),
                AnnotatedString.Range(colors.number, 8, 9),
                AnnotatedString.Range(colors.comment, 10, 14),
            ),
            text.spanStyles,
        )
    }

    @Test
    fun `no language gives the code unstyled`() {
        val text = highlightCode("val a = 1", null, colors)
        assertEquals("val a = 1", text.text)
        assertEquals(emptyList(), text.spanStyles)
    }

    @Test
    fun `styleOf returns the style of each kind`() {
        assertEquals(colors.keyword, colors.styleOf(CodeTokenKind.Keyword))
        assertEquals(colors.string, colors.styleOf(CodeTokenKind.String))
        assertEquals(colors.number, colors.styleOf(CodeTokenKind.Number))
        assertEquals(colors.comment, colors.styleOf(CodeTokenKind.Comment))
        assertEquals(colors.annotation, colors.styleOf(CodeTokenKind.Annotation))
    }
}
