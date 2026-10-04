package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Numbers outside CSS that content can carry in Arabic or Persian digits: HTML numeric
 * attributes and the typed ordered list trigger. See [CssNonAsciiDigitsTest] for CSS values.
 */
@OptIn(ExperimentalRichTextApi::class)
class HtmlNonAsciiDigitsTest {

    private fun RichTextState.listNumbers(): List<Int> =
        richParagraphList.map { assertIs<OrderedList>(it.type).number }

    @Test
    fun `an ordered list start written in arabic indic digits is read`() {
        val state = RichTextState()

        state.setHtml("""<ol start="٣"><li>a</li><li>b</li></ol>""")

        assertEquals(listOf(3, 4), state.listNumbers())
    }

    @Test
    fun `an ordered list start written in persian digits is read`() {
        val state = RichTextState()

        state.setHtml("""<ol start="۱۲"><li>a</li><li>b</li></ol>""")

        assertEquals(listOf(12, 13), state.listNumbers())
    }

    @Test
    fun `image dimensions written in arabic indic digits are read`() {
        val state = RichTextState()

        state.setHtml("""<p><img src="https://example.com/a.png" width="٤٠" height="٢٠"></p>""")

        val image = state.richParagraphList.first().children
            .firstNotNullOf { it.richSpanStyle as? RichSpanStyle.Image }
        assertEquals(40.sp, image.width)
        assertEquals(20.sp, image.height)
    }

    @Test
    fun `typing an arabic indic number and a dot starts an ordered list at that number`() {
        val state = RichTextState()

        state.onTextFieldValueChange(TextFieldValue("٣. ", TextRange(3)))

        assertEquals(listOf(3), state.listNumbers())
    }

    @Test
    fun `typing a persian number and a dot starts an ordered list at that number`() {
        val state = RichTextState()

        state.onTextFieldValueChange(TextFieldValue("۱۲. ", TextRange(4)))

        assertEquals(listOf(12), state.listNumbers())
    }
}
