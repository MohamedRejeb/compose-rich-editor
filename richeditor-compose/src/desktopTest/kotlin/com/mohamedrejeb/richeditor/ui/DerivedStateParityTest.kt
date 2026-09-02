package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A parity oracle for the state the toolbar reads.
 *
 * The derived values (`currentSpanStyle`, `isUnorderedList`, ...) are cached, recomputed by
 * `handleSelectionChanged`, and therefore able to go sticky-stale: a live editor can hold a value
 * that no longer describes where the caret actually is. A model-only assertion cannot catch that,
 * because it reads the very same cache.
 *
 * The oracle sidesteps the cache entirely. After driving a live editor, the document is serialized,
 * decoded into a FRESH state that has no editing history at all, and the caret is placed at the
 * same offset. The fresh state's derived values are computed from nothing but the document and the
 * caret, so they are the ground truth the live state must match. A mismatch means the live state
 * answered from a stale cache.
 *
 * Two serializers are used, and both must agree: `toRichTextDocument` / `setRichTextDocument`, and
 * `toHtml` / `setHtml`. Two independent decoders reaching the same derived values is a much harder
 * thing to satisfy by accident than one.
 *
 * Oracle validation lives in the three `the oracle agrees on ...` tests: each takes a static shape,
 * never edits it, and sweeps the caret across every offset. They prove both round trips preserve
 * exactly the properties compared here, so a later failure is the live state's fault and not the
 * round trip's.
 *
 * Excluded from the comparison, deliberately:
 *  - `currentParagraphStyle` as a whole. Only `textAlign` survives with certainty; line height and
 *    text indent are populated from config defaults and paragraph type at decode time, so comparing
 *    the whole object would test the parsers, not the caret.
 *  - Staged (not yet typed) styles. `toggleSpanStyle` at a collapsed caret stages a style that
 *    exists only in the live state and has no document representation, so parity is asserted after
 *    the staged style has been materialized by a keystroke, never before.
 *  - The HTML leg only, for a document that ends in an empty paragraph. `toHtml` writes a trailing
 *    empty paragraph as `<br>`, which `setHtml` reads back as two blank paragraphs, so the round
 *    trip grows the document by one line and its offsets no longer line up. Call sites that hit
 *    that shape pass `expectHtmlLeg = false` and assert the round trip really is lossy there, so
 *    the exclusion cannot outlive the defect. The document leg still runs, and it is lossless.
 */
@OptIn(ExperimentalTestApi::class)
class DerivedStateParityTest {

    /**
     * Compares every derived value the toolbar reads against a fresh state decoded from the live
     * document and parked at the same caret.
     *
     * @param expectHtmlLeg whether the html round trip is expected to reproduce the document. Pass
     * false only for a document ending in an empty paragraph, where `toHtml` is known to be lossy;
     * the call then asserts the loss really is there, so the exemption expires with the defect.
     */
    private fun assertDerivedParity(
        live: RichTextState,
        label: String,
        expectHtmlLeg: Boolean = true,
    ) {
        val selection = live.selection

        val fromDocument = RichTextState()
        fromDocument.setRichTextDocument(live.toRichTextDocument())
        assertEquals(
            live.annotatedString.text,
            fromDocument.annotatedString.text,
            "$label: the document round trip must reproduce the document",
        )
        compareDerived(live, fromDocument, selection, "$label [document]")

        val fromHtml = RichTextState()
        fromHtml.setHtml(live.toHtml())
        if (expectHtmlLeg) {
            assertEquals(
                live.annotatedString.text,
                fromHtml.annotatedString.text,
                "$label: the html round trip must reproduce the document",
            )
            compareDerived(live, fromHtml, selection, "$label [html]")
        } else {
            assertNotEquals(
                live.annotatedString.text,
                fromHtml.annotatedString.text,
                "$label: the html leg was waived but the round trip is faithful; drop the waiver",
            )
        }
    }

    private fun compareDerived(
        live: RichTextState,
        fresh: RichTextState,
        selection: TextRange,
        label: String,
    ) {
        // Two writes so the fresh state always runs a full selection pass, even when the caret the
        // live state holds is where the decode already left it.
        fresh.selection = TextRange(0)
        fresh.selection = selection
        assertEquals(selection, fresh.selection, "$label: the fresh state must accept the caret")

        assertEquals(
            fresh.currentSpanStyle,
            live.currentSpanStyle,
            "$label: currentSpanStyle at $selection",
        )
        assertEquals(
            fresh.currentRichSpanStyle,
            live.currentRichSpanStyle,
            "$label: currentRichSpanStyle at $selection",
        )
        assertEquals(
            fresh.isUnorderedList,
            live.isUnorderedList,
            "$label: isUnorderedList at $selection",
        )
        assertEquals(fresh.isOrderedList, live.isOrderedList, "$label: isOrderedList at $selection")
        assertEquals(fresh.isLink, live.isLink, "$label: isLink at $selection")
        assertEquals(fresh.isCodeSpan, live.isCodeSpan, "$label: isCodeSpan at $selection")
        assertEquals(
            fresh.currentParagraphStyle.textAlign,
            live.currentParagraphStyle.textAlign,
            "$label: textAlign at $selection",
        )
    }

    /** Validation sweep: a static document, never edited, compared at every caret offset. */
    private fun sweepStaticShape(html: String, label: String) {
        val state = RichTextState()
        state.setHtml(html)
        for (offset in 0..state.annotatedString.text.length) {
            state.selection = TextRange(offset)
            assertDerivedParity(state, "$label @$offset")
        }
    }

    // --- oracle validation on known good static shapes ---

    @Test
    fun `the oracle agrees on a bold run at every caret offset`() {
        sweepStaticShape("<p>Hello <b>bold</b> world</p>", "bold run")
    }

