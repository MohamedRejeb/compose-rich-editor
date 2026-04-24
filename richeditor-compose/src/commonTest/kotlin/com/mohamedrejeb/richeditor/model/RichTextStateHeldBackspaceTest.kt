package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for issue #624 - held backspace cascades into
 * end-trimming subsequent paragraph contents in long, styled content.
 *
 * The expected behavior: N consecutive backspaces with a collapsed
 * cursor at the end of the content must remove exactly the last N
 * characters, leaving the rest of the document untouched.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateHeldBackspaceTest {

    private fun RichTextState.simulateBackspace() {
        val current = textFieldValue.text
        val pos = selection.min
        if (pos <= 0) return
        val newText = current.substring(0, pos - 1) + current.substring(pos)
        onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(pos - 1))
        )
    }

    @Test
    fun testHeldBackspaceOnPlainMultiParagraph() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph text</p><p>Second paragraph text</p><p>Third paragraph text</p>")

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 5
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text, "Held backspace should only remove last N chars")
    }

    @Test
    fun testHeldBackspaceOnStyledMultiParagraph() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>First paragraph bold</b></p>" +
                "<p><i>Second paragraph italic</i></p>" +
                "<p><u>Third paragraph underline</u></p>"
        )

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 10
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text, "Held backspace on styled content should only remove last N chars")
    }

    @Test
    fun testHeldBackspaceOnLongStyledContent() {
        val state = RichTextState()
        val longPara1 = "A".repeat(100)
        val longPara2 = "B".repeat(100)
        val longPara3 = "C".repeat(100)
        state.setHtml(
            "<p><b>$longPara1</b></p>" +
                "<p><i>$longPara2</i></p>" +
                "<p><u>$longPara3</u></p>"
        )

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 50
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testHeldBackspaceOnMixedInlineStyles() {
        val state = RichTextState()
        state.setHtml(
            "<p>Normal <b>bold</b> and <i>italic</i> and <u>underline</u> text</p>" +
                "<p>Second <b>bold</b> paragraph with <i>italic</i> mix</p>" +
                "<p>Third with <b><i>bold italic</i></b> content</p>"
        )

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 20
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testHeldBackspaceFromMiddleOfStyledContent() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>AAAAAAAAAA</b></p>" +
                "<p><i>BBBBBBBBBB</i></p>" +
                "<p><u>CCCCCCCCCC</u></p>"
        )

        val originalText = state.textFieldValue.text
        // Position cursor in the middle of the second paragraph
        val bParaStart = originalText.indexOf('B')
        val startPos = bParaStart + 5
        state.selection = TextRange(startPos)

        val backspaceCount = 3
        repeat(backspaceCount) { state.simulateBackspace() }

        // Only 3 chars before pos should be removed; anything after the cursor stays intact.
        val expected =
            originalText.substring(0, startPos - backspaceCount) +
                originalText.substring(startPos)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testBatchedBackspaceRemovalAtEnd() {
        // Simulates typematic batch: Compose may coalesce rapid backspaces
        // into one TextFieldValue update with multiple chars removed.
        val state = RichTextState()
        state.setHtml(
            "<p><b>AAAAAAAAAA</b></p>" +
                "<p><i>BBBBBBBBBB</i></p>" +
                "<p><u>CCCCCCCCCC</u></p>"
        )

        val originalText = state.textFieldValue.text
        val removedCount = 5
        val newLength = originalText.length - removedCount

        state.onTextFieldValueChange(
            TextFieldValue(
                text = originalText.substring(0, newLength),
                selection = TextRange(newLength),
            )
        )

        val expected = originalText.substring(0, newLength)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testHeldBackspaceCrossesParagraphBoundaries() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>AAA</b></p>" +
                "<p><i>BBB</i></p>" +
                "<p><u>CCC</u></p>"
        )

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        // Delete enough characters to cross multiple paragraph boundaries
        val backspaceCount = originalText.length - 2
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, 2)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testHeldBackspaceOnLineBreakContent() {
        // Paragraphs created from <br> within a <p> - isFromLineBreak = true
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two<br>Line three<br>Line four</p>")

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 10
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testHeldBackspaceOnStyledLineBreakContent() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>Bold line one</b><br>" +
                "<i>Italic line two</i><br>" +
                "<u>Underline line three</u></p>"
        )

        val originalText = state.textFieldValue.text
        state.selection = TextRange(originalText.length)

        val backspaceCount = 15
        repeat(backspaceCount) { state.simulateBackspace() }

        val expected = originalText.substring(0, originalText.length - backspaceCount)
        assertEquals(expected, state.textFieldValue.text)
    }

    @Test
    fun testBatchedBackspaceRemovalInMiddle() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>AAAAAAAAAA</b></p>" +
                "<p><i>BBBBBBBBBB</i></p>" +
                "<p><u>CCCCCCCCCC</u></p>"
        )

        val originalText = state.textFieldValue.text
        // Remove chars from the middle of paragraph 2
        val bParaStart = originalText.indexOf('B')
        val startPos = bParaStart + 5
        val removedCount = 3
        val newText =
            originalText.substring(0, startPos - removedCount) +
                originalText.substring(startPos)

        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(startPos - removedCount))
        )

        assertEquals(newText, state.textFieldValue.text)
    }
}
