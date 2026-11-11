package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Issue 570: the look of a semantic element could only be changed by adding span styles to
 * the content, and those are written to the document as inline CSS. A heading level had a
 * fixed typography (`2em` bold for H1, and so on) with no way to replace it.
 *
 * `RichTextConfig.headingTextStyles` maps a heading level to a `TextStyle` that is applied
 * when the text is rendered. The document keeps storing the level only, so HTML and Markdown
 * output is the same with and without it, and styles the user adds on top still win.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue570HeadingTextStylesTest {

    private val brandH1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)

    // Rendering

    @Test
    fun `a heading renders with the style set for its level`() {
        val state = stateOf("<h1>Title</h1><p>Body</p>")

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1)

        val style = textStyleOf(state, paragraphIndex = 0)
        assertEquals(28.sp, style.fontSize)
        assertEquals(FontWeight.SemiBold, style.fontWeight)
    }

    @Test
    fun `a level without a style keeps the default typography`() {
        val state = stateOf("<h1>Title</h1><h2>Section</h2>")

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1)

        val style = textStyleOf(state, paragraphIndex = 1)
        assertEquals(1.5.em, style.fontSize)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `what the style leaves unset keeps coming from the default typography`() {
        val state = stateOf("<h1>Title</h1>")

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to TextStyle(color = Color.Red))

        val style = textStyleOf(state, paragraphIndex = 0)
        assertEquals(Color.Red, style.color)
        assertEquals(2.em, style.fontSize)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `a normal paragraph is not styled`() {
        val state = stateOf("<h1>Title</h1><p>Body</p>")

        state.config.headingTextStyles = mapOf(
            HeadingStyle.H1 to brandH1,
            HeadingStyle.Normal to TextStyle(color = Color.Red, lineHeight = 40.sp),
        )

        val style = textStyleOf(state, paragraphIndex = 1)
        assertEquals(TextUnit.Unspecified, style.fontSize)
        assertNull(style.fontWeight)
        assertEquals(Color.Unspecified, style.color)
        assertEquals(TextUnit.Unspecified, paragraphStyleOf(state, paragraphIndex = 1).lineHeight)
    }

    @Test
    fun `the paragraph part of the style applies to the heading paragraph only`() {
        val state = stateOf("<h1>Title</h1><p>Body</p>")

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1.copy(lineHeight = 36.sp))

        assertEquals(36.sp, paragraphStyleOf(state, paragraphIndex = 0).lineHeight)
        assertEquals(TextUnit.Unspecified, paragraphStyleOf(state, paragraphIndex = 1).lineHeight)
    }

    @Test
    fun `an alignment set on the heading wins over the one in the style`() {
        val state = stateOf("<h1 style=\"text-align: center\">Title</h1>")

        state.config.headingTextStyles = mapOf(
            HeadingStyle.H1 to TextStyle(textAlign = TextAlign.End, lineHeight = 36.sp),
        )

        val style = paragraphStyleOf(state, paragraphIndex = 0)
        assertEquals(TextAlign.Center, style.textAlign)
        assertEquals(36.sp, style.lineHeight)
    }

    @Test
    fun `changing the styles re-styles the headings already in the editor`() {
        val state = stateOf("<h1>Title</h1>")
        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1)

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to TextStyle(fontSize = 40.sp))

        val style = textStyleOf(state, paragraphIndex = 0)
        assertEquals(40.sp, style.fontSize)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `clearing the styles restores the default typography`() {
        val state = stateOf("<h1>Title</h1>")
        val before = state.annotatedString
        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1.copy(lineHeight = 36.sp))

        state.config.headingTextStyles = emptyMap()

        assertEquals(before, state.annotatedString)
    }

    @Test
    fun `the default changes nothing`() {
        val state = stateOf("<h1>Title</h1>")

        assertEquals(emptyMap(), state.config.headingTextStyles)
        assertEquals(2.em, textStyleOf(state, paragraphIndex = 0).fontSize)
        assertEquals(FontWeight.Bold, textStyleOf(state, paragraphIndex = 0).fontWeight)
    }

    // Styles the user adds on top of a heading

    @Test
    fun `a color on part of a heading renders on top of the style and is exported`() {
        val html = "<h2>Plain <span style=\"color: rgba(255, 0, 0, 1);\">red</span></h2>"
        val state = stateOf(html)
        val exported = state.toHtml()

        state.config.headingTextStyles = mapOf(
            HeadingStyle.H2 to TextStyle(fontSize = 22.sp, color = Color.Blue),
        )

        val plain = styleOver(state, TextRange(0, 1))
        assertEquals(Color.Blue, plain.color)
        assertEquals(22.sp, plain.fontSize)
        val red = styleOver(state, TextRange(6, 9))
        assertEquals(Color.Red, red.color)
        assertEquals(22.sp, red.fontSize)
        assertEquals(exported, state.toHtml())
        assertEquals(true, "color" in exported)
    }

    @Test
    fun `a font size added to part of a heading wins over the style`() {
        val state = stateOf("<h1>Title</h1>")
        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1)

        state.addSpanStyle(SpanStyle(fontSize = 12.sp), TextRange(0, 2))

        assertEquals(12.sp, styleOver(state, TextRange(0, 2)).fontSize)
        assertEquals(28.sp, styleOver(state, TextRange(2, 5)).fontSize)
        assertEquals(FontWeight.SemiBold, styleOver(state, TextRange(0, 2)).fontWeight)
    }

    // Round trips

    @Test
    fun `html output is the same with and without the styles`() {
        val html = "<h1>Title</h1><h2>Sec<i>tion</i></h2><p>Body</p>"
        val expected = stateOf(html).toHtml()
        val state = stateOf(html)

        state.config.headingTextStyles = mapOf(
            HeadingStyle.H1 to brandH1.copy(lineHeight = 36.sp, textAlign = TextAlign.Center),
            HeadingStyle.H2 to TextStyle(fontWeight = FontWeight.Normal, color = Color.Red),
        )

        assertEquals(expected, state.toHtml())
        assertEquals("<h1>Title</h1>", stateOf("<h1>Title</h1>", brandH1).toHtml())
    }

    @Test
    fun `markdown output is the same with and without the styles`() {
        val markdown = "# Title\n\n## Section\n\nBody"
        val expected = RichTextState().apply { setMarkdown(markdown) }.toMarkdown()
        val state = RichTextState()
        state.config.headingTextStyles = mapOf(
            HeadingStyle.H1 to brandH1,
            HeadingStyle.H2 to TextStyle(fontWeight = FontWeight.Normal),
        )

        state.setMarkdown(markdown)

        assertEquals(expected, state.toMarkdown())
        assertEquals(28.sp, textStyleOf(state, paragraphIndex = 0).fontSize)
        val sectionIndex = state.richParagraphList.indexOfFirst { it.headingStyle == HeadingStyle.H2 }
        assertEquals(FontWeight.Normal, textStyleOf(state, paragraphIndex = sectionIndex).fontWeight)
    }

    @Test
    fun `html loaded after the styles are set renders with them`() {
        val state = RichTextState()
        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to brandH1)

        state.setHtml("<h1>Title</h1>")

        assertEquals(28.sp, textStyleOf(state, paragraphIndex = 0).fontSize)
        assertEquals(FontWeight.SemiBold, textStyleOf(state, paragraphIndex = 0).fontWeight)
    }

    // Editing

    @Test
    fun `changing the level switches to the style of the new level`() {
        val state = stateOf("<h1>Title</h1>", brandH1)
        state.selection = TextRange(1)

        state.setHeadingStyle(HeadingStyle.H2)

        val style = textStyleOf(state, paragraphIndex = 0)
        assertEquals(1.5.em, style.fontSize)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals("<h2>Title</h2>", state.toHtml())
    }

    @Test
    fun `making a paragraph a heading renders it with the style`() {
        val state = stateOf("<p>Title</p>", brandH1)
        state.selection = TextRange(1)

        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals(28.sp, textStyleOf(state, paragraphIndex = 0).fontSize)
        assertEquals("<h1>Title</h1>", state.toHtml())
    }

    @Test
    fun `removing the heading removes the style`() {
        val state = stateOf("<h1>Title</h1>", brandH1.copy(lineHeight = 36.sp))
        state.selection = TextRange(1)

        state.setHeadingStyle(HeadingStyle.Normal)

        val style = textStyleOf(state, paragraphIndex = 0)
        assertEquals(TextUnit.Unspecified, style.fontSize)
        assertNull(style.fontWeight)
        assertEquals(TextUnit.Unspecified, paragraphStyleOf(state, paragraphIndex = 0).lineHeight)
        assertEquals("<p>Title</p>", state.toHtml())
    }

    @Test
    fun `enter inside a heading leaves two headings with the style`() {
        val state = stateOf("<h1>title</h1>", brandH1)

        state.pressEnter(at = 3)

        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.H1), state.richParagraphList.map { it.headingStyle })
        assertEquals(28.sp, textStyleOf(state, paragraphIndex = 0).fontSize)
        assertEquals(28.sp, textStyleOf(state, paragraphIndex = 1).fontSize)
        assertEquals("<h1>tit</h1><h1>le</h1>", state.toHtml())
    }

    @Test
    fun `enter at the end of a heading starts a plain paragraph`() {
        val state = stateOf("<h1>title</h1>", brandH1)

        state.pressEnter(at = 5)
        state.addTextAfterSelection("x")

        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.Normal), state.richParagraphList.map { it.headingStyle })
        val style = textStyleOf(state, paragraphIndex = 1)
        assertEquals(TextUnit.Unspecified, style.fontSize)
        assertNull(style.fontWeight)
    }

    // Lists and copies

    @Test
    fun `the marker of a heading list item follows the style`() {
        val state = stateOf("<h1>Item</h1>", brandH1)
        state.selection = TextRange(1)
        state.toggleUnorderedList()

        val marker = state.richParagraphList.first().type.startRichSpan.textRange
        val style = styleOver(state, marker)
        assertEquals(28.sp, style.fontSize)
        assertEquals(FontWeight.SemiBold, style.fontWeight)
    }

    @Test
    fun `a copy of the state keeps the styles`() {
        val state = stateOf("<h1>Title</h1>", brandH1)

        assertEquals(mapOf(HeadingStyle.H1 to brandH1), state.copy().config.headingTextStyles)
    }

    private fun stateOf(html: String, h1: TextStyle? = null): RichTextState =
        RichTextState().apply {
            if (h1 != null) config.headingTextStyles = mapOf(HeadingStyle.H1 to h1)
            setHtml(html)
        }

    private fun RichTextState.imeBatch(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        reconcileBufferWithModel(buffer)
        val text = buffer.asCharSequence().toString()
        val selection = buffer.selection
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(text, selection)
        handleSelectionChanged(textFieldState.selection, fromGestureObserver = true)
    }

    private fun RichTextState.pressEnter(at: Int) {
        selection = TextRange(at)
        imeBatch {
            replace(at, at, "\n")
            selection = TextRange(at + 1)
        }
    }

    /** The span style rendered over the first character of the paragraph's own text. */
    private fun textStyleOf(state: RichTextState, paragraphIndex: Int): SpanStyle {
        val paragraph = state.richParagraphList[paragraphIndex]
        val start = paragraph.getFirstNonEmptyChild()!!.textRange.min
        return styleOver(state, TextRange(start, start + 1))
    }

    private fun paragraphStyleOf(state: RichTextState, paragraphIndex: Int): ParagraphStyle {
        val paragraph = state.richParagraphList[paragraphIndex]
        val start = paragraph.getFirstNonEmptyChild()!!.textRange.min
        return state.annotatedString.paragraphStyles
            .first { it.start <= start && it.end > start }
            .item
    }

    private fun styleOver(state: RichTextState, range: TextRange): SpanStyle =
        state.annotatedString.spanStyles
            .filter { it.start <= range.min && it.end >= range.max }
            .fold(SpanStyle()) { style, spanRange -> style.merge(spanRange.item) }
}
