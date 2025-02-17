package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class ListBehaviorTest {
    @Test
    fun testBackspaceOnEmptyListLevel1() {
        val state = RichTextState()

        // Create a list with level 1
        state.addTextAfterSelection("1.")
        state.addTextAfterSelection(" ")

        // Verify that the list was created
        assertIs<OrderedList>(state.richParagraphList.first().type)

        // Simulate backspace at the start of empty list item
        state.onTextFieldValueChange(TextFieldValue(
            text = "1.",
            selection = TextRange(2)
        ))

        // Verify that the list was exited (converted to default paragraph)
        assertIsNot<OrderedList>(state.richParagraphList.first().type)
    }

    @Test
    fun testBackspaceOnEmptyListLevel2() {
        val state = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "a",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    ),
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

        // Simulate backspace at the start of empty list item
        val newText = state.annotatedString.text.dropLast(1)
        state.onTextFieldValueChange(TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        ))

        // Verify that the list level was decreased but still remains a list
        val firstParagraphType = state.richParagraphList[0].type
        assertIs<OrderedList>(firstParagraphType)
        assertEquals(1, firstParagraphType.number)
        assertEquals(1, firstParagraphType.level)

        val secondParagraphType = state.richParagraphList[1].type
        assertIs<OrderedList>(secondParagraphType)
        assertEquals(2, secondParagraphType.number)
        assertEquals(1, secondParagraphType.level)
    }
}
