package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Issue 224: a typed or pasted URL stayed plain text until the app called one of the link
 * functions. With `RichTextConfig.autoLinkEnabled` the editor turns a URL into a link when a
 * space or Enter is typed after it, or when the pasted text is a URL. The link is recorded as
 * its own undo step after the edit, so one undo removes the link and keeps the text.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue224AutoLinkUrlsTest {

    private fun autoLinkState(text: String = "") = RichTextState().apply {
        config.autoLinkEnabled = true
        if (text.isNotEmpty()) setText(text)
    }

    /** One user edit through the pipeline, then the write-back the editor does after it. */
    private fun RichTextState.replace(range: TextRange, text: String) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.replace(range.min, range.max, text)
        buffer.selection = TextRange(range.min + text.length)
        applyChangeList(buffer)
        val caret = pendingSelectionDuringSync ?: buffer.selection
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(text = annotatedString.text, selection = caret)
    }

    private fun RichTextState.type(text: String) {
        text.forEach { replace(selection, it.toString()) }
    }

    private fun RichTextState.paste(text: String) = replace(selection, text)

    private fun RichTextState.links(): List<Pair<String, String>> {
        fun fullText(span: RichSpan): String = span.text + span.children.joinToString("") { fullText(it) }
        fun collect(span: RichSpan): List<Pair<String, String>> {
            val link = span.richSpanStyle as? RichSpanStyle.Link
            return if (link != null) listOf(fullText(span) to link.url)
            else span.children.flatMap { collect(it) }
        }
        return richParagraphList.flatMap { paragraph -> paragraph.children.flatMap { collect(it) } }
    }

    @Test
    fun `auto linking is on by default`() {
        val state = RichTextState()
        assertTrue(state.config.autoLinkEnabled)

        state.type("https://example.com ")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
    }

    @Test
    fun `nothing is linked when auto linking is turned off`() {
        val state = RichTextState().apply { config.autoLinkEnabled = false }

        state.type("https://example.com ")
        state.paste("https://example.com")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `a url followed by a space becomes a link`() {
        val state = autoLinkState()

        state.type("see https://example.com ")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals("see https://example.com ", state.annotatedString.text)
        assertEquals(TextRange(24), state.selection)
    }

    @Test
    fun `the url is not linked while it is being typed`() {
        val state = autoLinkState()

        state.type("https://example.com")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `a url followed by Enter becomes a link`() {
        val state = autoLinkState()

        state.type("https://example.com\nnext")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals(2, state.richParagraphList.size)
        assertEquals("https://example.com next", state.annotatedString.text)
    }

    @Test
    fun `a www address links to its https form`() {
        val state = autoLinkState()

        state.type("www.example.com ")

        assertEquals(listOf("www.example.com" to "https://www.example.com"), state.links())
    }

    @Test
    fun `trailing punctuation is left out of the link`() {
        val state = autoLinkState()

        state.type("(see https://example.com/a). ")

        assertEquals(listOf("https://example.com/a" to "https://example.com/a"), state.links())
    }

    @Test
    fun `text typed after the linked url is not part of the link`() {
        val state = autoLinkState()

        state.type("https://example.com and more")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals("https://example.com and more", state.annotatedString.text)
    }

    @Test
    fun `words that are not urls are not linked`() {
        val state = autoLinkState()

        state.type("example.com user@example.com e.g. http:// \n")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `a space typed in the middle of a paragraph links the url before it`() {
        val state = autoLinkState("https://example.comtail")
        state.selection = TextRange(19)

        state.type(" ")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals("https://example.com tail", state.annotatedString.text)
    }

    @Test
    fun `a url in a later paragraph and in a list item is linked`() {
        val state = autoLinkState("first")
        state.selection = TextRange(5)
        state.type("\nhttps://a.com ")
        assertEquals(listOf("https://a.com" to "https://a.com"), state.links())

        val list = autoLinkState()
        list.type("- www.example.com\n")
        assertIs<UnorderedList>(list.richParagraphList.first().type)
        assertEquals(listOf("www.example.com" to "https://www.example.com"), list.links())
    }

    @Test
    fun `one undo removes the link and keeps the typed text`() {
        val state = autoLinkState()
        state.type("https://example.com ")

        state.history.undo()

        assertEquals(emptyList(), state.links())
        assertEquals("https://example.com ", state.annotatedString.text)

        state.history.redo()
        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
    }

    @Test
    fun `typing goes on after the link is undone without linking it again`() {
        val state = autoLinkState()
        state.type("https://example.com ")
        state.history.undo()

        state.type("more ")

        assertEquals(emptyList(), state.links())
        assertEquals("https://example.com more ", state.annotatedString.text)
    }

    @Test
    fun `a pasted url becomes a link`() {
        val state = autoLinkState("see ")
        state.selection = TextRange(4)

        state.paste("https://example.com/path?q=1")

        assertEquals(listOf("https://example.com/path?q=1" to "https://example.com/path?q=1"), state.links())
        assertEquals("see https://example.com/path?q=1", state.annotatedString.text)
    }

    @Test
    fun `a pasted url surrounded by whitespace is linked without the whitespace`() {
        val state = autoLinkState()

        state.paste(" www.example.com ")

        assertEquals(listOf("www.example.com" to "https://www.example.com"), state.links())
    }

    @Test
    fun `a pasted url replaces the selection and is linked`() {
        val state = autoLinkState("see this now")
        state.selection = TextRange(4, 8)

        state.paste("https://example.com")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals("see https://example.com now", state.annotatedString.text)
    }

    @Test
    fun `pasted text that only contains a url is not linked`() {
        val state = autoLinkState()

        state.paste("see https://example.com for more")
        state.paste("https://example.com.")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `one undo after a pasted url removes the link and keeps the text`() {
        val state = autoLinkState()
        state.paste("https://example.com")

        state.history.undo()

        assertEquals(emptyList(), state.links())
        assertEquals("https://example.com", state.annotatedString.text)
    }

    @Test
    fun `a url pasted with html that has no link is linked`() {
        val state = autoLinkState("ab")
        state.selection = TextRange(1)
        state.pendingClipboardHtml = "<span>https://example.com</span>"
        state.pendingClipboardPlainText = "https://example.com"

        state.paste("https://example.com")

        assertEquals(listOf("https://example.com" to "https://example.com"), state.links())
        assertEquals("ahttps://example.comb", state.annotatedString.text)
    }

    @Test
    fun `a link pasted with html keeps its own target`() {
        val state = autoLinkState()
        state.pendingClipboardHtml = "<a href=\"https://target.com\">https://example.com</a>"
        state.pendingClipboardPlainText = "https://example.com"

        state.paste("https://example.com")

        assertEquals(listOf("https://example.com" to "https://target.com"), state.links())
    }

    @Test
    fun `no link is made when links are not an allowed feature`() {
        val state = autoLinkState().apply { config.features = RichTextFeature.All - RichTextFeature.Link }

        state.type("https://example.com ")
        state.paste("https://example.com")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `a url inside a code span is not linked`() {
        val state = autoLinkState("https://example.com")
        state.addRichSpan(RichSpanStyle.Code(), TextRange(0, 19))
        state.selection = TextRange(19)

        state.type(" ")

        assertEquals(emptyList(), state.links())
    }

    @Test
    fun `a space after an existing link leaves the link as it is`() {
        val state = autoLinkState()
        state.addLink(text = "https://example.com", url = "https://target.com")

        state.type(" next")

        assertEquals(listOf("https://example.com" to "https://target.com"), state.links())
        assertEquals("https://example.com next", state.annotatedString.text)
    }

    @Test
    fun `a word that is only partly a link is not linked`() {
        val state = autoLinkState("https://example.com/page")
        state.addLinkToTextRange(url = "https://target.com", textRange = TextRange(0, 19))
        state.selection = TextRange(24)

        state.type(" ")

        assertEquals(listOf("https://example.com" to "https://target.com"), state.links())
    }

    @Test
    fun `a space typed inside an existing link keeps the one link`() {
        val state = autoLinkState()
        state.addLink(text = "go www.example.com now", url = "https://target.com")
        state.selection = TextRange(18)

        state.type(" ")

        assertEquals(listOf("go www.example.com  now" to "https://target.com"), state.links())
    }

    @Test
    fun `a token whose label is a url is not linked`() {
        val state = autoLinkState()
        state.registerTrigger(Trigger(id = "site", char = 'w'))
        state.addTextAtIndex(0, "wx")
        state.insertToken(triggerId = "site", id = "1", label = "www.example.com")
        val tokenEnd = state.annotatedString.text.indexOf("www.example.com") + "www.example.com".length
        state.replace(TextRange(tokenEnd, state.annotatedString.text.length), "")
        state.selection = TextRange(tokenEnd)

        state.type(" ")

        assertEquals(emptyList(), state.links())
        assertEquals("www.example.com ", state.annotatedString.text)
    }

    @Test
    fun `a copy of the state keeps the setting`() {
        assertTrue(autoLinkState().copy().config.autoLinkEnabled)
    }
}
