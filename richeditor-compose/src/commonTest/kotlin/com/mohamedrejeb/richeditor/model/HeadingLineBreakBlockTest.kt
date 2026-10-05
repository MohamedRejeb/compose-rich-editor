package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A heading set on one line of a `<br>` block applies to the whole block.
 *
 * The lines of `<p>a<br>b<br>c</p>` are separate paragraphs in the model, linked as line break
 * continuations of the first one. In html a block has one tag, so a heading belongs to all of
 * its lines: setting it on a single line used to cut the block into separate paragraphs.
 */
class HeadingLineBreakBlockTest {

    @Test
    fun `a heading set on the first line applies to every line of the block`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b<br>c</h1>", state.toHtml())
    }

    @Test
    fun `a heading set on a middle line applies to every line of the block`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = SECOND_LINE
        state.setHeadingStyle(HeadingStyle.H2)

        assertEquals("<h2>a<br>b<br>c</h2>", state.toHtml())
    }

    @Test
    fun `a heading set on the last line applies to every line of the block`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = THIRD_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b<br>c</h1>", state.toHtml())
    }

    @Test
    fun `a heading set with the caret in a line applies to the block`() {
        val state = stateOf("<p>a<br>b<br>c</p>")

        state.selection = TextRange(3)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b<br>c</h1>", state.toHtml())
    }

    @Test
    fun `removing the heading from one line removes it from the block`() {
        val state = stateOf("<h1>a<br>b<br>c</h1>")

        state.selection = SECOND_LINE
        state.setHeadingStyle(HeadingStyle.Normal)

        assertEquals("<p>a<br>b<br>c</p>", state.toHtml())
    }

    @Test
    fun `changing the level on one line changes the block`() {
        val state = stateOf("<h1>a<br>b<br>c</h1>")

        state.selection = THIRD_LINE
        state.setHeadingStyle(HeadingStyle.H3)

        assertEquals("<h3>a<br>b<br>c</h3>", state.toHtml())
    }

    @Test
    fun `the blocks around it are left alone`() {
        val state = stateOf("<p>before</p><p>a<br>b</p><p>after</p>")

        // "before a b after": the line "b" starts at 9.
        state.selection = TextRange(9, 10)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<p>before</p><h1>a<br>b</h1><p>after</p>", state.toHtml())
    }

    @Test
    fun `a selection across two blocks applies to both in full`() {
        val state = stateOf("<p>a<br>b</p><p>c<br>d</p>")

        // "a b c d": from the line "b" to the line "c".
        state.selection = TextRange(2, 5)
        state.setHeadingStyle(HeadingStyle.H1)

        assertEquals("<h1>a<br>b</h1><h1>c<br>d</h1>", state.toHtml())
    }

    @Test
    fun `every line of the block reports the heading`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        listOf(FIRST_LINE, SECOND_LINE, THIRD_LINE).forEach { line ->
            state.selection = line
            assertEquals(HeadingStyle.H1, state.currentHeadingStyle, "line at $line")
        }
    }

    @Test
    fun `undo restores the block as it was`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = SECOND_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        state.history.undo()

        assertEquals("<p>a<br>b<br>c</p>", state.toHtml())
    }

    @Test
    fun `the block survives an html round trip`() {
        val state = stateOf("<p>a<br>b<br>c</p>")
        state.selection = FIRST_LINE
        state.setHeadingStyle(HeadingStyle.H1)

        val reloaded = stateOf(state.toHtml())

        assertEquals("<h1>a<br>b<br>c</h1>", reloaded.toHtml())
    }

    private fun stateOf(html: String): RichTextState = RichTextState().apply { setHtml(html) }

    private companion object {
        // "a b c": one character per line, one separator between lines.
        val FIRST_LINE = TextRange(0, 1)
        val SECOND_LINE = TextRange(2, 3)
        val THIRD_LINE = TextRange(4, 5)
    }
}
