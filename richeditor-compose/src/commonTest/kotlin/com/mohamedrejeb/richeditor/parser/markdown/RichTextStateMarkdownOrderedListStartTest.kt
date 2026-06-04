package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for ordered-list numbering parsed from Markdown.
 *
 * The author's numbers must be preserved instead of being reset to 1, both
 * when the list starts at a number other than 1 (e.g. `5. `) and when a
 * non-list paragraph is interleaved between items.
 */
class RichTextStateMarkdownOrderedListStartTest {

    private fun orderedListNumbers(markdown: String): List<Int> {
        val state = RichTextState()
        state.setMarkdown(markdown)
        return state.richParagraphList.mapNotNull { (it.type as? OrderedList)?.number }
    }

    @Test
    fun listStartingAtOneIsSequential() {
        assertEquals(listOf(1, 2, 3), orderedListNumbers("1. a\n2. b\n3. c"))
    }

    @Test
    fun listStartingAtFivePreservesStartNumber() {
        assertEquals(listOf(5, 6), orderedListNumbers("5. five\n6. six"))
    }

    @Test
    fun listStartingAtMultiDigitPreservesStartNumber() {
        assertEquals(listOf(10, 11, 12), orderedListNumbers("10. ten\n11. eleven\n12. twelve"))
    }

    @Test
    fun numberingSurvivesParagraphBetweenItems() {
        val markdown = "1. first\n   continuation\n\n2. second\n3. third"
        assertEquals(listOf(1, 2, 3), orderedListNumbers(markdown))
    }
}
