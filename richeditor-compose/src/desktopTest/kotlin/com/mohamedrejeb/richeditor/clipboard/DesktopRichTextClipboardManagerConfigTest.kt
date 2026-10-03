package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun htmlTransferable(html: String, text: String): Transferable = object : StringSelection(text) {
    override fun getTransferDataFlavors(): Array<DataFlavor> =
        arrayOf(DataFlavor.fragmentHtmlFlavor, DataFlavor.stringFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor in getTransferDataFlavors()

    override fun getTransferData(flavor: DataFlavor?): Any =
        if (flavor == DataFlavor.fragmentHtmlFlavor) html else text
}

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

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, ExperimentalRichTextApi::class)
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

    @Test
    fun `reading the clipboard stashes html for a rich paste`() = runBlocking {
        val state = RichTextState()
        val clipboard = FakeClipboard()
        clipboard.awt.setContents(htmlTransferable("<b>Hi</b>", "Hi"), null)
        val manager = createRichTextClipboardManager(state, clipboard)

        manager.getClipEntry()

        assertEquals("<b>Hi</b>", state.pendingClipboardHtml)
        assertEquals("Hi", state.pendingClipboardPlainText)
    }

    @Test
    fun `reading the clipboard stashes nothing when no feature is allowed`() = runBlocking {
        val state = RichTextState()
        state.config.features = emptySet()
        val clipboard = FakeClipboard()
        clipboard.awt.setContents(htmlTransferable("<b>Hi</b>", "Hi"), null)
        val manager = createRichTextClipboardManager(state, clipboard)

        manager.getClipEntry()

        assertNull(state.pendingClipboardHtml)
        assertNull(state.pendingClipboardPlainText)
    }
}
