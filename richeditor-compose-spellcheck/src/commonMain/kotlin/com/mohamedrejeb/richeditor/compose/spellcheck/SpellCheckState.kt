package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.Verbosity
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.applyCapitalizationStrategy
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.isSpelledCorrectly
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.spellingIsCorrect
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.utils.WordSegment

public class SpellCheckState(
    public val richTextState: RichTextState,
    public var spellChecker: SpellChecker?
) {
    private var lastTextHash = -1
    private val misspelledWords = mutableListOf<WordSegment>()

    public fun handleSpanClick(span: RichSpanStyle, range: TextRange, click: Offset): WordSegment? {
        return if (span is SpellCheck) {
            findWordSegmentContainingRange(
                misspelledWords,
                range
            )
        } else {
            null
        }
    }

    public fun correctSpelling(segment: WordSegment, correction: String) {
        val currentStyle = richTextState.getSpanStyle(segment.range)
        richTextState.replaceTextRange(segment.range, correction)

        val correctionRange =
            TextRange(start = segment.range.start, end = segment.range.start + correction.length)
        richTextState.addSpanStyle(currentStyle, correctionRange)
    }

    /**
     * This is a very naive algorithm that just removes all spell check spans and
     * reruns the entire spell check again.
     */
    public fun runSpellCheck() {
        val sp = spellChecker ?: return

        richTextState.apply {
            // Remove all existing spell checks
            getAllRichSpans()
                .filter { it.first is SpellCheck }
                .forEach { span ->
                    removeRichSpan(SpellCheck, span.second)
                }

            misspelledWords.clear()
            getWords().mapNotNullTo(misspelledWords) { segment ->
                val suggestions = sp.lookup(segment.text)
                if (suggestions.spellingIsCorrect(segment.text)) {
                    null
                } else {
                    segment
                }
            }

            misspelledWords.fastForEach { wordSegment ->
                addRichSpan(SpellCheck, wordSegment.range)
            }
        }
    }

    public fun onTextChange(richTextState: RichTextState) {
        val newTextHash = richTextState.annotatedString.hashCode()
        if (lastTextHash != newTextHash) {
            runSpellCheck()
            lastTextHash = newTextHash
        }
    }

    private fun findWordSegmentContainingRange(
        segments: List<WordSegment>,
        range: TextRange
    ): WordSegment? {
        return segments.find { wordSegment ->
            val segmentRange = wordSegment.range
            range.start >= segmentRange.start && range.end <= segmentRange.end
        }
    }

    public fun getSuggestions(word: String): List<SuggestionItem> {
        val sp = spellChecker ?: return emptyList()

        val suggestions = sp.lookup(word, verbosity = Verbosity.Closest)
        val proposedSuggestions = if (word.isSpelledCorrectly(suggestions).not()) {
            // If things are misspelled, see if it just needs to be broken up
            val composition = sp.wordBreakSegmentation(word)
            val segmentedWord = composition.segmentedString
            if (segmentedWord != null
                && segmentedWord.equals(word, ignoreCase = true).not()
                && suggestions.find { it.term.equals(segmentedWord, ignoreCase = true) } == null
            ) {
                // Add the segmented suggest as first item if it didn't already exist
                listOf(SuggestionItem(segmentedWord, 1.0, 0.1)) + suggestions
            } else {
                suggestions
            }
        } else {
            emptyList()
        }

        return proposedSuggestions.map { suggestionItem ->
            suggestionItem.copy(
                term = applyCapitalizationStrategy(
                    source = word,
                    target = suggestionItem.term
                )
            )
        }
    }
}