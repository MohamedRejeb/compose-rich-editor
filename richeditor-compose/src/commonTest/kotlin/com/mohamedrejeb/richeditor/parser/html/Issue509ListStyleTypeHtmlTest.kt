package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Regression pins for #509: the CSS `list-style-type` of a list was dropped on import and
 * never written on export, so a lettered or roman list came back as a plain numbered one.
 * The type is now kept per list item, read from `<ol>`, `<ul>` and `<li>`, and written on
 * the tag that opens the list group.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue509ListStyleTypeHtmlTest {

    private fun RichTextState.orderedStyleTypes(): List<OrderedListStyleType?> =
        richParagraphList.map { (it.type as OrderedList).styleTypeOverride }

    private fun RichTextState.unorderedStyleTypes(): List<UnorderedListStyleType?> =
        richParagraphList.map { (it.type as UnorderedList).styleTypeOverride }

    private fun RichTextState.markers(): List<String> =
        richParagraphList.map { it.type.startRichSpan.text }

    @Test
    fun `list-style-type on ol is read on every item of the list`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: lower-alpha"><li>a</li><li>b</li></ol>""")
        }

        assertEquals(
            listOf<OrderedListStyleType?>(OrderedListStyleType.LowerAlpha, OrderedListStyleType.LowerAlpha),
            state.orderedStyleTypes(),
        )
        assertEquals(listOf("a. ", "b. "), state.markers())
    }

    @Test
    fun `every ordered keyword maps to its style type`() {
        val expected = mapOf(
            "decimal" to OrderedListStyleType.Decimal,
            "lower-alpha" to OrderedListStyleType.LowerAlpha,
            "lower-latin" to OrderedListStyleType.LowerAlpha,
            "upper-alpha" to OrderedListStyleType.UpperAlpha,
            "upper-latin" to OrderedListStyleType.UpperAlpha,
            "lower-roman" to OrderedListStyleType.LowerRoman,
            "upper-roman" to OrderedListStyleType.UpperRoman,
            "arabic-indic" to OrderedListStyleType.ArabicIndic,
        )

        expected.forEach { (keyword, styleType) ->
            val state = RichTextState().apply { setHtml("""<ol style="list-style-type: $keyword"><li>a</li></ol>""") }
            assertEquals(styleType, state.orderedStyleTypes().single(), "keyword $keyword")
        }
    }

    @Test
    fun `every unordered keyword maps to its style type`() {
        val expected = mapOf(
            "disc" to UnorderedListStyleType.Disc,
            "circle" to UnorderedListStyleType.Circle,
            "square" to UnorderedListStyleType.Square,
        )

        expected.forEach { (keyword, styleType) ->
            val state = RichTextState().apply { setHtml("""<ul style="list-style-type: $keyword"><li>a</li></ul>""") }
            assertEquals(styleType, state.unorderedStyleTypes().single(), "keyword $keyword")
        }
    }

    @Test
    fun `keyword case and surrounding declarations do not matter`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="margin: 0; list-style-type: Upper-Roman ; padding: 0"><li>a</li></ol>""")
        }

        assertEquals(OrderedListStyleType.UpperRoman, state.orderedStyleTypes().single())
    }

    @Test
    fun `an unknown keyword leaves the type unset`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: cjk-ideographic"><li>a</li></ol><ul style="list-style-type: none"><li>b</li></ul>""")
        }

        assertNull((state.richParagraphList[0].type as OrderedList).styleTypeOverride)
        assertNull((state.richParagraphList[1].type as UnorderedList).styleTypeOverride)
    }

    @Test
    fun `a bullet keyword on an ordered list is ignored`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: disc"><li>a</li></ol>""")
        }

        assertNull(state.orderedStyleTypes().single())
    }

    @Test
    fun `list-style-type on li overrides the list`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: lower-alpha"><li>a</li><li style="list-style-type: upper-roman">b</li><li>c</li></ol>""")
        }

        assertEquals(
            listOf<OrderedListStyleType?>(
                OrderedListStyleType.LowerAlpha,
                OrderedListStyleType.UpperRoman,
                OrderedListStyleType.LowerAlpha,
            ),
            state.orderedStyleTypes(),
        )
    }

    @Test
    fun `a nested list keeps its own type and the outer list keeps its own`() {
        val state = RichTextState().apply {
            setHtml(
                """<ol style="list-style-type: upper-roman"><li>a<ol style="list-style-type: lower-alpha"><li>b</li></ol></li><li>c</li></ol>"""
            )
        }

        assertEquals(
            listOf<OrderedListStyleType?>(
                OrderedListStyleType.UpperRoman,
                OrderedListStyleType.LowerAlpha,
                OrderedListStyleType.UpperRoman,
            ),
            state.orderedStyleTypes(),
        )
        assertEquals(listOf("I. ", "a. ", "II. "), state.markers())
    }

    @Test
    fun `a nested list without a type follows the config while the outer list keeps its type`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: upper-alpha"><li>a<ol><li>b</li></ol></li></ol>""")
        }

        assertEquals(
            listOf<OrderedListStyleType?>(OrderedListStyleType.UpperAlpha, null),
            state.orderedStyleTypes(),
        )
        assertEquals(listOf("A. ", "i. "), state.markers())
    }

    @Test
    fun `the type is written on the tag that opens the list`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: lower-alpha"><li>a</li><li>b</li></ol>""")
        }

        assertEquals(
            """<ol style="list-style-type: lower-alpha;"><li>a</li><li>b</li></ol>""",
            state.toHtml(),
        )
    }

    @Test
    fun `a list without a type is written without a style`() {
        val state = RichTextState().apply { setHtml("""<ol><li>a</li></ol><ul><li>b</li></ul>""") }

        assertEquals("""<ol><li>a</li></ol><ul><li>b</li></ul>""", state.toHtml())
    }

    @Test
    fun `an item whose type differs from the list is written with its own style`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: lower-alpha"><li>a</li><li style="list-style-type: upper-roman">b</li><li>c</li></ol>""")
        }

        val output = state.toHtml()

        assertEquals(
            """<ol style="list-style-type: lower-alpha;"><li>a</li><li style="list-style-type: upper-roman;">b</li><li>c</li></ol>""",
            output,
        )
    }

    @Test
    fun `the type and the start number share the ol tag`() {
        val state = RichTextState().apply {
            setHtml("""<ol start="3" style="list-style-type: upper-roman"><li>a</li><li>b</li></ol>""")
        }

        val output = state.toHtml()

        assertContains(output, """<ol start="3" style="list-style-type: upper-roman;">""")
        assertEquals(listOf("III. ", "IV. "), state.markers())
        val reloaded = RichTextState().apply { setHtml(output) }
        assertEquals(listOf("III. ", "IV. "), reloaded.markers())
    }

    @Test
    fun `unordered and nested types survive an html round trip`() {
        val html =
            """<ul style="list-style-type: square;"><li>a<ol style="list-style-type: upper-alpha;"><li>b</li><li>c</li></ol></li><li>d</li></ul>"""
        val state = RichTextState().apply { setHtml(html) }

        val output = state.toHtml()

        assertEquals(html, output)
        assertEquals(listOf("▪ ", "A. ", "B. ", "▪ "), state.markers())
    }

    @Test
    fun `a list style set in the editor is exported`() {
        val state = RichTextState().apply { setHtml("""<ul><li>a</li><li>b</li></ul>""") }
        state.selection = androidx.compose.ui.text.TextRange(state.annotatedString.text.indexOf("a"))

        state.setUnorderedListStyleType(UnorderedListStyleType.Circle)

        assertEquals("""<ul style="list-style-type: circle;"><li>a</li><li>b</li></ul>""", state.toHtml())
    }

    @Test
    fun `a custom style type has no keyword and is not written`() {
        val state = RichTextState().apply { setHtml("""<ul><li>a</li></ul>""") }
        state.selection = androidx.compose.ui.text.TextRange(state.annotatedString.text.indexOf("a"))

        state.setUnorderedListStyleType(UnorderedListStyleType.from("-"))

        assertEquals(listOf("- "), state.markers())
        assertFalse(state.toHtml().contains("list-style-type"))
    }
}
