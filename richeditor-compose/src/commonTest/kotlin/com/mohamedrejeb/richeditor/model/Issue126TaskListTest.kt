package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.TaskList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Issue 126: the editor had ordered and unordered lists but no task list, the checklist of
 * GitHub Markdown, Notion and Google Docs.
 *
 * A task list item is a paragraph type of its own, [TaskList], with a checked state and a
 * nesting level. Its marker is a ballot box character in the text, like a bullet, so Enter,
 * Backspace, indent and outdent behave like they do on the other lists. Checking an item
 * swaps the marker for one of the same length and is one undo step.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue126TaskListTest {

    private fun RichTextState.insert(typed: String) {
        val text = annotatedString.text
        onTextFieldValueChange(
            TextFieldValue(
                text = text.substring(0, selection.min) + typed + text.substring(selection.max),
                selection = TextRange(selection.min + typed.length),
            )
        )
    }

    private fun RichTextState.backspace() {
        val text = annotatedString.text
        val caret = selection.min
        onTextFieldValueChange(
            TextFieldValue(
                text = text.removeRange(caret - 1, caret),
                selection = TextRange(caret - 1),
            )
        )
    }

    private fun RichTextState.taskItem(index: Int): TaskList =
        assertIs<TaskList>(richParagraphList[index].type, "paragraph $index")

    private fun stateOf(text: String): RichTextState =
        RichTextState().apply { setText(text) }

    @Test
    fun `toggleTaskList turns the paragraph into an unchecked item and back`() {
        val state = stateOf("buy milk")

        state.toggleTaskList()

        assertFalse(state.taskItem(0).checked)
        assertEquals("☐ buy milk", state.annotatedString.text)
        assertEquals(TextRange(10), state.selection)
        assertTrue(state.isTaskList)
        assertTrue(state.isList)
        assertFalse(state.isUnorderedList)
        assertFalse(state.isTaskListItemChecked)

        state.toggleTaskList()

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertEquals("buy milk", state.annotatedString.text)
        assertFalse(state.isTaskList)
        assertFalse(state.isList)
    }

    @Test
    fun `toggleTaskList covers every selected paragraph`() {
        val state = stateOf("a")
        state.insert("\n")
        state.insert("b")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.toggleTaskList()

        assertEquals("☐ a ☐ b", state.annotatedString.text)
        assertTrue(state.isTaskList)
    }

    @Test
    fun `addTaskList converts other lists and keeps their level`() {
        val state = RichTextState()
        state.setHtml("<ul><li>a<ul><li>b</li></ul></li></ul><ol><li>c</li></ol>")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.addTaskList()

        assertEquals(1, state.taskItem(0).level)
        assertEquals(2, state.taskItem(1).level)
        assertEquals(1, state.taskItem(2).level)
        assertEquals("☐ a ☐ b ☐ c", state.annotatedString.text)
    }

    @Test
    fun `addTaskList keeps the checked state of an item that already is one`() {
        val state = stateOf("a")
        state.addTaskList()
        state.setTaskListItemsChecked(true)

        state.addTaskList()

        assertTrue(state.taskItem(0).checked)
    }

    @Test
    fun `removeTaskList leaves other lists alone`() {
        val state = stateOf("a")
        state.toggleUnorderedList()

        state.removeTaskList()

        assertIs<UnorderedList>(state.richParagraphList[0].type)
    }

    @Test
    fun `the other list toggles replace a task list`() {
        val state = stateOf("a")
        state.addTaskList()

        state.toggleUnorderedList()
        assertIs<UnorderedList>(state.richParagraphList[0].type)
        assertFalse(state.isTaskList)

        state.addTaskList()
        state.toggleOrderedList()
        assertIs<OrderedList>(state.richParagraphList[0].type)
        assertEquals("1. a", state.annotatedString.text)
    }

    @Test
    fun `checking an item swaps the marker and keeps the selection`() {
        val state = stateOf("buy milk")
        state.addTaskList()
        state.selection = TextRange(5)

        state.toggleTaskListItemChecked()

        assertTrue(state.taskItem(0).checked)
        assertEquals("☑ buy milk", state.annotatedString.text)
        assertEquals(TextRange(5), state.selection)
        assertTrue(state.isTaskListItemChecked)

        state.toggleTaskListItemChecked()

        assertFalse(state.taskItem(0).checked)
        assertEquals("☐ buy milk", state.annotatedString.text)
        assertFalse(state.isTaskListItemChecked)
    }

    @Test
    fun `setTaskListItemsChecked sets every selected item and skips other paragraphs`() {
        val state = RichTextState()
        state.setMarkdown("- [ ] a\n- [x] b\n- c")
        state.selection = TextRange(0, state.annotatedString.text.length)
        assertFalse(state.isTaskList)

        state.setTaskListItemsChecked(true)

        assertTrue(state.taskItem(0).checked)
        assertTrue(state.taskItem(1).checked)
        assertIs<UnorderedList>(state.richParagraphList[2].type)
        assertEquals("☑ a ☑ b • c", state.annotatedString.text)
    }

    @Test
    fun `toggleTaskListItemChecked follows the first selected item`() {
        val state = RichTextState()
        state.setMarkdown("- [x] a\n- [ ] b")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.toggleTaskListItemChecked()

        assertFalse(state.taskItem(0).checked)
        assertFalse(state.taskItem(1).checked)
    }

    @Test
    fun `the checked functions do nothing outside a task list`() {
        val state = stateOf("a")

        state.toggleTaskListItemChecked()
        state.setTaskListItemsChecked(true)

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertFalse(state.history.canUndo)
    }

    @Test
    fun `the toolbar state follows the caret`() {
        val state = RichTextState()
        state.setMarkdown("- [x] a\n- [ ] b\n\nc")

        state.selection = TextRange(3)
        assertTrue(state.isTaskList)
        assertTrue(state.isTaskListItemChecked)

        state.selection = TextRange(7)
        assertTrue(state.isTaskList)
        assertFalse(state.isTaskListItemChecked)

        state.selection = TextRange(state.annotatedString.text.length)
        assertFalse(state.isTaskList)
        assertFalse(state.isTaskListItemChecked)
    }

    @Test
    fun `Enter on a checked item starts an unchecked one`() {
        val state = stateOf("a")
        state.addTaskList()
        state.setTaskListItemsChecked(true)

        state.insert("\n")
        state.insert("b")

        assertTrue(state.taskItem(0).checked)
        assertFalse(state.taskItem(1).checked)
        assertEquals("☑ a ☐ b", state.annotatedString.text)
    }

    @Test
    fun `Enter in the middle of an item splits it into two items`() {
        val state = stateOf("ab")
        state.addTaskList()
        state.selection = TextRange(3)

        state.insert("\n")

        assertEquals("☐ a ☐ b", state.annotatedString.text)
        assertEquals(2, state.richParagraphList.size)
    }

    @Test
    fun `Enter on an empty item leaves the list`() {
        val state = stateOf("a")
        state.addTaskList()
        state.insert("\n")
        assertEquals(2, state.richParagraphList.size)

        state.insert("\n")

        assertEquals(2, state.richParagraphList.size)
        assertIs<TaskList>(state.richParagraphList[0].type)
        assertIs<DefaultParagraph>(state.richParagraphList[1].type)
        assertEquals("☐ a ", state.annotatedString.text)
    }

    @Test
    fun `Enter on an empty item stays in the list when exitListOnEmptyItem is off`() {
        val state = stateOf("a")
        state.config.exitListOnEmptyItem = false
        state.addTaskList()
        state.insert("\n")

        state.insert("\n")

        assertEquals(3, state.richParagraphList.size)
        assertIs<TaskList>(state.richParagraphList[2].type)
    }

    @Test
    fun `Backspace at the start of an item turns it into a paragraph`() {
        val state = stateOf("a")
        state.addTaskList()
        state.setTaskListItemsChecked(true)
        state.selection = TextRange(2)

        state.backspace()

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertEquals("a", state.annotatedString.text)
    }

    @Test
    fun `Backspace at the start of a nested item lifts it one level and keeps its state`() {
        val state = RichTextState()
        state.setMarkdown("- [ ] a\n  - [x] b")
        assertEquals(2, state.taskItem(1).level)
        state.selection = TextRange(6)

        state.backspace()

        assertEquals(1, state.taskItem(1).level)
        assertTrue(state.taskItem(1).checked)
        assertEquals("☐ a ☑ b", state.annotatedString.text)
    }

    @Test
    fun `indent and outdent move an item between levels`() {
        val state = RichTextState()
        state.setMarkdown("- [ ] a\n- [x] b")
        state.selection = TextRange(state.annotatedString.text.length)
        assertTrue(state.canIncreaseListLevel)
        assertFalse(state.canDecreaseListLevel)

        state.increaseListLevel()

        assertEquals(2, state.taskItem(1).level)
        assertTrue(state.taskItem(1).checked)
        assertTrue(state.canDecreaseListLevel)

        state.decreaseListLevel()

        assertEquals(1, state.taskItem(1).level)
        assertTrue(state.taskItem(1).checked)
    }

    @Test
    fun `a task item nests under a bullet item`() {
        val state = RichTextState()
        state.setMarkdown("- a\n- [ ] b")
        state.selection = TextRange(state.annotatedString.text.length)

        state.increaseListLevel()

        assertIs<UnorderedList>(state.richParagraphList[0].type)
        assertEquals(2, state.taskItem(1).level)
    }

    @Test
    fun `a task item between ordered items restarts their numbering like a bullet does`() {
        val state = RichTextState()
        state.setHtml("<ol><li>a</li><li>b</li><li>c</li></ol>")
        state.selection = TextRange(5)

        state.addTaskList()

        assertEquals("1. a ☐ b 1. c", state.annotatedString.text)
    }

    @Test
    fun `turning a paragraph into a task item is one undo step`() {
        val state = stateOf("a")
        state.history.clear()

        state.toggleTaskList()
        state.history.undo()

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertEquals("a", state.annotatedString.text)
        assertFalse(state.history.canUndo)
    }

    @Test
    fun `checking an item is one undo step`() {
        val state = stateOf("a")
        state.addTaskList()
        state.history.clear()

        state.toggleTaskListItemChecked()
        assertTrue(state.history.canUndo)
        state.history.undo()

        assertFalse(state.taskItem(0).checked)
        assertEquals("☐ a", state.annotatedString.text)
        assertFalse(state.history.canUndo)

        state.history.redo()

        assertTrue(state.taskItem(0).checked)
        assertEquals("☑ a", state.annotatedString.text)
    }

    @Test
    fun `typing a box at the start of a paragraph starts a task item`() {
        for ((typed, checked) in listOf("[ ] " to false, "[x] " to true, "[X] " to true)) {
            val state = RichTextState()

            typed.forEach { state.insert(it.toString()) }

            assertEquals(checked, state.taskItem(0).checked, "typed \"$typed\"")
            assertEquals(if (checked) "☑ " else "☐ ", state.annotatedString.text, "typed \"$typed\"")
            assertEquals(TextRange(2), state.selection, "typed \"$typed\"")
        }
    }

    @Test
    fun `typing the Markdown form goes through a bullet item to a task item`() {
        val state = RichTextState()

        "- [ ] milk".forEach { state.insert(it.toString()) }

        assertFalse(state.taskItem(0).checked)
        assertEquals("☐ milk", state.annotatedString.text)
        assertEquals(TextRange(6), state.selection)
    }

    @Test
    fun `a box typed in a nested bullet item keeps the level`() {
        val state = RichTextState()
        state.setMarkdown("- a\n  - b")
        state.selection = TextRange(state.annotatedString.text.length)
        state.backspace()

        "[x] ".forEach { state.insert(it.toString()) }

        assertEquals(2, state.taskItem(1).level)
        assertTrue(state.taskItem(1).checked)
        assertEquals("• a ☑ ", state.annotatedString.text)
    }

    @Test
    fun `a box typed later in a paragraph stays text`() {
        val state = stateOf("a ")

        "[ ] ".forEach { state.insert(it.toString()) }

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertEquals("a [ ] ", state.annotatedString.text)
    }

    @Test
    fun `the typed box stays text when the shortcuts are off`() {
        val state = RichTextState().apply { config.listTypingShortcutsEnabled = false }

        "[ ] ".forEach { state.insert(it.toString()) }

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertEquals("[ ] ", state.annotatedString.text)
    }

    @Test
    fun `without the feature the mutators and the shortcut do nothing`() {
        val state = stateOf("a")
        state.config.features = RichTextFeature.All - RichTextFeature.TaskList

        state.toggleTaskList()
        state.addTaskList()
        assertIs<DefaultParagraph>(state.richParagraphList[0].type)

        val typed = RichTextState().apply { config.features = RichTextFeature.All - RichTextFeature.TaskList }
        "[ ] ".forEach { typed.insert(it.toString()) }
        assertIs<DefaultParagraph>(typed.richParagraphList[0].type)
        assertEquals("[ ] ", typed.annotatedString.text)
    }

    @Test
    fun `without the feature loaded task items become paragraphs`() {
        val state = RichTextState().apply { config.features = RichTextFeature.All - RichTextFeature.TaskList }

        state.setMarkdown("- [x] a\n- b")

        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
        assertIs<UnorderedList>(state.richParagraphList[1].type)
        assertEquals("a • b", state.annotatedString.text)
    }

    @Test
    fun `an existing task item can still be removed and checked without the feature`() {
        val state = stateOf("a")
        state.addTaskList()
        state.config.features = RichTextFeature.All - RichTextFeature.TaskList

        state.toggleTaskListItemChecked()
        assertTrue(state.taskItem(0).checked)

        state.toggleTaskList()
        assertIs<DefaultParagraph>(state.richParagraphList[0].type)
    }

    @Test
    fun `a copy of the state keeps the items and their state`() {
        val state = RichTextState()
        state.setMarkdown("- [x] a\n  - [ ] b")

        val copy = state.copy()

        assertTrue(assertIs<TaskList>(copy.richParagraphList[0].type).checked)
        assertEquals(2, assertIs<TaskList>(copy.richParagraphList[1].type).level)
        assertEquals("☑ a ☐ b", copy.annotatedString.text)
    }
}
