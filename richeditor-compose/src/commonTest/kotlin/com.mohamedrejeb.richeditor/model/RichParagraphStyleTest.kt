package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RichParagraphStyleTest {
    val paragraph = RichParagraphStyle(key = 0)
    val richSpanStyleList = listOf(
        RichSpanStyle(
            key = 0,
            paragraph = paragraph,
            text = "012",
            textRange = TextRange(0, 3),
            fullTextRange = TextRange(0, 7),
            children = mutableStateListOf(
                RichSpanStyle(
                    key = 10,
                    paragraph = paragraph,
                    text = "345",
                    textRange = TextRange(3, 6),
                    fullTextRange = TextRange(3, 6),
                ),
                RichSpanStyle(
                    key = 11,
                    paragraph = paragraph,
                    text = "6",
                    textRange = TextRange(6, 7),
                    fullTextRange = TextRange(6, 7),
                ),
            )
        ),
        RichSpanStyle(
            key = 1,
            paragraph = paragraph,
            text = "8",
            textRange = TextRange(8, 9),
            fullTextRange = TextRange(8, 9),
        )
    )
    val richParagraphStyle = RichParagraphStyle(key = 0,)

    @Test
    fun testRemoveTextRange() {
        richParagraphStyle.children.clear()
        richParagraphStyle.children.addAll(richSpanStyleList)
        assertEquals(
            null,
            richParagraphStyle.removeTextRange(TextRange(0, 20))
        )

        richParagraphStyle.children.clear()
        richParagraphStyle.children.addAll(richSpanStyleList)
        assertEquals(
            1,
            richParagraphStyle.removeTextRange(TextRange(0, 8))?.children?.size
        )
    }

}