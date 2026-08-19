package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.SpanStyle
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
    override fun getSpanStyle(config: RichTextConfig): SpanStyle = SpanStyle()
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

        val decoded = codecDecode(
            codecEncode(document),
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
