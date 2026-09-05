package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Enter used to render as a trailing space until the next edit: the paragraph separator lives
 * inside the previous paragraph's ParagraphStyle range, so a trailing empty paragraph produced a
 * zero-length range that BTF2's tracked-range styling dropped. The pipeline now lets such a range
 * re-own the separator before it reaches the buffer.
 */
@OptIn(ExperimentalTestApi::class)
class Btf2EnterRendersNewLineTest {

    @Test
    fun `enter renders a second line immediately`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size, "model should have two paragraphs")
        assertEquals(
            2,
            state.textLayoutResult?.lineCount,
            "layout should render two lines right after enter",
        )
    }

    @Test
    fun `enter on an empty document renders two lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size, "model should have two paragraphs")
        val layout = state.textLayoutResult
        assertEquals(2, layout?.lineCount, "layout should render two lines on an empty document")
        assertEquals(
            1,
            layout?.getLineForOffset(state.textFieldValue.text.length),
            "the caret at text end should sit on the second line",
        )
    }

    @Test
    fun `two enters render three lines immediately`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(3, state.richParagraphList.size, "model should have three paragraphs")
        assertEquals(
            3,
            state.textLayoutResult?.lineCount,
            "layout should render three lines right after the second enter",
        )
    }

    @Test
    fun `enter in the middle of the text renders two lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("ab")
        waitForIdle()
        state.selection = TextRange(1)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size, "model should have two paragraphs")
        assertEquals(
            2,
            state.textLayoutResult?.lineCount,
            "layout should render two lines right after the split",
        )
    }

    @Test
    fun `typing after enter renders the typed char on the second line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("b")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size, "model should have two paragraphs")
        assertEquals("b", state.richParagraphList[1].children.joinToString("") { it.text })

        val layout = state.textLayoutResult
        assertEquals(2, layout?.lineCount, "layout should still render two lines")
        val typedCharOffset = state.textFieldValue.text.lastIndexOf('b')
        assertEquals(
            1,
            layout?.getLineForOffset(typedCharOffset),
            "the typed char should sit on the second line",
        )
    }
}
