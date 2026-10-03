package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [RichTextConfig.features] at every content entry: the HTML, Markdown and document loads,
 * the HTML and Markdown inserts, and a recognized clipboard paste through both the legacy
 * bridge and the ChangeList pipeline. An empty set turns a rich paste into the plain paste.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextFeaturesStateTest {

    private val richHtml =
        "<h1><u>Hi</u> <i>there</i> <span style=\"color:#ff0000\">red</span></h1><ul><li><b>item</b></li></ul>"

    private val boldOnly = setOf(RichTextFeature.Bold)

    private fun boldOnlyState(): RichTextState = RichTextState().apply { config.features = boldOnly }

    private fun RichTextState.blocks(): List<RichTextBlock> = toRichTextDocument().blocks

    @Test
    fun `features default to All`() {
        assertEquals(RichTextFeature.All, RichTextState().config.features)
    }

    @Test
    fun `setHtml keeps only the allowed features`() {
        val state = RichTextState().apply { config.features = setOf(RichTextFeature.Underline) }

        state.setHtml(richHtml)

        assertEquals(
            listOf(
                RichTextBlock("Hi there red", spans = listOf(RichTextSpanMark.Underline(0..1))),
                RichTextBlock("item"),
            ),
            state.blocks(),
        )
    }

    @Test
    fun `setMarkdown keeps only the allowed features`() {
        val state = RichTextState().apply { config.features = setOf(RichTextFeature.Italic) }

        state.setMarkdown("- **li** *lo*\n- la")

        assertEquals(
            listOf(
                RichTextBlock("li lo", spans = listOf(RichTextSpanMark.Italic(3..4))),
                RichTextBlock("la"),
            ),
            state.blocks(),
        )
    }

    @Test
    fun `setRichTextDocument keeps only the allowed features`() {
        val state = boldOnlyState()

        state.setRichTextDocument(
            RichTextDocument(
                listOf(
                    RichTextBlock(
                        "ab",
                        type = RichTextBlockType.ListItem(ordered = true),
                        headingLevel = 2,
                        spans = listOf(RichTextSpanMark.Bold(0..0), RichTextSpanMark.Underline(1..1)),
                    ),
                ),
            ),
        )

        assertEquals(listOf(RichTextBlock("ab", spans = listOf(RichTextSpanMark.Bold(0..0)))), state.blocks())
    }

    @Test
    fun `insertHtml keeps only the allowed features`() {
        val state = boldOnlyState()
        state.setText("ab")

        state.insertHtml("<b>X</b><i>Y</i>", position = 1)

        assertEquals(
            listOf(RichTextBlock("aXYb", spans = listOf(RichTextSpanMark.Bold(1..1)))),
            state.blocks(),
        )
    }

    @Test
    fun `insertMarkdownAfterSelection keeps only the allowed features`() {
        val state = boldOnlyState()
        state.setText("ab", TextRange(2))

        state.insertMarkdownAfterSelection("**X**_Y_")

        assertEquals(
            listOf(RichTextBlock("abXY", spans = listOf(RichTextSpanMark.Bold(2..2)))),
            state.blocks(),
        )
    }

    @Test
    fun `a recognized paste through the legacy bridge keeps only the allowed features`() {
        val state = boldOnlyState()
        state.setText("Hello ")
        state.pendingClipboardHtml = "<b>bold</b> <i>it</i>"
        state.pendingClipboardPlainText = "bold it"

        state.onTextFieldValueChange(TextFieldValue("Hello bold it", selection = TextRange(13)))

        assertEquals(
            listOf(RichTextBlock("Hello bold it", spans = listOf(RichTextSpanMark.Bold(6..9)))),
            state.blocks(),
        )
    }

    @Test
    fun `a recognized paste through the pipeline keeps only the allowed features`() {
        val state = boldOnlyState()
        state.setText("abcd", TextRange(2))
        state.pendingClipboardHtml = "<ul><li><b>X</b><u>Y</u></li></ul>"
        state.pendingClipboardPlainText = "XY"

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 2, "XY")
        state.applyChangeList(buffer)

        assertEquals(
            listOf(RichTextBlock("abXYcd", spans = listOf(RichTextSpanMark.Bold(2..2)))),
            state.blocks(),
        )
    }

    @Test
    fun `an empty feature set pastes plain text that inherits the caret style`() {
        val state = RichTextState()
        state.setText("Hello")
        state.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold), TextRange(0, 5))
        state.config.features = emptySet()
        state.selection = TextRange(5)
        state.pendingClipboardHtml = "<i>x</i>"
        state.pendingClipboardPlainText = "x"

        state.onTextFieldValueChange(TextFieldValue("Hellox", selection = TextRange(6)))

        assertEquals(
            listOf(RichTextBlock("Hellox", spans = listOf(RichTextSpanMark.Bold(0..5)))),
            state.blocks(),
        )
    }

    @Test
    fun `an empty feature set pastes plain text through the pipeline too`() {
        val state = RichTextState().apply { config.features = emptySet() }
        state.setText("ab", TextRange(1))
        state.pendingClipboardHtml = "<b>X</b>"
        state.pendingClipboardPlainText = "X"

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(1, 1, "X")
        state.applyChangeList(buffer)

        assertEquals(listOf(RichTextBlock("aXb")), state.blocks())
    }

    @Test
    fun `an empty feature set clears the clipboard stash after the paste`() {
        val state = RichTextState().apply { config.features = emptySet() }
        state.setText("ab", TextRange(1))
        state.pendingClipboardHtml = "<b>X</b>"
        state.pendingClipboardPlainText = "X"

        state.onTextFieldValueChange(TextFieldValue("aXb", selection = TextRange(2)))

        assertEquals(null, state.pendingClipboardHtml)
        assertEquals(null, state.pendingClipboardPlainText)
    }

    @Test
    fun `an edit that is not the announced paste stays plain and drops the stash`() {
        val state = boldOnlyState()
        state.setText("ab", TextRange(1))
        state.pendingClipboardHtml = "<b>X</b>"
        state.pendingClipboardPlainText = "X"

        state.onTextFieldValueChange(TextFieldValue("aYb", selection = TextRange(2)))

        assertEquals(listOf(RichTextBlock("aYb")), state.blocks())
        assertEquals(null, state.pendingClipboardHtml)
    }

    @Test
    fun `insertHtmlAfterSelection and insertMarkdown at a position keep only the allowed features`() {
        val state = boldOnlyState()
        state.setText("ab", TextRange(1))

        state.insertHtmlAfterSelection("<b>X</b><i>Y</i>")
        state.insertMarkdown("**M**_N_", position = 0)

        assertEquals(
            listOf(RichTextBlock("MNaXYb", spans = listOf(RichTextSpanMark.Bold(0..0), RichTextSpanMark.Bold(3..3)))),
            state.blocks(),
        )
    }

    @Test
    fun `an allowed ordered list keeps its start number through the admission round trip`() {
        val state = RichTextState().apply { config.features = setOf(RichTextFeature.OrderedList) }

        state.setHtml("<ol start=\"3\"><li><b>a</b></li><li>b</li></ol>")

        assertEquals("<ol start=\"3\"><li>a</li><li>b</li></ol>", state.toHtml())
    }

    @Test
    fun `All leaves loaded content identical to an unrestricted state`() {
        val restricted = RichTextState().apply { config.features = RichTextFeature.entries.toSet() }
        val reference = RichTextState()

        restricted.setHtml(richHtml)
        reference.setHtml(richHtml)

        assertEquals(reference.toRichTextDocument(), restricted.toRichTextDocument())
        assertTrue(restricted.blocks().first().headingLevel == 1)
    }

    @Test
    fun `copy carries the feature set`() {
        val state = boldOnlyState()

        assertEquals(boldOnly, state.copy().config.features)
    }
}
