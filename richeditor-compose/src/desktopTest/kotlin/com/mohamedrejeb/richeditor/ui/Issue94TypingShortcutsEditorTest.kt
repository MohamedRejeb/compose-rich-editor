package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue 94: with `RichTextConfig.inlineTypingShortcutsEnabled` and
 * `RichTextConfig.headingTypingShortcutsEnabled`, Markdown marks typed into the editor format
 * the text as they are completed. These tests drive the real editor, so they also pin that the
 * text field buffer, the caret and the undo history stay in step with the model after the marks
 * are removed behind the edit.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue94TypingShortcutsEditorTest {

    private fun SemanticsNodeInteraction.typeEach(text: String) {
        text.forEach { performTextInput(it.toString()) }
    }

    @Test
    fun `typing two stars around a word makes it bold`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.inlineTypingShortcutsEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        editor.typeEach("**bold*")
        waitForIdle()
        assertEquals("<p>**bold*</p>", state.toHtml())

        editor.performTextInput("*")
        waitForIdle()
        assertEquals("<p><b>bold</b></p>", state.toHtml())
        assertEquals("bold", state.textFieldState.text.toString())
        assertEquals(TextRange(4), state.selection)

        editor.typeEach(" on")
        waitForIdle()
        assertEquals("<p><b>bold</b> on</p>", state.toHtml())
        assertEquals("bold on", state.textFieldState.text.toString())
        assertEquals(TextRange(7), state.selection)
    }

    @Test
    fun `one undo after the conversion restores the typed marks`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.inlineTypingShortcutsEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")
        editor.typeEach("_it_")
        waitForIdle()
        assertEquals("<p><i>it</i></p>", state.toHtml())

        runOnIdle { state.history.undo() }
        waitForIdle()

        assertEquals("<p>_it_</p>", state.toHtml())
        assertEquals("_it_", state.textFieldState.text.toString())
        assertEquals(TextRange(4), state.selection)
    }

    @Test
    fun `typing backticks around a word makes a code span`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.inlineTypingShortcutsEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        editor.typeEach("see `code` here")
        waitForIdle()

        assertEquals("<p>see <code>code</code> here</p>", state.toHtml())
        assertEquals("see code here", state.textFieldState.text.toString())
    }

    @Test
    fun `typing a hash and a space starts a heading`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.headingTypingShortcutsEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        editor.typeEach("## ")
        waitForIdle()
        assertEquals(HeadingStyle.H2, state.currentHeadingStyle)
        assertEquals("", state.textFieldState.text.toString())
        assertEquals(TextRange(0), state.selection)

        editor.typeEach("Title")
        editor.performTextInput("\n")
        editor.typeEach("body")
        waitForIdle()

        assertEquals("<h2>Title</h2><p>body</p>", state.toHtml())
        assertEquals("Title body", state.textFieldState.text.toString())
    }

    @Test
    fun `nothing is converted with the settings off`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        editor.typeEach("# **bold**")
        waitForIdle()

        assertEquals("<p># **bold**</p>", state.toHtml())
    }
}
