package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Issue 94: Markdown marks typed into the editor had no effect, the characters stayed as text.
 * With `RichTextConfig.inlineTypingShortcutsEnabled`, typing the closing mark of `**bold**`,
 * `__bold__`, `*italic*`, `_italic_`, `` `code` `` or `~~strike~~` formats the text between the
 * marks and removes the marks, the way Notion, Slack and Google Docs do. The conversion is its
 * own undo step and does not stage the style for what is typed next.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue94InlineTypingShortcutsTest {

    private fun newState(): RichTextState =
        RichTextState().apply { config.inlineTypingShortcutsEnabled = true }

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
    fun `the shortcuts are off by default`() {
        val state = RichTextState()
        assertFalse(state.config.inlineTypingShortcutsEnabled)

        state.type("**bold**")

        assertEquals("<p>**bold**</p>", state.toHtml())
    }

    @Test
    fun `two stars make bold`() {
        val state = newState()

        state.type("**bold**")

        assertEquals("<p><b>bold</b></p>", state.toHtml())
        assertEquals("bold", state.annotatedString.text)
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun `two underscores make bold`() {
        val state = newState()

        state.type("__bold__")

        assertEquals("<p><b>bold</b></p>", state.toHtml())
    }

    @Test
    fun `one star makes italic`() {
        val state = newState()

        state.type("*italic*")

        assertEquals("<p><i>italic</i></p>", state.toHtml())
    }

    @Test
    fun `one underscore makes italic`() {
        val state = newState()

        state.type("_italic_")

        assertEquals("<p><i>italic</i></p>", state.toHtml())
    }

    @Test
    fun `backticks make a code span`() {
        val state = newState()

        state.type("`code`")

        assertEquals("<p><code>code</code></p>", state.toHtml())
    }

    @Test
    fun `two tildes make strikethrough`() {
        val state = newState()

        state.type("~~gone~~")

        assertEquals("<p><s>gone</s></p>", state.toHtml())
    }

    @Test
    fun `the text typed after the conversion is not formatted`() {
        val state = newState()

        state.type("**bold** on")

        assertEquals("<p><b>bold</b> on</p>", state.toHtml())
        assertFalse(state.currentSpanStyle.fontWeight == FontWeight.Bold)
    }

    @Test
    fun `the text typed after a code span is plain`() {
        val state = newState()

        state.type("`code` on")

        assertEquals("<p><code>code</code> on</p>", state.toHtml())
        assertFalse(state.isCodeSpan)
    }

    @Test
    fun `a bold in progress does not fire italic`() {
        val state = newState()

        state.type("**bold*")

        assertEquals("<p>**bold*</p>", state.toHtml())
    }

    @Test
    fun `marks around nothing or around spaces stay as text`() {
        for (typed in listOf("****", "** a**", "**a **", "x* *", "``", "~~~~")) {
            val state = newState()

            state.type(typed)

            assertEquals("<p>$typed</p>", state.toHtml(), "typed \"$typed\"")
        }
    }

    @Test
    fun `an underscore inside a word is not a mark`() {
        val state = newState()

        state.type("snake_case_name")

        assertEquals("<p>snake_case_name</p>", state.toHtml())
    }

    @Test
    fun `an underscore closed inside a word is not a mark`() {
        val state = newState()
        state.type("_ab")
        state.selection = TextRange(2)

        state.type("_")

        assertEquals("<p>_a_b</p>", state.toHtml())
    }

    @Test
    fun `a star inside a word is a mark`() {
        val state = newState()

        state.type("2*3*4")

        assertEquals("<p>2<i>3</i>4</p>", state.toHtml())
    }

    @Test
    fun `the marks must sit in one paragraph`() {
        val state = newState()

        state.type("*a\nb*")

        assertEquals("<p>*a</p><p>b*</p>", state.toHtml())
    }

    @Test
    fun `nothing fires inside a code span`() {
        val state = newState()
        state.setHtml("<p><code>code</code></p>")
        state.selection = TextRange(4)

        state.type("*a*")

        assertEquals("<p><code>code*a*</code></p>", state.toHtml())
    }

    @Test
    fun `nothing fires inside a link`() {
        val state = newState()
        state.setHtml("<p><a href=\"https://example.com\">link</a></p>")
        state.selection = TextRange(2)

        state.type("*a*")

        assertEquals("<p><a href=\"https://example.com\" target=\"_blank\">li*a*nk</a></p>", state.toHtml())
    }

    @Test
    fun `an opening mark inside a code span does not count`() {
        val state = newState()
        state.setHtml("<p><code>*</code>a</p>")
        state.selection = TextRange(2)

        state.type("*")

        assertEquals("<p><code>*</code>a*</p>", state.toHtml())
    }

    @Test
    fun `italic typed inside bold text adds to the bold`() {
        val state = newState()
        state.setHtml("<p><b>bold</b></p>")
        state.selection = TextRange(4)

        state.type("*it*")

        assertEquals("<p><b>bold<i>it</i></b></p>", state.toHtml())
    }

    @Test
    fun `the conversion works in the middle of a paragraph`() {
        val state = newState()
        state.type("a c")
        state.selection = TextRange(1)

        state.type("*b*")

        assertEquals("<p>a<i>b</i> c</p>", state.toHtml())
        assertEquals(TextRange(2), state.selection)
    }

    @Test
    fun `the conversion works in a list item`() {
        val state = newState()

        state.type("- **bold**")

        assertEquals("<ul><li><b>bold</b></li></ul>", state.toHtml())
        assertEquals("• bold", state.annotatedString.text)
    }

    @Test
    fun `the conversion works in a second paragraph`() {
        val state = newState()

        state.type("first\n*b*")

        assertEquals("<p>first</p><p><i>b</i></p>", state.toHtml())
        assertEquals(TextRange(7), state.selection)
    }

    @Test
    fun `a mark whose feature is not allowed stays as text`() {
        val state = newState()
        state.config.features = RichTextFeature.All - RichTextFeature.Bold

        state.type("**bold** *it*")

        assertEquals("<p>**bold** <i>it</i></p>", state.toHtml())
    }

    @Test
    fun `a code span is not made when code spans are not allowed`() {
        val state = newState()
        state.config.features = RichTextFeature.All - RichTextFeature.CodeSpan

        state.type("`code`")

        assertEquals("<p>`code`</p>", state.toHtml())
    }

    @Test
    fun `one undo restores the typed marks`() {
        val state = newState()
        state.type("**bold**")

        assertTrue(state.history.undo())

        assertEquals("<p>**bold**</p>", state.toHtml())
        assertEquals(TextRange(8), state.selection)
        assertNull(state.currentSpanStyle.fontWeight)
    }

    @Test
    fun `redo applies the conversion again`() {
        val state = newState()
        state.type("**bold**")
        state.history.undo()

        assertTrue(state.history.redo())

        assertEquals("<p><b>bold</b></p>", state.toHtml())
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun `the typed text after the conversion is its own undo step`() {
        val state = newState()
        state.type("*a* b")

        state.history.undo()
        assertEquals("<p><i>a</i></p>", state.toHtml())

        state.history.undo()
        assertEquals("<p>*a*</p>", state.toHtml())
    }

    @Test
    fun `a whole pattern inserted at once is not converted`() {
        val state = newState()

        state.onTextFieldValueChange(TextFieldValue("**bold**", TextRange(8)))

        assertEquals("<p>**bold**</p>", state.toHtml())
    }

    @Test
    fun `a copy of the state keeps the setting`() {
        val state = newState()

        assertTrue(state.copy().config.inlineTypingShortcutsEnabled)
    }
}
