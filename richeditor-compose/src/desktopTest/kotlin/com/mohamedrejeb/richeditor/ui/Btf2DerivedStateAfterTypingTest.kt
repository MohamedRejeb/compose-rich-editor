package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The derived state the toolbar reads (currentSpanStyle, isUnorderedList, ...) used to fall one
 * edit behind while typing. It was computed from the public `selection` getter, which returns
 * `textFieldState.selection`; during the BTF2 InputTransformation replay that value is still the
 * pre-edit selection, and the post-commit observer echo was deduped away by lastHandledSelection.
 * The fix reads the pending-aware `textFieldValue.selection` instead.
 */
@OptIn(ExperimentalTestApi::class)
class Btf2DerivedStateAfterTypingTest {

    @Test
    fun `staged bold survives the first typed char in the toolbar state`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "staged bold should show before typing",
        )

        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()

        assertEquals(
            FontWeight.Bold,
            state.getSpanStyle(TextRange(5, 6)).fontWeight,
            "the typed char itself should be bold in the model",
        )
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the toolbar state should stay bold after the first typed char",
        )
    }

    @Test
    fun `staged bold survives two typed chars in the toolbar state`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the toolbar state should stay bold after the first typed char",
        )

        onNodeWithTag("editor").performTextInput("b")
        waitForIdle()
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "the toolbar state should stay bold after the second typed char",
        )
        assertEquals(
            FontWeight.Bold,
            state.getSpanStyle(TextRange(5, 7)).fontWeight,
            "both typed chars should be bold in the model",
        )
        assertEquals("helloab", state.textFieldValue.text)
    }

    @Test
    fun `unordered list stays active in the toolbar state after typing`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()
        assertTrue(state.isUnorderedList, "the list toggle should show before typing")

        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()

        assertTrue(state.isUnorderedList, "the list toggle should stay on after the first typed char")
        assertTrue(state.isList, "isList should stay on after the first typed char")
    }

    @Test
    fun `enter on an empty list item exits the list in the toolbar state immediately`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()
        assertTrue(state.isUnorderedList, "the list toggle should show before the enter")

        // The standard exit gesture: one enter opens an empty item, the next leaves the list.
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertTrue(state.isUnorderedList, "the enter should still be inside the list")

        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertFalse(state.isUnorderedList, "the list toggle should turn off with the exit itself")
        assertFalse(state.isList, "isList should turn off with the exit itself")
    }

    /**
     * The exit gesture above reads correctly even from the stale selection, because that
     * selection points past the shortened text and the null lookup happens to answer "not a
     * list". This one crosses a paragraph boundary the other way, where the stale answer is
     * wrong on its face: the merge puts the caret back inside the list item above.
     */
    @Test
    fun `backspace merging a line into the list above turns the toolbar list back on`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.toggleUnorderedList()
        waitForIdle()
        onNodeWithTag("editor").performTextInput("a")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("b")
        waitForIdle()
        assertFalse(state.isUnorderedList, "the exited line should not be a list item")

        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals(1, state.richParagraphList.size, "the empty line should merge into the item above")
        assertTrue(state.isUnorderedList, "the list toggle should turn on with the merge itself")
        assertTrue(state.isList, "isList should turn on with the merge itself")
    }
}
