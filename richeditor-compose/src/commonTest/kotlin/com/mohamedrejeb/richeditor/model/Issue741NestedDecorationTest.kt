package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression pins for #741 (and the styling half of #740): nested underline and
 * strikethrough must render together. The child's textDecoration used to replace the
 * parent's, both in [RichSpan.fullSpanStyle] (plain merge) and in the annotated
 * string builder (style nesting), while the toggle path already combined them via
 * customMerge.
 */
class Issue741NestedDecorationTest {

    private fun collectSpans(state: RichTextState): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        fun walk(span: RichSpan) {
            result += span
            span.children.forEach(::walk)
        }
        state.richParagraphList.forEach { p -> p.children.forEach(::walk) }
        return result
    }

    private fun assertBothDecorations(state: RichTextState, context: String) {
        // Model level
        val span = collectSpans(state).first { it.text.contains("sample") }
        val modelDeco = span.fullSpanStyle.textDecoration
        assertTrue(
            modelDeco != null &&
                modelDeco.contains(TextDecoration.Underline) &&
                modelDeco.contains(TextDecoration.LineThrough),
            "$context: fullSpanStyle.textDecoration=$modelDeco",
        )

        // Render level: the innermost annotated-string style covering the text must
        // carry both decorations (nesting would otherwise drop the outer one)
        val start = state.annotatedString.text.indexOf("sample")
        val styles = state.annotatedString.spanStyles.filter {
            start >= it.start && start < it.end && it.item.textDecoration != null
        }
        val renderDeco = styles.lastOrNull()?.item?.textDecoration
        assertTrue(
            renderDeco != null &&
                renderDeco.contains(TextDecoration.Underline) &&
                renderDeco.contains(TextDecoration.LineThrough),
            "$context: rendered textDecoration=$renderDeco",
        )
    }

    @Test
    fun `html u outer s inner renders both`() {
        val state = RichTextState()
        state.setHtml("<p><u><s>sample</s></u></p>")
        assertBothDecorations(state, "u outer")
    }

    @Test
    fun `html s outer u inner renders both`() {
        val state = RichTextState()
        state.setHtml("<p><s><u>sample</u></s></p>")
        assertBothDecorations(state, "s outer")
    }

    @Test
    fun `toggling both decorations keeps both`() {
        val state = RichTextState()
        state.setText("sample text")
        state.selection = TextRange(0, 6)
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        assertBothDecorations(state, "toggle path")
    }

    @Test
    fun `combined decorations survive html round trip`() {
        val state = RichTextState()
        state.setHtml("<p><u><s>sample</s></u></p>")
        val reimported = RichTextState()
        reimported.setHtml(state.toHtml())
        assertBothDecorations(reimported, "round trip")
    }
}
