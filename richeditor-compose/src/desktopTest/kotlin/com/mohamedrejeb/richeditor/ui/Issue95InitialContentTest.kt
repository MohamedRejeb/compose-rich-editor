package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Issue 95: a state could only be created empty, so initial content had to be pushed in from a
 * `LaunchedEffect` after the first frame.
 *
 * `rememberRichTextState { ... }` runs its block once on the new state, before the first
 * composition uses it. The block is the single entry point for every kind of content: text,
 * HTML, Markdown or a `RichTextDocument`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue95InitialContentTest {

    @Test
    fun `html content is there on the first composition`() = runDesktopComposeUiTest {
        var firstFrameHtml: String? = null
        setContent {
            val state = rememberRichTextState { setHtml(HTML) }
            if (firstFrameHtml == null) firstFrameHtml = state.toHtml()
        }
        waitForIdle()

        assertEquals(HTML, firstFrameHtml)
    }

    @Test
    fun `text markdown and a document work the same way`() = runDesktopComposeUiTest {
        val document = RichTextState().setHtml(HTML).toRichTextDocument()
        var text: RichTextState? = null
        var markdown: RichTextState? = null
        var fromDocument: RichTextState? = null
        setContent {
            text = rememberRichTextState { setText("plain") }
            markdown = rememberRichTextState { setMarkdown("Hello **bold**") }
            fromDocument = rememberRichTextState { setRichTextDocument(document) }
        }
        waitForIdle()

        assertEquals("plain", text?.toText())
        assertEquals("<p>Hello <b>bold</b></p>", markdown?.toHtml())
        assertEquals(HTML, fromDocument?.toHtml())
    }

    @Test
    fun `the initial content is not an undo step`() = runDesktopComposeUiTest {
        var state: RichTextState? = null
        setContent {
            state = rememberRichTextState {
                setHtml(HTML)
                selection = TextRange(0, 5)
                toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
        }
        waitForIdle()

        assertFalse(assertNotNull(state).history.canUndo)
    }

    @Test
    fun `undo after typing returns to the initial content`() = runDesktopComposeUiTest {
        var state: RichTextState? = null
        setContent {
            val remembered = rememberRichTextState { setHtml(HTML) }
            state = remembered
            BasicRichTextEditor(state = remembered, modifier = Modifier.fillMaxWidth().testTag(EDITOR_TAG))
        }
        onNodeWithTag(EDITOR_TAG).performMouseInput { click(Offset(5f, 5f)) }
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performTextInput("x")
        waitForIdle()

        val editor = assertNotNull(state)
        editor.history.undo()

        assertEquals(HTML, editor.toHtml())
    }

    @Test
    fun `the block runs once across recompositions`() = runDesktopComposeUiTest {
        var runs = 0
        var tick by mutableIntStateOf(0)
        setContent {
            // Read so that every tick recomposes this scope.
            tick
            rememberRichTextState {
                runs++
                setHtml(HTML)
            }
        }
        waitForIdle()

        repeat(3) {
            tick++
            waitForIdle()
        }

        assertEquals(1, runs)
    }

    @Test
    fun `a restored state keeps the user's content and does not run the block again`() = runDesktopComposeUiTest {
        var runs = 0
        var state: RichTextState? = null
        // Stands in for the platform's save and restore: the content leaves the composition,
        // then comes back at the same place under a registry built from what the first saved.
        var registry by mutableStateOf(SaveableStateRegistry(restoredValues = null) { true })
        var shown by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (shown) {
                    state = rememberRichTextState {
                        runs++
                        setHtml(HTML)
                    }
                }
            }
        }
        waitForIdle()
        val beforeRestore = assertNotNull(state)
        beforeRestore.setHtml("<p>Edited by the user</p>")
        waitForIdle()

        val saved = registry.performSave()
        shown = false
        waitForIdle()
        registry = SaveableStateRegistry(restoredValues = saved) { true }
        shown = true
        waitForIdle()

        val restored = assertNotNull(state)
        assertNotSame(beforeRestore, restored)
        assertEquals("<p>Edited by the user</p>", restored.toHtml())
        assertEquals(1, runs)
    }

    private companion object {
        const val EDITOR_TAG = "editor"
        const val HTML = "<p>Hello <b>world</b></p><ul><li>Item</li></ul>"
    }
}
