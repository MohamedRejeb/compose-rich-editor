package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for the `visualTransformation` snapshot fix in `updateAnnotatedString`:
 * a captured lambda must return the annotatedString it was paired with, not a live
 * read, and `visualTransformation.filter(...)` must always match
 * `textFieldValue.text.length` (the invariant Compose validates at scroll-measure
 * time, #717).
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateVisualTransformationRaceTest {

    // ---- Helpers ----

    private fun RichTextState.transformedLength(): Int {
        val input = AnnotatedString(textFieldValue.text)
        return visualTransformation.filter(input).text.length
    }

    private fun RichTextState.assertVtInvariant(label: String) {
        val tfvLen = textFieldValue.text.length
        val transformedLen = transformedLength()
        if (tfvLen != transformedLen) {
            fail(
                "VT/textFieldValue length mismatch ($label): " +
                    "textFieldValue.text.length=$tfvLen, " +
                    "visualTransformation.filter(...).text.length=$transformedLen, " +
                    "annotatedString.length=${annotatedString.text.length}"
            )
        }
        assertEquals(
            tfvLen,
            annotatedString.text.length,
            "annotatedString/textFieldValue length mismatch ($label)"
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
    // 1. Direct race verification: captured VT must return paired snapshot
    // ====================================================================

    @Test
    fun capturedVisualTransformationReturnsPairedSnapshotAfterSubsequentSetHtml() {
        val state = RichTextState()
        state.setHtml("<p>Hello world example</p>")

        val captured = state.visualTransformation
        val pairedTextFieldValue = state.textFieldValue
        val pairedLength = pairedTextFieldValue.text.length

        // Mutate state. After this, state.annotatedString has a different length
        // than the snapshot the captured VT was paired with.
        state.setHtml("<p>X</p>")
        assertTrue(
            state.annotatedString.text.length != pairedLength,
            "Test setup: subsequent setHtml must change annotatedString length"
        )

        // Invoking the captured lambda with its paired textFieldValue must produce
        // a transformed string of the original paired length, not the current.
        val transformed = captured.filter(AnnotatedString(pairedTextFieldValue.text))
        assertEquals(
            pairedLength,
            transformed.text.length,
            "Captured visualTransformation must return its paired snapshot length, " +
                "not whatever state.annotatedString currently holds. " +
                "If this fails, the lambda is doing a live read of state.annotatedString " +
                "and the OffsetMapping.Identity race is back."
        )
    }

    @Test
    fun capturedVisualTransformationStaysPairedAcrossManyMutations() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Two</li><li>Three</li></ol>")

        val captured = state.visualTransformation
        val pairedTextFieldValue = state.textFieldValue
        val pairedLength = pairedTextFieldValue.text.length

        // Drive a long sequence of mutations. The captured VT, called with its
        // paired textFieldValue, must keep returning the original length.
        repeat(20) { i ->
            state.setHtml("<p>Iteration $i with extra text $i $i $i $i</p>")
            state.setMarkdown("- a\n- b\n- c\n- d\n- e\n")
            state.setText("plain text $i")
        }

        val transformed = captured.filter(AnnotatedString(pairedTextFieldValue.text))
        assertEquals(
            pairedLength,
            transformed.text.length,
            "Captured VT lost its paired snapshot during heavy mutations"
        )
    }

    @Test
    fun capturedVisualTransformationStaysPairedAcrossListBoundaryRenumbering() {
        // The 9 -> 10 renumbering changes prefix length for paragraph 10 from
        // "10. " (4 chars) compared to "9. " (3 chars). If the captured VT did a
        // live read it would suddenly disagree on length.
        val state = RichTextState()
        state.setHtml("<ol>" + (1..9).joinToString("") { "<li>Item</li>" } + "</ol>")

        val captured = state.visualTransformation
        val pairedTextFieldValue = state.textFieldValue
        val pairedLength = pairedTextFieldValue.text.length

        // Push past the 9 -> 10 boundary
        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateEnter()
        state.simulateTyping("Item 10")

        val transformed = captured.filter(AnnotatedString(pairedTextFieldValue.text))
        assertEquals(pairedLength, transformed.text.length)
    }

    // ====================================================================
    // 2. Invariant under content-shape variation
    // ====================================================================

    @Test
    fun invariantHoldsForEmptyState() {
        val state = RichTextState()
        state.assertVtInvariant("fresh state")
    }

    @Test
    fun invariantHoldsForPlainText() {
        val state = RichTextState()
        state.setText("Just a single line of plain text")
        state.assertVtInvariant("setText plain")
    }

    @Test
    fun invariantHoldsForMultiParagraphPlainHtml() {
        val state = RichTextState()
        state.setHtml("<p>One</p><p>Two</p><p>Three</p><p>Four</p>")
        state.assertVtInvariant("multi paragraph html")
    }

    @Test
    fun invariantHoldsForInlineStyledHtml() {
        val state = RichTextState()
        state.setHtml(
            "<p>Mixed <b>bold</b> and <i>italic</i> and " +
                "<u>underline</u> and <b><i>bold-italic</i></b> together.</p>"
        )
        state.assertVtInvariant("inline styled html")
    }

    @Test
    fun invariantHoldsForAllHeadingLevels() {
        val state = RichTextState()
        state.setHtml(
            "<h1>H1</h1><h2>H2</h2><h3>H3</h3>" +
                "<h4>H4</h4><h5>H5</h5><h6>H6</h6><p>Body</p>"
        )
        state.assertVtInvariant("all heading levels")
    }

    @Test
    fun invariantHoldsForOrderedListShortAndLong() {
        val state = RichTextState()
        state.setHtml("<ol>" + (1..5).joinToString("") { "<li>Item $it</li>" } + "</ol>")
        state.assertVtInvariant("short ordered list")

        state.setHtml("<ol>" + (1..120).joinToString("") { "<li>Item $it</li>" } + "</ol>")
        state.assertVtInvariant("long ordered list crossing 99->100")
    }

    @Test
    fun invariantHoldsForUnorderedList() {
        val state = RichTextState()
        state.setHtml("<ul><li>A</li><li>B</li><li>C</li><li>D</li></ul>")
        state.assertVtInvariant("unordered list")
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
        state.assertVtInvariant("nested mixed lists")
    }

    @Test
    fun invariantHoldsForLinks() {
        val state = RichTextState()
        state.setHtml(
            "<p>Visit <a href=\"https://example.com\">example.com</a> or " +
                "<a href=\"https://kotlinlang.org\">kotlinlang.org</a>.</p>"
        )
        state.assertVtInvariant("links")
    }

    @Test
    fun invariantHoldsForInlineCodeSpans() {
        val state = RichTextState()
        state.setHtml("<p>Use <code>val x = 1</code> and <code>fun foo()</code>.</p>")
        state.assertVtInvariant("inline code spans")
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
        state.assertVtInvariant("mention tokens")
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
        state.assertVtInvariant("hashtag tokens")
    }

    @Test
    fun invariantHoldsForEmptyAndTrailingParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>One</p><p></p><p>Three</p><p></p>")
        state.assertVtInvariant("empty and trailing paragraphs")
    }

    @Test
    fun invariantHoldsForLongFlowingParagraph() {
        val state = RichTextState()
        state.setHtml(
            "<p>" +
                "This is a long paragraph that wraps and wraps. ".repeat(40) +
                "</p>"
        )
        state.assertVtInvariant("long flowing paragraph")
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
        state.assertVtInvariant("kitchen sink markdown")
    }

    // ====================================================================
    // 3. Invariant after individual mutation paths
    // ====================================================================

    @Test
    fun invariantHoldsThroughTypingIntoOrderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Start</li></ol>")
        state.assertVtInvariant("initial list")

        state.selection = TextRange(state.annotatedString.text.length)
        state.simulateTyping(" and more text here")
        state.assertVtInvariant("after typing in ordered list")
    }

    @Test
    fun invariantHoldsThroughEnterCreatingNewListItems() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")
        state.assertVtInvariant("initial")

        for (i in 2..6) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertVtInvariant("after Enter for item $i")
            state.simulateTyping("Item $i")
            state.assertVtInvariant("after typing item $i")
        }
    }

    @Test
    fun invariantHoldsThroughBackspaceAtListBoundary() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello</li><li>World</li></ol>")
        state.assertVtInvariant("initial")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("Hello") + "Hello".length
        state.selection = TextRange(helloEnd + 1)
        val newText = text.substring(0, helloEnd) + text.substring(helloEnd + 1)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(helloEnd))
        )
        state.assertVtInvariant("after backspace at list boundary")
    }

    @Test
    fun invariantHoldsThroughSelectAllReplace() {
        val state = RichTextState()
        state.setHtml(
            "<p>" + "A".repeat(50) + " <b>" + "B".repeat(50) + "</b> " +
                "<i>" + "C".repeat(50) + "</i></p>"
        )
        state.assertVtInvariant("initial styled text")

        val full = state.annotatedString.text
        state.selection = TextRange(0, full.length)
        state.onTextFieldValueChange(TextFieldValue(text = "Replaced", selection = TextRange(8)))
        state.assertVtInvariant("after select-all replace")
    }

    @Test
    fun invariantHoldsThroughDeleteAcrossParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p><p>Third paragraph</p>")
        state.assertVtInvariant("initial")

        val text = state.annotatedString.text
        val firstEnd = text.indexOf("paragraph") + "paragraph".length
        val thirdStart = text.lastIndexOf("Third")
        state.selection = TextRange(firstEnd, thirdStart)
        val newText = text.substring(0, firstEnd) + text.substring(thirdStart)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(firstEnd))
        )
        state.assertVtInvariant("after cross-paragraph delete")
    }

    @Test
    fun invariantHoldsThroughToggleListSpamming() {
        val state = RichTextState()
        state.setHtml("<p>Line 1</p><p>Line 2</p><p>Line 3</p>")
        state.assertVtInvariant("initial")

        state.selection = TextRange(0, state.annotatedString.text.length)
        repeat(8) { i ->
            state.toggleOrderedList()
            state.assertVtInvariant("toggle ordered #$i")
            state.toggleUnorderedList()
            state.assertVtInvariant("toggle unordered #$i")
        }
    }

    @Test
    fun invariantHoldsThroughInlineStyleToggling() {
        val state = RichTextState()
        state.setText("Hello world for styling")
        state.assertVtInvariant("initial plain")

        state.selection = TextRange(0, state.annotatedString.text.length)
        repeat(5) {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            state.assertVtInvariant("toggle bold")
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            state.assertVtInvariant("untoggle bold")
        }
    }

    @Test
    fun invariantHoldsThroughHeadingLevelChanges() {
        val state = RichTextState()
        state.setText("Title text")
        state.assertVtInvariant("plain title text")

        state.selection = TextRange(0, state.annotatedString.text.length)
        for (level in listOf(1, 2, 3, 4, 5, 6, 0, 1, 6)) {
            state.setHeadingStyle(HeadingStyle.fromLevel(level))
            state.assertVtInvariant("heading level=$level")
        }
    }

    @Test
    fun invariantHoldsThroughFormatSwitching() {
        val state = RichTextState()
        // Cycle through setText, setHtml, setMarkdown rapidly. Different parsers,
        // different paragraph trees, different prefix shapes.
        repeat(10) { i ->
            state.setText("plain $i with extra text repeated $i $i")
            state.assertVtInvariant("setText iter $i")

            state.setHtml(
                "<h${(i % 6) + 1}>Heading</h${(i % 6) + 1}>" +
                    "<ol>" + (1..(i % 5 + 2)).joinToString("") { "<li>x$it</li>" } + "</ol>" +
                    "<p>Body $i with <b>bold</b> and <i>italic</i></p>"
            )
            state.assertVtInvariant("setHtml iter $i")

            state.setMarkdown(
                "# Title $i\n\n" +
                    (1..(i % 4 + 2)).joinToString("\n") { "- bullet $it" } + "\n\n" +
                    "Body **strong** and *em* paragraph $i."
            )
            state.assertVtInvariant("setMarkdown iter $i")
        }
    }

    @Test
    fun invariantHoldsThroughUndoRedoCycles() {
        val state = RichTextState()
        state.setHtml("<p>Initial</p>")

        for (i in 1..5) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateTyping(" step $i")
            state.assertVtInvariant("after typing step $i")
        }

        // Undo all
        repeat(5) { i ->
            if (state.history.canUndo) {
                state.history.undo()
                state.assertVtInvariant("after undo $i")
            }
        }

        // Redo all
        repeat(5) { i ->
            if (state.history.canRedo) {
                state.history.redo()
                state.assertVtInvariant("after redo $i")
            }
        }
    }

    // ====================================================================
    // 4. Combined chaos
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
        state.assertVtInvariant("phase 1 setHtml")

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
            state.assertVtInvariant("cursor jump $p")
        }

        // Phase 3: typing into the middle
        val midText = state.annotatedString.text.length / 2
        state.selection = TextRange(midText)
        state.simulateTyping("INSERTED")
        state.assertVtInvariant("after middle typing")

        // Phase 4: multiple Enters at end pushes past 9 -> 10 list boundary
        for (i in 1..6) {
            state.selection = TextRange(state.annotatedString.text.length)
            state.simulateEnter()
            state.assertVtInvariant("phase 4 Enter $i")
            state.simulateTyping("New $i")
            state.assertVtInvariant("phase 4 typing $i")
        }

        // Phase 5: select-all replace
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.onTextFieldValueChange(
            TextFieldValue(text = "Fresh content", selection = TextRange(13))
        )
        state.assertVtInvariant("phase 5 select-all replace")

        // Phase 6: rebuild via markdown with token spans
        state.setMarkdown(
            "**Notes:** [@alice](trigger:mention:u-a) on " +
                "[#release](trigger:hashtag:r)\n\n" +
                "1. one\n2. two\n3. three\n\n" +
                "- bullet A\n- bullet B"
        )
        state.assertVtInvariant("phase 6 setMarkdown with tokens")

        // Phase 7: backspace through the document
        state.selection = TextRange(state.annotatedString.text.length)
        repeat(15) { i ->
            if (state.selection.min > 0) {
                state.simulateBackspace()
                state.assertVtInvariant("phase 7 backspace $i")
            }
        }

        // Phase 8: rapid format switching
        repeat(6) { i ->
            state.setText("plain text $i")
            state.assertVtInvariant("phase 8 setText $i")

            state.setHtml("<ol><li>Only</li></ol>")
            state.assertVtInvariant("phase 8 setHtml $i")

            state.setMarkdown("# Title\n\nBody $i.")
            state.assertVtInvariant("phase 8 setMarkdown $i")
        }
    }

    @Test
    fun invariantHoldsThroughManyCapturedSnapshotsInterleaved() {
        // Capture VTs at multiple stages, mutate between captures, and verify
        // each captured VT still produces its own paired length.
        val state = RichTextState()

        data class Capture(
            val vt: androidx.compose.ui.text.input.VisualTransformation,
            val length: Int,
        )
        val captures = mutableListOf<Capture>()

        for (i in 0 until 12) {
            state.setHtml("<p>Stage $i with extra " + "x".repeat(i * 3) + "</p>")
            captures.add(Capture(state.visualTransformation, state.textFieldValue.text.length))
        }

        // Now invoke each captured VT with its paired-length input, verify match.
        captures.forEachIndexed { i, capture ->
            val input = AnnotatedString(buildString { repeat(capture.length) { append('x') } })
            val transformed = capture.vt.filter(input)
            assertEquals(
                capture.length,
                transformed.text.length,
                "Capture #$i lost its paired length"
            )
        }
    }
}
