package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Headings and `<br>` blocks.
 *
 * The lines of `<p>a<br>b<br>c</p>` are separate paragraphs in the model, linked as line break
 * continuations of the first one. Such a block only comes from imported html; in the editor
 * its lines look like any other paragraphs. So an imported heading covers every line of its
 * block, while a heading set in the editor applies to the selected lines only: a line whose
 * level now differs from its neighbour's leaves the block, and the other links stay.
 *
 * Setting a heading on the first line used to cut every link in the block, including the one
 * between lines the change did not touch.
 */
class HeadingLineBreakBlockTest {

    @Test
    fun `an imported heading block has the heading on every line`() {
        val state = stateOf("<h1>a<br>b<br>c</h1>")

        assertEquals(List(3) { HeadingStyle.H1 }, state.richParagraphList.map { it.headingStyle })
        assertEquals("<h1>a<br>b<br>c</h1>", state.toHtml())
    }

    @Test
    fun `a heading on the first line leaves the other two linked`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a</h1><p>b<br>c</p>", state.toHtml())
    }

    @Test
    fun `a heading on the middle line separates all three`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = SECOND_LINE
        state.setHeadingStyle(HeadingStyle.H2)

        assertEquals("<p>a</p><h2>b</h2><p>c</p>", state.toHtml())
    }

    @Test
    fun `a heading on the last line leaves the first two linked`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = THIRD_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<p>a<br>b</p><h1>c</h1>", state.toHtml())
    }

    @Test
    fun `a heading on two selected lines keeps those two linked`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = TextRange(0, 3)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b</h1><p>c</p>", state.toHtml())
    }

    @Test
    fun `a heading on every line keeps the block whole`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = TextRange(0, 5)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b<br>c</h1>", state.toHtml())
    }

    @Test
    fun `removing the heading from one line of a heading block takes that line out`() {
        val state = stateOf("<h1>a<br>b<br>c</h1>")

        state.selection = THIRD_LINE
        state.setHeadingStyle(HeadingStyle.Normal)

        assertEquals("<h1>a<br>b</h1><p>c</p>", state.toHtml())
    }

    @Test
    fun `only the selected line reports the heading`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        val levels = listOf(FIRST_LINE, SECOND_LINE, THIRD_LINE).map { line ->
            state.selection = line
            state.currentHeadingStyle
        }

        assertEquals(listOf(HeadingStyle.H1, HeadingStyle.Normal, HeadingStyle.Normal), levels)
    }

    @Test
    fun `the blocks around it are left alone`() {
        val state = stateOf("<p>x<br>y</p><p>a<br>b</p><p>after</p>")

        // "x y a b after": the line "a" is at 4.
        state.selection = TextRange(4, 5)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<p>x<br>y</p><h1>a</h1><p>b</p><p>after</p>", state.toHtml())
    }

    @Test
    fun `undo restores the block as it was`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        state.history.undo()

        assertEquals("<p>a<br>b<br>c</p>", state.toHtml())
    }

    @Test
    fun `the result survives an html round trip`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        val reloaded = stateOf(state.toHtml())

        assertEquals("<h1>a</h1><p>b<br>c</p>", reloaded.toHtml())
    }

    private fun stateOf(html: String): RichTextState = RichTextState().apply { setHtml(html) }

    private companion object {
        // "a b c": one character per line, one separator between lines.
        val FIRST_LINE = TextRange(0, 1)
        val SECOND_LINE = TextRange(2, 3)
        val THIRD_LINE = TextRange(4, 5)
    }
}
