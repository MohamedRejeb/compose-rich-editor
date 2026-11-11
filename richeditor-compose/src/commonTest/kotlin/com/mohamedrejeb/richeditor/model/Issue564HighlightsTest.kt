package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Issue #564: an app could not mark ranges of the text (the matches of a find-in-text feature)
 * without writing a background span into the document, which then showed up in the exports, in
 * the undo history and on the clipboard.
 *
 * [RichTextState.highlights] holds presentation-only style ranges. They are projected over the
 * text when it is rendered and are kept out of the paragraph tree, so nothing that reads the
 * document can see them. This suite pins that separation and the range rules: ranges are
 * offsets in the current text, clamped to it, and dropped when nothing is left.
 */
@OptIn(ExperimentalRichTextApi::class, ExperimentalFoundationApi::class)
class Issue564HighlightsTest {

    private val match = SpanStyle(background = Color.Yellow)
    private val focusedMatch = SpanStyle(background = Color.Red, color = Color.White)

    private fun stateWithHtml(html: String = "<p><b>Hello</b> World</p><p>Hello again</p>"): RichTextState =
        RichTextState().apply { setHtml(html) }

    private fun range(style: SpanStyle, start: Int, end: Int) = AnnotatedString.Range(style, start, end)

    @Test
    fun `a state has no highlights by default`() {
        assertTrue(RichTextState().highlights.isEmpty())
    }

    @Test
    fun `the list given to highlights is copied`() {
        val state = stateWithHtml()
        val source = mutableListOf(RichTextHighlight(TextRange(0, 5), match))

        state.highlights = source
        source.clear()

        assertEquals(listOf(RichTextHighlight(TextRange(0, 5), match)), state.highlights)
    }

    @Test
    fun `highlights leave the document and its exports unchanged`() {
        val state = stateWithHtml()
        val annotatedString = state.annotatedString
        val html = state.toHtml()
        val markdown = state.toMarkdown()
        val text = state.toText()
        val document = state.toRichTextDocument()

        state.highlights = listOf(
            RichTextHighlight(TextRange(0, 5), match),
            RichTextHighlight(TextRange(12, 17), focusedMatch),
        )

        assertEquals(annotatedString, state.annotatedString)
        assertEquals(html, state.toHtml())
        assertEquals(markdown, state.toMarkdown())
        assertEquals(text, state.toText())
        assertEquals(document, state.toRichTextDocument())
    }

