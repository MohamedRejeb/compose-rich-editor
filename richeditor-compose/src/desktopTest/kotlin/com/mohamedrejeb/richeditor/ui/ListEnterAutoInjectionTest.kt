package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ListEnterAutoInjectionTest {

    @Test
    fun `enter in an ordered list injects the next number prefix into the buffer`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("item one")
        waitForIdle()
        state.toggleOrderedList()
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertEquals(2, state.richParagraphList.size)
        assertTrue(state.textFieldState.text.toString().contains("2. "))
        assertEquals(state.annotatedString.text, state.textFieldState.text.toString())
    }
}
