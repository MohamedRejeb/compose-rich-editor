package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A style resolving its render resource through a provider, the pattern for async-loaded
 * fonts: the instance never changes, the provider's answer does.
 */
@OptIn(ExperimentalRichTextApi::class)
private class ProviderFontStyle(
    private val provider: () -> FontFamily?,
) : RichSpanStyle {
    override val spanStyle: (RichTextConfig) -> SpanStyle = {
        SpanStyle(fontFamily = provider())
    }

    override val acceptNewTextInTheEdges: Boolean = true

    override fun DrawScope.drawCustomStyle(
        layoutResult: TextLayoutResult,
        textRange: TextRange,
        richTextConfig: RichTextConfig,
        topPadding: Float,
        startPadding: Float,
    ): Unit = Unit
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
    fun `invalidateStyles keeps selection and history untouched`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.selection = TextRange(2, 5)

        state.invalidateStyles()

        assertTrue(state.selection == TextRange(2, 5))
        assertFalse(state.history.canUndo)
    }
}
