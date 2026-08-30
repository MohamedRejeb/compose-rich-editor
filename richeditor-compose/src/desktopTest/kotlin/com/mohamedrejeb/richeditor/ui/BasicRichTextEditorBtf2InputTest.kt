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

@OptIn(ExperimentalTestApi::class)
class BasicRichTextEditorBtf2InputTest {

    @Test
    fun `typing reaches richParagraphList through the BTF2 pipeline`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.testTag("editor"),
            )
        }
        onNodeWithTag("editor").performTextInput("Hello")
        waitForIdle()
        assertEquals("Hello", state.toText())
        assertEquals("Hello", state.textFieldState.text.toString())
    }
}
