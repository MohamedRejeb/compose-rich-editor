package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import org.intellij.markdown.lexer.Compat.assert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalRichTextApi::class)
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

    @Test
    fun testAddListItemWithEnter() {
        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "test",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        // Simulate pressing Enter on non-empty list item
        richTextState.selection = TextRange(richTextState.annotatedString.length)
        richTextState.addTextAfterSelection("\n")

        // Verify that list formatting is removed
        assertEquals(2, richTextState.richParagraphList.size)
        assertIs<OrderedList>(richTextState.richParagraphList[0].type)
        assertIs<OrderedList>(richTextState.richParagraphList[1].type)
    }

    @Test
    fun testExitEmptyListItem() {
        // Test with exitListOnEmptyItem = true (default)
        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        // Simulate pressing Enter on empty list item
        richTextState.selection = TextRange(richTextState.annotatedString.length)
        richTextState.addTextAfterSelection("\n")

        // Verify that list formatting is removed
        assertEquals(1, richTextState.richParagraphList.size)
        assertIs<DefaultParagraph>(richTextState.richParagraphList[0].type)

        // Test with exitListOnEmptyItem = false
        val richTextState2 = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                }
            )
        )
        richTextState2.config.exitListOnEmptyItem = false

        // Simulate pressing Enter on empty list item
        richTextState2.selection = TextRange(richTextState2.annotatedString.length)
        richTextState2.addTextAfterSelection("\n")

        // Verify that list formatting is preserved and a new list item is created
        assertEquals(2, richTextState2.richParagraphList.size)
        assertIs<OrderedList>(richTextState2.richParagraphList[0].type)
        assertIs<OrderedList>(richTextState2.richParagraphList[1].type)
    }
}
