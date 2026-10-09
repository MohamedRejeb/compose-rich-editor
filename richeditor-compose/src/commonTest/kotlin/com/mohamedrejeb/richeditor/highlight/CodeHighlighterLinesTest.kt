package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Comment
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Keyword
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class CodeHighlighterLinesTest {

    @Test
    fun `offsets are relative to each line`() {
        val lines = CodeHighlighter.tokenizeLines(listOf("val a", "  val b"), CodeLanguage.Kotlin)
        assertEquals(
            listOf(listOf(CodeToken(0, 3, Keyword)), listOf(CodeToken(2, 5, Keyword))),
            lines,
        )
    }

    @Test
    fun `a comment opened on one line colours the next lines`() {
        val lines = CodeHighlighter.tokenizeLines(listOf("a /* b", "val c", "d */ val"), CodeLanguage.Kotlin)
        assertEquals(
            listOf(
                listOf(CodeToken(2, 6, Comment)),
                listOf(CodeToken(0, 5, Comment)),
                listOf(CodeToken(0, 4, Comment), CodeToken(5, 8, Keyword)),
            ),
            lines,
        )
    }

    @Test
    fun `an empty line inside a comment has no token`() {
        val lines = CodeHighlighter.tokenizeLines(listOf("/*", "", "*/"), CodeLanguage.Kotlin)
        assertEquals(listOf(listOf(CodeToken(0, 2, Comment)), emptyList(), listOf(CodeToken(0, 2, Comment))), lines)
    }

    @Test
    fun `a carriage return at a line end is not part of a token`() {
        val lines = CodeHighlighter.tokenizeLines(listOf("// a\r", "val\r"), CodeLanguage.Kotlin)
        assertEquals(listOf(listOf(CodeToken(0, 4, Comment)), listOf(CodeToken(0, 3, Keyword))), lines)
    }

    @Test
    fun `no lines give no lines`() {
        assertEquals(emptyList(), CodeHighlighter.tokenizeLines(emptyList(), CodeLanguage.Kotlin))
    }
}
