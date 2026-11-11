package com.mohamedrejeb.richeditor.parser.markdown

private const val MinFenceLength = 3
private const val MaxFenceIndent = 3

/** The fence a line opens: its marker character and length, or null when the line is not a fence. */
private fun fenceOf(line: String): Pair<Char, Int>? {
    val indent = line.length - line.trimStart(' ').length
    if (indent > MaxFenceIndent) return null
    val marker = line.getOrNull(indent) ?: return null
    if (marker != '`' && marker != '~') return null
    val length = line.drop(indent).takeWhile { it == marker }.length
    if (length < MinFenceLength) return null
    // A backtick fence cannot have a backtick in its info string: that is inline code.
    if (marker == '`' && '`' in line.drop(indent + length)) return null
    return marker to length
}

/**
 * [correctMarkdownText] applied to everything outside fenced code blocks. Code is left exactly
 * as written, because the correction moves spaces and indentation.
 */
internal fun correctMarkdownOutsideFences(text: String): String {
    val segments = mutableListOf<String>()
    val pending = mutableListOf<String>()
    var open: Pair<Char, Int>? = null

    fun flush(isCode: Boolean) {
        if (pending.isEmpty()) return
        val joined = pending.joinToString("\n")
        segments += if (isCode) joined else correctMarkdownText(joined)
        pending.clear()
    }

    text.split('\n').forEach { line ->
        val fence = fenceOf(line)
        val opened = open
        when {
            opened == null && fence != null -> {
                flush(isCode = false)
                pending += line
                open = fence
            }
            opened != null && fence != null && fence.first == opened.first && fence.second >= opened.second &&
                line.trim().all { it == opened.first } -> {
                pending += line
                flush(isCode = true)
                open = null
            }
            else -> pending += line
        }
    }
    flush(isCode = open != null)
    return segments.joinToString("\n")
}

/** A backtick fence long enough to hold [code]: longer than any run of backticks inside it. */
internal fun fenceFor(code: String): String {
    var longest = 0
    var run = 0
    code.forEach { char ->
        run = if (char == '`') run + 1 else 0
        if (run > longest) longest = run
    }
    return "`".repeat(maxOf(MinFenceLength, longest + 1))
}
