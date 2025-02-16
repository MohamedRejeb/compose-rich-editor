package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals

class RichParagraphTest {
    private val paragraph = RichParagraph(key = 0)

    @OptIn(ExperimentalRichTextApi::class)
    private val richSpanLists
        get() = listOf(
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
                text = "78",
                textRange = TextRange(7, 9),
            )
        )
    private val richParagraph = RichParagraph(key = 0)

    @Test
    fun testRemoveTextRange() {
        richParagraph.children.clear()
        richParagraph.children.addAll(richSpanLists)
        assertEquals(
            null,
            richParagraph.removeTextRange(TextRange(0, 20), 0)
        )

        richParagraph.children.clear()
        richParagraph.children.addAll(richSpanLists)
        assertEquals(
            1,
            richParagraph.removeTextRange(TextRange(0, 8), 0)?.children?.size
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testTrimStart() {
        val paragraph = RichParagraph(key = 0)
        val richSpanLists = listOf(
            RichSpan(
                key = 0,
                paragraph = paragraph,
                text = "    ",
                textRange = TextRange(0, 3),
                children = mutableStateListOf(
                    RichSpan(
                        key = 10,
                        paragraph = paragraph,
                        text = "   345",
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
                text = "78",
                textRange = TextRange(7, 9),
            )
        )
        paragraph.children.addAll(richSpanLists)

        paragraph.trimStart()

        val firstChild = paragraph.children[0]
        val secondChild = paragraph.children[1]

        assertEquals("", firstChild.text)
        assertEquals("78", secondChild.text)

        val firstGrandChild = firstChild.children[0]
        val secondGrandChild = firstChild.children[1]

        assertEquals("345", firstGrandChild.text)
        assertEquals("6", secondGrandChild.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testTrimEnd() {
        val paragraph = RichParagraph(key = 0)
        val richSpanLists = listOf(
            RichSpan(
                key = 0,
                paragraph = paragraph,
                text = "   012",
                children = mutableStateListOf(
                    RichSpan(
                        key = 10,
                        paragraph = paragraph,
                        text = "   345",
                    ),
                    RichSpan(
                        key = 11,
                        paragraph = paragraph,
                        text = "6   ",
                    ),
                    RichSpan(
                        key = 12,
                        paragraph = paragraph,
                        text = "   ",
                    ),
                )
            ),
            RichSpan(
                key = 1,
                paragraph = paragraph,
                text = "    ",
            )
        )
        paragraph.children.addAll(richSpanLists)

        paragraph.trimEnd()

        val firstChild = paragraph.children[0]
        val secondChild = paragraph.children[1]

        assertEquals(2, firstChild.children.size)

        assertEquals("   012", firstChild.text)
        assertEquals("", secondChild.text)

        val firstGrandChild = firstChild.children[0]
        val secondGrandChild = firstChild.children[1]

        assertEquals("   345", firstGrandChild.text)
        assertEquals("6", secondGrandChild.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testTrim() {
        val paragraph = RichParagraph(key = 0)
        val richSpanLists = listOf(
            RichSpan(
                key = 0,
                paragraph = paragraph,
                text = "   ",
                children = mutableStateListOf(
                    RichSpan(
                        key = 10,
                        paragraph = paragraph,
                        text = "   345",
                    ),
                    RichSpan(
                        key = 11,
                        paragraph = paragraph,
                        text = "6   ",
                    ),
                    RichSpan(
                        key = 12,
                        paragraph = paragraph,
                        text = "   ",
                    ),
                )
            ),
            RichSpan(
                key = 1,
                paragraph = paragraph,
                text = "    ",
            )
        )
        paragraph.children.addAll(richSpanLists)

        paragraph.trim()

        val firstChild = paragraph.children[0]
        val secondChild = paragraph.children[1]

        assertEquals(2, firstChild.children.size)

        assertEquals("", firstChild.text)
        assertEquals("", secondChild.text)

        val firstGrandChild = firstChild.children[0]
        val secondGrandChild = firstChild.children[1]

        assertEquals("345", firstGrandChild.text)
        assertEquals("6", secondGrandChild.text)
    }

}