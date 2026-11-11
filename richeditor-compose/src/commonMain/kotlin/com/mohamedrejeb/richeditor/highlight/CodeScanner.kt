package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Reads code left to right once. Every branch moves forward by at least one character and the
 * rule lists are short and fixed, so the work is linear in the length of the code.
 */
@OptIn(ExperimentalRichTextApi::class)
internal object CodeScanner {

    fun scan(code: String, spec: LanguageSpec): List<CodeToken> {
        val tokens = ArrayList<CodeToken>()
        var index = 0
        while (index < code.length) {
            val char = code[index]
            val end = when {
                char.isWhitespace() -> index + 1
                else -> tokenEnd(code, index, spec, tokens)
            }
            index = end
        }
        return tokens
    }

    /** Adds the token starting at [start], if there is one, and returns where to read next. */
    private fun tokenEnd(code: String, start: Int, spec: LanguageSpec, tokens: MutableList<CodeToken>): Int {
        val char = code[start]

        val blockComment = spec.blockComments.firstOrNull { code.startsWith(it.open, start) }
        if (blockComment != null) {
            return blockCommentEnd(code, start, blockComment).also { tokens += CodeToken(start, it, CodeTokenKind.Comment) }
        }
        if (startsLineComment(code, start, spec)) {
            return lineEnd(code, start).also { tokens += CodeToken(start, it, CodeTokenKind.Comment) }
        }
        val string = spec.strings.firstOrNull { code.startsWith(it.open, start) }
        if (string != null) {
            return stringEnd(code, start, string).also { tokens += CodeToken(start, it, CodeTokenKind.String) }
        }
        if (
            char == spec.annotationPrefix &&
            isIdentifierStart(code.getOrNull(start + 1)) &&
            !isIdentifierPart(code.getOrNull(start - 1))
        ) {
            return identifierEnd(code, start + 1).also { tokens += CodeToken(start, it, CodeTokenKind.Annotation) }
        }
        if (char.isDigit()) {
            return numberEnd(code, start).also { tokens += CodeToken(start, it, CodeTokenKind.Number) }
        }
        if (isIdentifierStart(char)) {
            val end = identifierEnd(code, start)
            val word = code.substring(start, end)
            if ((if (spec.ignoreKeywordCase) word.lowercase() else word) in spec.keywords) {
                tokens += CodeToken(start, end, CodeTokenKind.Keyword)
            }
            return end
        }
        return start + 1
    }

    private fun startsLineComment(code: String, start: Int, spec: LanguageSpec): Boolean =
        spec.lineComments.any { code.startsWith(it, start) } &&
            (!spec.commentNeedsLeadingSpace || start == 0 || code[start - 1].isWhitespace())

    private fun lineEnd(code: String, start: Int): Int =
        code.indexOf('\n', start).let { if (it == -1) code.length else it }

    private fun blockCommentEnd(code: String, start: Int, rule: BlockComment): Int {
        var depth = 1
        var index = start + rule.open.length
        while (index < code.length) {
            when {
                code.startsWith(rule.close, index) -> {
                    index += rule.close.length
                    depth--
                    if (depth == 0) return index
                }
                rule.nests && code.startsWith(rule.open, index) -> {
                    index += rule.open.length
                    depth++
                }
                else -> index++
            }
        }
        return code.length
    }

    private fun stringEnd(code: String, start: Int, rule: StringRule): Int {
        var index = start + rule.open.length
        while (index < code.length) {
            val char = code[index]
            when {
                char == rule.escape -> index += 2
                code.startsWith(rule.close, index) -> return index + rule.close.length
                char == '\n' && !rule.multiline -> return index
                else -> index++
            }
        }
        return code.length
    }

    private fun numberEnd(code: String, start: Int): Int {
        var index = start
        while (index < code.length) {
            val char = code[index]
            val isFraction = char == '.' && code.getOrNull(index + 1)?.isDigit() == true
            if (!char.isLetterOrDigit() && char != '_' && !isFraction) break
            index++
        }
        return index
    }

    private fun identifierEnd(code: String, start: Int): Int {
        var index = start
        while (index < code.length && isIdentifierPart(code[index])) index++
        return index
    }

    private fun isIdentifierStart(char: Char?): Boolean = char != null && (char.isLetter() || char == '_')

    private fun isIdentifierPart(char: Char?): Boolean = char != null && (char.isLetterOrDigit() || char == '_')
}
