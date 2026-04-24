package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for issue #626 - copied plain text loses line
 * breaks when the selection spans multiple paragraphs or line-break
 * ([isFromLineBreak]) paragraphs produced from HTML `<br>`.
 *
 * The clipboard plain-text payload is produced by
 * [RichTextState.toText] with the copy selection. When the text
 * lacks `\n` between paragraphs, pasting into a plain-text target
 * (e.g. a notes app) collapses everything onto a single line.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateCopyLineBreaksTest {

    @Test
    fun testCopyAcrossParagraphsPreservesLineBreaks() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p><p>Third paragraph</p>")
        val text = state.textFieldValue.text

        val copied = state.toText(TextRange(0, text.length))

        assertEquals(2, copied.count { it == '\n' }, "Three paragraphs should be separated by two newlines: <$copied>")
        assertTrue(copied.contains("First"))
        assertTrue(copied.contains("Second"))
        assertTrue(copied.contains("Third"))
    }

    @Test
    fun testCopyAcrossStyledParagraphsPreservesLineBreaks() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>First paragraph</b></p>" +
                "<p><i>Second paragraph</i></p>" +
                "<p><u>Third paragraph</u></p>"
        )
        val text = state.textFieldValue.text

        val copied = state.toText(TextRange(0, text.length))

        assertEquals(
            "First paragraph\nSecond paragraph\nThird paragraph",
            copied,
        )
    }

    @Test
    fun testCopyAcrossBrLineBreaksPreservesNewlines() {
        // <br> inside a <p> produces sibling paragraphs with isFromLineBreak = true
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two<br>Line three</p>")
        val text = state.textFieldValue.text

        val copied = state.toText(TextRange(0, text.length))

        assertEquals(
            "Line one\nLine two\nLine three",
            copied,
        )
    }

    @Test
    fun testCopyMidRangeAcrossParagraphsPreservesLineBreak() {
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p>")
        val text = state.textFieldValue.text

        // Select "paragraph\nSecond"
        val start = text.indexOf("paragraph")
        val end = text.indexOf("Second") + "Second".length
        val copied = state.toText(TextRange(start, end))

        assertEquals("paragraph\nSecond", copied)
    }

    @Test
    fun testCopyAcrossOrderedListPreservesLineBreaks() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li><li>Second</li><li>Third</li></ol>")
        val text = state.textFieldValue.text

        val copied = state.toText(TextRange(0, text.length))

        assertEquals(2, copied.count { it == '\n' }, "List items should be separated by newlines: <$copied>")
    }

    @Test
    fun testCopyAcrossBrParagraphsWithStylesPreservesLineBreaks() {
        val state = RichTextState()
        state.setHtml(
            "<p><b>Bold line</b><br><i>Italic line</i><br><u>Underline line</u></p>"
        )
        val text = state.textFieldValue.text

        val copied = state.toText(TextRange(0, text.length))

        assertEquals(
            "Bold line\nItalic line\nUnderline line",
            copied,
        )
    }

    @Test
    fun testCopyAcrossParagraphsProducesHtmlWithBlockBreaks() {
        // Clipboard consumers that read the HTML payload (e.g. Android's
        // Html.fromHtml in receiving apps) rely on block-level tags to insert
        // line breaks. Ensure the HTML preserves paragraph structure.
        val state = RichTextState()
        state.setHtml("<p>First paragraph</p><p>Second paragraph</p>")
        val text = state.textFieldValue.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(
            html.contains("<p") || html.contains("<br"),
            "Copied HTML should contain block or line-break tags to preserve paragraphs: <$html>",
        )
    }
}
