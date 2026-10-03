package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.tripleClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Paragraph-wise selection and navigation. The framework finds paragraph boundaries by
 * searching the visible text for newlines, and the editor's text has none (paragraphs are
 * joined by a one-character separator), so left alone a triple click selects the whole
 * document and the paragraph keys jump to its ends. The editor answers both from its own
 * paragraph list instead.
 */
@OptIn(ExperimentalTestApi::class)
class ParagraphNavigationTest {

    // "alpha beta gamma delta": paragraph 1 is 0..10, paragraph 2 is 11..22.
    @Test
    fun `a triple click selects the clicked paragraph only`() = runEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput { tripleClick(caretAt(state, 7)) }
        waitForIdle()

        assertEquals(TextRange(0, 10), state.selection)
    }

    @Test
    fun `a triple click on the second paragraph selects it only`() = runEditor { state ->
        onNodeWithTag(EDITOR_TAG).performMouseInput { tripleClick(caretAt(state, 15)) }
        waitForIdle()

        assertEquals(TextRange(11, 22), state.selection)
    }

    @Test
    fun `paragraph down moves the caret to the paragraph end and then to the next paragraph end`() =
        runEditor { state ->
            placeCaret(state, 2)

            paragraphKey(Key.DirectionDown, shift = false)
            assertEquals(TextRange(10), state.selection)

            paragraphKey(Key.DirectionDown, shift = false)
            assertEquals(TextRange(22), state.selection)
        }

    @Test
    fun `paragraph up moves the caret to the paragraph start and then to the previous paragraph start`() =
        runEditor { state ->
            placeCaret(state, 15)

            paragraphKey(Key.DirectionUp, shift = false)
            assertEquals(TextRange(11), state.selection)

            paragraphKey(Key.DirectionUp, shift = false)
            assertEquals(TextRange(0), state.selection)
        }

    @Test
    fun `shift paragraph down extends the selection to the paragraph end`() = runEditor { state ->
        placeCaret(state, 2)

        paragraphKey(Key.DirectionDown, shift = true)

        assertEquals(TextRange(2, 10), state.selection)
    }

    @Test
    fun `shift paragraph up extends the selection to the paragraph start`() = runEditor { state ->
        placeCaret(state, 15)

        paragraphKey(Key.DirectionUp, shift = true)

        assertEquals(TextRange(15, 11), state.selection)
    }

    @Test
    fun `a triple click on a list item selects its text without the prefix`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            state.setHtml("<p>Before</p><ul><li>First item</li></ul>")
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EDITOR_TAG),
                )
            }
            waitForIdle()
            val text = state.annotatedString.text
            val itemStart = text.indexOf("First item")

            onNodeWithTag(EDITOR_TAG).performMouseInput { tripleClick(caretAt(state, itemStart + 7)) }
            waitForIdle()

            assertEquals(TextRange(itemStart, itemStart + "First item".length), state.selection)
        }

    /**
     * A single-paragraph editor keeps its newlines in the text, so the framework's own
     * paragraph navigation is right there and the editor must stay out of it.
     */
    @Test
    fun `paragraph down in a single paragraph editor stops at the newline`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    singleParagraph = true,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EDITOR_TAG),
                )
            }
            waitForIdle()
            state.setText("alpha beta\ngamma delta")
            waitForIdle()
            assertEquals("alpha beta\ngamma delta", state.textFieldState.text.toString(), "setup: newline kept")
            placeCaret(state, 2)

            paragraphKey(Key.DirectionDown, shift = false)

            assertEquals(TextRange(10), state.selection)
        }

    private fun runEditor(block: DesktopComposeUiTest.(state: RichTextState) -> Unit) =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            state.setText("alpha beta\ngamma delta")
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EDITOR_TAG),
                )
            }
            waitForIdle()
            block(state)
        }

    private fun DesktopComposeUiTest.placeCaret(state: RichTextState, offset: Int) {
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(caretAt(state, offset)) }
        waitForIdle()
        assertEquals(TextRange(offset), state.selection, "setup: caret at $offset")
    }

    private fun DesktopComposeUiTest.paragraphKey(key: Key, shift: Boolean) {
        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(PARAGRAPH_MODIFIER)
            if (shift) keyDown(Key.ShiftLeft)
            pressKey(key)
            if (shift) keyUp(Key.ShiftLeft)
            keyUp(PARAGRAPH_MODIFIER)
        }
        waitForIdle()
    }

    private fun caretAt(state: RichTextState, offset: Int): Offset {
        val rect = checkNotNull(state.textLayoutResult).getCursorRect(offset)
        return Offset(rect.left + 1f, rect.center.y)
    }

    private companion object {
        const val EDITOR_TAG = "editor"

        /** The framework's paragraph modifier: Alt on macOS, Ctrl everywhere else. */
        val PARAGRAPH_MODIFIER: Key =
            if (System.getProperty("os.name") == "Mac OS X") Key.AltLeft else Key.CtrlLeft
    }
}
