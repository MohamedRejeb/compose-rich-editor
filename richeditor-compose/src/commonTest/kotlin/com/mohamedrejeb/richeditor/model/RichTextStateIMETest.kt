package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.*

/**
 * Tests for IME composition behavior with lists.
 *
 * Issue #640: On Android, pressing Enter at the end of a list item while using a keyboard
 * with active IME composition (e.g. SwiftKey) randomly loses list formatting.
 *
 * The root cause is that IME keyboards can commit composed text AND insert a newline
 * in a single onValueChange call, which can confuse the paragraph detection logic.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateIMETest {

    // ---- Helpers ----

    private fun RichTextState.assertInvariants(context: String = "") {
        val msg = if (context.isNotEmpty()) " ($context)" else ""
        assertEquals(
            annotatedString.text.length,
            textFieldValue.text.length,
            "Length mismatch$msg"
        )
        assertEquals(
            annotatedString.text,
            textFieldValue.text,
            "Text mismatch$msg"
        )
    }

    // ========================================================================
    // Normal Enter (no composition) - baseline
    // ========================================================================

    @Test
    fun testEnterAtEndOfOrderedListItem_noComposition() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li></ol>")
        state.assertInvariants("initial")

        val text = state.annotatedString.text
        state.selection = TextRange(text.length)

        // Simulate normal Enter (no composition)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "\n",
                selection = TextRange(text.length + 1),
            )
        )
        state.assertInvariants("after Enter")

        assertEquals(2, state.richParagraphList.size, "Should create second list item")
        assertIs<OrderedList>(state.richParagraphList[1].type, "Second paragraph should be OrderedList")
    }

    // ========================================================================
    // IME composition + Enter - the #640 scenario
    // ========================================================================

    @Test
    fun testEnterWithActiveComposition_orderedList() {
        // Simulate: user types "world" with composition in list item, then presses Enter
        // IME commits "world" and adds newline in one onValueChange
        val state = RichTextState()
        state.setHtml("<ol><li>Hello </li></ol>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Step 1: Simulate IME composition starting - user starts typing "wor"
        // The composed text is part of the text but has a composition range
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "wor",
                selection = TextRange(cursorPos + 3),
                composition = TextRange(cursorPos, cursorPos + 3),
            )
        )
        state.assertInvariants("after composition start")

        // Step 2: IME commits "world" and adds newline in a single change
        // This is what SwiftKey does when you press Enter during composition
        val textAfterComposition = state.annotatedString.text
        val committed = textAfterComposition.substring(0, cursorPos) + "world\n"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = committed + textAfterComposition.substring(cursorPos + 3),
                selection = TextRange(cursorPos + 6), // after "world\n"
                composition = null, // composition ended
            )
        )
        state.assertInvariants("after IME commit + Enter")

        // The new paragraph should be an OrderedList, not a DefaultParagraph
        assertTrue(
            state.richParagraphList.size >= 2,
            "#640: Should have at least 2 paragraphs after Enter. Got: ${state.richParagraphList.size}"
        )
        assertIs<OrderedList>(
            state.richParagraphList.last().type,
            "#640: New paragraph after Enter should be OrderedList, got ${state.richParagraphList.last().type::class.simpleName}"
        )
    }

    @Test
    fun testEnterWithActiveComposition_unorderedList() {
        // Same as above but with unordered list
        val state = RichTextState()
        state.setHtml("<ul><li>Hello </li></ul>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Step 1: IME composition
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "wor",
                selection = TextRange(cursorPos + 3),
                composition = TextRange(cursorPos, cursorPos + 3),
            )
        )

        // Step 2: IME commits + Enter
        val textAfterComposition = state.annotatedString.text
        val committed = textAfterComposition.substring(0, cursorPos) + "world\n"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = committed + textAfterComposition.substring(cursorPos + 3),
                selection = TextRange(cursorPos + 6),
                composition = null,
            )
        )
        state.assertInvariants("after IME commit + Enter")

        assertTrue(state.richParagraphList.size >= 2)
        assertIs<UnorderedList>(
            state.richParagraphList.last().type,
            "#640: New paragraph should be UnorderedList"
        )
    }

    @Test
    fun testEnterWithComposition_replacesComposedText() {
        // IME replaces the composed text with a different word + newline
        // e.g., user typed "hel" but autocorrect changes it to "hello" + Enter
        val state = RichTextState()
        state.setHtml("<ol><li>Start </li></ol>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Step 1: IME composition "hel"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "hel",
                selection = TextRange(cursorPos + 3),
                composition = TextRange(cursorPos, cursorPos + 3),
            )
        )

        // Step 2: Autocorrect replaces "hel" with "hello" + newline
        val textBefore = state.annotatedString.text.substring(0, cursorPos)
        val textAfterComposed = state.annotatedString.text.substring(cursorPos + 3)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "hello\n" + textAfterComposed,
                selection = TextRange(cursorPos + 6),
                composition = null,
            )
        )
        state.assertInvariants("after autocorrect + Enter")

        assertTrue(state.richParagraphList.size >= 2)
        assertIs<OrderedList>(
            state.richParagraphList.last().type,
            "#640: Autocorrect + Enter should still create OrderedList"
        )
    }

    @Test
    fun testEnterWithComposition_middleOfListItem() {
        // User has composition in the middle of text, presses Enter
        val state = RichTextState()
        state.setHtml("<ol><li>Hello World</li></ol>")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("World")

        // Step 1: IME composition "new" between Hello and World
        val textWithComposition = text.substring(0, helloEnd) + "new" + text.substring(helloEnd)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textWithComposition,
                selection = TextRange(helloEnd + 3),
                composition = TextRange(helloEnd, helloEnd + 3),
            )
        )

        // Step 2: IME commits "new" and adds newline
        val currentText = state.annotatedString.text
        val insertPos = helloEnd + 3 // after "new"
        val newText = currentText.substring(0, insertPos) + "\n" + currentText.substring(insertPos)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(insertPos + 1),
                composition = null,
            )
        )
        state.assertInvariants("after Enter in middle with composition")

        assertTrue(state.richParagraphList.size >= 2)
        assertIs<OrderedList>(
            state.richParagraphList[1].type,
            "#640: Enter in middle should create OrderedList"
        )
    }

    @Test
    fun testMultipleEntersWithComposition() {
        // Simulate adding multiple list items with IME composition between each
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")

        for (i in 2..4) {
            val text = state.annotatedString.text
            val end = text.length

            // Composition: type "Item"
            state.onTextFieldValueChange(
                TextFieldValue(
                    text = text + "Ite",
                    selection = TextRange(end + 3),
                    composition = TextRange(end, end + 3),
                )
            )

            // Commit "Item N" + Enter
            val current = state.annotatedString.text
            val beforeComposed = current.substring(0, end)
            val afterComposed = current.substring(end + 3)
            state.onTextFieldValueChange(
                TextFieldValue(
                    text = beforeComposed + "Item $i\n" + afterComposed,
                    selection = TextRange(end + "Item $i\n".length),
                    composition = null,
                )
            )
            state.assertInvariants("after item $i")
        }

        // All paragraphs should be ordered list items
        state.richParagraphList.forEachIndexed { idx, paragraph ->
            assertIs<OrderedList>(
                paragraph.type,
                "#640: Paragraph $idx should be OrderedList after IME Entry, got ${paragraph.type::class.simpleName}"
            )
        }
    }

    // ========================================================================
    // #640 core bug: autocorrect SHORTENS text + adds newline → net removal
    // checkForParagraphs skips the newline because it's before old selection
    // ========================================================================

    @Test
    fun testEnterThenIMERemovesPrefixExactLogs() {
        // Exact reproduction from device logs:
        // 1. User types "brot" char by char with composition in an ordered list
        // 2. Enter → "1. brot\n" → creates "2. " prefix → "1. brot 2. " (11 chars)
        // 3. IME sends "1. brot\n" (8 chars) - removes "2. " prefix it doesn't know about
        val state = RichTextState()
        state.setHtml("<ol><li>brot</li></ol>")

        val textBeforeEnter = state.annotatedString.text // "1. brot"
        assertEquals("1. brot", textBeforeEnter)
        assertEquals(1, state.richParagraphList.size)
        assertIs<OrderedList>(state.richParagraphList[0].type)

        // Move cursor to end
        state.selection = TextRange(textBeforeEnter.length)

        // Enter pressed - newline added at end
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBeforeEnter + "\n",
                selection = TextRange(textBeforeEnter.length + 1),
            )
        )
        // After this, checkForParagraphs creates second list item with "2. " prefix
        assertEquals(2, state.richParagraphList.size, "Enter should create 2 paragraphs")
        assertIs<OrderedList>(state.richParagraphList[1].type, "Second should be OrderedList")

        val textAfterEnter = state.annotatedString.text // "1. brot 2. " (11 chars)

        // IME sends its own version - doesn't know about "2. " prefix, sends "1. brot\n"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBeforeEnter + "\n", // "1. brot\n" (8 chars)
                selection = TextRange(textBeforeEnter.length + 1),
            )
        )

        // After the IME's update, we should STILL have 2 ordered list paragraphs
        assertTrue(
            state.richParagraphList.size >= 2,
            "#640 exact: Should still have 2 paragraphs after IME prefix removal. Got ${state.richParagraphList.size}"
        )
        assertIs<OrderedList>(
            state.richParagraphList.last().type,
            "#640 exact: Last paragraph should still be OrderedList, got ${state.richParagraphList.last().type::class.simpleName}"
        )
    }

    @Test
    fun testEnterWithAutocorrectThatShortensText() {
        // This is the exact #640 scenario:
        // Composed: "hellooo" (7 chars) → autocorrect: "hello" (5 chars) + "\n" → net -1
        // The newline ends up BEFORE the old selection, so checkForParagraphs skips it
        val state = RichTextState()
        state.setHtml("<ol><li>Start </li></ol>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Step 1: User types "hellooo" with composition
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "hellooo",
                selection = TextRange(cursorPos + 7),
                composition = TextRange(cursorPos, cursorPos + 7),
            )
        )
        val textWithComposition = state.annotatedString.text

        // Step 2: Keyboard autocorrects "hellooo" → "hello" + Enter
        // Net change: 7 chars removed, 6 added (5 + newline) = -1
        val beforeComposed = textWithComposition.substring(0, cursorPos)
        val afterComposed = textWithComposition.substring(cursorPos + 7)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = beforeComposed + "hello\n" + afterComposed,
                selection = TextRange(cursorPos + 6),
                composition = null,
            )
        )
        state.assertInvariants("after autocorrect shortening + Enter")

        assertTrue(
            state.richParagraphList.size >= 2,
            "#640: Should create new paragraph. Got ${state.richParagraphList.size} paragraphs"
        )
        assertIs<OrderedList>(
            state.richParagraphList.last().type,
            "#640: New paragraph should be OrderedList when autocorrect shortens text + Enter"
        )
    }

    @Test
    fun testEnterWithAutocorrectSameLength() {
        // Autocorrect replaces with SAME length word + newline → net +1
        // This should work because handleAddingCharacters runs
        val state = RichTextState()
        state.setHtml("<ol><li>Start </li></ol>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Compose "wrld" (4 chars)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "wrld",
                selection = TextRange(cursorPos + 4),
                composition = TextRange(cursorPos, cursorPos + 4),
            )
        )
        val textWithComposition = state.annotatedString.text

        // Autocorrect "wrld" → "word" (same length) + Enter → net +1
        val before = textWithComposition.substring(0, cursorPos)
        val after = textWithComposition.substring(cursorPos + 4)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = before + "word\n" + after,
                selection = TextRange(cursorPos + 5),
                composition = null,
            )
        )
        state.assertInvariants("after same-length autocorrect + Enter")

        assertTrue(state.richParagraphList.size >= 2)
        assertIs<OrderedList>(
            state.richParagraphList.last().type,
            "Same-length autocorrect + Enter should create OrderedList"
        )
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Test
    fun testCompositionCommitWithoutEnter_noNewParagraph() {
        // IME commits text without Enter - should NOT create a new paragraph
        val state = RichTextState()
        state.setHtml("<ol><li>Hello </li></ol>")

        val text = state.annotatedString.text
        val cursorPos = text.length

        // Composition
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "wor",
                selection = TextRange(cursorPos + 3),
                composition = TextRange(cursorPos, cursorPos + 3),
            )
        )

        // Commit without Enter
        val current = state.annotatedString.text
        val newText = current.substring(0, cursorPos) + "world" + current.substring(cursorPos + 3)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(cursorPos + 5),
                composition = null,
            )
        )
        state.assertInvariants("after commit without Enter")

        assertEquals(1, state.richParagraphList.size, "Should still have 1 paragraph")
    }

    @Test
    fun testEnterOnEmptyListItemWithComposition() {
        // User has composition on empty list item, presses Enter - should exit list
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li></li></ol>")

        val text = state.annotatedString.text
        val emptyItemStart = text.length

        // Place cursor at end (in empty item)
        state.selection = TextRange(emptyItemStart)

        // Enter on empty list item
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text + "\n",
                selection = TextRange(emptyItemStart + 1),
            )
        )
        state.assertInvariants("after Enter on empty list item")
    }

    // ========================================================================
    // Regression tests: ensure IME revert fix doesn't break other scenarios
    // ========================================================================

    // This was causing a crash after a wrong fix for the composition issue
    @Test
    fun testReplacingParagraphWithEnter() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        // Non-collapsed selection - should NOT trigger IME revert
        state.selection = TextRange(5, 11)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "Hello\n",
                selection = TextRange(6),
            )
        )
        state.assertInvariants("after replacing paragraph with enter")
    }

    @Test
    fun testBackspaceAtListBoundaryDoesNotTriggerMerge() {
        // Backspace between two list items - text gets shorter, no new newlines
        // Should NOT trigger IME revert (no new newlines in the text)
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")

        val text = state.annotatedString.text
        val worldStart = text.indexOf("World")

        // Cursor at start of "World", backspace removes the separator
        state.selection = TextRange(worldStart)
        val newText = text.substring(0, worldStart - 1) + text.substring(worldStart)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(worldStart - 1),
            )
        )
        state.assertInvariants("after backspace at list boundary")
    }

    @Test
    fun testSelectAllDeleteOnMultiParagraph() {
        // Select all + delete - non-collapsed selection, should NOT trigger
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")

        val text = state.annotatedString.text
        state.selection = TextRange(0, text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "", selection = TextRange(0))
        )
        state.assertInvariants("after select all delete")
    }

    @Test
    fun testIMERevertOnThreeItemList() {
        // Same as #640 but with 3 list items - ensure merge + rebuild handles 3 paragraphs
        val state = RichTextState()
        state.setHtml("<ol><li>AAA</li><li>BBB</li></ol>")

        val textBefore = state.annotatedString.text
        state.selection = TextRange(textBefore.length)

        // Enter creates third item
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )
        assertEquals(3, state.richParagraphList.size)

        val textAfterEnter = state.annotatedString.text

        // IME reverts - removes "3. " prefix
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )
        state.assertInvariants("after IME revert on 3-item list")

        assertTrue(
            state.richParagraphList.size >= 3,
            "Should still have 3 paragraphs after IME revert. Got ${state.richParagraphList.size}"
        )
    }

    @Test
    fun testIMERevertOnUnorderedList() {
        // Same pattern but with unordered list
        val state = RichTextState()
        state.setHtml("<ul><li>Hello</li></ul>")

        val textBefore = state.annotatedString.text
        state.selection = TextRange(textBefore.length)

        // Enter
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )
        assertEquals(2, state.richParagraphList.size)

        // IME revert
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )
        state.assertInvariants("after IME revert on unordered list")

        assertTrue(
            state.richParagraphList.size >= 2,
            "Should have 2 paragraphs. Got ${state.richParagraphList.size}"
        )
        assertIs<UnorderedList>(
            state.richParagraphList.last().type,
            "Last paragraph should be UnorderedList"
        )
    }

    @Test
    fun testNormalTypingAfterIMERevert() {
        // After the IME revert is handled, normal typing should work
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li></ol>")

        val textBefore = state.annotatedString.text
        state.selection = TextRange(textBefore.length)

        // Enter → IME revert cycle
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )
        val textAfterEnter = state.annotatedString.text
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textBefore + "\n",
                selection = TextRange(textBefore.length + 1),
            )
        )

        // Now type in the new list item
        val currentText = state.annotatedString.text
        val cursorPos = state.selection.min
        val textWithChar = currentText.substring(0, cursorPos) + "A" + currentText.substring(cursorPos)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = textWithChar,
                selection = TextRange(cursorPos + 1),
            )
        )
        state.assertInvariants("after typing in recovered list item")
        assertTrue(state.annotatedString.text.contains("A"), "Typed char should be present")
    }

    @Test
    fun testReplacingSelectionWithNewlineAcrossParagraphs() {
        // Select across two paragraphs and replace with text+newline - non-collapsed
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p>")

        val text = state.annotatedString.text
        val firstEnd = text.indexOf("paragraph") + 3 // mid-word
        val secondStart = text.indexOf("Second") + 3 // mid-word

        state.selection = TextRange(firstEnd, secondStart)
        val newText = text.substring(0, firstEnd) + "X\n" + text.substring(secondStart)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(firstEnd + 2),
            )
        )
        state.assertInvariants("after cross-paragraph replace with newline")
    }

    @Test
    fun testCursorAtEndThenDeleteAll() {
        // Cursor collapsed at end, then text becomes empty (shorter + no newlines)
        // Should NOT trigger IME revert - no new newlines
        val state = RichTextState()
        state.setHtml("<ol><li>Hi</li><li>There</li></ol>")

        state.selection = TextRange(state.annotatedString.text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "", selection = TextRange(0))
        )
        state.assertInvariants("after deleting all with collapsed cursor")
    }

    @Test
    fun testDeleteReplaceParagraphWithLineBreak() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hi</li><li>Man</li></ol>")
        state.selection = TextRange(5, state.annotatedString.text.length)

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hi\n",
                selection = TextRange(6),
            )
        )
        state.assertInvariants("after replacing paragraph with line break")
    }

    @Test
    fun testDeleteReplaceParagraphWithLineText() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hi</li><li>Man</li></ol>")
        state.selection = TextRange(5, state.annotatedString.text.length)

        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hib",
                selection = TextRange(6),
            )
        )
        assertEquals("1. Hib", state.annotatedString.text)
        assertEquals(1, state.richParagraphList.size)
    }

    // ========================================================================
    // Selection replacement edge cases
    // ========================================================================

    @Test
    fun testSelectTwoParagraphEndsAndTypeChar() {
        // Select the ends of two paragraphs and type a char
        // "1. Hello 2. World" → select "lo 2. World" (positions 6-17) → type "x"
        // Expected: "1. Helx" (1 paragraph)
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        val text = state.annotatedString.text // "1. Hello 2. World"

        state.selection = TextRange(6, text.length)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Helx",
                selection = TextRange(7),
            )
        )
        assertEquals("1. Helx", state.annotatedString.text)
        assertEquals(1, state.richParagraphList.size)
    }

    @Test
    fun testSelectTwoParagraphEndsAndTypeEnter() {
        // Select ends of two paragraphs and type enter
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(6, text.length)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. Hel\n",
                selection = TextRange(7),
            )
        )
        state.assertInvariants("after selecting two paragraph ends + enter")
    }

    @Test
    fun testSelectParagraphStartsAndTypeChar() {
        // Select from paragraph 1 start through paragraph 2 start, then type
        // "1. Hello 2. World" → select from 3 to 12 → type "x"
        // Positions 0-2 = "1. ", 3-7 = "Hello", 8 = " ", 9-11 = "2. ", 12-16 = "World"
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")

        state.selection = TextRange(3, 12)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. xWorld",
                selection = TextRange(4),
            )
        )
        state.assertInvariants("after selecting paragraph starts + typing")
    }

    @Test
    fun testSelectEntireListAndTypeChar() {
        // Select all items in a list and type a single char
        val state = RichTextState()
        state.setHtml("<ol><li>First</li><li>Second</li><li>Third</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(0, text.length)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "X",
                selection = TextRange(1),
            )
        )
        state.assertInvariants("after select all + type X")
        assertTrue(state.annotatedString.text.contains("X"))
    }

    @Test
    fun testSelectAcrossNumberedItemsAndType() {
        // 10-item list - select across items 3-7 and type
        val state = RichTextState()
        state.setHtml(
            "<ol>" + (1..10).joinToString("") { "<li>Item $it</li>" } + "</ol>"
        )

        val text = state.annotatedString.text
        val item3Start = text.indexOf("Item 3")
        val item7End = text.indexOf("Item 7") + "Item 7".length

        state.selection = TextRange(item3Start, item7End)
        val replacement = text.substring(0, item3Start) + "Replaced" + text.substring(item7End)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = replacement,
                selection = TextRange(item3Start + "Replaced".length),
            )
        )
        state.assertInvariants("after selecting across numbered items + replace")
    }

    @Test
    fun testSelectionReplacementWithEmptyText() {
        // Select content and replace with empty string (pure deletion via selection)
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(5, 8) // "lo " (end of "Hello" + space separator)
        val newText = text.substring(0, 5) + text.substring(8)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(5),
            )
        )
        state.assertInvariants("after selection + empty replacement")
    }

    @Test
    fun testSelectionReplacementAcrossUnorderedList() {
        // Same pattern but on unordered list
        val state = RichTextState()
        state.setHtml("<ul><li>Apple</li><li>Banana</li></ul>")
        val text = state.annotatedString.text

        state.selection = TextRange(4, text.length)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "- Apx",
                selection = TextRange(5),
            )
        )
        state.assertInvariants("after unordered list selection replace")
    }

    @Test
    fun testSelectionReplacementWithNewlineBetweenParagraphs() {
        // Select between two paragraphs and type newline (splits a different way)
        val state = RichTextState()
        state.setHtml("<ol><li>ABC</li><li>DEF</li></ol>")
        val text = state.annotatedString.text // "1. ABC 2. DEF"

        // Select just the separator and 2. prefix
        state.selection = TextRange(6, 10) // " 2. " (space + prefix)
        val newText = text.substring(0, 6) + "\n" + text.substring(10)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(7),
            )
        )
        state.assertInvariants("after replacing separator with newline")
    }

    @Test
    fun testSelectionReplacementInPlainParagraphs() {
        // Same pattern with plain paragraphs (no lists)
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p>")
        val text = state.annotatedString.text

        val firstEnd = text.indexOf("paragraph") + "paragraph".length
        val secondStart = text.lastIndexOf("Second")

        state.selection = TextRange(firstEnd, secondStart + "Second".length)
        val newText = text.substring(0, firstEnd) + "X" + text.substring(secondStart + "Second".length)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(firstEnd + 1),
            )
        )
        state.assertInvariants("after plain paragraph selection replace")
    }

    @Test
    fun testSelectionReplaceAtStartOfDocument() {
        // Select from position 0 across a paragraph boundary
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(0, 10)
        val newText = "Z" + text.substring(10)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(1),
            )
        )
        state.assertInvariants("after replacing from start")
    }

    @Test
    fun testSelectionReplaceAtEndOfDocument() {
        // Select to the very end of document
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(8, text.length)
        val newText = text.substring(0, 8) + "X"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(9),
            )
        )
        state.assertInvariants("after replacing to end")
    }

    @Test
    fun testSelectionReplaceWithSameLengthText() {
        // Replace selection with same-length text (text length unchanged → not "shorter")
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li></ol>")
        val text = state.annotatedString.text

        state.selection = TextRange(3, 8) // "Hello"
        state.onTextFieldValueChange(
            TextFieldValue(
                text = "1. World",
                selection = TextRange(8),
            )
        )
        state.assertInvariants("after same-length replacement")
        assertTrue(state.annotatedString.text.contains("World"))
    }
}
