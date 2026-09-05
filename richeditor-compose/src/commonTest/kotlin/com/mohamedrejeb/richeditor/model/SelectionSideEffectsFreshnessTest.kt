package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Derived state and the buffer must agree with the selection the user actually has, on the two
 * paths where the selection reaches [RichTextState] without a full [updateTextFieldValue] pass:
 * gated mid-drag observer ticks, and a document load.
 */
class SelectionSideEffectsFreshnessTest {

    /** One observer tick as BTF2 delivers it: buffer first, handler second. */
    private fun RichTextState.tick(newSelection: TextRange) {
        val previous = isApplyingProgrammaticSync
        isApplyingProgrammaticSync = true
        try {
            textFieldState.edit { selection = newSelection }
        } finally {
            isApplyingProgrammaticSync = previous
        }
        handleSelectionChanged(newSelection, fromGestureObserver = true)
    }

    /**
     * Every tick after the first is gated by the mid-drag rule. The legacy mirror still feeds
     * the style mutators, so it has to keep up even on the gated ticks: otherwise the toggle
     * writes the stale range back into the buffer and the user watches their selection snap
     * back to where the drag was two ticks ago.
     */
    @Test
    fun `a style toggle after gated drag ticks keeps the full selection`() {
        val state = RichTextState()
        state.setText("hello world this is text")

        state.tick(TextRange(0, 5))
        state.tick(TextRange(0, 8))
        state.tick(TextRange(0, 11))

        assertEquals(TextRange(0, 11), state.selection)

        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        assertEquals(
            TextRange(0, 11),
            state.selection,
            "The toggle must not push a stale selection back into the buffer",
        )
        assertEquals(
            FontWeight.Bold,
            state.currentSpanStyle.fontWeight,
            "currentSpanStyle must describe the whole selected range",
        )
        assertTrue(
            state.annotatedString.spanStyles.any {
                it.item.fontWeight == FontWeight.Bold && it.start <= 0 && it.end >= 11
            },
            "The bold must cover the range the user had selected. " +
                "Spans: ${state.annotatedString.spanStyles.map { "(${it.start}..${it.end} ${it.item.fontWeight})" }}",
        )
    }

    /**
     * A document load writes the buffer directly. The selection handler dedupes against the last
     * selection it processed, so the load has to advance that marker; otherwise a caret move to
     * the document start collides with its initial [TextRange.Zero] value and every derived
     * style silently keeps describing wherever the load left the caret.
     */
    @Test
    fun `moving the caret to the document start after a load refreshes the derived styles`() {
        val state = RichTextState()
        state.setHtml("<ol><li>one</li></ol><p>two</p>")

        assertEquals(
            false,
            state.isOrderedList,
            "The load leaves the caret in the trailing plain paragraph",
        )

        state.selection = TextRange(0)

        assertEquals(
            true,
            state.isOrderedList,
            "The caret is in the ordered list item now, so the derived paragraph state must say so",
        )
    }
}
