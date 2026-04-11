package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.*

/**
 * Tests for OffsetMapping consistency — verifies that `annotatedString.text.length`
 * and `textFieldValue.text.length` always stay in sync after every operation.
 *
 * When these diverge, Compose crashes with:
 * `IllegalStateException: OffsetMapping.transformedToOriginal returned invalid mapping`
 *
 * Related issues: #632, #623, #390, #611
 *
 * Covers three root cause areas:
 * 1. `updateParagraphType` text surgery with stale textRange (#3)
 * 2. `checkForParagraphs` paragraph split with prefix insertion (#4)
 * 3. `adjustOrderedListsNumbers` chained prefix mutations (#5)
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateOffsetMappingTest {

    // ---- Invariant assertion helpers ----

    /**
     * The core invariant: annotatedString and textFieldValue must always
     * have the same text. If they diverge, OffsetMapping.Identity crashes.
     */
    private fun RichTextState.assertLengthsInSync(context: String = "") {
        val msg = if (context.isNotEmpty()) " ($context)" else ""
        assertEquals(
            annotatedString.text.length,
            textFieldValue.text.length,
            "Length mismatch$msg: annotatedString=${annotatedString.text.length}, " +
                "textFieldValue=${textFieldValue.text.length}"
        )
        assertEquals(
            annotatedString.text,
            textFieldValue.text,
            "Text mismatch$msg"
        )
    }

    /**
     * Asserts tree span lengths add up to annotatedString length.
     */
    private fun RichTextState.assertTreeConsistency(context: String = "") {
        val msg = if (context.isNotEmpty()) " ($context)" else ""
        var expected = 0
        richParagraphList.forEachIndexed { i, paragraph ->
            if (i > 0) expected++ // paragraph separator
            expected += paragraph.type.startText.length
            expected += paragraph.totalChildTextLength()
        }
        assertEquals(
            annotatedString.text.length,
            expected,
            "Tree inconsistency$msg: annotatedString.length=${annotatedString.text.length}, " +
                "tree total=$expected"
        )
    }

    private fun RichParagraph.totalChildTextLength(): Int =
        children.sumOf { it.text.length + it.totalChildTextLength() }

    private fun RichSpan.totalChildTextLength(): Int =
        children.sumOf { it.text.length + it.totalChildTextLength() }

    /**
     * Runs both invariant checks.
     */
    private fun RichTextState.assertInvariants(context: String = "") {
        assertLengthsInSync(context)
        assertTreeConsistency(context)
    }

    private fun RichTextState.simulateTyping(text: String) {
        for (char in text) {
            val current = annotatedString.text
            val pos = selection.min
            val newText = current.substring(0, pos) + char + current.substring(pos)
            onTextFieldValueChange(
                TextFieldValue(text = newText, selection = TextRange(pos + 1))
            )
        }
    }

    private fun RichTextState.simulateEnter() {
        val current = annotatedString.text
        val pos = selection.min
        val newText = current.substring(0, pos) + "\n" + current.substring(pos)
        onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(pos + 1))
        )
    }

    private fun RichTextState.simulateBackspace() {
        val current = annotatedString.text
        val pos = selection.min
        if (pos <= 0) return
        val newText = current.substring(0, pos - 1) + current.substring(pos)
        onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(pos - 1))
        )
    }

    // ========================================================================
    // #3: updateParagraphType — list prefix text surgery
    // ========================================================================

    @Test
    fun testToggleOrderedListOnPlainText() {
        val state = RichTextState()
        state.setHtml("<p>Hello World</p>")
        state.assertInvariants("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after toggle ordered list on")

        state.toggleOrderedList()
        state.assertInvariants("after toggle ordered list off")
    }

    @Test
    fun testToggleUnorderedListOnPlainText() {
        val state = RichTextState()
        state.setHtml("<p>Hello World</p>")
        state.assertInvariants("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleUnorderedList()
        state.assertInvariants("after toggle unordered list on")

        state.toggleUnorderedList()
        state.assertInvariants("after toggle unordered list off")
    }

    @Test
    fun testSwitchOrderedToUnorderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Item one</li><li>Item two</li></ol>")
        state.assertInvariants("initial ordered list")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleUnorderedList()
        state.assertInvariants("after switching to unordered")

        state.toggleOrderedList()
        state.assertInvariants("after switching back to ordered")
    }

    @Test
    fun testToggleListOnMultipleParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>First</p><p>Second</p><p>Third</p>")
        state.assertInvariants("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after toggling ordered list on all paragraphs")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after toggling ordered list off all paragraphs")
    }

    @Test
    fun testToggleListOnSingleParagraphInMultiParagraphDoc() {
        val state = RichTextState()
        state.setHtml("<p>First</p><p>Second</p><p>Third</p>")
        state.assertInvariants("initial")

        // Select only the second paragraph
        val secondStart = state.annotatedString.text.indexOf("Second")
        val secondEnd = secondStart + "Second".length
        state.selection = TextRange(secondStart, secondEnd)
        state.toggleOrderedList()
        state.assertInvariants("after toggling ordered list on middle paragraph")
    }

    // ========================================================================
    // #4: checkForParagraphs — Enter key creating new list items
    // ========================================================================

    @Test
    fun testEnterAtEndOfOrderedListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li></ol>")
        state.assertInvariants("initial")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after Enter at end of ordered list item")

        assertEquals(2, state.richParagraphList.size, "Should have 2 paragraphs")
        assertIs<OrderedList>(state.richParagraphList[1].type)
    }

    @Test
    fun testEnterAtEndOfUnorderedListItem() {
        val state = RichTextState()
        state.setHtml("<ul><li>Hello</li></ul>")
        state.assertInvariants("initial")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after Enter at end of unordered list item")

        assertEquals(2, state.richParagraphList.size)
        assertIs<UnorderedList>(state.richParagraphList[1].type)
    }

    @Test
    fun testEnterInMiddleOfListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>HelloWorld</li></ol>")
        state.assertInvariants("initial")

        val splitPos = state.annotatedString.text.indexOf("World")
        state.selection = TextRange(splitPos)
        state.simulateEnter()
        state.assertInvariants("after Enter in middle of list item")

        assertEquals(2, state.richParagraphList.size)
        assertTrue(state.annotatedString.text.contains("Hello"))
        assertTrue(state.annotatedString.text.contains("World"))
    }

    @Test
    fun testMultipleEntersCreatingOrderedListItems() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")
        state.assertInvariants("initial")

        for (i in 2..5) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertInvariants("after Enter #$i")

            state.simulateTyping("Item $i")
            state.assertInvariants("after typing item $i")
        }

        assertEquals(5, state.richParagraphList.size, "Should have 5 list items")
    }

    @Test
    fun testEnterAtStartOfListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li></ol>")
        state.assertInvariants("initial")

        // Place cursor right after the list prefix, before "Hello"
        val helloStart = state.annotatedString.text.indexOf("Hello")
        state.selection = TextRange(helloStart)
        state.simulateEnter()
        state.assertInvariants("after Enter at start of list item content")
    }

    @Test
    fun testEnterOnEmptyListItemRemovesList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li></li></ol>")
        state.assertInvariants("initial with empty list item")

        // Cursor at the end (in the empty second item)
        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after Enter on empty list item")
    }

    // ========================================================================
    // #5: adjustOrderedListsNumbers — chained renumbering
    // ========================================================================

    @Test
    fun testOrderedListNumbersStayConsistentAfterInsert() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..10).joinToString("") { "<li>Item $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial 10-item list")

        // Verify all numbers are correct
        state.richParagraphList.forEachIndexed { i, p ->
            val type = p.type
            assertIs<OrderedList>(type, "Paragraph $i should be OrderedList")
            assertEquals(i + 1, type.number, "Paragraph $i should have number ${i + 1}")
        }
    }

    @Test
    fun testOrderedListRenumberingOn9to10Boundary() {
        // The "9." → "10." transition changes prefix length from 3 to 4 chars.
        // This is where off-by-one errors in chained adjustments are most likely.
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..9).joinToString("") { "<li>Item $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial 9-item list")

        // Add item 10 by pressing Enter at end of item 9
        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after creating 10th item")

        state.simulateTyping("Item 10")
        state.assertInvariants("after typing in 10th item")

        assertEquals(10, state.richParagraphList.size)
    }

    @Test
    fun testOrderedListRenumberingOn99to100Boundary() {
        // "99." → "100." changes prefix length from 4 to 5 chars.
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..99).joinToString("") { "<li>X</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial 99-item list")

        // Add item 100
        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after creating 100th item")

        state.simulateTyping("Y")
        state.assertInvariants("after typing in 100th item")
    }

    @Test
    fun testDeleteMiddleItemRenumbersSubsequentItems() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..10).joinToString("") { "<li>Item $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial")

        // Delete item 5 by backspacing at its start to merge with item 4
        val item5Text = "Item 5"
        val item5Start = state.annotatedString.text.indexOf(item5Text)
        val prefixLen = state.richParagraphList[4].type.startText.length
        val mergePoint = item5Start - prefixLen

        state.selection = TextRange(mergePoint)
        state.simulateBackspace()
        state.assertInvariants("after merging item 4 and 5")
    }

    @Test
    fun testDeleteFirstItemRenumbersAllSubsequent() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..10).joinToString("") { "<li>Item $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial")

        // Select from start through end of "Item 1" + the separator after it
        val text = state.annotatedString.text
        val item2Idx = text.indexOf("Item 2")
        assertTrue(item2Idx > 0, "Should find 'Item 2' in: $text")

        // Select everything before "Item 2" including its prefix and the separator
        val item2PrefixLen = state.richParagraphList[1].type.startText.length
        val deleteEnd = item2Idx - item2PrefixLen - 1 // before separator + prefix of item 2

        state.selection = TextRange(0, deleteEnd)
        val newText = text.substring(0, 0) + text.substring(deleteEnd)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(0))
        )
        state.assertInvariants("after deleting first item")
    }

    @Test
    fun testInsertNewItemInMiddleRenumbersSubsequent() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..10).joinToString("") { "<li>Item $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial")

        // Press Enter at end of item 5 to insert new item
        val item5End = state.annotatedString.text.indexOf("Item 6")
        val prefixLen = state.richParagraphList[5].type.startText.length
        state.selection = TextRange(item5End - prefixLen - 1) // before separator

        state.simulateEnter()
        state.assertInvariants("after inserting item after 5")

        state.simulateTyping("New item")
        state.assertInvariants("after typing in new item")
    }

    // ========================================================================
    // Combined: operations that exercise multiple code paths
    // ========================================================================

    @Test
    fun testRapidToggleListTypeBack() {
        val state = RichTextState()
        state.setHtml("<p>Line 1</p><p>Line 2</p><p>Line 3</p>")
        state.assertInvariants("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)

        // Rapidly toggle between list types
        repeat(5) { i ->
            state.toggleOrderedList()
            state.assertInvariants("toggle ordered #${i + 1}")
            state.toggleUnorderedList()
            state.assertInvariants("toggle unordered #${i + 1}")
        }
    }

    @Test
    fun testTypingAfterListToggle() {
        val state = RichTextState()
        state.setHtml("<p>Hello</p>")
        state.assertInvariants("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after toggle")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateTyping(" World")
        state.assertInvariants("after typing")
    }

    @Test
    fun testCreateListThenAddMultipleItemsThenRemoveList() {
        val state = RichTextState()
        state.setHtml("<p>Start</p>")
        state.assertInvariants("initial")

        // Make it a list
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after making list")

        // Add items
        for (i in 2..12) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertInvariants("after Enter for item $i")
            state.simulateTyping("Item $i")
            state.assertInvariants("after typing item $i")
        }

        // Remove list from all items
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after removing list from 12 items")
    }

    @Test
    fun testListLevelChangesMaintainSync() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                "<li>Item 1</li>" +
                "<li>Item 2</li>" +
                "<li>Item 3</li>" +
                "</ol>"
        )
        state.assertInvariants("initial")

        // Select item 2 and increase indent
        val item2Start = state.annotatedString.text.indexOf("Item 2")
        state.selection = TextRange(item2Start, item2Start + "Item 2".length)

        if (state.canIncreaseListLevel()) {
            state.increaseListLevel()
            state.assertInvariants("after increasing indent on item 2")
        }

        if (state.canDecreaseListLevel()) {
            state.decreaseListLevel()
            state.assertInvariants("after decreasing indent on item 2")
        }
    }

    @Test
    fun testMixedContentWithListsAndPlainText() {
        val state = RichTextState()
        state.setHtml(
            "<p>Intro</p>" +
                "<ol><li>One</li><li>Two</li><li>Three</li></ol>" +
                "<p>Middle</p>" +
                "<ul><li>Bullet A</li><li>Bullet B</li></ul>" +
                "<p>End</p>"
        )
        state.assertInvariants("initial mixed content")

        // Type in the middle paragraph
        val middleStart = state.annotatedString.text.indexOf("Middle")
        state.selection = TextRange(middleStart + "Middle".length)
        state.simulateTyping(" text")
        state.assertInvariants("after typing in middle paragraph")

        // Add new list item at end of ordered list
        val threeEnd = state.annotatedString.text.indexOf("Three") + "Three".length
        state.selection = TextRange(threeEnd)
        state.simulateEnter()
        state.assertInvariants("after Enter at end of ordered list")

        state.simulateTyping("Four")
        state.assertInvariants("after typing Four")
    }

    @Test
    fun testSelectAllDeleteOnListDocument() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..15).joinToString("") { "<li>Item number $it</li>" } +
                "</ol>"
        )
        state.assertInvariants("initial 15-item list")

        // Select all and delete
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "", selection = TextRange(0))
        )
        state.assertInvariants("after select all delete")
    }

    // ========================================================================
    // Chaos test: large document, rapid mixed operations
    // ========================================================================

    @Test
    fun testChaosOnLargeDocument() {
        val state = RichTextState()

        // Start with a complex mixed HTML document
        state.setHtml(
            "<h1>Document Title</h1>" +
                "<p>Introduction paragraph with <b>bold text</b> and <i>italic text</i> and " +
                "<b><i>bold italic</i></b> mixed together in a single paragraph.</p>" +
                "<ol>" +
                "<li>First ordered item with <a href=\"https://example.com\">a link inside</a></li>" +
                "<li>Second item that is <b>partially bold</b> and has more content after</li>" +
                "<li>Third item</li>" +
                "<li>Fourth item</li>" +
                "<li>Fifth item</li>" +
                "<li>Sixth item</li>" +
                "<li>Seventh item</li>" +
                "<li>Eighth item</li>" +
                "<li>Ninth item</li>" +
                "</ol>" +
                "<p>A plain paragraph between lists.</p>" +
                "<ul>" +
                "<li>Bullet <b>one</b></li>" +
                "<li>Bullet <i>two</i></li>" +
                "<li>Bullet three</li>" +
                "</ul>" +
                "<p>Another paragraph with <code>inline code</code> and regular text.</p>" +
                "<ol>" +
                "<li>Resume numbering item A</li>" +
                "<li>Resume numbering item B</li>" +
                "</ol>" +
                "<p>Final paragraph.</p>"
        )
        state.assertInvariants("initial large doc")

        val text = state.annotatedString.text

        // --- Phase 1: Jump selection around rapidly ---
        val positions = listOf(
            0,
            text.length / 4,
            text.length / 2,
            text.length * 3 / 4,
            text.length,
            text.indexOf("bold"),
            text.indexOf("italic"),
            text.indexOf("Third"),
            text.indexOf("Bullet"),
            text.indexOf("Final"),
        ).filter { it in 0..text.length }

        for (pos in positions) {
            state.selection = TextRange(pos)
            state.assertInvariants("selection jump to $pos")
        }

        // --- Phase 2: Type in the middle of the ordered list ---
        val thirdIdx = state.annotatedString.text.indexOf("Third")
        if (thirdIdx >= 0) {
            state.selection = TextRange(thirdIdx + 3) // inside "Third"
            state.simulateTyping("XYZ")
            state.assertInvariants("after typing XYZ inside Third")
        }

        // --- Phase 3: Add new ordered list item (pushes count to 10 = prefix length change) ---
        val ninthEnd = state.annotatedString.text.indexOf("Ninth")
        if (ninthEnd >= 0) {
            state.selection = TextRange(ninthEnd + "Ninth item".length)
            state.simulateEnter()
            state.assertInvariants("after Enter creating 10th ordered item")

            state.simulateTyping("Tenth item")
            state.assertInvariants("after typing Tenth item")
        }

        // --- Phase 4: Delete across paragraph boundaries ---
        val bulletIdx = state.annotatedString.text.indexOf("Bullet")
        val plainBetween = state.annotatedString.text.indexOf("plain paragraph")
        if (bulletIdx >= 0 && plainBetween >= 0 && plainBetween < bulletIdx) {
            // Delete from middle of "plain paragraph" to "Bullet"
            val from = plainBetween + 5
            val to = bulletIdx + 3
            if (from < to && to <= state.annotatedString.text.length) {
                val currentText = state.annotatedString.text
                state.selection = TextRange(from, to)
                val newText = currentText.substring(0, from) + currentText.substring(to)
                state.onTextFieldValueChange(
                    TextFieldValue(text = newText, selection = TextRange(from))
                )
                state.assertInvariants("after cross-paragraph delete")
            }
        }

        // --- Phase 5: Toggle list types on remaining content ---
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleOrderedList()
        state.assertInvariants("after toggling everything to ordered")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleUnorderedList()
        state.assertInvariants("after toggling everything to unordered")

        state.selection = TextRange(0, state.annotatedString.text.length)
        state.toggleUnorderedList()
        state.assertInvariants("after removing all lists")

        // --- Phase 6: Rapid typing with line breaks ---
        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.assertInvariants("after Enter at end")

        state.simulateTyping("New paragraph ")
        state.assertInvariants("after typing new paragraph")

        state.simulateEnter()
        state.assertInvariants("after second Enter")

        state.simulateTyping("Another one")
        state.assertInvariants("after typing another one")

        // Make it a list and add items
        state.selection = TextRange(
            state.annotatedString.text.indexOf("New paragraph"),
            state.annotatedString.text.length
        )
        state.toggleOrderedList()
        state.assertInvariants("after making new paragraphs a list")

        state.selection = TextRange(state.annotatedString.text.length)
        for (i in 1..5) {
            state.simulateEnter()
            state.assertInvariants("after Enter for extra item $i")
            state.simulateTyping("Extra $i")
            state.assertInvariants("after typing extra $i")
        }

        // --- Phase 7: Jump cursor around and type single chars ---
        val finalText = state.annotatedString.text
        val jumpPoints = listOf(0, 5, 10, 20, finalText.length / 2, finalText.length - 10, finalText.length)
            .filter { it in 0..finalText.length }

        for (pos in jumpPoints) {
            state.selection = TextRange(pos)
            state.assertInvariants("cursor jump to $pos in phase 7")
            if (pos < finalText.length) {
                state.simulateTyping(".")
                state.assertInvariants("after typing dot at $pos")
            }
        }

        // --- Phase 8: Backspace through list prefix boundaries ---
        for (i in 1..10) {
            if (state.selection.min > 0) {
                state.simulateBackspace()
                state.assertInvariants("backspace #$i in phase 8")
            }
        }

        // --- Phase 9: Select chunks and replace ---
        val t = state.annotatedString.text
        if (t.length > 20) {
            state.selection = TextRange(5, 15)
            val replaced = t.substring(0, 5) + "REPLACED" + t.substring(15)
            state.onTextFieldValueChange(
                TextFieldValue(text = replaced, selection = TextRange(13))
            )
            state.assertInvariants("after replacing text range")
        }

        // --- Phase 10: Select all and replace with fresh content, then rebuild ---
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Clean slate", selection = TextRange(11))
        )
        state.assertInvariants("after select-all replace")

        state.selection = TextRange(5)
        state.simulateEnter()
        state.assertInvariants("after splitting clean slate")

        state.simulateTyping("inserted")
        state.assertInvariants("final state")
    }
}
