package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A style resolving its render resource through a provider, the pattern for async-loaded
 * fonts: the instance never changes, the provider's answer does.
 */
@OptIn(ExperimentalRichTextApi::class)
private class ProviderFontStyle(
    private val provider: () -> FontFamily?,
) : RichSpanStyle {
    override fun getSpanStyle(config: RichTextConfig): SpanStyle =
        SpanStyle(fontFamily = provider())
}

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateInvalidateStylesTest {

    @Test
    fun `invalidateStyles re-resolves span styles without a content change`() {
        var resolved: FontFamily? = null
        val state = RichTextState()
        state.setText("Hello")
        state.addRichSpan(ProviderFontStyle { resolved }, TextRange(0, 5))
        assertFalse(state.annotatedString.spanStyles.any { it.item.fontFamily == FontFamily.Serif })

        resolved = FontFamily.Serif
        state.invalidateStyles()

        assertTrue(state.annotatedString.spanStyles.any { it.item.fontFamily == FontFamily.Serif })
        assertTrue(state.toText() == "Hello")
    }

    @Test
    fun `invalidateStyles does not rewrite the text buffer`() {
        // The IME composition lives on textFieldState, which no test can set: an edit of
        // that buffer is what would drop it. So pin the property that keeps a composition
        // alive instead, that a pure style refresh leaves the buffer's value untouched.
        val state = RichTextState()
        state.setText("Hello")
        state.selection = TextRange(3)
        val before = state.textFieldState.text

        state.invalidateStyles()

        assertSame(before, state.textFieldState.text)
        assertTrue(state.selection == TextRange(3))
    }

    @Test
    fun `invalidateStyles keeps staged styles for the next typed text`() {
        val state = RichTextState()
        state.setText("Hello")
        state.toggleSpanStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))

        state.invalidateStyles()

        state.onTextFieldValueChange(
            androidx.compose.ui.text.input.TextFieldValue("HelloX", selection = TextRange(6)),
        )
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .any { it is com.mohamedrejeb.richeditor.document.RichTextSpanMark.Bold && it.range == 5..5 },
        )
    }

    @Test
    fun `invalidateStyles keeps selection and history untouched`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(2, 5)

        state.invalidateStyles()

        assertTrue(state.selection == TextRange(2, 5))
        assertFalse(state.history.canUndo)
    }
}
