package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
private class CodeBlockTestClipboard : Clipboard {
    val awt = java.awt.datatransfer.Clipboard("test")

    override val nativeClipboard: NativeClipboard get() = awt

    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        awt.setContents(clipEntry?.nativeClipEntry as? java.awt.datatransfer.Transferable, null)
    }
}

/**
 * Copy, cut and keyboard selection of code through the real editor: the clipboard gets the
 * code line by line, with its indentation.
 */
@OptIn(ExperimentalTestApi::class)
class CodeBlockCopyTest {

    private val markdown = "Before\n\n```kotlin\nval a = 1\n    val b = 2\n```\n\nAfter"

    @Test
    fun `copying a selection inside a block puts the code on the clipboard`() = runEditorTest { state, clipboard ->
        val text = state.annotatedString.text
        select(state, TextRange(text.indexOf("val a"), text.indexOf("val b = 2") + 9))

        pressShortcut(Key.C)

        assertEquals("val a = 1\n    val b = 2", clipboard.plainText())
        assertEquals(
            "<pre><code class=\"language-kotlin\">val a = 1\n    val b = 2</code></pre>",
            clipboard.html(),
        )
        assertEquals(markdown, state.toMarkdown())
    }

    @Test
    fun `copying across text and code puts both on the clipboard`() = runEditorTest { state, clipboard ->
        select(state, TextRange(0, state.annotatedString.text.length))

        pressShortcut(Key.C)

        assertEquals("Before\nval a = 1\n    val b = 2\nAfter", clipboard.plainText())
        assertEquals(
            "<p>Before</p><pre><code class=\"language-kotlin\">val a = 1\n    val b = 2</code></pre><p>After</p>",
            clipboard.html(),
        )
    }

    @Test
    fun `cutting a line of a block removes it and keeps the block`() = runEditorTest { state, clipboard ->
        val text = state.annotatedString.text
        select(state, TextRange(text.indexOf("val a"), text.indexOf("val b") - INDENT))

        pressShortcut(Key.X)

        assertEquals("val a = 1", clipboard.plainText())
        assertEquals("Before\n\n```kotlin\n    val b = 2\n```\n\nAfter", state.toMarkdown())
    }

    @Test
    fun `keyboard selection extends across the lines of a block`() = runEditorTest { state, _ ->
        val text = state.annotatedString.text
        select(state, TextRange(text.indexOf("val a")))

        onNodeWithTag(TAG).performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()

        assertEquals(TextRange(text.indexOf("val a"), text.indexOf("val b") - INDENT), state.selection)
        assertEquals("val a = 1", state.toText(state.selection))
    }

    private fun runEditorTest(
        block: DesktopComposeUiTest.(state: RichTextState, clipboard: CodeBlockTestClipboard) -> Unit,
    ) = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        val clipboard = CodeBlockTestClipboard()
        setContent {
            val focusRequester = remember { FocusRequester() }
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                Box {
                    BasicRichTextEditor(
                        state = state,
                        modifier = Modifier.focusRequester(focusRequester).testTag(TAG),
                    )
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()
        block(state, clipboard)
    }

    private fun DesktopComposeUiTest.select(state: RichTextState, range: TextRange) {
        state.textFieldState.edit { selection = range }
        waitForIdle()
    }

    private fun DesktopComposeUiTest.pressShortcut(key: Key) {
        onNodeWithTag(TAG).performKeyInput {
            keyDown(shortcutModifierKey)
            pressKey(key)
            keyUp(shortcutModifierKey)
        }
        waitForIdle()
    }

    private fun CodeBlockTestClipboard.plainText(): String {
        val contents = assertNotNull(awt.getContents(null), "Expected clipboard contents")
        assertTrue(contents.isDataFlavorSupported(DataFlavor.stringFlavor), "Expected plain text")
        return contents.getTransferData(DataFlavor.stringFlavor) as String
    }

    private fun CodeBlockTestClipboard.html(): String {
        val contents = assertNotNull(awt.getContents(null), "Expected clipboard contents")
        assertTrue(contents.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor), "Expected html on the clipboard")
        return contents.getTransferData(DataFlavor.fragmentHtmlFlavor) as String
    }

    private companion object {
        const val TAG = "editor"
        const val INDENT = 4

        // The framework maps clipboard shortcuts to Meta on macOS and to Ctrl elsewhere.
        val shortcutModifierKey: Key =
            if (System.getProperty("os.name") == "Mac OS X") Key.MetaLeft else Key.CtrlLeft
    }
}
