package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.ListMarkerStyleBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Issue 440: there was no way to color the bullets and numbers of a list on their own; a
 * marker could only inherit from the item's text or use the default style.
 *
 * `RichTextConfig.listMarkerStyle` is an editor-wide style applied on top of whatever
 * [ListMarkerStyleBehavior] gives the marker. It styles the marker only and is not part of
 * the document.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue440ListMarkerStyleTest {

    @Test
    fun `the marker style colors the bullet and leaves the text alone`() {
        val state = stateOf("<ul><li>Item</li></ul>")

        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        assertEquals(Color.Red, markerStyleOf(state).color)
        assertEquals(Color.Unspecified, textStyleOf(state).color)
    }

    @Test
    fun `the marker style applies to ordered list numbers`() {
        val state = stateOf("<ol><li>One</li><li>Two</li></ol>")

        state.config.listMarkerStyle = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)

        state.richParagraphList.indices.forEach { index ->
            val style = markerStyleOf(state, paragraphIndex = index)
            assertEquals(Color.Red, style.color, "marker $index")
            assertEquals(FontWeight.Bold, style.fontWeight, "marker $index")
        }
    }

    @Test
    fun `the marker style wins over what the marker inherits from the text`() {
        val state = stateOf("<ul><li><span style=\"color: #0000ff; font-size: 20px\">Item</span></li></ul>")

        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        val style = markerStyleOf(state)
        assertEquals(Color.Red, style.color)
        assertEquals(20.sp, style.fontSize, "what the marker style leaves unset is still inherited")
    }

    @Test
    fun `the marker style applies with the always default behavior`() {
        val state = stateOf("<ul><li><b>Item</b></li></ul>")
        state.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault

        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        val style = markerStyleOf(state)
        assertEquals(Color.Red, style.color)
        assertNull(style.fontWeight)
    }

    @Test
    fun `a decoration in the marker style is applied as given`() {
        val state = stateOf("<ul><li>Item</li></ul>")

        state.config.listMarkerStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        assertEquals(TextDecoration.Underline, markerStyleOf(state).textDecoration)
    }

    @Test
    fun `the default marker style changes nothing`() {
        val state = stateOf("<ul><li><b>Item</b></li></ul>")

        assertEquals(SpanStyle(), state.config.listMarkerStyle)
        assertEquals(Color.Unspecified, markerStyleOf(state).color)
        assertEquals(FontWeight.Bold, markerStyleOf(state).fontWeight)
    }

    @Test
    fun `resetting the marker style restores the inherited marker`() {
        val state = stateOf("<ul><li>Item</li></ul>")
        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        state.config.listMarkerStyle = SpanStyle()

        assertEquals(Color.Unspecified, markerStyleOf(state).color)
    }

    @Test
    fun `the marker style is not written to html`() {
        val state = stateOf("<ul><li>Item</li></ul>")
        val html = state.toHtml()

        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        assertEquals(html, state.toHtml())
    }

    @Test
    fun `a copy of the state keeps the marker style`() {
        val state = stateOf("<ul><li>Item</li></ul>")
        state.config.listMarkerStyle = SpanStyle(color = Color.Red)

        assertEquals(SpanStyle(color = Color.Red), state.copy().config.listMarkerStyle)
    }

    private fun stateOf(html: String): RichTextState = RichTextState().apply { setHtml(html) }

    private fun markerStyleOf(state: RichTextState, paragraphIndex: Int = 0): SpanStyle {
        val marker = state.richParagraphList[paragraphIndex].type.startRichSpan.textRange
        return styleOver(state, marker)
    }

    private fun textStyleOf(state: RichTextState): SpanStyle {
        val markerEnd = state.richParagraphList.first().type.startRichSpan.textRange.max
        return styleOver(state, TextRange(markerEnd, markerEnd + 1))
    }

    private fun styleOver(state: RichTextState, range: TextRange): SpanStyle =
        state.annotatedString.spanStyles
            .filter { it.start <= range.min && it.end >= range.max }
            .fold(SpanStyle()) { style, spanRange -> style.merge(spanRange.item) }
}
