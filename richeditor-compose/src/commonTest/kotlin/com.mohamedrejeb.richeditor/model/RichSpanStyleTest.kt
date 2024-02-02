package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals

class RichSpanTest {
    private val paragraph = RichParagraph(key = 0)
    private val richSpan get() = RichSpan(
        key = 0,
        paragraph = paragraph,
        text = "012",
        textRange = TextRange(0, 3),
        children = mutableListOf()
    ).also {
        it.children.addAll(
            listOf(
                RichSpan(
                    key = 10,
                    paragraph = paragraph,
                    text = "34",
                    textRange = TextRange(3, 5),
                    parent = it,
                ),
                RichSpan(
                    key = 11,
                    paragraph = paragraph,
                    text = "567",
                    textRange = TextRange(5, 8),
                    parent = it,
                )
            )
        )
    }

    @Test
    fun testGetSpanStyleByTextIndex() {
        assertEquals(
            0,
            richSpan.getRichSpanByTextIndex(textIndex = 0).second?.key,
        )
        assertEquals(
            0,
            richSpan.getRichSpanByTextIndex(textIndex = 2).second?.key,
        )
        assertEquals(
            null,
            richSpan.getRichSpanByTextIndex(textIndex = 8).second?.key,
        )
        assertEquals(
            null,
            richSpan.getRichSpanByTextIndex(textIndex = 9).second?.key,
        )

        assertEquals(
            10,
            richSpan.getRichSpanByTextIndex(textIndex = 3).second?.key,
        )
        assertEquals(
            11,
            richSpan.getRichSpanByTextIndex(textIndex = 6).second?.key,
        )

        assertEquals(
            11,
            richSpan.getRichSpanByTextIndex(textIndex = 7).second?.key,
        )
    }

    @Test
    fun testRemoveTextRangeStart() {
        // Remove all start text
        val removeAllStartText = richSpan.removeTextRange(TextRange(0, 3), 0)
        assertEquals(
            "",
            removeAllStartText.second?.text
        )
        assertEquals(
            2,
            removeAllStartText.second?.children?.size
        )
        assertEquals(
            "34",
            removeAllStartText.second?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeStartPart() {
        // Remove part of start text
        val removePartOfStartText = richSpan.removeTextRange(TextRange(1, 3), 0)
        assertEquals(
            "0",
            removePartOfStartText.second?.text
        )
        assertEquals(
            2,
            removePartOfStartText.second?.children?.size
        )
        assertEquals(
            "34",
            removePartOfStartText.second?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeFirstChild() {
        // Remove first child
        val removeFirstChild = richSpan.removeTextRange(TextRange(3, 7), 0)
        assertEquals(
            "012",
            removeFirstChild.second?.text
        )
        assertEquals(
            1,
            removeFirstChild.second?.children?.size
        )
        assertEquals(
            "7",
            removeFirstChild.second?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeLastChild() {
        // Remove last child
        val removeLastChild = richSpan.removeTextRange(TextRange(5, 8), 0)
        assertEquals(
            "012",
            removeLastChild.second?.text
        )
        assertEquals(
            1,
            removeLastChild.second?.children?.size
        )
        assertEquals(
            "34",
            removeLastChild.second?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeTwoChildren() {
        // Remove the two children
        val removeTwoChildren = richSpan.removeTextRange(TextRange(3, 8), 0)
        assertEquals(
            "012",
            removeTwoChildren.second?.text
        )
        assertEquals(
            0,
            removeTwoChildren.second?.children?.size
        )
    }

    @Test
    fun testRemoveTextRangeAlLText() {
        // Remove all the text
        val removeAllText = richSpan.removeTextRange(TextRange(0, 20), 0)
        assertEquals(
            null,
            removeAllText.second
        )
    }

    @Test
    fun testBefore() {
        val before1 = richSpan.children.first().before

        assertEquals(
            0,
            before1?.key,
        )

        val before2 = richSpan.children.last().before

        assertEquals(
            10,
            before2?.key,
        )

        val before3 = richSpan.before

        assertEquals(
            null,
            before3,
        )
    }

    @Test
    fun testAfter() {
        val after1 = richSpan.children.first().after

        assertEquals(
            11,
            after1?.key,
        )

        val after2 = richSpan.children.last().after

        assertEquals(
            null,
            after2,
        )

        val after3 = richSpan.after

        assertEquals(
            10,
            after3?.key,
        )
    }

}