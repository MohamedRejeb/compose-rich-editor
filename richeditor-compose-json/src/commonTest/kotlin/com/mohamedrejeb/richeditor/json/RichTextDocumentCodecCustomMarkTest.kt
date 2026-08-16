package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextConfig
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
private class AppStyle : RichSpanStyle {
    override val spanStyle: (RichTextConfig) -> SpanStyle = { SpanStyle() }
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
class RichTextDocumentCodecCustomMarkTest {

    @Test
    fun `custom marks are skipped by the json codec instead of failing the encode`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello",
                    spans = listOf(
                        RichTextSpanMark.Bold(range = 0..4),
                        RichTextSpanMark.Custom(range = 0..4, style = AppStyle()),
                    ),
                ),
            ),
        )

        val decoded = RichTextDocumentCodec.decodeFromString(
            RichTextDocumentCodec.encodeToString(document),
        )

        val expected = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello",
                    spans = listOf(RichTextSpanMark.Bold(range = 0..4)),
                ),
            ),
        )
        assertEquals(expected, decoded)
    }
}
