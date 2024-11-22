package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

public data class WordSegment(
    var text: String,
    var range: TextRange,
)

private data class WordSplitState(
    var currentOffset: Int = 0,
    var pendingWord: StringBuilder = StringBuilder(),
    var pendingStartIndex: Int = -1,
)

/**
 * Return a sequence of words and their TextRange for all words in the list of RichParagraph
 * and their nested RichSpan children.
 */
internal fun List<RichParagraph>.getWords(): Sequence<WordSegment> = sequence {
    var currentOffset = 0
    for (paragraph in this@getWords) {
        val state = WordSplitState(currentOffset)
        yieldAll(paragraph.getWords(state))
        currentOffset += paragraph.getTotalLength()
    }
}

private fun RichParagraph.getWords(state: WordSplitState): Sequence<WordSegment> = sequence {
    for (span in children) {
        yieldAll(span.getWords(state))
    }
    state.apply {
        // If there is a pending word after all children are processed, yield it.
        if (pendingWord.isNotEmpty()) {
            yield(WordSegment(pendingWord.toString(), TextRange(pendingStartIndex, currentOffset)))
            pendingWord.clear()
            pendingStartIndex = -1
        }
    }
}

private fun RichSpan.getWords(state: WordSplitState): Sequence<WordSegment> = sequence {
    state.apply {
        // Process the current span's text if it's not empty.
        if (text.isNotEmpty()) {
            var localStartIndex = -1
            // Search this spans text for boundaries
            for (i in text.indices) {
                val c = text[i]
                //println("'$c'")

                if (c.isLetterOrDigit()) {
                    // Starting a new word
                    if (localStartIndex == -1) {
                        localStartIndex = i
                    }
                } else {
                    suspend fun SequenceScope<WordSegment>.returnPendingWord() {
                        yield(WordSegment(pendingWord.toString(), TextRange(pendingStartIndex, currentOffset + i)))
                        pendingWord.clear()
                        pendingStartIndex = -1
                    }

                    // Ending a word
                    if (localStartIndex != -1) {
                        // Ending a words that is at least partially in this RichSpan
                        val word = text.substring(localStartIndex, i)
                        if (pendingWord.isNotEmpty()) {
                            // Continue the word from the previous state
                            pendingWord.append(word)
                            returnPendingWord()
                        } else {
                            yield(WordSegment(word, TextRange(currentOffset + localStartIndex, currentOffset + i)))
                        }
                        localStartIndex = -1
                    } else if(state.pendingStartIndex != -1) { // Ending a word wholly in a previous span
                        // Word from previous span is ending, nothing in this span to append
                        if (pendingWord.isNotEmpty()) {
                            returnPendingWord()
                        }
                    }
                }
            }

            // Handle pending characters at the end of this RichSpan
            if (localStartIndex != -1) {
                val word = text.substring(localStartIndex)
                if (pendingWord.isNotEmpty()) {
                    // Continue the word from the previous state
                    pendingWord.append(word)
                    pendingStartIndex = if (pendingStartIndex == -1) currentOffset + localStartIndex else pendingStartIndex
                } else {
                    pendingWord.append(word)
                    pendingStartIndex = currentOffset + localStartIndex
                }
            }
            currentOffset += text.length
        }

        // Process each child recursively.
        if(children.isNotEmpty()) {
            for (child in children) {
                yieldAll(child.getWords(state))
            }
        }
    }
}

/**
 * Calculate the total length of a RichSpan, including all nested children.
 */
private fun RichSpan.getTotalLength(): Int {
    var totalLength = text.length
    for (child in children) {
        totalLength += child.getTotalLength()
    }
    return totalLength
}

/**
 * Calculate the total length of a RichParagraph, including all nested spans.
 */
private fun RichParagraph.getTotalLength(): Int {
    var totalLength = 0
    for (span in children) {
        totalLength += span.getTotalLength()
    }
    return totalLength
}

public fun findWordSegmentContainingRange(segments: List<WordSegment>, range: TextRange): WordSegment? {
    return segments.find { wordSegment ->
        val segmentRange = wordSegment.range
        range.start >= segmentRange.start && range.end <= segmentRange.end
    }
}
