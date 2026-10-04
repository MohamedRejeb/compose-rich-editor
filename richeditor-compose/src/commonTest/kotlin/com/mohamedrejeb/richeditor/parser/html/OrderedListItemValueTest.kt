package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * `<li value="N">` renumbers an ordered list from that item on. The number survives the
 * editor's renumbering passes, edits around it, and the HTML and document round trips.
 */
@OptIn(ExperimentalRichTextApi::class)
class OrderedListItemValueTest {

    private val html = """<ol><li>a</li><li value="5">b</li><li>c</li></ol>"""

    private fun RichTextState.listNumbers(): List<Int?> =
        richParagraphList.map { (it.type as? OrderedList)?.number }

    private fun RichTextState.type(text: String, at: Int) {
        val current = annotatedString.text
        onTextFieldValueChange(
            TextFieldValue(current.substring(0, at) + text + current.substring(at), TextRange(at + text.length))
        )
    }

    @Test
    fun `a value on a later item restarts the numbering there`() {
        val state = RichTextState().apply { setHtml(html) }

        assertEquals(listOf<Int?>(1, 5, 6), state.listNumbers())
    }

    @Test
    fun `a value written in arabic indic digits is read`() {
        val state = RichTextState().apply { setHtml("""<ol><li>a</li><li value="٧">b</li><li>c</li></ol>""") }

        assertEquals(listOf<Int?>(1, 7, 8), state.listNumbers())
    }

    @Test
    fun `a value on the first item is the list's start`() {
        val state = RichTextState().apply { setHtml("""<ol><li value="3">a</li><li>b</li></ol>""") }

        assertEquals(listOf<Int?>(3, 4), state.listNumbers())
    }

    @Test
    fun `the value is written back and survives an html round trip`() {
        val state = RichTextState().apply { setHtml(html) }

        val output = state.toHtml()
        assertContains(output, """<li value="5">""")
        val reloaded = RichTextState().apply { setHtml(output) }
        assertEquals(listOf<Int?>(1, 5, 6), reloaded.listNumbers())
    }

    @Test
    fun `the value survives a document round trip`() {
        val state = RichTextState().apply { setHtml(html) }

        val reloaded = RichTextState().apply { setRichTextDocument(state.toRichTextDocument()) }

        assertEquals(listOf<Int?>(1, 5, 6), reloaded.listNumbers())
    }

    @Test
    fun `typing inside the list keeps the value`() {
        val state = RichTextState().apply { setHtml(html) }
        val firstItemEnd = state.annotatedString.text.indexOf("a") + 1

        state.type("x", at = firstItemEnd)

        assertEquals(listOf<Int?>(1, 5, 6), state.listNumbers())
    }

    @Test
    fun `a new item before the valued item does not shift it`() {
        val state = RichTextState().apply { setHtml(html) }
        val firstItemEnd = state.annotatedString.text.indexOf("a") + 1

        state.type("\n", at = firstItemEnd)

        assertEquals(listOf<Int?>(1, 2, 5, 6), state.listNumbers())
    }

    @Test
    fun `a new item after the valued item counts up from it`() {
        val state = RichTextState().apply { setHtml(html) }
        val secondItemEnd = state.annotatedString.text.indexOf("b") + 1

        state.type("\n", at = secondItemEnd)

        assertEquals(listOf<Int?>(1, 5, 6, 7), state.listNumbers())
    }

    @Test
    fun `sequential markdown numbers are not pinned`() {
        val state = RichTextState().apply { setMarkdown("1. a\n2. b\n3. c") }
        val firstItemEnd = state.annotatedString.text.indexOf("a") + 1

        state.type("\n", at = firstItemEnd)

        assertEquals(listOf<Int?>(1, 2, 3, 4), state.listNumbers())
    }
}
