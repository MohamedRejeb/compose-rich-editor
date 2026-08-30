package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class Issue601OverlayScrollTest {

    @Test
    fun `editor scroll moves the internal scroll state`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.testTag("editor").height(60.dp),
            )
        }
        state.setHtml((1..40).joinToString("") { "<p>line $it</p>" })
        waitForIdle()
        runBlocking { state.scrollState.scrollTo(state.scrollState.maxValue) }
        waitForIdle()
        assertTrue(state.scrollState.value > 0)
    }
}
