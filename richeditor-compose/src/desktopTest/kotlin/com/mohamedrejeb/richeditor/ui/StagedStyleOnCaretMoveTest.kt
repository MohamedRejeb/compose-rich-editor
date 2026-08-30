package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A span style toggled with a collapsed caret is staged for the next characters typed at that
 * caret. Moving the caret abandons it, matching what `updateTextFieldValue` did on its
 * selection-only path before `handleSelectionChanged` took over pure selection changes.
 */
@OptIn(ExperimentalTestApi::class)
class StagedStyleOnCaretMoveTest {

    private fun RichTextState.isBoldAt(index: Int): Boolean =
        annotatedString.spanStyles.any { range ->
            range.item.fontWeight == FontWeight.Bold && index >= range.start && index < range.end
        }

    @Test
    fun `moving the caret discards a staged span style`() = runDesktopComposeUiTest {
        val state = RichTextState()

        setContent {
            val focusRequester = remember { FocusRequester() }
            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag("editor"),
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()

        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()

        // Collapsed caret at the end: the toggle only stages the style for future typing.
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        // Caret move through the canonical setter, which routes into handleSelectionChanged.
        state.selection = TextRange(0)
        waitForIdle()

        onNodeWithTag("editor").performTextInput("X")
        waitForIdle()

        assertEquals("Xhello", state.annotatedString.text)
        assertEquals(
            false,
            state.isBoldAt(0),
            "The staged bold was abandoned by the caret move, so the typed X must be plain. " +
                "Spans: ${state.annotatedString.spanStyles.map { "(${it.start}..${it.end} ${it.item.fontWeight})" }}",
        )
    }

    @Test
    fun `staged span style still applies when the caret does not move`() = runDesktopComposeUiTest {
        val state = RichTextState()

        setContent {
            val focusRequester = remember { FocusRequester() }
            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag("editor"),
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()

        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        onNodeWithTag("editor").performTextInput("X")
        waitForIdle()

        assertEquals("helloX", state.annotatedString.text)
        assertEquals(
            true,
            state.isBoldAt(5),
            "Typing straight after the toggle must consume the staged bold. " +
                "Spans: ${state.annotatedString.spanStyles.map { "(${it.start}..${it.end} ${it.item.fontWeight})" }}",
        )
    }
}
