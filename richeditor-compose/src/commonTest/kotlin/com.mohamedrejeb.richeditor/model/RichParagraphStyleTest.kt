package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RichParagraphStyleTest {
    private val paragraph = RichParagraph(key = 0)
    private val richSpanLists = listOf(
        RichSpan(
            key = 0,
            paragraph = paragraph,
            text = "012",
            textRange = TextRange(0, 3),
            children = mutableStateListOf(
                RichSpan(
                    key = 10,
                    paragraph = paragraph,
                    text = "345",
                    textRange = TextRange(3, 6),
                ),
                RichSpan(
                    key = 11,
                    paragraph = paragraph,
                    text = "6",
                    textRange = TextRange(6, 7),
                ),
            )
        ),
        RichSpan(
            key = 1,
            paragraph = paragraph,
            text = "8",
            textRange = TextRange(8, 9),
        )
    )
    private val richParagraph = RichParagraph(key = 0,)

    @Test
    fun testRemoveTextRange() {
        richParagraph.children.clear()
        richParagraph.children.addAll(richSpanLists)
        assertEquals(
            null,
            richParagraph.removeTextRange(TextRange(0, 20))
        )

        richParagraph.children.clear()
        richParagraph.children.addAll(richSpanLists)
        assertEquals(
            1,
            richParagraph.removeTextRange(TextRange(0, 8))?.children?.size
        )
    }

}