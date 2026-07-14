package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression pins for #709: typing immediately after a hyperlink (whose
 * RichSpanStyle.Link has acceptNewTextInTheEdges = false) must land inline in the
 * link's paragraph. Three cooperating defects put it elsewhere: the following
 * sibling only accepted a left-edge position when first in a paragraph, the next
 * paragraph claimed any smaller textIndex, and the full-miss fallback appended to
 * the LAST paragraph.
 */
class Issue709LinkEdgeTypingTest {

    private fun paragraphText(state: RichTextState, index: Int): String = buildString {
        fun walk(span: RichSpan) {
            append(span.text)
            span.children.forEach(::walk)
        }
        state.richParagraphList[index].children.forEach(::walk)
    }

    private fun typeAfter(state: RichTextState, anchor: String): RichTextState {
        val text = state.textFieldValue.text
        val idx = text.indexOf(anchor) + anchor.length
        state.selection = TextRange(idx)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text.substring(0, idx) + "X" + text.substring(idx),
                selection = TextRange(idx + 1),
            )
        )
        return state
    }

    @Test
    fun `typing after a link mid-paragraph lands after the link`() {
        val state = RichTextState()
        state.setHtml("<p>See <a href=\"https://example.com\">this link</a> more</p><p>-- Signature</p>")
        typeAfter(state, "this link")
        assertTrue(
            paragraphText(state, 0).contains("X"),
            "typed char left the link paragraph: p0=\"${paragraphText(state, 0)}\" " +
                "full=\"${state.annotatedString.text}\"",
        )
    }

    @Test
    fun `typing after a link that ends its paragraph stays in that paragraph`() {
        val state = RichTextState()
        state.setHtml("<p>See <a href=\"https://example.com\">this link</a></p><p>-- Signature</p>")
        typeAfter(state, "this link")
        assertTrue(
            paragraphText(state, 0).contains("X"),
            "typed char left the link paragraph: p0=\"${paragraphText(state, 0)}\" " +
                "full=\"${state.annotatedString.text}\"",
        )
        assertTrue(
            !paragraphText(state, 1).contains("X"),
            "typed char leaked into the signature paragraph",
        )
    }

    @Test
    fun `typing after a link at the end of the document appends inline`() {
        val state = RichTextState()
        state.setHtml("<p>See <a href=\"https://example.com\">this link</a></p>")
        typeAfter(state, "this link")
        assertTrue(
            state.annotatedString.text.endsWith("this linkX"),
            "typed char misplaced: \"${state.annotatedString.text}\"",
        )
    }

    @Test
    fun `typed char after link is not part of the link`() {
        val state = RichTextState()
        state.setHtml("<p>See <a href=\"https://example.com\">this link</a> more</p>")
        typeAfter(state, "this link")
        val html = state.toHtml()
        assertTrue(
            html.contains(">this link</a>"),
            "typing extended the link itself: $html",
        )
    }
}
