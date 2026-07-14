package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ordered-list numbering parsed from Markdown must preserve the author's numbers
 * instead of resetting to 1, both when the list starts at a number other than 1 and
 * when a non-list paragraph is interleaved between items (#734).
 */
class Issue734OrderedListStartTest {

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

    @Test
    fun indentedItemAfterTextDoesNotCrash() {
        // correctMarkdownText shifts node offsets relative to the raw input; the
        // LIST_NUMBER seed must read from the corrected text or this throws.
        val state = RichTextState()
        state.setMarkdown("a\n  1. b")
        assertTrue(state.annotatedString.text.contains("a"))
    }
}
