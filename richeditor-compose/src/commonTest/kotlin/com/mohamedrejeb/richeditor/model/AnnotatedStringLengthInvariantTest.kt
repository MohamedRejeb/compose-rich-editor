package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `annotatedString` and the BTF2 buffer must always have the same length: the editor's
 * OutputTransformation projects the annotated string's style ranges onto the buffer, and a
 * mismatch is what #717 crashed on at scroll-measure time under the old text field. Checked
 * across document shapes and mutation sequences. The buffer is the comparison, not
 * `textFieldValue.text`, which is derived from the annotated string itself.
 */
@OptIn(ExperimentalRichTextApi::class)
class AnnotatedStringLengthInvariantTest {

    // Helpers

    private fun RichTextState.assertLengthInvariant(label: String) {
        assertEquals(
            textFieldState.text.length,
            annotatedString.text.length,
            "annotatedString/buffer length mismatch ($label)"
        )
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

    // ====================================================================
    // 1. Invariant under content-shape variation
    // ====================================================================

    @Test
    fun invariantHoldsForEmptyState() {
        val state = RichTextState()
        state.assertLengthInvariant("fresh state")
    }

    @Test
    fun invariantHoldsForPlainText() {
        val state = RichTextState()
        state.setText("Just a single line of plain text")
        state.assertLengthInvariant("setText plain")
    }

    @Test
    fun invariantHoldsForMultiParagraphPlainHtml() {
        val state = RichTextState()
        state.setHtml("<p>One</p><p>Two</p><p>Three</p><p>Four</p>")
        state.assertLengthInvariant("multi paragraph html")
    }

    @Test
    fun invariantHoldsForInlineStyledHtml() {
        val state = RichTextState()
        state.setHtml(
            "<p>Mixed <b>bold</b> and <i>italic</i> and " +
                "<u>underline</u> and <b><i>bold-italic</i></b> together.</p>"
        )
        state.assertLengthInvariant("inline styled html")
    }

    @Test
    fun invariantHoldsForAllHeadingLevels() {
        val state = RichTextState()
        state.setHtml(
            "<h1>H1</h1><h2>H2</h2><h3>H3</h3>" +
                "<h4>H4</h4><h5>H5</h5><h6>H6</h6><p>Body</p>"
        )
        state.assertLengthInvariant("all heading levels")
    }

    @Test
    fun invariantHoldsForOrderedListShortAndLong() {
        val state = RichTextState()
        state.setHtml("<ol>" + (1..5).joinToString("") { "<li>Item $it</li>" } + "</ol>")
        state.assertLengthInvariant("short ordered list")

        state.setHtml("<ol>" + (1..120).joinToString("") { "<li>Item $it</li>" } + "</ol>")
        state.assertLengthInvariant("long ordered list crossing 99->100")
    }

    @Test
    fun invariantHoldsForUnorderedList() {
        val state = RichTextState()
        state.setHtml("<ul><li>A</li><li>B</li><li>C</li><li>D</li></ul>")
        state.assertLengthInvariant("unordered list")
    }

    @Test
    fun invariantHoldsForNestedMixedLists() {
        val state = RichTextState()
        state.setHtml(
            "<ol>" +
                "<li>Top one<ul><li>a</li><li>b</li></ul></li>" +
                "<li>Top two<ol><li>i</li><li>ii</li></ol></li>" +
                "<li>Top three</li>" +
                "</ol>"
        )
        state.assertLengthInvariant("nested mixed lists")
    }

    @Test
    fun invariantHoldsForLinks() {
        val state = RichTextState()
        state.setHtml(
            "<p>Visit <a href=\"https://example.com\">example.com</a> or " +
                "<a href=\"https://kotlinlang.org\">kotlinlang.org</a>.</p>"
        )
        state.assertLengthInvariant("links")
    }

    @Test
    fun invariantHoldsForInlineCodeSpans() {
        val state = RichTextState()
        state.setHtml("<p>Use <code>val x = 1</code> and <code>fun foo()</code>.</p>")
        state.assertLengthInvariant("inline code spans")
    }

    @Test
    fun invariantHoldsForMentionTokens() {
        val state = RichTextState()
        state.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { SpanStyle(fontWeight = FontWeight.Medium) },
            )
        )
        state.setMarkdown(
            "Hi [@alice](trigger:mention:u-alice), " +
                "[@bob](trigger:mention:u-bob), and " +
                "[@carol](trigger:mention:u-carol)!"
        )
        state.assertLengthInvariant("mention tokens")
    }

    @Test
    fun invariantHoldsForHashtagTokens() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "hashtag", char = '#'))
        state.setMarkdown(
            "[#release](trigger:hashtag:release) " +
                "[#design](trigger:hashtag:design) " +
                "[#bug](trigger:hashtag:bug)"
        )
        state.assertLengthInvariant("hashtag tokens")
    }

    @Test
    fun invariantHoldsForEmptyAndTrailingParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>One</p><p></p><p>Three</p><p></p>")
        state.assertLengthInvariant("empty and trailing paragraphs")
    }

    @Test
    fun invariantHoldsForLongFlowingParagraph() {
        val state = RichTextState()
        state.setHtml(
            "<p>" +
                "This is a long paragraph that wraps and wraps. ".repeat(40) +
                "</p>"
        )
        state.assertLengthInvariant("long flowing paragraph")
    }

    @Test
    fun invariantHoldsForKitchenSinkContent() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.registerTrigger(Trigger(id = "hashtag", char = '#'))
        state.setMarkdown(
            "# Kitchen sink\n\n" +
                "Intro from [@mohamed](trigger:mention:u-m) " +
                "tagging [#release](trigger:hashtag:release).\n\n" +
                "## Numbered\n\n" +
                (1..12).joinToString("\n") { "$it. Item $it" } + "\n\n" +
                "## Bullets\n\n" +
                "- Alpha **bold**\n- Beta *italic*\n- Gamma `code`\n\n" +
                "Closing line."
        )
        state.assertLengthInvariant("kitchen sink markdown")
    }

    // ====================================================================
    // 2. Invariant after individual mutation paths
    // ====================================================================

    @Test
    fun invariantHoldsThroughTypingIntoOrderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Start</li></ol>")
        state.assertLengthInvariant("initial list")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateTyping(" and more text here")
        state.assertLengthInvariant("after typing in ordered list")
    }

    @Test
    fun invariantHoldsThroughEnterCreatingNewListItems() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")
        state.assertLengthInvariant("initial")

        for (i in 2..6) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertLengthInvariant("after Enter for item $i")
            state.simulateTyping("Item $i")
            state.assertLengthInvariant("after typing item $i")
        }
    }

    @Test
    fun invariantHoldsThroughBackspaceAtListBoundary() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        state.assertLengthInvariant("initial")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("Hello") + "Hello".length
        state.selection = TextRange(helloEnd + 1)
        val newText = text.substring(0, helloEnd) + text.substring(helloEnd + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(helloEnd))
        )
        state.assertLengthInvariant("after backspace at list boundary")
    }

    @Test
    fun invariantHoldsThroughSelectAllReplace() {
        val state = RichTextState()
        state.setHtml(
            "<p>" + "A".repeat(50) + " <b>" + "B".repeat(50) + "</b> " +
                "<i>" + "C".repeat(50) + "</i></p>"
        )
        state.assertLengthInvariant("initial styled text")

        val full = state.annotatedString.text
        state.selection = TextRange(0, full.length)
        state.onTextFieldValueChange(TextFieldValue(text = "Replaced", selection = TextRange(8)))
        state.assertLengthInvariant("after select-all replace")
    }

    @Test
    fun invariantHoldsThroughDeleteAcrossParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p><p>Third paragraph</p>")
        state.assertLengthInvariant("initial")

        val text = state.annotatedString.text
        val firstEnd = text.indexOf("paragraph") + "paragraph".length
        val thirdStart = text.lastIndexOf("Third")
        state.selection = TextRange(firstEnd, thirdStart)
        val newText = text.substring(0, firstEnd) + text.substring(thirdStart)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(firstEnd))
        )
        state.assertLengthInvariant("after cross-paragraph delete")
    }

    @Test
    fun invariantHoldsThroughToggleListSpamming() {
        val state = RichTextState()
        state.setHtml("<p>Line 1</p><p>Line 2</p><p>Line 3</p>")
        state.assertLengthInvariant("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        repeat(8) { i ->
            state.toggleOrderedList()
            state.assertLengthInvariant("toggle ordered #$i")
            state.toggleUnorderedList()
            state.assertLengthInvariant("toggle unordered #$i")
        }
    }

    @Test
    fun invariantHoldsThroughInlineStyleToggling() {
        val state = RichTextState()
        state.setText("Hello world for styling")
        state.assertLengthInvariant("initial plain")

        state.selection = TextRange(0, state.annotatedString.text.length)
        repeat(5) {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            state.assertLengthInvariant("toggle bold")
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            state.assertLengthInvariant("untoggle bold")
        }
    }

    @Test
    fun invariantHoldsThroughHeadingLevelChanges() {
        val state = RichTextState()
        state.setText("Title text")
        state.assertLengthInvariant("plain title text")

        state.selection = TextRange(0, state.annotatedString.text.length)
        for (level in listOf(1, 2, 3, 4, 5, 6, 0, 1, 6)) {
            state.setHeadingStyle(HeadingStyle.fromLevel(level))
            state.assertLengthInvariant("heading level=$level")
        }
    }

    @Test
    fun invariantHoldsThroughFormatSwitching() {
        val state = RichTextState()
        // Cycle through setText, setHtml, setMarkdown rapidly. Different parsers,
        // different paragraph trees, different prefix shapes.
        repeat(10) { i ->
            state.setText("plain $i with extra text repeated $i $i")
            state.assertLengthInvariant("setText iter $i")

            state.setHtml(
                "<h${(i % 6) + 1}>Heading</h${(i % 6) + 1}>" +
                    "<ol>" + (1..(i % 5 + 2)).joinToString("") { "<li>x$it</li>" } + "</ol>" +
                    "<p>Body $i with <b>bold</b> and <i>italic</i></p>"
            )
            state.assertLengthInvariant("setHtml iter $i")

            state.setMarkdown(
                "# Title $i\n\n" +
                    (1..(i % 4 + 2)).joinToString("\n") { "- bullet $it" } + "\n\n" +
                    "Body **strong** and *em* paragraph $i."
            )
            state.assertLengthInvariant("setMarkdown iter $i")
        }
    }

    @Test
    fun invariantHoldsThroughUndoRedoCycles() {
        val state = RichTextState()
        state.setHtml("<p>Initial</p>")

        for (i in 1..5) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateTyping(" step $i")
            state.assertLengthInvariant("after typing step $i")
        }

        // Undo all
        repeat(5) { i ->
            if (state.history.canUndo) {
                state.history.undo()
                state.assertLengthInvariant("after undo $i")
            }
        }

        // Redo all
        repeat(5) { i ->
            if (state.history.canRedo) {
                state.history.redo()
                state.assertLengthInvariant("after redo $i")
            }
        }
    }

    // ====================================================================
    // 3. Combined chaos
    // ====================================================================

    @Test
    fun invariantHoldsThroughCombinedChaos() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.registerTrigger(Trigger(id = "hashtag", char = '#'))

        // Phase 1: build a doc through multiple format paths
        state.setHtml(
            "<h2>Doc</h2>" +
                "<p>Intro with <b>bold</b> and <i>italic</i> and " +
                "<a href=\"https://x.com\">link</a>.</p>" +
                "<ol>" + (1..7).joinToString("") { "<li>Item $it</li>" } + "</ol>" +
                "<ul><li>A</li><li>B</li></ul>" +
                "<p>Outro <code>code</code></p>"
        )
        state.assertLengthInvariant("phase 1 setHtml")

        // Phase 2: cursor jumps
        val jumpPoints = listOf(
            0,
            state.annotatedString.text.length / 4,
            state.annotatedString.text.length / 2,
            state.annotatedString.text.length * 3 / 4,
            state.annotatedString.text.length,
        )
        for (p in jumpPoints) {
            state.selection = TextRange(p)
            state.assertLengthInvariant("cursor jump $p")
        }

        // Phase 3: typing into the middle
        val midText = state.annotatedString.text.length / 2
        state.selection = TextRange(midText)
        state.simulateTyping("INSERTED")
        state.assertLengthInvariant("after middle typing")

        // Phase 4: multiple Enters at end pushes past 9 -> 10 list boundary
        for (i in 1..6) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertLengthInvariant("phase 4 Enter $i")
            state.simulateTyping("New $i")
            state.assertLengthInvariant("phase 4 typing $i")
        }

        // Phase 5: select-all replace
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Fresh content", selection = TextRange(13))
        )
        state.assertLengthInvariant("phase 5 select-all replace")

        // Phase 6: rebuild via markdown with token spans
        state.setMarkdown(
            "**Notes:** [@alice](trigger:mention:u-a) on " +
                "[#release](trigger:hashtag:r)\n\n" +
                "1. one\n2. two\n3. three\n\n" +
                "- bullet A\n- bullet B"
        )
        state.assertLengthInvariant("phase 6 setMarkdown with tokens")

        // Phase 7: backspace through the document
        state.selection = TextRange(state.annotatedString.text.length)
        repeat(15) { i ->
            if (state.selection.min > 0) {
                state.simulateBackspace()
                state.assertLengthInvariant("phase 7 backspace $i")
            }
        }

        // Phase 8: rapid format switching
        repeat(6) { i ->
            state.setText("plain text $i")
            state.assertLengthInvariant("phase 8 setText $i")

            state.setHtml("<ol><li>Only</li></ol>")
            state.assertLengthInvariant("phase 8 setHtml $i")

            state.setMarkdown("# Title\n\nBody $i.")
            state.assertLengthInvariant("phase 8 setMarkdown $i")
        }
    }
}
