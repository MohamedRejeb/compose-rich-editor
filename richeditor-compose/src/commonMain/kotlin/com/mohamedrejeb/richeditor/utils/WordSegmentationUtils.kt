package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

public data class WordSegment(
    val text: String,
    val range: TextRange,
)

private data class WordSplitState(
    var currentOffset: Int = 0,
    var pendingWord: StringBuilder = StringBuilder(),
    var pendingStartIndex: Int = -1,
)

/**
 * Return a sequence of [WordSegment] and their [TextRange] for all words in the list of
 * [RichParagraph] and their nested [RichSpan] children.
 *
 * @receiver List<RichParagraph> The list of paragraphs to process.
 * @return A sequence of WordSegment
 */
internal fun List<RichParagraph>.getWords(): Sequence<WordSegment> = sequence {
    var currentOffset = 0
    for (paragraph in this@getWords) {
        // Individual words may not cross a RichParagraph boundary,
        // so a new WordSplitState is created for each Paragraph.
        val state = WordSplitState(currentOffset)
        yieldAll(paragraph.getWords(state))
        currentOffset += paragraph.getTotalLength()
    }
}

/**
 * Extracts and yields [WordSegment]s from the tree of [RichSpan] objects within a [RichParagraph].
 *
 * @receiver [RichParagraph] The paragraph containing RichSpan children to process.
 * @param state [WordSplitState] Tracks partial words and their starting indices across spans.
 * @return A sequence of WordSegment objects
 *
 * This function iterates through the RichSpan children of the paragraph, yielding
 * word segments from each. After processing all children, it checks for any remaining
 * partial word in the state and yields it if present.
 */
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

/**
 * Extracts and yields [WordSegment]s and their [TextRange]s from this sub-tree of [RichSpan] objects.
 *
 * @receiver [RichSpan] The current node in the tree being processed.
 * @param state [WordSplitState] Tracks partial words and their starting indices across spans.
 * @return A sequence of WordSegments
 *
 * This function identifies word boundaries in the text of the current [RichSpan], using
 * alphanumeric characters as word components and non-alphanumeric characters as delimiters.
 * It handles words spanning multiple nodes by leveraging the provided state and processes
 * child nodes recursively, yielding their results as part of the sequence.
 */
private fun RichSpan.getWords(state: WordSplitState): Sequence<WordSegment> = sequence {
    state.apply {
        // Process the current span's text if it's not empty.
        if (text.isNotEmpty()) {
            var localStartIndex = -1
            // Search this spans text for boundaries
            for (i in text.indices) {
                if (text[i].isLetterOrDigit()) {
                    // Starting a new word
                    if (localStartIndex == -1) {
                        localStartIndex = i
                    }
                } else {
                    // Calculate the WordSegment TextRange, yield the segment
                    // and then clear the pending word
                    suspend fun SequenceScope<WordSegment>.returnPendingWord() {
                        yield(
                            WordSegment(
                                pendingWord.toString(),
                                TextRange(pendingStartIndex, currentOffset + i)
                            )
                        )
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
                            yield(
                                WordSegment(
                                    word,
                                    TextRange(currentOffset + localStartIndex, currentOffset + i)
                                )
                            )
                        }
                        localStartIndex = -1
                    } else if (state.pendingStartIndex != -1) { // Ending a word wholly in a previous span
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
                    pendingStartIndex =
                        if (pendingStartIndex == -1) currentOffset + localStartIndex else pendingStartIndex
                } else {
                    pendingWord.append(word)
                    pendingStartIndex = currentOffset + localStartIndex
                }
            }
            currentOffset += text.length
        }

        // Process each child recursively.
        if (children.isNotEmpty()) {
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
    var totalLength = 1 // Each paragraph counts as a new line, 1 character
    if (children.isNotEmpty()) {
        for (span in children) {
            totalLength += span.getTotalLength()
        }
    }

    return totalLength
}
