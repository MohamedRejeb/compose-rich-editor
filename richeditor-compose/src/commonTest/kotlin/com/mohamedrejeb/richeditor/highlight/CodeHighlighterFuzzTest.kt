package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Oracles: tokenizing never throws, tokens are in bounds, non-empty, sorted and never overlap,
 * and the per-line tokens are the whole-text tokens cut at line ends. Covers every language.
 */
@OptIn(ExperimentalRichTextApi::class)
class CodeHighlighterFuzzTest {

    private val alphabet = "ab_1 \n\t\"'`/*#@\\.-{}<>\$val fun if 0x".toList()

    private fun randomCode(random: Random): String =
        buildString { repeat(random.nextInt(0, 200)) { append(alphabet[random.nextInt(alphabet.size)]) } }

    private fun assertWellFormed(code: String, tokens: List<CodeToken>) {
        var previousEnd = 0
        tokens.forEach { token ->
            assertTrue(token.start >= previousEnd, "overlap or disorder at $token in <$code>")
            assertTrue(token.start < token.end, "empty token $token in <$code>")
            assertTrue(token.end <= code.length, "out of bounds $token in <$code>")
            previousEnd = token.end
        }
    }

    @Test
    fun `random code gives well formed tokens in every language`() {
        val random = Random(20261008)
        repeat(2_000) {
            val code = randomCode(random)
            CodeLanguage.all.forEach { language ->
                assertWellFormed(code, CodeHighlighter.tokenize(code, language))
            }
        }
    }

    @Test
    fun `line tokens are the whole text tokens cut at line ends`() {
        val random = Random(8102026)
        repeat(1_000) {
            val code = randomCode(random)
            val lines = code.split('\n')
            CodeLanguage.all.forEach { language ->
                val whole = CodeHighlighter.tokenize(code, language)
                var lineStart = 0
                val rebuilt = CodeHighlighter.tokenizeLines(lines, language).flatMapIndexed { index, lineTokens ->
                    val start = lineStart
                    lineStart += lines[index].length + 1
                    lineTokens.map { Triple(it.start + start, it.end + start, it.kind) }
                }
                val expected = whole.flatMap { token ->
                    var start = 0
                    lines.mapNotNull { line ->
                        val from = maxOf(token.start, start)
                        val to = minOf(token.end, start + line.length)
                        start += line.length + 1
                        if (from < to) Triple(from, to, token.kind) else null
                    }
                }
                assertEquals(expected, rebuilt, "language ${language.name} code <$code>")
            }
        }
    }

    @Test
    fun `huge unterminated and nested input finishes`() {
        val nested = "/*".repeat(100_000)
        val unterminated = "\"" + "a\\".repeat(100_000)
        CodeLanguage.all.forEach { language ->
            assertWellFormed(nested, CodeHighlighter.tokenize(nested, language))
            assertWellFormed(unterminated, CodeHighlighter.tokenize(unterminated, language))
        }
    }
}
