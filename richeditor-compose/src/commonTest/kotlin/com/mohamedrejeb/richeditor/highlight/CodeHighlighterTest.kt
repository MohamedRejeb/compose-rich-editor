package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Annotation
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Comment
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Keyword
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Number
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.String
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class CodeHighlighterTest {

    @Test
    fun `keywords numbers and strings are found`() {
        assertEquals(
            listOf("val" to Keyword, "42" to Number, "\"hi\"" to String),
            tokenTexts("val x = 42 + \"hi\".length", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `a keyword inside an identifier is not a keyword`() {
        assertEquals(emptyList(), tokenTexts("valid = interval", CodeLanguage.Kotlin))
    }

    @Test
    fun `a line comment runs to the end of its line`() {
        assertEquals(
            listOf("// val x" to Comment, "val" to Keyword),
            tokenTexts("// val x\nval y", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `a block comment spans lines and nests`() {
        assertEquals(
            listOf("/* a /* b */ c\n*/" to Comment, "fun" to Keyword),
            tokenTexts("/* a /* b */ c\n*/ fun", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `an escaped quote does not end a string`() {
        assertEquals(
            listOf("\"a\\\"b\"" to String),
            tokenTexts("\"a\\\"b\"", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `a raw string spans lines`() {
        val code = "\"\"\"a\nval\"\"\" val"
        assertEquals(
            listOf("\"\"\"a\nval\"\"\"" to String, "val" to Keyword),
            tokenTexts(code, CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `an unterminated string stops at the end of its line`() {
        assertEquals(
            listOf("\"abc" to String, "val" to Keyword),
            tokenTexts("\"abc\nval", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `an unterminated block comment runs to the end`() {
        assertEquals(listOf("/* val x" to Comment), tokenTexts("/* val x", CodeLanguage.Kotlin))
    }

    @Test
    fun `an annotation is found and a label is not one`() {
        assertEquals(
            listOf("@Composable" to Annotation, "fun" to Keyword, "return" to Keyword),
            tokenTexts("@Composable fun a() { return@forEach }", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `hex and decimal numbers are single tokens`() {
        assertEquals(
            listOf("0xFF" to Number, "1.5f" to Number, "1_000" to Number, "1" to Number, "10" to Number),
            tokenTexts("0xFF 1.5f 1_000 1..10", CodeLanguage.Kotlin),
        )
    }

    @Test
    fun `empty code has no tokens`() {
        assertEquals(emptyList(), CodeHighlighter.tokenize("", CodeLanguage.Kotlin))
    }
}
