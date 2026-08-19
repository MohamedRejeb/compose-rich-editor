package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.FontRunStyle
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Editing semantics for app-defined [RichSpanStyle] classes: overlap replacement, coalescing,
 * edge acceptance, splitting, merging, and history. Assertions read the state through
 * [com.mohamedrejeb.richeditor.model.RichTextState.toRichTextDocument] custom marks.
 */
@OptIn(ExperimentalRichTextApi::class)
class CustomRichSpanStyleSemanticsTest {

    private fun RichTextState.customMarks(blockIndex: Int = 0): List<RichTextSpanMark.Custom> =
        toRichTextDocument().blocks[blockIndex].spans.filterIsInstance<RichTextSpanMark.Custom>()

    private fun documentOf(text: String, vararg spans: RichTextSpanMark): RichTextDocument =
        RichTextDocument(blocks = listOf(RichTextBlock(text = text, spans = spans.toList())))

    @Test
    fun `applying a custom span over an overlapping same class span replaces the overlap`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 8))
        state.addRichSpan(FontRunStyle(slug = "cairo"), TextRange(4, 11))

        assertEquals(
            listOf(0..3 to FontRunStyle(slug = "amiri"), 4..10 to FontRunStyle(slug = "cairo")),
            state.customMarks().map { it.range to it.style },
        )
    }

    @Test
    fun `typing at the end edge of an accepting custom span extends the span`() {
        val state = RichTextState().setRichTextDocument(
            documentOf(
                "Hello",
                RichTextSpanMark.Custom(range = 0..4, style = FontRunStyle(slug = "amiri")),
            ),
        )
        state.onTextFieldValueChange(TextFieldValue("Hello world", selection = TextRange(11)))

        assertEquals(listOf(0..10), state.customMarks().map { it.range })
    }

    @Test
    fun `typing at the end edge of a non accepting custom span leaves the span unchanged`() {
        val state = RichTextState().setRichTextDocument(
            documentOf(
                "Hello",
                RichTextSpanMark.Custom(
                    range = 0..4,
                    style = FontRunStyle(slug = "amiri", acceptsEdges = false),
                ),
            ),
        )
        state.onTextFieldValueChange(TextFieldValue("Hello world", selection = TextRange(11)))

        assertEquals(listOf(0..4), state.customMarks().map { it.range })
    }

    @Test
    fun `removing a custom span from a sub-range splits the span`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 11))
        state.removeRichSpan(FontRunStyle(slug = "amiri"), TextRange(4, 7))

        assertEquals(listOf(0..3, 7..10), state.customMarks().map { it.range })
        assertTrue(state.customMarks().all { it.style == FontRunStyle(slug = "amiri") })
    }

    @Test
    fun `deleting across a custom span boundary keeps the style on the remaining text`() {
        val state = RichTextState().setRichTextDocument(
            documentOf(
                "Hello world",
                RichTextSpanMark.Custom(range = 6..10, style = FontRunStyle(slug = "amiri")),
            ),
        )
        state.onTextFieldValueChange(TextFieldValue("Helrld", selection = TextRange(3)))

        assertEquals("Helrld", state.toText())
        assertEquals(listOf(3..5), state.customMarks().map { it.range })
    }

    @Test
    fun `pressing enter inside a custom span keeps the style on both halves`() {
        val state = RichTextState().setRichTextDocument(
            documentOf(
                "Hello world",
                RichTextSpanMark.Custom(range = 0..10, style = FontRunStyle(slug = "amiri")),
            ),
        )
        state.onTextFieldValueChange(TextFieldValue("Hello\n world", selection = TextRange(6)))

        val document = state.toRichTextDocument()
        assertEquals(2, document.blocks.size)
        assertEquals(listOf(0..4), state.customMarks(blockIndex = 0).map { it.range })
        assertEquals(
            listOf(FontRunStyle(slug = "amiri")),
            state.customMarks(blockIndex = 1).map { it.style },
        )
    }

    @Test
    fun `pressing enter inside a nested segment keeps the custom style on detached siblings`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 11))
        state.selection = TextRange(0, 4)
        state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))

        state.onTextFieldValueChange(TextFieldValue("He\nllo world", selection = TextRange(3)))

        val document = state.toRichTextDocument()
        assertEquals(2, document.blocks.size)
        assertEquals(listOf(0..1), state.customMarks(blockIndex = 0).map { it.range })
        assertEquals(listOf(0..8), state.customMarks(blockIndex = 1).map { it.range })
    }

    @Test
    fun `merging two paragraphs with the same custom style coalesces into one run`() {
        val state = RichTextState().setRichTextDocument(
            RichTextDocument(
                blocks = listOf(
                    RichTextBlock(
                        text = "Hello",
                        spans = listOf(
                            RichTextSpanMark.Custom(range = 0..4, style = FontRunStyle(slug = "amiri")),
                        ),
                    ),
                    RichTextBlock(
                        text = "world",
                        spans = listOf(
                            RichTextSpanMark.Custom(range = 0..4, style = FontRunStyle(slug = "amiri")),
                        ),
                    ),
                ),
            ),
        )
        state.onTextFieldValueChange(TextFieldValue("Helloworld", selection = TextRange(5)))

        val document = state.toRichTextDocument()
        assertEquals(1, document.blocks.size)
        assertEquals(listOf(0..9), state.customMarks().map { it.range })
    }

    @Test
    fun `undo removes a custom span and redo restores it`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(0, 5)
        state.addRichSpan(FontRunStyle(slug = "amiri"))
        val withSpan = state.toRichTextDocument()

        state.history.undo()
        assertTrue(state.customMarks().isEmpty())

        state.history.redo()
        assertEquals(withSpan, state.toRichTextDocument())
    }

    @Test
    fun `re-applying an equal identity instance with a new resource replaces the stored instance`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri", fontFamily = null), TextRange(0, 5))
        val resolved = FontRunStyle(slug = "amiri", fontFamily = FontFamily.Serif)
        state.addRichSpan(resolved, TextRange(0, 5))

        assertSame(resolved, state.customMarks().single().style)
    }
}
