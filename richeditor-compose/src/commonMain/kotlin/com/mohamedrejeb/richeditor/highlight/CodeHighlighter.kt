package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** Finds the parts of a piece of code that are drawn in a colour of their own. */
@ExperimentalRichTextApi
public object CodeHighlighter {

    /**
     * The tokens of [code], in order, inside the text and never overlapping. It never throws:
     * a string or comment that is not closed runs to the end of its line or of the code.
     */
    public fun tokenize(code: String, language: CodeLanguage): List<CodeToken> =
        CodeScanner.scan(code, language.spec)

    /**
     * The tokens of each line, with offsets relative to that line. The lines are read as one
     * piece of code, so a comment or string opened on one line is still coloured on the next.
     * This is what a view that draws its own rows, such as a diff, needs. Lines that do not
     * follow each other in the file (two hunks) are two calls.
     */
    public fun tokenizeLines(lines: List<String>, language: CodeLanguage): List<List<CodeToken>> {
        if (lines.isEmpty()) return emptyList()
        val clean = lines.map { it.removeSuffix("\r") }
        val tokens = tokenize(clean.joinToString("\n"), language)
        var lineStart = 0
        var next = 0
        return clean.map { line ->
            val lineEnd = lineStart + line.length
            val lineTokens = ArrayList<CodeToken>()
            while (next < tokens.size && tokens[next].start < lineEnd) {
                val token = tokens[next]
                val start = maxOf(token.start, lineStart)
                val end = minOf(token.end, lineEnd)
                if (start < end) lineTokens += CodeToken(start - lineStart, end - lineStart, token.kind)
                if (token.end > lineEnd + 1) break
                next++
            }
            lineStart = lineEnd + 1
            lineTokens
        }
    }
}
