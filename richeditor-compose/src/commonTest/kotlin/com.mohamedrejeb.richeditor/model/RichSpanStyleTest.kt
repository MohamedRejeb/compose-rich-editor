package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RichSpanStyleTest {
    private val paragraph = RichParagraphStyle(key = 0)
    private val richSpanStyle = RichSpanStyle(
        key = 0,
        paragraph = paragraph,
        text = "012",
        textRange = TextRange(0, 3),
        children = mutableStateListOf(
            RichSpanStyle(
                key = 10,
                paragraph = paragraph,
                text = "34",
                textRange = TextRange(3, 5),
            ),
            RichSpanStyle(
                key = 11,
                paragraph = paragraph,
                text = "567",
                textRange = TextRange(5, 8),
            ),
        )
    )

    @Test
    fun testGetSpanStyleByTextIndex() {
        assertEquals(
            0,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 0).second?.key,
        )
        assertEquals(
            0,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 2).second?.key,
        )
        assertEquals(
            null,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 8).second?.key,
        )
        assertEquals(
            null,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 9).second?.key,
        )

        assertEquals(
            10,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 3).second?.key,
        )
        assertEquals(
            11,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 6).second?.key,
        )

        assertEquals(
            11,
            richSpanStyle.getSpanStyleByTextIndex(textIndex = 7).second?.key,
        )
    }

    @Test
    fun testRemoveTextRangeStart() {
        // Remove all start text
        val removeAllStartText = richSpanStyle.removeTextRange(TextRange(0, 3))
        assertEquals(
            "",
            removeAllStartText?.text
        )
        assertEquals(
            2,
            removeAllStartText?.children?.size
        )
        assertEquals(
            "34",
            removeAllStartText?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeStartPart() {
        // Remove part of start text
        val removePartOfStartText = richSpanStyle.removeTextRange(TextRange(1, 3))
        assertEquals(
            "0",
            removePartOfStartText?.text
        )
        assertEquals(
            2,
            removePartOfStartText?.children?.size
        )
        assertEquals(
            "34",
            removePartOfStartText?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeFirstChild() {
        // Remove first child
        val removeFirstChild = richSpanStyle.removeTextRange(TextRange(3, 7))
        assertEquals(
            "012",
            removeFirstChild?.text
        )
        assertEquals(
            1,
            removeFirstChild?.children?.size
        )
        assertEquals(
            "7",
            removeFirstChild?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeLastChild() {
        // Remove last child
        val removeLastChild = richSpanStyle.removeTextRange(TextRange(5, 8))
        assertEquals(
            "012",
            removeLastChild?.text
        )
        assertEquals(
            1,
            removeLastChild?.children?.size
        )
        assertEquals(
            "34",
            removeLastChild?.children?.first()?.text
        )
    }

    @Test
    fun testRemoveTextRangeTwoChildren() {
        // Remove the two children
        val removeTwoChildren = richSpanStyle.removeTextRange(TextRange(3, 8))
        assertEquals(
            "012",
            removeTwoChildren?.text
        )
        assertEquals(
            0,
            removeTwoChildren?.children?.size
        )
    }

    @Test
    fun testRemoveTextRangeAlLText() {
        // Remove all the text
        val removeAllText = richSpanStyle.removeTextRange(TextRange(0, 20))
        assertEquals(
            null,
            removeAllText
        )
    }

}