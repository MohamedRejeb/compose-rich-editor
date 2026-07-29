package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression pin for https://github.com/MohamedRejeb/compose-rich-editor/issues/204.
 *
 * Cause: editor content was only observable through HTML/Markdown strings and mutable
 * state, so plain unit tests could not assert on content or styling reliably.
 * Fix: [RichTextState.toRichTextDocument] exposes an immutable structural snapshot with
 * value equality, usable in headless unit tests without composition.
 */
class Issue204DocumentTestabilityTest {

    @Test
    fun `paragraph style toggled in a plain unit test is visible in the document`() {
        val state = RichTextState().apply {
            setText("Hello")
            toggleParagraphStyle(currentParagraphStyle.copy(textAlign = TextAlign.Center))
        }
        assertEquals(TextAlign.Center, state.toRichTextDocument().blocks.single().textAlign)
    }

    @Test
    fun `two states with the same content are equal as documents`() {
        val a = RichTextState().apply { setHtml("<p>Hi <b>there</b></p>") }
        val b = RichTextState().apply {
            setText("Hi there")
            selection = TextRange(3, 8)
            toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        assertEquals(a.toRichTextDocument(), b.toRichTextDocument())
    }

    @Test
    fun `range overload extracts only the selected content`() {
        val state = RichTextState().apply { setHtml("<p>Hello World</p>") }
        assertEquals("Hello", state.toRichTextDocument(TextRange(0, 5)).blocks.single().text)
    }
}
