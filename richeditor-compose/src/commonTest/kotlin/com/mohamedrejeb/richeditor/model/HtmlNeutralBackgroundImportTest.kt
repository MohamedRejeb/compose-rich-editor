package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins for neutral-background filtering on HTML import (see PR #723, credit tfkci):
 * white/transparent backgrounds in clipboard HTML are the source document's page
 * background, not a highlight; importing them paints opaque boxes behind pasted
 * text. Explicit non-neutral highlights (including near-whites) must survive.
 */
class HtmlNeutralBackgroundImportTest {

    private fun collectSpans(state: RichTextState): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        fun walk(span: RichSpan) {
            result += span
            span.children.forEach(::walk)
        }
        state.richParagraphList.forEach { p -> p.children.forEach(::walk) }
        return result
    }

    private fun pasteHtml(html: String): RichTextState {
        val state = RichTextState()
        state.setText("hello ")
        state.selection = TextRange(6)
        state.pendingClipboardHtml = html
        state.onTextFieldValueChange(
            TextFieldValue(text = "hello pasted", selection = TextRange(12)),
        )
        return state
    }

    @Test
    fun `pasted white and transparent backgrounds are dropped`() {
        val offenders = mutableListOf<String>()
        for (css in listOf(
            "white", "#ffffff", "#fff", "rgb(255, 255, 255)",
            "rgba(255, 255, 255, 1)", "rgba(255, 255, 255, 0)", "transparent",
        )) {
            val state = pasteHtml("<span style=\"background-color: $css\">pasted</span>")
            val hasBackground = collectSpans(state).any {
                it.text.isNotEmpty() && it.fullSpanStyle.background.isSpecified
            }
            if (hasBackground) offenders += css
        }
        assertTrue(offenders.isEmpty(), "neutral backgrounds imported for: $offenders")
    }

    @Test
    fun `background shorthand is filtered too`() {
        val state = pasteHtml("<span style=\"background: white\">pasted</span>")
        val hasBackground = collectSpans(state).any {
            it.text.isNotEmpty() && it.fullSpanStyle.background.isSpecified
        }
        assertTrue(!hasBackground, "background: white shorthand slipped through")
    }

    @Test
    fun `real highlights survive including near-white`() {
        val kept = mutableListOf<String>()
        for ((css, color) in listOf(
            "yellow" to Color(255, 255, 0),
            "#fffab0" to Color(0xFFFFFAB0),
        )) {
            val state = pasteHtml("<span style=\"background-color: $css\">pasted</span>")
            val hasIt = collectSpans(state).any {
                it.text.isNotEmpty() && it.fullSpanStyle.background == color
            }
            if (!hasIt) kept += css
        }
        assertTrue(kept.isEmpty(), "intentional highlights lost: $kept")
    }
}
