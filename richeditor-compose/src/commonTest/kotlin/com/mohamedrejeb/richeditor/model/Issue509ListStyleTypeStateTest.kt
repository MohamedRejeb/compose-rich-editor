package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression pins for #509: a list can carry its own marker style, set from the editor
 * with [RichTextState.setOrderedListStyleType] and [RichTextState.setUnorderedListStyleType].
 * The style applies to the whole list at the selection, is the one the markers render
 * with, is independent of the editor-wide config, and survives new items, copies, undo
 * and the document round trip.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue509ListStyleTypeStateTest {

    private fun RichTextState.markers(): List<String> =
        richParagraphList.map { it.type.startRichSpan.text }

    private fun RichTextState.orderedStyleTypes(): List<OrderedListStyleType?> =
        richParagraphList.map { (it.type as? OrderedList)?.styleTypeOverride }

    private fun RichTextState.placeCursorIn(text: String) {
        selection = TextRange(annotatedString.text.indexOf(text) + 1)
    }

    @Test
    fun `setting the ordered style applies to the whole list at the selection`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li><li>c</li></ol><p>p</p><ol><li>d</li></ol>") }
        state.placeCursorIn("b")

        state.setOrderedListStyleType(OrderedListStyleType.UpperRoman)

        assertEquals(listOf("I. ", "II. ", "III. ", "", "1. "), state.markers())
        assertEquals(
            listOf(OrderedListStyleType.UpperRoman, OrderedListStyleType.UpperRoman, OrderedListStyleType.UpperRoman, null, null),
            state.orderedStyleTypes(),
        )
    }

    @Test
    fun `setting the unordered style applies to the whole list at the selection`() {
        val state = RichTextState().apply { setHtml("<ul><li>a</li><li>b</li></ul>") }
        state.placeCursorIn("a")

        state.setUnorderedListStyleType(UnorderedListStyleType.Square)

        assertEquals(listOf("▪ ", "▪ "), state.markers())
    }

    @Test
    fun `the raw text and the selection follow a marker whose width changes`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li><li>c</li><li>d</li></ol>") }
        state.placeCursorIn("d")
        val textBefore = state.annotatedString.text

        state.setOrderedListStyleType(OrderedListStyleType.LowerRoman)

        assertEquals("i. a ii. b iii. c iv. d", state.annotatedString.text)
        assertEquals(state.annotatedString.text.indexOf("d") + 1, state.selection.start)
        assertEquals(textBefore.length + 4, state.annotatedString.text.length)
    }

    @Test
    fun `an empty item in the list is restyled with the others`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li></li><li>c</li></ol>") }
        state.placeCursorIn("c")

        state.setOrderedListStyleType(OrderedListStyleType.LowerRoman)

        assertEquals("i. a ii.  iii. c", state.annotatedString.text)
        assertEquals(state.annotatedString.text.length, state.selection.start)
    }

    @Test
    fun `a nested list is its own list and keeps the outer list unchanged`() {
        val state = RichTextState().apply {
            setHtml("<ol><li>a<ol><li>b</li><li>c</li></ol></li><li>d</li></ol>")
        }
        state.placeCursorIn("c")

        state.setOrderedListStyleType(OrderedListStyleType.UpperAlpha)

        assertEquals(listOf("1. ", "A. ", "B. ", "2. "), state.markers())
    }

    @Test
    fun `an unordered list splits an ordered list at the same level into two lists`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol><ul><li>b</li></ul><ol><li>c</li></ol>") }
        state.placeCursorIn("a")

        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        assertEquals(listOf("a. ", "• ", "1. "), state.markers())
    }

    @Test
    fun `a selection spanning two lists styles both`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol><p>p</p><ol><li>b</li></ol>") }
        state.selection = TextRange(state.annotatedString.text.indexOf("a"), state.annotatedString.text.indexOf("b") + 1)

        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        assertEquals(listOf("a. ", "", "a. "), state.markers())
    }

    @Test
    fun `setting a style outside a list does nothing`() {
        val state = RichTextState().apply { setHtml("<p>p</p><ol><li>a</li></ol>") }
        state.placeCursorIn("p")

        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        assertEquals(listOf("", "1. "), state.markers())
        assertFalse(state.history.canUndo)
    }

    @Test
    fun `setting null returns the list to the config style`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li></ol>") }
        state.config.orderedListStyleType = OrderedListStyleType.UpperAlpha
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.LowerRoman)
        assertEquals(listOf("i. ", "ii. "), state.markers())

        state.setOrderedListStyleType(null)

        assertEquals(listOf("A. ", "B. "), state.markers())
        assertEquals(listOf(null, null), state.orderedStyleTypes())
    }

    @Test
    fun `changing the config afterwards does not change a list with an explicit style`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol><p>p</p><ol start=\"5\"><li>b</li></ol>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        state.config.orderedListStyleType = OrderedListStyleType.UpperRoman

        assertEquals(listOf("a. ", "", "V. "), state.markers())
    }

    @Test
    fun `an explicit style ignores the level based config style`() {
        val state = RichTextState().apply { setHtml("<ul><li>a<ul><li>b</li></ul></li></ul>") }
        state.placeCursorIn("b")

        state.setUnorderedListStyleType(UnorderedListStyleType.Disc)

        assertEquals(listOf("• ", "• "), state.markers())
    }

    @Test
    fun `the current style type reflects the list at the selection`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol><p>p</p><ol><li>b</li></ol><ul><li>c</li></ul>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.UpperAlpha)
        state.placeCursorIn("c")
        state.setUnorderedListStyleType(UnorderedListStyleType.Circle)

        state.placeCursorIn("a")
        assertEquals(OrderedListStyleType.UpperAlpha, state.currentOrderedListStyleType)
        assertNull(state.currentUnorderedListStyleType)

        state.placeCursorIn("b")
        assertNull(state.currentOrderedListStyleType)

        state.placeCursorIn("c")
        assertEquals(UnorderedListStyleType.Circle, state.currentUnorderedListStyleType)
        assertNull(state.currentOrderedListStyleType)
    }

    @Test
    fun `a new item created with enter keeps the list style`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        state.addTextAfterSelection("\n")

        assertEquals(listOf("a. ", "b. "), state.markers())
    }

    @Test
    fun `a paragraph turned into a list item joins the style of the list above it`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li></ol><p>c</p>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)
        state.placeCursorIn("c")

        state.toggleOrderedList()

        assertEquals(listOf("a. ", "b. ", "c. "), state.markers())
    }

    @Test
    fun `a paragraph turned into a bullet joins the style of the list above it`() {
        val state = RichTextState().apply { setHtml("<ul><li>a</li></ul><p>b</p>") }
        state.placeCursorIn("a")
        state.setUnorderedListStyleType(UnorderedListStyleType.Square)
        state.placeCursorIn("b")

        state.toggleUnorderedList()

        assertEquals(listOf("▪ ", "▪ "), state.markers())
    }

    @Test
    fun `typing in the list keeps the style`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li></ol>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.UpperRoman)

        state.addTextAfterSelection("x")

        assertEquals("I. ax II. b", state.annotatedString.text)
    }

    @Test
    fun `changing the style is one undo step`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li><li>b</li></ol>") }
        state.placeCursorIn("a")

        state.setOrderedListStyleType(OrderedListStyleType.UpperRoman)
        assertEquals(listOf("I. ", "II. "), state.markers())

        assertTrue(state.history.undo())
        assertEquals(listOf("1. ", "2. "), state.markers())
        assertFalse(state.history.canUndo)

        assertTrue(state.history.redo())
        assertEquals(listOf("I. ", "II. "), state.markers())
    }

    @Test
    fun `the style survives a copy of the paragraph`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol><ul><li>b</li></ul>") }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)
        state.placeCursorIn("b")
        state.setUnorderedListStyleType(UnorderedListStyleType.Circle)

        val orderedCopy = state.richParagraphList[0].copy().type as OrderedList
        val unorderedCopy = state.richParagraphList[1].copy().type as UnorderedList

        assertEquals(OrderedListStyleType.LowerAlpha, orderedCopy.styleTypeOverride)
        assertEquals("a. ", orderedCopy.startRichSpan.text)
        assertEquals(UnorderedListStyleType.Circle, unorderedCopy.styleTypeOverride)
        assertEquals("◦ ", unorderedCopy.startRichSpan.text)
    }

    @Test
    fun `the style survives a document round trip`() {
        val state = RichTextState().apply {
            setHtml("<ol><li>a<ul><li>b</li></ul></li></ol><p>p</p><ol><li>c</li></ol>")
        }
        state.placeCursorIn("a")
        state.setOrderedListStyleType(OrderedListStyleType.UpperRoman)
        state.placeCursorIn("b")
        state.setUnorderedListStyleType(UnorderedListStyleType.Square)

        val reloaded = RichTextState().apply { setRichTextDocument(state.toRichTextDocument()) }

        assertEquals(listOf("I. ", "▪ ", "", "1. "), reloaded.markers())
        assertEquals(state.toHtml(), reloaded.toHtml())
    }

    @Test
    fun `the style is not applied when the list feature is off`() {
        val state = RichTextState().apply { setHtml("<ol><li>a</li></ol>") }
        state.placeCursorIn("a")
        state.config.features = RichTextFeature.All - RichTextFeature.OrderedList

        state.setOrderedListStyleType(OrderedListStyleType.LowerAlpha)

        assertEquals(listOf("1. "), state.markers())
        assertFalse(state.history.canUndo)
    }
}
