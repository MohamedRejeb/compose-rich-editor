package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class FakeClipboard : Clipboard {
    val awt = java.awt.datatransfer.Clipboard("test")
    var delegatedEntry: ClipEntry? = null
    var delegateCalls = 0

    override val nativeClipboard: NativeClipboard get() = awt

    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        delegatedEntry = clipEntry
        delegateCalls++
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
class DesktopRichTextClipboardManagerConfigTest {

    private fun stateWithSelection(): RichTextState {
        val state = RichTextState()
        state.setHtml("<p><b>Hello</b> world</p>")
        state.selection = TextRange(0, 5)
        return state
    }

    @Test
    fun `enabled rich clipboard writes html to the awt clipboard`() = runBlocking {
        val state = stateWithSelection()
        val clipboard = FakeClipboard()
        val manager = createRichTextClipboardManager(state, clipboard)

        manager.setClipEntry(ClipEntry(StringSelection("Hello")))

        val contents = clipboard.awt.getContents(null)
        assertTrue(contents != null && contents.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor))
        assertEquals(0, clipboard.delegateCalls)
    }

    @Test
    fun `disabled rich clipboard writes newline joined plain text without html`() = runBlocking {
        val state = RichTextState()
        state.setHtml("<p>Hello</p><p>World</p>")
        state.selection = TextRange(0, state.annotatedString.text.length)
        state.config.richClipboardEnabled = false
        val clipboard = FakeClipboard()
        val manager = createRichTextClipboardManager(state, clipboard)

        manager.setClipEntry(ClipEntry(StringSelection("raw")))

        val contents = clipboard.awt.getContents(null)
        assertTrue(contents != null && !contents.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor))
        assertEquals(
            "Hello\nWorld",
            contents.getTransferData(DataFlavor.stringFlavor) as String,
        )
        assertEquals(0, clipboard.delegateCalls)
    }

    @Test
    fun `disabled rich clipboard without a selection delegates the raw entry`() = runBlocking {
        val state = RichTextState()
        state.setText("Hello")
        state.config.richClipboardEnabled = false
        val clipboard = FakeClipboard()
        val manager = createRichTextClipboardManager(state, clipboard)

        val entry = ClipEntry(StringSelection("raw"))
        manager.setClipEntry(entry)

        assertEquals(1, clipboard.delegateCalls)
        assertSame(entry, clipboard.delegatedEntry)
    }
}
