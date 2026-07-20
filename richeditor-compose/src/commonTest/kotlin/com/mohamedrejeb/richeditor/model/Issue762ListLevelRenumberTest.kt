package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Regression pins for #762: demoting a nested ordered-list item back to level 1,
 * whether via backspace on its marker or via decreaseListLevel, must renumber it to
 * continue the outer list's count. Broken in rc14, fixed by the renumbering rework.
 */
class Issue762ListLevelRenumberTest {

    // #762: demote-by-backspace should renumber like decreaseListLevel does
    @Test
    fun `demoting last nested item via backspace renumbers it at the new level`() {
        val state = RichTextState()
        state.setMarkdown("1. hello\n    1. hi\n    2. how\n    3. are\n    4. you")
        val text = state.textFieldValue.text
        val youIndex = text.indexOf("you")
        state.selection = TextRange(youIndex)

        // Backspace: removes the char just before "you" (inside the "4. " marker)
        state.onTextFieldValueChange(
            TextFieldValue(
                text = text.substring(0, youIndex - 1) + text.substring(youIndex),
                selection = TextRange(youIndex - 1),
            )
        )

        val lastType = state.richParagraphList.last().type
        assertIs<OrderedList>(lastType, "item should stay an ordered list item")
        assertEquals(1, lastType.level, "backspace on the marker should demote to level 1")
        assertEquals(
            2,
            lastType.number,
            "#762: demoted item should continue level-1 numbering (hello=1, you=2). " +
                "text=\"${state.annotatedString.text}\"",
        )
    }

    // #762 control: the shift+tab path reportedly works
    @Test
    fun `demoting last nested item via decreaseListLevel renumbers it`() {
        val state = RichTextState()
        state.setMarkdown("1. hello\n    1. hi\n    2. how\n    3. are\n    4. you")
        val text = state.textFieldValue.text
        state.selection = TextRange(text.indexOf("you") + 1)
        state.decreaseListLevel()

        val lastType = state.richParagraphList.last().type
        assertIs<OrderedList>(lastType)
        assertEquals(1, lastType.level)
        assertEquals(
            2,
            lastType.number,
            "decreaseListLevel: demoted item should continue level-1 numbering. " +
                "text=\"${state.annotatedString.text}\"",
        )
    }
}
