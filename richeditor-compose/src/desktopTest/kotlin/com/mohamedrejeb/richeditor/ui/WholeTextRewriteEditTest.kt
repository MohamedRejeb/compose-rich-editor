package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An edit that reaches the editor as a rewrite of the whole text must only change what differs.
 *
 * Compose web commits every typed character that way: the text field reports one change from
 * the whole old text to the whole new text. Replayed literally, it replaced the document with a
 * single unformatted paragraph. The pipeline now trims such a change to the part that differs.
 * `performTextReplacement` produces the same change shape on desktop.
 */
@OptIn(ExperimentalTestApi::class)
class WholeTextRewriteEditTest {

    @Test
    fun `a character appended by a whole text rewrite keeps paragraphs and formatting`() = runEditorTest { state ->
        rewrite(state, caret = state.textFieldState.text.length) { "${it}q" }

        assertEquals("<p>One <b>bold</b> word</p><p>Second lineq</p>", state.toHtml())
        assertEquals(TextRange(SOURCE_TEXT.length + 1), state.selection)
    }

    @Test
    fun `a character inserted in the first paragraph keeps the rest`() = runEditorTest { state ->
        rewrite(state, caret = 2) { it.replaceRange(2, 2, "x") }

        assertEquals("<p>Onxe <b>bold</b> word</p><p>Second line</p>", state.toHtml())
    }

    @Test
    fun `a character deleted by a whole text rewrite keeps the rest`() = runEditorTest { state ->
        rewrite(state, caret = 4) { it.removeRange(4, 5) }

        assertEquals("<p>One <b>old</b> word</p><p>Second line</p>", state.toHtml())
    }

    @Test
    fun `typing over a select all still replaces everything`() = runEditorTest { state ->
        state.selection = TextRange(0, state.textFieldState.text.length)
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performTextReplacement("One")
        waitForIdle()

        assertEquals("One", state.toText())
        assertEquals(1, state.richParagraphList.size)
    }

    private fun runEditorTest(block: DesktopComposeUiTest.(RichTextState) -> Unit) = runDesktopComposeUiTest {
        val state = RichTextState().apply { setHtml(SOURCE_HTML) }
        setEditor(state)
        assertEquals(SOURCE_TEXT, state.textFieldState.text.toString())
        block(state)
    }

    private fun DesktopComposeUiTest.setEditor(state: RichTextState) {
        setContent { BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG)) }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()
    }

    /** Places the caret, then commits [change] applied to the whole text as one rewrite. */
    private fun DesktopComposeUiTest.rewrite(state: RichTextState, caret: Int, change: (String) -> String) {
        state.selection = TextRange(caret)
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performTextReplacement(change(state.textFieldState.text.toString()))
        waitForIdle()
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        const val SOURCE_HTML = "<p>One <b>bold</b> word</p><p>Second line</p>"
        const val SOURCE_TEXT = "One bold word Second line"
    }
}
