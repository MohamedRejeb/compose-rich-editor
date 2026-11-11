package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** The part of a word, from [start] to [end], that is a URL, and the [url] it links to. */
internal class AutoLinkMatch(val start: Int, val end: Int, val url: String)

/**
 * The URL in [word], a run of text without whitespace, or null when it holds none. A URL starts
 * with `http://`, `https://` or `www.`; a bare domain or an email address is not one, since
 * telling those from ordinary words takes a list of top level domains. Opening punctuation in
 * front of the URL and punctuation that closes the sentence after it are left out, and a closing
 * parenthesis is kept only when the URL itself opened it.
 */
internal fun findAutoLinkUrl(word: String): AutoLinkMatch? {
    val start = word.indexOfFirst { it !in OpeningPunctuation }
    if (start < 0) return null
    var end = word.length
    while (end > start && isTrailingPunctuation(word, start, end)) end -= 1

    val candidate = word.substring(start, end)
    val url = when {
        SchemePrefixes.any { candidate.startsWith(it, ignoreCase = true) } ->
            candidate.takeIf { hostOf(candidate.substringAfter("://")).firstOrNull()?.isLetterOrDigit() == true }

        candidate.startsWith(WwwPrefix, ignoreCase = true) ->
            "https://$candidate".takeIf {
                val labels = hostOf(candidate).split('.')
                labels.size >= 3 && labels.all { it.isNotEmpty() }
            }

        else -> null
    } ?: return null
    return AutoLinkMatch(start = start, end = end, url = url)
}

private fun isTrailingPunctuation(word: String, start: Int, end: Int): Boolean {
    val char = word[end - 1]
    if (char != ')') return char in TrailingPunctuation
    val closed = (start until end).count { word[it] == ')' }
    val opened = (start until end).count { word[it] == '(' }
    return closed > opened
}

private fun hostOf(address: String): String = address.takeWhile { it !in HostTerminators }

private val SchemePrefixes = listOf("http://", "https://")
private const val WwwPrefix = "www."
private const val OpeningPunctuation = "([{<\"'"
private const val TrailingPunctuation = ".,;:!?\"']}>"
private const val HostTerminators = "/?#:"

/**
 * Links the URL an edit completed, when [RichTextConfig.autoLinkEnabled] is set. [inserted] is
 * the text the edit put at [at]. A space or a line break links the URL in the word in front of
 * it; any other text is linked when the whole of it, whitespace around it aside, is a URL (a
 * paste). Runs after the edit's own history record, so the link is a separate undo step.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichTextState.autoLinkInsertedText(at: Int, inserted: String) {
    if (!config.autoLinkEnabled || RichTextFeature.Link !in config.features) return
    val text = textFieldValue.text
    if (at !in 0..text.length) return

    val isTrigger = inserted == " " || inserted == "\n"
    val word = if (isTrigger) text.substring(text.indexOfLast(before = at) { it.isWhitespace() } + 1, at) else inserted.trim()
    val wordStart = if (isTrigger) at - word.length else at + inserted.indexOf(word)
    val match = findAutoLinkUrl(word) ?: return
    if (!isTrigger) {
        val wholeTextIsUrl = match.start == 0 && match.end == word.length && word.none { it.isWhitespace() }
        if (!wholeTextIsUrl || !text.startsWith(word, wordStart)) return
    }

    val range = TextRange(wordStart + match.start, wordStart + match.end)
    val isPlainText = (range.min until range.max).all {
        getRichSpanByTextIndex(it, ignoreCustomFiltering = true)?.fullStyle is RichSpanStyle.Default
    }
    if (isPlainText) addLinkToTextRange(url = match.url, textRange = range)
}

private inline fun String.indexOfLast(before: Int, predicate: (Char) -> Boolean): Int {
    for (index in before - 1 downTo 0) if (predicate(this[index])) return index
    return -1
}
