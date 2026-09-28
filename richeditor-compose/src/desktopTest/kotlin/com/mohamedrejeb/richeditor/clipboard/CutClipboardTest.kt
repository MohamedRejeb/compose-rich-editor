package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class RecordingClipboard : Clipboard {
    val awt = java.awt.datatransfer.Clipboard("test")

    override val nativeClipboard: NativeClipboard get() = awt

    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
}

/**
 * Cut and copy through the real editor, by shortcut and by semantics action.
 *
 * BTF2 deletes the selection before it hands the cut text to the clipboard, so the clipboard
 * manager runs after the model has dropped the content that was cut.
 */
@OptIn(ExperimentalTestApi::class)
class CutClipboardTest {

    @Test
    fun `rich cut puts the cut content on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        val range = TextRange(0, 5)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.X)

        assertEquals(" World", state.annotatedString.text)
        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `plain cut puts the cut text on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        state.config.richClipboardEnabled = false
        select(state, TextRange(0, 5))

        pressShortcut(Key.X)

        assertEquals(" World", state.annotatedString.text)
        assertEquals("Hello", clipboard.plainText())
        assertFalse(clipboard.hasHtml(), "A plain cut must not write html")
    }

    @Test
    fun `rich cut of a reversed selection puts the cut content on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        val expectedHtml = state.toHtml(TextRange(0, 5))
        select(state, TextRange(5, 0))
        assertEquals(TextRange(5, 0), state.selection)

        pressShortcut(Key.X)

        assertEquals(" World", state.annotatedString.text)
        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `plain cut across paragraphs puts newline joined text on the clipboard`() = runClipboardTest(
        html = "<p>Hello</p><p>World</p><p>Again</p>",
    ) { state, clipboard ->
        state.config.richClipboardEnabled = false
        select(state, TextRange(0, "Hello World".length))

        pressShortcut(Key.X)

        assertEquals(" Again", state.annotatedString.text)
        assertEquals("Hello\nWorld", clipboard.plainText())
        assertFalse(clipboard.hasHtml(), "A plain cut must not write html")
    }

    @Test
    fun `rich cut of the whole document puts every paragraph on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b></p><p>World</p>",
    ) { state, clipboard ->
        val range = TextRange(0, state.annotatedString.text.length)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.X)

        assertEquals("", state.annotatedString.text)
        assertEquals("Hello\nWorld", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `undo after a cut restores the text and keeps the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        val htmlBefore = state.toHtml()
        val range = TextRange(0, 5)
        val expectedHtml = state.toHtml(range)
        select(state, range)
        pressShortcut(Key.X)
        assertEquals(" World", state.annotatedString.text)

        pressShortcut(Key.Z)

        assertEquals(htmlBefore, state.toHtml())
        assertEquals("Hello World", state.textFieldState.text.toString())
        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `rich cut across paragraphs puts every paragraph on the clipboard`() = runClipboardTest(
        html = "<p>Hello</p><p>World</p><p>Again</p>",
    ) { state, clipboard ->
        val range = TextRange(0, "Hello World".length)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.X)

        // The separator after the second paragraph is outside the range, so its line stays.
        assertEquals(" Again", state.annotatedString.text)
        assertEquals("Hello\nWorld", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `rich cut of a list item puts the item on the clipboard`() = runClipboardTest(
        html = "<ul><li>One</li><li>Two</li></ul>",
    ) { state, clipboard ->
        val start = state.annotatedString.text.indexOf("Two")
        val range = TextRange(start, start + "Two".length)
        val expectedHtml = state.toHtml(range)
        // The plain text of a list item carries its marker, the same as on copy.
        val expectedText = state.toText(range)
        assertTrue("Two" in expectedText)
        select(state, range)

        pressShortcut(Key.X)

        assertEquals(expectedText, clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `rich copy puts the selection on the clipboard and keeps the text`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        val range = TextRange(0, 5)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.C)

        assertEquals("Hello World", state.annotatedString.text)
        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `copy after deleting a selection puts the copied selection on the clipboard`() = runClipboardTest(
        html = "<p>Hello <b>World</b></p>",
    ) { state, clipboard ->
        select(state, TextRange(6, 11))
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals("Hello ", state.annotatedString.text)

        val range = TextRange(0, 5)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.C)

        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `collapsing copy after deleting a selection does not put the deleted content on the clipboard`() =
        runClipboardTest(
            html = "<p>Hello <b>World</b></p>",
        ) { state, clipboard ->
            select(state, TextRange(6, 11))
            onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Backspace) }
            waitForIdle()
            assertEquals(TextRange(6), state.selection)

            // Collapses to the caret the deletion left behind, on the text it left behind:
            // the state a cut ends in.
            select(state, TextRange(0, 6))
            performCollapsingCopy()

            assertEquals(TextRange(6), state.selection)
            assertNotEquals("World", clipboard.plainText())
        }

    @Test
    fun `cut through the semantics action puts the cut content on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        val range = TextRange(0, 5)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        onNodeWithTag(EDITOR_TAG).performSemanticsAction(SemanticsActions.CutText)
        waitForIdle()

        assertEquals(" World", state.annotatedString.text)
        assertEquals("Hello", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    @Test
    fun `cut after cut puts the second cut on the clipboard`() = runClipboardTest(
        html = "<p><b>Hello</b> World</p>",
    ) { state, clipboard ->
        select(state, TextRange(0, 5))
        pressShortcut(Key.X)
        assertEquals("Hello", clipboard.plainText())

        val range = TextRange(1, 6)
        val expectedHtml = state.toHtml(range)
        select(state, range)

        pressShortcut(Key.X)

        assertEquals(" ", state.annotatedString.text)
        assertEquals("World", clipboard.plainText())
        assertEquals(expectedHtml, clipboard.html())
    }

    private fun runClipboardTest(
        html: String,
        block: DesktopComposeUiTest.(state: RichTextState, clipboard: RecordingClipboard) -> Unit,
    ) = runDesktopComposeUiTest {
        val state = RichTextState()
        state.setHtml(html)
        val clipboard = RecordingClipboard()

        setContent {
            val focusRequester = remember { FocusRequester() }
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                Box {
                    BasicRichTextEditor(
                        state = state,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .testTag(EDITOR_TAG),
                    )
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()

        block(state, clipboard)
    }

    /**
     * A programmatic write: unlike a gesture it does not run the InputTransformation, so
     * whatever an earlier edit left pending is still there when the clipboard is written.
     */
    private fun DesktopComposeUiTest.select(state: RichTextState, range: TextRange) {
        state.textFieldState.edit { selection = range }
        waitForIdle()
    }

    private fun DesktopComposeUiTest.pressShortcut(key: Key) {
        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(shortcutModifierKey)
            pressKey(key)
            keyUp(shortcutModifierKey)
        }
        waitForIdle()
    }

    /**
     * The copy the touch toolbar runs: unlike the keyboard shortcut it collapses the selection
     * before the clipboard is written.
     */
    private fun DesktopComposeUiTest.performCollapsingCopy() {
        onNodeWithTag(EDITOR_TAG).performSemanticsAction(SemanticsActions.CopyText)
        waitForIdle()
    }

    private fun RecordingClipboard.plainText(): String {
        val contents = assertNotNull(awt.getContents(null), "Expected clipboard contents")
        assertTrue(contents.isDataFlavorSupported(DataFlavor.stringFlavor), "Expected plain text")
        return contents.getTransferData(DataFlavor.stringFlavor) as String
    }

    private fun RecordingClipboard.hasHtml(): Boolean =
        awt.getContents(null)?.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) == true

    private fun RecordingClipboard.html(): String {
        assertTrue(hasHtml(), "Expected html on the clipboard")
        return awt.getContents(null).getTransferData(DataFlavor.fragmentHtmlFlavor) as String
    }

    private companion object {
        const val EDITOR_TAG = "editor"

        // The framework maps clipboard shortcuts to Meta on macOS and to Ctrl elsewhere.
        val shortcutModifierKey: Key =
            if (System.getProperty("os.name") == "Mac OS X") Key.MetaLeft else Key.CtrlLeft
    }
}