    @Test
    fun `exporting a highlighted range exports it without the highlight`() {
        val state = stateWithHtml()
        val range = TextRange(0, 11)
        val html = state.toHtml(range)
        val document = state.toRichTextDocument(range)

        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))

        assertEquals(html, state.toHtml(range))
        assertEquals(document, state.toRichTextDocument(range))
    }

    @Test
    fun `highlights are not recorded in the undo history`() {
        val state = stateWithHtml()
        assertFalse(state.history.canUndo)

        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
        state.highlights = emptyList()

        assertFalse(state.history.canUndo)
        assertFalse(state.history.canRedo)
    }

    @Test
    fun `undo keeps the highlights`() {
        val state = stateWithHtml()
        val highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
        state.highlights = highlights
        state.selection = TextRange(6, 11)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        assertTrue(state.history.canUndo)

        state.history.undo()

        assertEquals(highlights, state.highlights)
    }

    @Test
    fun `a highlight under the caret does not change currentSpanStyle`() {
        val state = stateWithHtml()
        state.selection = TextRange(8)
        val before = state.currentSpanStyle

        state.highlights = listOf(RichTextHighlight(TextRange(6, 11), focusedMatch))
        state.selection = TextRange(9)

        assertEquals(before, state.currentSpanStyle)
        assertEquals(Color.Unspecified, state.currentSpanStyle.background)
    }

    @Test
    fun `text typed inside a highlight does not take the highlight style`() {
        val state = stateWithHtml("<p>Hello World</p>")
        state.highlights = listOf(RichTextHighlight(TextRange(0, 11), match))
        val buffer = TextFieldState(state.annotatedString.text, TextRange(5)).toTextFieldBuffer()

        buffer.insert(5, "!")
        state.applyChangeList(buffer)

        assertEquals("<p>Hello! World</p>", state.toHtml())
    }

    @Test
    fun `a copy of the state has no highlights`() {
        val state = stateWithHtml()
        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))

        val copy = state.copy()

        assertTrue(copy.highlights.isEmpty())
        assertEquals(state.toHtml(), copy.toHtml())
    }

    @Test
    fun `the saver does not save highlights`() {
        val state = stateWithHtml()
        val savedWithout = with(RichTextState.Saver) { SaverScope { true }.save(state) }
        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))

        @Suppress("UNCHECKED_CAST")
        val saver = RichTextState.Saver as androidx.compose.runtime.saveable.Saver<RichTextState, Any>
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        val restored = saver.restore(saved)!!

        assertEquals(savedWithout, saved)
        assertTrue(restored.highlights.isEmpty())
        assertEquals(state.toHtml(), restored.toHtml())
    }

    @Test
    fun `replacing the text keeps the highlights as given`() {
        val state = stateWithHtml()
        val highlights = listOf(RichTextHighlight(TextRange(12, 17), match))
        state.highlights = highlights

        state.setText("Hi")

        assertEquals(highlights, state.highlights)
    }

    @Test
    fun `highlights become style ranges in list order`() {
        val highlights = listOf(
            RichTextHighlight(TextRange(0, 5), match),
            RichTextHighlight(TextRange(3, 8), focusedMatch),
        )

        assertEquals(
            listOf(range(match, 0, 5), range(focusedMatch, 3, 8)),
            highlights.toSpanStyleRanges(textLength = 20),
        )
    }

    @Test
    fun `a reversed range is read as its min and max`() {
        val highlights = listOf(RichTextHighlight(TextRange(8, 3), match))

        assertEquals(listOf(range(match, 3, 8)), highlights.toSpanStyleRanges(textLength = 20))
    }

    @Test
    fun `a range past the end of the text is clamped`() {
        val highlights = listOf(RichTextHighlight(TextRange(3, 50), match))

        assertEquals(listOf(range(match, 3, 10)), highlights.toSpanStyleRanges(textLength = 10))
    }

    @Test
    fun `collapsed and out of text ranges are dropped`() {
        val highlights = listOf(
            RichTextHighlight(TextRange(4), match),
            RichTextHighlight(TextRange(10, 12), match),
            RichTextHighlight(TextRange(30, 40), match),
        )

        assertTrue(highlights.toSpanStyleRanges(textLength = 10).isEmpty())
        assertTrue(highlights.toSpanStyleRanges(textLength = 0).isEmpty())
    }

    @Test
    fun `a highlight background is masked under the selection`() {
        val highlights = listOf(RichTextHighlight(TextRange(2, 10), focusedMatch))
        val masked = focusedMatch.copy(background = Color.Transparent)

        assertEquals(
            listOf(range(focusedMatch, 2, 4), range(masked, 4, 6), range(focusedMatch, 6, 10)),
            highlights.toSpanStyleRanges(textLength = 20, selection = TextRange(6, 4)),
        )
        assertEquals(
            listOf(range(masked, 2, 10)),
            highlights.toSpanStyleRanges(textLength = 20, selection = TextRange(0, 20)),
        )
        assertEquals(
            listOf(range(focusedMatch, 2, 10)),
            highlights.toSpanStyleRanges(textLength = 20, selection = TextRange(12, 15)),
        )
    }

    @Test
    fun `a highlight without a background is not split by the selection`() {
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        val highlights = listOf(RichTextHighlight(TextRange(2, 10), bold))

        assertEquals(
            listOf(range(bold, 2, 10)),
            highlights.toSpanStyleRanges(textLength = 20, selection = TextRange(4, 6)),
        )
    }

    @Test
    fun `the read-only text carries the highlights after the document styles`() {
        val state = stateWithHtml("<p><span style=\"background: blue\">Hello</span> World</p>")
        val highlights = listOf(RichTextHighlight(TextRange(0, 50), match))

        val rendered = state.annotatedString.withHighlights(highlights)

        assertEquals(state.annotatedString.text, rendered.text)
        assertEquals(state.annotatedString.paragraphStyles, rendered.paragraphStyles)
        assertEquals(
            state.annotatedString.spanStyles + range(match, 0, 11),
            rendered.spanStyles,
        )
        assertEquals(state.annotatedString, state.annotatedString.withHighlights(emptyList()))
    }

    @Test
    fun `styling the output with ranges outside the text does not throw`() {
        val state = stateWithHtml("<p>Hi</p>")
        state.highlights = listOf(
            RichTextHighlight(TextRange(0, 100), match),
            RichTextHighlight(TextRange(50, 60), focusedMatch),
        )
        val buffer = TextFieldState(state.annotatedString.text).toTextFieldBuffer()

        state.applyRichTextStyles(buffer)

        assertEquals("Hi", buffer.asCharSequence().toString())
    }

    @Test
    fun `equal highlights are equal`() {
        assertEquals(RichTextHighlight(TextRange(0, 5), match), RichTextHighlight(TextRange(0, 5), match))
        assertEquals(
            RichTextHighlight(TextRange(0, 5), match).hashCode(),
            RichTextHighlight(TextRange(0, 5), match).hashCode(),
        )
        assertFalse(RichTextHighlight(TextRange(0, 5), match) == RichTextHighlight(TextRange(0, 6), match))
    }
}
