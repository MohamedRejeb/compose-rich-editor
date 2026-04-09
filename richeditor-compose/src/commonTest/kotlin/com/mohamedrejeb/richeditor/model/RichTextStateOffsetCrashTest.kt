package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import kotlin.test.*

/**
 * Regression tests for offset mapping / index crash issues:
 * - #632: OffsetMapping.originalToTransformed invalid mapping
 * - #623: OffsetMapping crash in production (Crashlytics)
 * - #631: IllegalArgumentException in adjustRichParagraphLayout
 * - #627: IndexOutOfBoundsException: index: 18, size: 0
 * - #611: StringIndexOutOfBoundsException during paste
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateOffsetCrashTest {

    // ---- Helpers ----

    /**
     * Simulates typing text character-by-character into the state.
     */
    private fun RichTextState.simulateTyping(text: String) {
        var current = annotatedString.text
        for (char in text) {
            current += char
            onTextFieldValueChange(
                TextFieldValue(text = current, selection = TextRange(current.length))
            )
        }
    }

    /**
     * After every state mutation, asserts selection is within annotated string bounds.
     */
    private fun RichTextState.assertSelectionInBounds(context: String = "") {
        val msg = if (context.isNotEmpty()) " ($context)" else ""
        assertTrue(
            selection.start >= 0 && selection.end <= annotatedString.text.length,
            "Selection out of bounds$msg: selection=$selection, textLength=${annotatedString.text.length}"
        )
    }

    /**
     * Asserts that the total span text across all paragraphs matches the annotated string.
     * This catches silent data corruption where spans get out of sync.
     */
    private fun RichTextState.assertSpanTextConsistency(context: String = "") {
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
            "Span text inconsistency$msg: annotatedString.length=${annotatedString.text.length}, " +
                "computed span total=$expected"
        )
    }

    private fun RichParagraph.totalChildTextLength(): Int {
        var total = 0
        for (child in children) {
            total += child.text.length + child.totalChildTextLength()
        }
        return total
    }

    private fun RichSpan.totalChildTextLength(): Int {
        var total = 0
        for (child in children) {
            total += child.text.length + child.totalChildTextLength()
        }
        return total
    }

    // ---- Bug #611: Paragraph boundary operations ----

    @Test
    fun testBackspaceAtOrderedListBoundary() {
        // Two ordered list items, backspace at the separator
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")

        val text = state.annotatedString.text
        val firstItemEnd = text.indexOf("Hello") + "Hello".length

        // Backspace — remove the separator between the two list items
        state.selection = TextRange(firstItemEnd + 1)
        val newText = text.substring(0, firstItemEnd) + text.substring(firstItemEnd + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(firstItemEnd))
        )

        state.assertSelectionInBounds("after backspace at ordered list boundary")
        // Verify text content has both "Hello" and "World"
        assertTrue(
            state.annotatedString.text.contains("Hello"),
            "Should contain 'Hello' after merge, got: '${state.annotatedString.text}'"
        )
        assertTrue(
            state.annotatedString.text.contains("World"),
            "Should contain 'World' after merge, got: '${state.annotatedString.text}'"
        )
    }

    @Test
    fun testBackspaceAtUnorderedListBoundary() {
        val state = RichTextState()
        state.setHtml("<ul><li>First</li><li>Second</li></ul>")

        val text = state.annotatedString.text
        val firstItemEnd = text.indexOf("First") + "First".length

        // Backspace the separator
        state.selection = TextRange(firstItemEnd + 1)
        val newText = text.substring(0, firstItemEnd) + text.substring(firstItemEnd + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(firstItemEnd))
        )

        state.assertSelectionInBounds("after backspace at unordered list boundary")
        assertTrue(state.annotatedString.text.contains("First"))
        assertTrue(state.annotatedString.text.contains("Second"))
    }

    @Test
    fun testBackspaceAtStartOfSecondParagraph() {
        // Backspace at start of second paragraph should merge with first
        val state = RichTextState()
        state.setHtml("<p>First</p><p>Second</p>")

        val text = state.annotatedString.text
        val secondStart = text.indexOf("Second")

        // Backspace removes the separator
        state.selection = TextRange(secondStart)
        val newText = text.substring(0, secondStart - 1) + text.substring(secondStart)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(secondStart - 1))
        )

        state.assertSelectionInBounds("after backspace merging paragraphs")
        assertTrue(state.annotatedString.text.contains("First"))
        assertTrue(state.annotatedString.text.contains("Second"))
    }

    @Test
    fun testDeleteForwardAtEndOfFirstParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Hello</p><p>World</p>")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("Hello") + "Hello".length

        // Delete forward removes the separator after "Hello"
        state.selection = TextRange(helloEnd)
        val newText = text.substring(0, helloEnd) + text.substring(helloEnd + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(helloEnd))
        )

        state.assertSelectionInBounds("after delete-forward merging paragraphs")
        assertTrue(state.annotatedString.text.contains("Hello"))
        assertTrue(state.annotatedString.text.contains("World"))
    }

    // ---- Bug #611: Select All + Paste ----

    @Test
    fun testSelectAllPasteOnLongStyledText() {
        val state = RichTextState()
        state.setHtml(
            "<p>" + "A".repeat(100) + " <b>" + "B".repeat(100) + "</b> " +
                "<i>" + "C".repeat(100) + "</i> " + "D".repeat(100) + "</p>" +
                "<p>" + "E".repeat(100) + " <b>" + "F".repeat(50) + "</b></p>"
        )

        val fullText = state.annotatedString.text
        assertTrue(fullText.length > 500, "Should have long text")

        // Select all and paste shorter text
        state.selection = TextRange(0, fullText.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Pasted", selection = TextRange(6))
        )

        state.assertSelectionInBounds("after select-all paste on long styled text")
    }

    @Test
    fun testSelectAllPasteOnMultiParagraphList() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                (1..10).joinToString("") { "<li>Item number $it with some text</li>" } +
                "</ol>"
        )

        val fullText = state.annotatedString.text

        state.selection = TextRange(0, fullText.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Replaced", selection = TextRange(8))
        )

        state.assertSelectionInBounds("after paste on multi-item ordered list")
    }

    @Test
    fun testDeleteAcrossMultipleParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p><p>Third paragraph</p>")

        val text = state.annotatedString.text
        val firstEnd = text.indexOf("paragraph") + "paragraph".length
        val thirdStart = text.lastIndexOf("Third")

        state.selection = TextRange(firstEnd, thirdStart)
        val newText = text.substring(0, firstEnd) + text.substring(thirdStart)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(firstEnd))
        )

        state.assertSelectionInBounds("after delete across paragraphs")
    }

    // ---- Bug #632/#623: Fast typing ----

    @Test
    fun testRapidTypingInOrderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Start</li></ol>")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateTyping(" and more text here")

        state.assertSelectionInBounds("after rapid typing in ordered list")
    }

    @Test
    fun testRapidTypingThenNewlineInList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Item one</li></ol>")

        val text1 = state.annotatedString.text
        state.selection = TextRange(text1.length)

        // Press Enter
        state.onTextFieldValueChange(
            TextFieldValue(text = text1 + "\n", selection = TextRange(text1.length + 1))
        )

        // Type in new list item
        state.simulateTyping("Item two")

        state.assertSelectionInBounds("after typing in new list item")
    }

    @Test
    fun testMultipleNewlinesCreatingListItems() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")

        for (i in 2..5) {
            val currentText = state.annotatedString.text
            state.selection = TextRange(currentText.length)

            state.onTextFieldValueChange(
                TextFieldValue(
                    text = currentText + "\n",
                    selection = TextRange(currentText.length + 1),
                )
            )

            state.simulateTyping("Item $i")
            state.assertSelectionInBounds("after creating list item $i")
        }
    }

    // ---- List prefix removal ----

    @Test
    fun testBackspaceRemovesListPrefix() {
        val state = RichTextState()
        state.setHtml("<ol><li>Text</li></ol>")

        val text = state.annotatedString.text
        val textStart = text.indexOf("Text")

        state.selection = TextRange(textStart)
        val newText = text.substring(0, textStart - 1) + text.substring(textStart)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(textStart - 1))
        )

        state.assertSelectionInBounds("after removing list prefix")
    }

    @Test
    fun testDeleteEntireListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>AAA</li><li>BBB</li><li>CCC</li></ol>")

        val text = state.annotatedString.text
        val bbbStart = text.indexOf("BBB")
        val bbbPrefixLen = state.richParagraphList[1].type.startText.length

        // Select from separator before BBB through end of BBB
        val beforeBbb = bbbStart - bbbPrefixLen - 1
        val afterBbb = bbbStart + 3

        val newText = text.substring(0, beforeBbb) + text.substring(afterBbb)
        state.selection = TextRange(beforeBbb, afterBbb)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(beforeBbb))
        )

        state.assertSelectionInBounds("after deleting entire list item")
    }

    @Test
    fun testConvertOrderedListToUnorderedByBackspace() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li></ol><ul><li>Two</li></ul>")

        val text = state.annotatedString.text
        val twoStart = text.indexOf("Two")
        val prefixLen = state.richParagraphList.last().type.startText.length
        val boundary = twoStart - prefixLen - 1

        state.selection = TextRange(boundary + 1)
        val newText = text.substring(0, boundary) + text.substring(boundary + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(boundary))
        )

        state.assertSelectionInBounds("after backspace at mixed list boundary")
    }

    // ---- Stress / combined ----

    @Test
    fun testSelectionAlwaysInBoundsAfterOperations() {
        val state = RichTextState()

        state.setHtml(
            "<p>Plain text</p>" +
                "<ol><li>Item 1</li><li>Item 2</li></ol>" +
                "<p><b>Bold</b> and <i>italic</i></p>" +
                "<ul><li>Bullet A</li><li>Bullet B</li></ul>"
        )
        state.assertSelectionInBounds("after setHtml")

        // Delete from middle to near end
        val text = state.annotatedString.text
        val mid = text.length / 2
        val nearEnd = text.length - 5
        state.selection = TextRange(mid, nearEnd)
        val newText = text.substring(0, mid) + text.substring(nearEnd)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(mid))
        )
        state.assertSelectionInBounds("after large middle deletion")

        // Type new text
        state.simulateTyping("new content")
        state.assertSelectionInBounds("after typing new content")

        // Select all and replace
        val fullText = state.annotatedString.text
        state.selection = TextRange(0, fullText.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Fresh", selection = TextRange(5))
        )
        state.assertSelectionInBounds("after select-all replace")
    }

    @Test
    fun testSpanTextMatchesAnnotatedStringAfterListOperations() {
        // This test checks that spans stay consistent with annotated string
        // after various list operations (the root cause area for #611)
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Two</li><li>Three</li></ol>")

        state.assertSpanTextConsistency("initial list state")

        // Type into the second item
        val twoIdx = state.annotatedString.text.indexOf("Two")
        state.selection = TextRange(twoIdx + 3)
        state.simulateTyping(" more")
        state.assertSpanTextConsistency("after typing in list item")

        // Add new line to create new item
        val currentText = state.annotatedString.text
        state.selection = TextRange(currentText.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = currentText + "\n", selection = TextRange(currentText.length + 1))
        )
        state.assertSpanTextConsistency("after creating new list item")

        // Type in new item
        state.simulateTyping("Four")
        state.assertSpanTextConsistency("after typing in new list item")
    }
}
