package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Issue 417: an app could not keep lists and turn off the typing shortcuts that create them
 * ("- ", "* " or "1. " at the start of a paragraph). Leaving the list features out of
 * `RichTextConfig.features` turns the shortcuts off, but removes lists altogether.
 * `RichTextConfig.listTypingShortcutsEnabled` controls the shortcuts alone.
 */
class Issue417ListTypingShortcutsTest {

    private fun RichTextState.type(text: String) {
        onTextFieldValueChange(TextFieldValue(text, TextRange(text.length)))
    }

    private fun RichTextState.firstType() = richParagraphList.first().type

    @Test
    fun `the shortcuts are on by default`() {
        assertTrue(RichTextState().config.listTypingShortcutsEnabled)

        assertIs<UnorderedList>(RichTextState().apply { type("- ") }.firstType())
        assertIs<UnorderedList>(RichTextState().apply { type("* ") }.firstType())
        assertIs<OrderedList>(RichTextState().apply { type("1. ") }.firstType())
    }

    @Test
    fun `with the shortcuts off the typed text stays as text`() {
        for (typed in listOf("- ", "* ", "1. ", "57. ")) {
            val state = RichTextState().apply { config.listTypingShortcutsEnabled = false }

            state.type(typed)

            assertIs<DefaultParagraph>(state.firstType(), "typed \"$typed\"")
            assertEquals(typed, state.annotatedString.text, "typed \"$typed\"")
        }
    }

    @Test
    fun `with the shortcuts off the list toggles still work`() {
        val state = RichTextState().apply {
            config.listTypingShortcutsEnabled = false
            setText("item")
        }

        state.toggleUnorderedList()
        assertIs<UnorderedList>(state.firstType())

        state.toggleOrderedList()
        assertIs<OrderedList>(state.firstType())
    }

    @Test
    fun `with the shortcuts off loaded lists are kept`() {
        val state = RichTextState().apply { config.listTypingShortcutsEnabled = false }

        state.setHtml("<ul><li>a</li></ul><ol><li>b</li></ol>")

        assertIs<UnorderedList>(state.richParagraphList[0].type)
        assertIs<OrderedList>(state.richParagraphList[1].type)
    }

    @Test
    fun `turning the shortcuts back on restores them`() {
        val state = RichTextState().apply { config.listTypingShortcutsEnabled = false }
        state.config.listTypingShortcutsEnabled = true

        state.type("- ")

        assertIs<UnorderedList>(state.firstType())
    }

    @Test
    fun `a copy of the state keeps the setting`() {
        val state = RichTextState().apply { config.listTypingShortcutsEnabled = false }

        assertEquals(false, state.copy().config.listTypingShortcutsEnabled)
    }
}
