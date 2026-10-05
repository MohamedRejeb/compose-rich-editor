package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue 224: with `RichTextConfig.autoLinkEnabled`, a URL typed into the editor becomes a link
 * when a space or Enter follows it, and a pasted URL becomes a link at once. These tests drive
 * the real editor, so they also pin that the text field buffer, the caret and the undo history
 * stay in step with the model after the link is added behind the edit.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue224AutoLinkUrlsEditorTest {

    private val url = "https://example.com"
    private val link = "<a href=\"$url\" target=\"_blank\">$url</a>"

    @Test
    fun `a typed url followed by a space becomes a link`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.autoLinkEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        url.forEach { editor.performTextInput(it.toString()) }
        waitForIdle()
        assertEquals("<p>$url</p>", state.toHtml())

        editor.performTextInput(" ")
        waitForIdle()
        assertEquals("<p>$link </p>", state.toHtml())

        "ok".forEach { editor.performTextInput(it.toString()) }
        waitForIdle()
        assertEquals("<p>$link ok</p>", state.toHtml())
        assertEquals("$url ok", state.textFieldState.text.toString())
        assertEquals(state.annotatedString.text.length, state.selection.min)
    }

    @Test
    fun `one undo after the space removes the link and keeps the text`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.autoLinkEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")
        url.forEach { editor.performTextInput(it.toString()) }
        editor.performTextInput(" ")
        waitForIdle()

        runOnIdle { state.history.undo() }
        waitForIdle()

        assertEquals("<p>$url </p>", state.toHtml())
        assertEquals("$url ", state.textFieldState.text.toString())
    }

    @Test
    fun `a typed url followed by Enter becomes a link`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.autoLinkEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        url.forEach { editor.performTextInput(it.toString()) }
        editor.performTextInput("\n")
        editor.performTextInput("x")
        waitForIdle()

        assertEquals("<p>$link</p><p>x</p>", state.toHtml())
        assertEquals("$url x", state.textFieldState.text.toString())
    }

    @Test
    fun `a url inserted in one piece becomes a link`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState().apply { config.autoLinkEnabled = true }
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }

        onNodeWithTag("editor").performTextInput(url)
        waitForIdle()

        assertEquals("<p>$link</p>", state.toHtml())
    }

    @Test
    fun `nothing is linked with the setting off`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        val editor = onNodeWithTag("editor")

        url.forEach { editor.performTextInput(it.toString()) }
        editor.performTextInput(" ")
        waitForIdle()

        assertEquals("<p>$url </p>", state.toHtml())
    }
}
