package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import org.intellij.markdown.lexer.Compat.assert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RichTextStateOrderedListTest {

    @Test
    fun testDeletingTenOrderedListItems() {
        // This was causing a crash
        val richTextState = RichTextState()

        richTextState.setMarkdown(
            """
                1. a
                2. a
                3. a
                4. a
                5. a
                6. a
                7. a
                8. a
                9. a
                10. a
            """.trimIndent()
        )

        richTextState.selection = TextRange(0, richTextState.annotatedString.length)

        richTextState.removeSelectedText()
    }

    @Test
    fun testAddingNewOrderedListBetweenDifferentLevels() {
        val richTextState = RichTextState()

        richTextState.setMarkdown(
            """
                1. a
                2. a
                    1. a
            """.trimIndent()
        )

        richTextState.selection =
            TextRange(richTextState.richParagraphList[1].getLastNonEmptyChild()!!.fullTextRange.max)

        richTextState.addTextAfterSelection("\n")

        assertEquals(4, richTextState.richParagraphList.size)

        val addedParagraphType = richTextState.richParagraphList[2].type

        assertIs<OrderedList>(addedParagraphType)
        assertEquals(3, addedParagraphType.number)
        assertEquals(1, addedParagraphType.level)

        val lastParagraphType = richTextState.richParagraphList[3].type

        assertIs<OrderedList>(lastParagraphType)
        assertEquals(1, lastParagraphType.number)
        assertEquals(2, lastParagraphType.level)
    }

}