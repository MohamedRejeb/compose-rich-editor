package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Issue 94: a Markdown heading marker typed at the start of a paragraph stayed as text. With
 * `RichTextConfig.headingTypingShortcutsEnabled`, `# ` to `###### ` typed at the start of a
 * paragraph set that heading level and the marker is removed, as in Notion and Google Docs.
 * The conversion is its own undo step.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue94HeadingTypingShortcutsTest {

    private fun newState(): RichTextState =
        RichTextState().apply { config.headingTypingShortcutsEnabled = true }

    /** Types [text] one character at a time at the caret, as a keyboard does. */
    private fun RichTextState.type(text: String) {
        text.forEach { char ->
            val caret = selection.min
            val current = annotatedString.text
            val newText = current.substring(0, caret) + char + current.substring(caret)
            onTextFieldValueChange(TextFieldValue(newText, TextRange(caret + 1)))
        }
    }

    @Test
    fun `the shortcut is off by default`() {
        val state = RichTextState()
        assertFalse(state.config.headingTypingShortcutsEnabled)

        state.type("# Title")

        assertEquals("<p># Title</p>", state.toHtml())
    }

    @Test
    fun `a hash and a space start a heading`() {
        val state = newState()

        state.type("# ")

        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)
        assertEquals("", state.annotatedString.text)
        assertEquals(TextRange(0), state.selection)
    }

    @Test
    fun `the text typed after the marker is the heading`() {
        val state = newState()

        state.type("## Title")

        assertEquals("<h2>Title</h2>", state.toHtml())
        assertEquals(HeadingStyle.H2, state.currentHeadingStyle)
    }

    @Test
    fun `every level up to six has a marker`() {
        for (level in 1..6) {
            val state = newState()

            state.type("#".repeat(level) + " T")

            assertEquals("<h$level>T</h$level>", state.toHtml(), "level $level")
        }
    }

    @Test
    fun `seven hashes are text`() {
        val state = newState()

        state.type("####### T")

        assertEquals("<p>####### T</p>", state.toHtml())
    }

    @Test
    fun `a hash without a space is text`() {
        val state = newState()

        state.type("#tag ")

        assertEquals("<p>#tag </p>", state.toHtml())
    }

    @Test
    fun `a marker away from the paragraph start is text`() {
        val state = newState()

        state.type("a # b")

        assertEquals("<p>a # b</p>", state.toHtml())
    }

    @Test
    fun `the marker works in a second paragraph`() {
        val state = newState()

        state.type("first\n### Third")

        assertEquals("<p>first</p><h3>Third</h3>", state.toHtml())
    }

    @Test
    fun `the marker changes the level of a heading`() {
        val state = newState()
        state.type("## ")

        state.type("# ")

        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)
        assertEquals("", state.annotatedString.text)
    }

    @Test
    fun `the marker is text in a list item`() {
        val state = newState()

        state.type("- # a")

        assertIs<UnorderedList>(state.richParagraphList.first().type)
        assertEquals("<ul><li># a</li></ul>", state.toHtml())
    }

    @Test
    fun `the marker is text when headings are not allowed`() {
        val state = newState()
        state.config.features = RichTextFeature.All - RichTextFeature.Heading

        state.type("# T")

        assertEquals("<p># T</p>", state.toHtml())
    }

    @Test
    fun `one undo restores the typed marker`() {
        val state = newState()
        state.type("## ")

        assertTrue(state.history.undo())

        assertEquals("<p>## </p>", state.toHtml())
        assertEquals(HeadingStyle.Normal, state.currentHeadingStyle)
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun `redo applies the heading again`() {
        val state = newState()
        state.type("## ")
        state.history.undo()

        assertTrue(state.history.redo())

        assertEquals(HeadingStyle.H2, state.currentHeadingStyle)
        assertEquals("", state.annotatedString.text)
    }

    @Test
    fun `the list shortcuts still work beside it`() {
        val state = newState()

        state.type("- a")

        assertIs<UnorderedList>(state.richParagraphList.first().type)
    }

    @Test
    fun `a copy of the state keeps the setting`() {
        val state = newState()

        assertTrue(state.copy().config.headingTypingShortcutsEnabled)
    }
}
