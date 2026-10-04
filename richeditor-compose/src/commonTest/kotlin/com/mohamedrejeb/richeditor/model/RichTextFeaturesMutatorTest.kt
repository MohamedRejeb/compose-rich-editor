package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The formatting mutators honor [RichTextConfig.features]: adding a disallowed feature is a
 * no-op (a partially allowed [SpanStyle] applies its allowed fields), removing always works,
 * and the "- " / "1. " auto list trigger respects the list features. Together with the
 * content filter this makes the set an invariant of the editor's content.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextFeaturesMutatorTest {

    private object Marker : RichSpanStyle {
        override fun getSpanStyle(config: RichTextConfig): SpanStyle = SpanStyle()
    }

    private val bold = SpanStyle(fontWeight = FontWeight.Bold)

    private fun stateWith(vararg features: RichTextFeature, text: String = "Hello"): RichTextState =
        RichTextState().apply {
            setText(text, TextRange(0, text.length))
            config.features = features.toSet()
        }

    private fun RichTextState.block(): RichTextBlock = toRichTextDocument().blocks.single()

    private fun RichTextState.spans(): List<RichTextSpanMark> = block().spans

    private fun RichTextState.imeBatch(edit: TextFieldBuffer.() -> Unit) {
        val buffer = textFieldState.toTextFieldBuffer()
        buffer.edit()
        applyChangeList(buffer)
        reconcileBufferWithModel(buffer)
        pendingSelectionDuringSync = null
        setTextFieldStateFromValue(buffer.asCharSequence().toString(), buffer.selection)
        handleSelectionChanged(textFieldState.selection, fromGestureObserver = true)
    }

    @Test
    fun `toggleSpanStyle applies only the allowed fields`() {
        val state = stateWith(RichTextFeature.Bold)

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red))

        assertEquals(listOf(RichTextSpanMark.Bold(0..4)), state.spans())
    }

    @Test
    fun `toggleSpanStyle of a disallowed style is a no-op without an undo entry`() {
        val state = stateWith(RichTextFeature.Italic)

        state.toggleSpanStyle(bold)

        assertEquals(emptyList(), state.spans())
        assertFalse(state.history.canUndo)
    }

    @Test
    fun `a staged disallowed style does not reach typed text`() {
        val state = stateWith(RichTextFeature.Italic)
        state.selection = TextRange(5)

        state.toggleSpanStyle(bold)
        state.onTextFieldValueChange(TextFieldValue("Hellox", selection = TextRange(6)))

        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `addSpanStyle on a range keeps only the allowed fields`() {
        val state = stateWith(RichTextFeature.TextColor)

        state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red), TextRange(1, 3))

        assertEquals(listOf(RichTextSpanMark.TextColor(1..2, argb = 0xFFFF0000)), state.spans())
    }

    @Test
    fun `removeSpanStyle still removes styling the set no longer allows`() {
        val state = RichTextState()
        state.setHtml("<b>Hello</b>")
        state.config.features = setOf(RichTextFeature.Italic)

        state.removeSpanStyle(bold, TextRange(0, 5))

        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `a combined text decoration keeps only the allowed half`() {
        val state = stateWith(RichTextFeature.Underline)

        state.toggleSpanStyle(
            SpanStyle(textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))),
        )

        assertEquals(listOf(RichTextSpanMark.Underline(0..4)), state.spans())
    }

    @Test
    fun `rich spans of a disallowed kind are not added`() {
        val state = stateWith(RichTextFeature.Bold)

        state.toggleCodeSpan()
        state.addLinkToSelection("https://example.com")
        state.addRichSpan(Marker)
        state.addRichSpan(Marker, TextRange(0, 2))

        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `a disallowed link is not staged for typed text either`() {
        val state = stateWith(RichTextFeature.Bold)
        state.selection = TextRange(5)

        state.addLinkToSelection("https://example.com")
        state.onTextFieldValueChange(TextFieldValue("Hellox", selection = TextRange(6)))

        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `custom rich spans need the CustomSpanStyle feature`() {
        val state = stateWith(RichTextFeature.CustomSpanStyle)

        state.addRichSpan(Marker)

        assertEquals(listOf(RichTextSpanMark.Custom(0..4, Marker)), state.spans())
    }

    @Test
    fun `addLink inserts plain text when links are disallowed`() {
        val state = stateWith(RichTextFeature.Bold)
        state.selection = TextRange(5)

        state.addLink(text = " site", url = "https://example.com")

        assertEquals("Hello site", state.toText())
        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `addLinkToTextRange is a no-op when links are disallowed and works when allowed`() {
        val restricted = stateWith(RichTextFeature.Bold)
        restricted.addLinkToTextRange("https://example.com", TextRange(0, 2))
        assertEquals(emptyList(), restricted.spans())

        val allowed = stateWith(RichTextFeature.Link)
        allowed.addLinkToTextRange("https://example.com", TextRange(0, 2))
        assertEquals(listOf(RichTextSpanMark.Link(0..1, url = "https://example.com")), allowed.spans())
    }

    @Test
    fun `addLink with links disallowed leaves the caret after the inserted text`() {
        val state = stateWith(RichTextFeature.Bold)
        state.selection = TextRange(5)

        state.addLink(text = " site", url = "https://example.com")

        assertEquals(TextRange(10), state.selection)
    }

    @Test
    fun `a staged allowed style still reaches typed text under a restricted set`() {
        val state = stateWith(RichTextFeature.Bold)
        state.selection = TextRange(5)

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red))
        state.onTextFieldValueChange(TextFieldValue("Hellox", selection = TextRange(6)))

        assertEquals(listOf(RichTextSpanMark.Bold(5..5)), state.spans())
    }

    @Test
    fun `a disallowed token is not inserted and the trigger query is cancelled`() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.config.features = setOf(RichTextFeature.Bold)
        state.addTextAtIndex(0, "Hi @moh")

        state.insertToken(triggerId = "mention", id = "u1", label = "@mohamed")

        assertEquals("Hi @moh", state.toText())
        assertEquals(null, state.activeTriggerQuery)
        assertEquals(emptyList(), state.spans())
    }

    @Test
    fun `setHeadingStyle is a no-op when headings are disallowed and Normal still applies`() {
        val state = RichTextState()
        state.setHtml("<h2>Title</h2>")
        state.config.features = setOf(RichTextFeature.Bold)
        state.selection = TextRange(0, 5)

        state.setHeadingStyle(HeadingStyle.H1)
        assertEquals(2, state.block().headingLevel)

        state.setHeadingStyle(HeadingStyle.Normal)
        assertEquals(0, state.block().headingLevel)
    }

    @Test
    fun `addParagraphStyle is a no-op when paragraph style is disallowed and removal still works`() {
        val state = RichTextState()
        state.setHtml("<p style=\"text-align:center\">Hello</p>")
        state.config.features = setOf(RichTextFeature.Bold)
        state.selection = TextRange(0, 5)

        state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.End))
        assertEquals(TextAlign.Center, state.block().textAlign)

        state.removeParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
        assertEquals(TextAlign.Unspecified, state.block().textAlign)
    }

    @Test
    fun `list toggles do not create a disallowed list but still remove an existing one`() {
        val state = stateWith(RichTextFeature.Bold)

        state.toggleOrderedList()
        state.toggleUnorderedList()
        assertEquals(RichTextBlockType.Paragraph, state.block().type)

        state.config.features = RichTextFeature.All
        state.toggleUnorderedList()
        state.config.features = setOf(RichTextFeature.Bold)
        state.toggleUnorderedList()
        assertEquals(RichTextBlockType.Paragraph, state.block().type)
    }

    @Test
    fun `typing dash space starts a list only when unordered lists are allowed`() {
        val restricted = RichTextState().apply { config.features = setOf(RichTextFeature.OrderedList) }
        restricted.imeBatch { replace(0, 0, "-"); selection = TextRange(1) }
        restricted.imeBatch { replace(1, 1, " "); selection = TextRange(2) }
        assertEquals("- ", restricted.toText())
        assertEquals(RichTextBlockType.Paragraph, restricted.block().type)

        val allowed = RichTextState()
        allowed.imeBatch { replace(0, 0, "-"); selection = TextRange(1) }
        allowed.imeBatch { replace(1, 1, " "); selection = TextRange(2) }
        assertTrue(allowed.block().type is RichTextBlockType.ListItem)
    }

    @Test
    fun `typing a number dot space starts a list only when ordered lists are allowed`() {
        val restricted = RichTextState().apply { config.features = setOf(RichTextFeature.UnorderedList) }
        restricted.imeBatch { replace(0, 0, "1"); selection = TextRange(1) }
        restricted.imeBatch { replace(1, 1, "."); selection = TextRange(2) }
        restricted.imeBatch { replace(2, 2, " "); selection = TextRange(3) }

        assertEquals("1. ", restricted.toText())
        assertEquals(RichTextBlockType.Paragraph, restricted.block().type)
    }
}
