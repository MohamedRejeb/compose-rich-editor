package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import org.junit.Test
import kotlin.test.assertTrue

class RichTextStateTest {

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testApplyStyleToLink() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    key = 1,
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Before Link After",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        richTextState.selection = TextRange(6, 9)
        richTextState.addLinkToSelection("https://www.google.com")

        richTextState.selection = TextRange(1, 12)
        richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        richTextState.selection = TextRange(7)
        assertTrue(richTextState.isLink)
    }

}