    @Test
    fun `the oracle agrees on a two item unordered list at every caret offset`() {
        sweepStaticShape("<ul><li>one</li><li>two</li></ul>", "unordered list")
    }

    @Test
    fun `the oracle agrees on a link and a code span at every caret offset`() {
        sweepStaticShape(
            "<p style=\"text-align: center;\">" +
                "<a href=\"https://example.com\">link</a> and <code>code</code></p>",
            "link and code",
        )
    }

    /**
     * Headings are the shape where the oracle earns its keep: the level's bold and font size live
     * on the spans, not on the paragraph, so an edit can leave them behind. This proves both round
     * trips carry them faithfully when nothing is edited, which is what makes a divergence after an
     * edit attributable to the edit.
     */
    @Test
    fun `the oracle agrees on a heading at every caret offset`() {
        sweepStaticShape("<h1>title</h1><p>body <i>text</i> here</p>", "heading")
    }

    // --- the oracle applied after real editing ---

    @Test
    fun `typing inside bold text keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p>Hello <b>bold</b> world</p>")
        waitForIdle()
        state.selection = TextRange(8)
        waitForIdle()

        repeat(3) { index ->
            onNodeWithTag("editor").performTextInput("x")
            waitForIdle()
            assertDerivedParity(state, "typing inside bold, keystroke ${index + 1}")
        }
    }

    @Test
    fun `toggling bold then typing keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        // A collapsed caret only stages the style; parity is asserted once a keystroke has
        // materialized it into the document.
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        repeat(3) { index ->
            onNodeWithTag("editor").performTextInput("b")
            waitForIdle()
            assertDerivedParity(state, "staged bold, keystroke ${index + 1}")
        }
    }

    @Test
    fun `the enter that exits a list keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("item")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()

        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertDerivedParity(state, "the new empty list item")

        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertDerivedParity(state, "the list exit", expectHtmlLeg = false)

        onNodeWithTag("editor").performTextInput("plain")
        waitForIdle()
        assertDerivedParity(state, "typing after the list exit")
    }

    @Test
    fun `a list toggle then typing keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("one")
        waitForIdle()
        state.toggleOrderedList()
        waitForIdle()
        assertDerivedParity(state, "ordered list toggled on")

        repeat(3) { index ->
            onNodeWithTag("editor").performTextInput("y")
            waitForIdle()
            assertDerivedParity(state, "typing in an ordered list, keystroke ${index + 1}")
        }
    }

    @Test
    fun `a backspace merge keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.toggleUnorderedList()
        waitForIdle()
        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("b")
        waitForIdle()
        assertDerivedParity(state, "the exited line")

        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertDerivedParity(state, "after deleting the char", expectHtmlLeg = false)

        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertDerivedParity(state, "after merging back into the list item")
    }

    @Test
    fun `undo and redo keep the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.selection = TextRange(0, 5)
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()
        assertDerivedParity(state, "bold applied")

        state.history.undo()
        waitForIdle()
        assertDerivedParity(state, "after undo")

        state.history.redo()
        waitForIdle()
        assertDerivedParity(state, "after redo")
    }

    @Test
    fun `undo and redo of a list toggle keep the derived state in parity`() =
        runDesktopComposeUiTest {
            lateinit var state: RichTextState
            setContent {
                state = rememberRichTextState()
                BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
            }
            onNodeWithTag("editor").performTextInput("item")
            waitForIdle()
            state.toggleUnorderedList()
            waitForIdle()
            assertDerivedParity(state, "list on")

            state.history.undo()
            waitForIdle()
            assertDerivedParity(state, "list toggle undone")

            state.history.redo()
            waitForIdle()
            assertDerivedParity(state, "list toggle redone")
        }

    /**
     * The paste path: the clipboard manager stashes the html and the plain text it is about to
     * hand the platform, then the platform's insertion arrives as a buffer change that
     * `applyChangeList` recognizes as that paste. Driving it this way exercises the real branch,
     * which runs under `skipTextFieldStateSync` and therefore has its own stale-selection hazard.
     */
    @Test
    fun `a paste through the pipeline keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("start ")
        waitForIdle()

        state.pendingClipboardHtml = "<p>pasted <b>bold</b></p>"
        state.pendingClipboardPlainText = "pasted bold"
        onNodeWithTag("editor").performTextInput("pasted bold")
        waitForIdle()

        assertEquals("start pasted bold", state.toText(), "the paste should have landed")
        assertDerivedParity(state, "right after the paste")

        onNodeWithTag("editor").performTextInput("z")
        waitForIdle()
        assertDerivedParity(state, "typing after the paste")
    }

    @Test
    fun `selection jumps keep the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml(
            "<p>plain</p><ul><li>an <b>item</b></li></ul>" +
                "<p><a href=\"https://example.com\">link</a> and <code>code</code></p>"
        )
        waitForIdle()

        val length = state.annotatedString.text.length
        listOf(0, 3, length / 2, length - 1, length, 1, length - 4, 6).forEach { offset ->
            state.selection = TextRange(offset.coerceIn(0, length))
            waitForIdle()
            assertDerivedParity(state, "selection jump")
        }
    }

    @Test
    fun `typing at each jump target keeps the derived state in parity`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p>plain</p><ul><li>an <b>item</b></li></ul>")
        waitForIdle()

        listOf(2, 9, 13).forEach { offset ->
            state.selection = TextRange(offset.coerceIn(0, state.annotatedString.text.length))
            waitForIdle()
            onNodeWithTag("editor").performTextInput("q")
            waitForIdle()
            assertDerivedParity(state, "typing after a jump to $offset")
        }
    }
}
