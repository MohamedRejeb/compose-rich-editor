package com.mohamedrejeb.richeditor.ui

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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.EmptyLineAnchor
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.isForModelText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue 369: the caret on an empty paragraph fell back to the editor's base font size, then
 * jumped to the right size on the first typed character.
 *
 * Cause: an empty paragraph renders no character of its own, so nothing gave its line a font.
 * Fix: the output buffer styles the one character that stands for the empty line (its
 * separator, or the anchor appended for a trailing empty paragraph) with the font the next
 * typed character would have.
 */
@OptIn(ExperimentalTestApi::class)
class Issue369CaretSizeEmptyParagraphTest {

    @Test
    fun `enter after big text keeps the caret big on the new empty paragraph`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        onNodeWithTag(EDITOR_TAG).performTextInput("Big")
        waitForIdle()
        val typedCaret = caretHeight(state)

        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(typedCaret, caretHeight(state), 1f)
    }

    @Test
    fun `a staged font size on an empty editor sizes the caret`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        val baseCaret = caretHeight(state)

        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        waitForIdle()

        assertTrue(caretHeight(state) > baseCaret * 1.5f, "base=$baseCaret staged=${caretHeight(state)}")
    }

    @Test
    fun `deleting all the big text keeps the caret big`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        onNodeWithTag(EDITOR_TAG).performTextInput("Hi")
        waitForIdle()
        val typedCaret = caretHeight(state)

        onNodeWithTag(EDITOR_TAG).performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
        }
        waitForIdle()

        assertEquals(typedCaret, caretHeight(state), 1f)
    }

    @Test
    fun `an empty paragraph between big paragraphs has a big caret`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        state.setHtml("<p><span style=\"font-size: 40px\">One</span></p><p><span style=\"font-size: 40px\"></span></p><p><span style=\"font-size: 40px\">Two</span></p>")
        waitForIdle()
        state.selection = TextRange(1)
        waitForIdle()
        val bigCaret = caretHeight(state)
        state.selection = TextRange(4)
        waitForIdle()

        assertEquals(bigCaret, caretHeight(state), 1f)
    }

    @Test
    fun `removing the staged font size shrinks the caret back`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        val baseCaret = caretHeight(state)
        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        waitForIdle()

        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        waitForIdle()

        assertEquals(baseCaret, caretHeight(state), 1f)
    }

    @Test
    fun `a bigger empty last paragraph leaves the line above at its own height`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        onNodeWithTag(EDITOR_TAG).performTextInput("small")
        waitForIdle()
        val smallLine = caretHeight(state)
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        state.toggleSpanStyle(SpanStyle(fontSize = BIG))
        waitForIdle()

        val layout = checkNotNull(state.textLayoutResult)
        assertEquals(smallLine, layout.getLineBottom(0) - layout.getLineTop(0), 1f)
        assertTrue(caretHeight(state) > smallLine * 1.5f)
    }

    @Test
    fun `an empty last paragraph renders with its own alignment`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        onNodeWithTag(EDITOR_TAG).performTextInput("left")
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
        waitForIdle()

        val layout = checkNotNull(state.textLayoutResult)
        val caretX = layout.getCursorRect(state.selection.min).left
        assertEquals(layout.size.width / 2f, caretX, 2f)
    }

    @Test
    fun `the layout of a document with an empty last paragraph still matches the model`() = runDesktopComposeUiTest {
        val state = RichTextState()
        setEditor(state)
        onNodeWithTag(EDITOR_TAG).performTextInput("text")
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        val layout = checkNotNull(state.textLayoutResult)
        assertEquals(state.textFieldState.text.length + EmptyLineAnchor.length, layout.layoutInput.text.length)
        assertTrue(layout.isForModelText(state.textFieldState.text.length))
        assertEquals(2, layout.lineCount)
    }

    private fun DesktopComposeUiTest.setEditor(state: RichTextState) {
        setContent {
            BasicRichTextEditor(
                state = state,
                textStyle = TextStyle(fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG),
            )
        }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()
    }

    private fun caretHeight(state: RichTextState): Float {
        val layout = checkNotNull(state.textLayoutResult)
        return layout.getCursorRect(state.selection.min).height
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        val BIG = 40.sp
    }
}
