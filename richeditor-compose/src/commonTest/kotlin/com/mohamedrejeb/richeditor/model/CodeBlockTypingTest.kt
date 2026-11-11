package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Typing inside a code block: Markdown marks are code there, so the typing shortcuts that
 * format ordinary text must leave them alone.
 */
@OptIn(ExperimentalRichTextApi::class)
class CodeBlockTypingTest {

    private fun codeState(): RichTextState = RichTextState().apply {
        config.inlineTypingShortcutsEnabled = true
        config.headingTypingShortcutsEnabled = true
        setMarkdown("```python\nx\n```")
        selection = TextRange(annotatedString.text.length)
    }

    /** Types [text] one character at a time at the caret, as a keyboard does. */
    private fun RichTextState.type(text: String) {
        text.forEach { char ->
            val caret = selection.min
            val current = annotatedString.text
            val newText = current.substring(0, caret) + char + current.substring(caret)
            onTextFieldValueChange(TextFieldValue(newText, TextRange(caret + 1)))
        }
    }

    /** Commits [text] at the caret as one edit of the text field, the path automatic links run on. */
    private fun RichTextState.commit(text: String) {
        val caret = selection.min
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.replace(caret, caret, text)
        buffer.selection = TextRange(caret + text.length)
        applyChangeList(buffer)
        val newCaret = pendingSelectionDuringSync ?: buffer.selection
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(text = annotatedString.text, selection = newCaret)
    }

    @Test
    fun `underscores typed in a block stay as typed`() {
        val state = codeState()
        state.type(" = __init__")
        assertEquals("```python\nx = __init__\n```", state.toMarkdown())
        assertEquals(true, state.toRichTextDocument().blocks.single().spans.isEmpty())
    }

    @Test
    fun `stars and backticks typed in a block stay as typed`() {
        val state = codeState()
        state.type(" = *a* + **b** + `c` + ~~d~~")
        assertEquals("x = *a* + **b** + `c` + ~~d~~", state.annotatedString.text)
        assertEquals(true, state.toRichTextDocument().blocks.single().spans.isEmpty())
    }

    @Test
    fun `a URL typed in a block stays as typed`() {
        val state = codeState()
        state.commit(" = https://example.com")
        state.commit(" ")
        assertEquals("x = https://example.com ", state.annotatedString.text)
        assertEquals(true, state.toRichTextDocument().blocks.single().spans.isEmpty())
    }

    @Test
    fun `the same marks still format ordinary text`() {
        val state = RichTextState().apply { config.inlineTypingShortcutsEnabled = true }
        state.type("**bold**")
        assertEquals("<p><b>bold</b></p>", state.toHtml())
    }
}
