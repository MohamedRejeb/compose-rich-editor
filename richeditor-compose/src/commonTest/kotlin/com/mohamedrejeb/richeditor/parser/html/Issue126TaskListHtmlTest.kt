package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.TaskList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Issue 126, the HTML form of a task list. Export writes what GitHub renders, a list item
 * of class `task-list-item` that starts with a disabled checkbox, inside a list of class
 * `contains-task-list`. Import takes any list item that starts with a checkbox input, so the
 * bare `<li><input type="checkbox">text</li>` loads too.
 */
class Issue126TaskListHtmlTest {

    private fun stateOf(html: String): RichTextState =
        RichTextState().apply { setHtml(html) }

    /** One entry per paragraph: "x" or "o" for a task item, "-" for a bullet, "1" for a number, "p". */
    private fun RichTextState.outline(): List<String> =
        richParagraphList.map { paragraph ->
            val type = paragraph.type
            val kind = when (type) {
                is TaskList -> if (type.checked) "x" else "o"
                is UnorderedList -> "-"
                is OrderedList -> "1"
                else -> "p"
            }
            val level = (type as? ConfigurableListLevel)?.level ?: 0
            "$kind$level:" + paragraph.children.joinToString("") { it.text }
        }

    @Test
    fun `the GitHub form loads as task items`() {
        val state = stateOf(
            """
            <ul class="contains-task-list">
            <li class="task-list-item"><input type="checkbox" class="task-list-item-checkbox" disabled=""> todo</li>
            <li class="task-list-item"><input type="checkbox" class="task-list-item-checkbox" disabled="" checked=""> done</li>
            </ul>
            """.trimIndent()
        )

        assertEquals(listOf("o1:todo", "x1:done"), state.outline())
        assertEquals("☐ todo ☑ done", state.annotatedString.text)
    }

    @Test
    fun `the bare form loads as task items`() {
        val state = stateOf("<ul><li><input type=\"checkbox\" checked>done</li><li><input type=checkbox>todo</li></ul>")

        assertEquals(listOf("x1:done", "o1:todo"), state.outline())
    }

    @Test
    fun `an uppercase type and a self closing input load too`() {
        val state = stateOf("<ul><li><input type=\"CHECKBOX\" checked=\"checked\" />done</li></ul>")

        assertEquals(listOf("x1:done"), state.outline())
    }

    @Test
    fun `an item that holds only a checkbox and a nested list is an empty task item`() {
        val state = stateOf("<ul><li><input type=\"checkbox\" checked><ul><li>a</li></ul></li></ul>")

        assertEquals(listOf("x1:", "-2:a"), state.outline())
    }

    @Test
    fun `an item of an ordered list with a checkbox becomes a task item`() {
        val state = stateOf("<ol><li><input type=\"checkbox\"> a</li><li>b</li></ol>")

        assertEquals(listOf("o1:a", "11:b"), state.outline())
    }

    @Test
    fun `a checkbox after the text of an item or outside a list is dropped`() {
        val state = stateOf("<ul><li>a <input type=\"checkbox\" checked> b</li></ul><p><input type=\"checkbox\">c</p>")

        assertEquals(listOf("-1:a b", "p0:c"), state.outline())
    }

    @Test
    fun `other inputs do not make a task item`() {
        val state = stateOf("<ul><li><input type=\"radio\" checked>a</li></ul>")

        assertEquals(listOf("-1:a"), state.outline())
    }

    @Test
    fun `text styles after the checkbox are kept`() {
        val state = stateOf("<ul><li><input type=\"checkbox\"> <b>bold</b> and plain</li></ul>")

        assertEquals(listOf("o1:bold and plain"), state.outline())
        assertEquals(
            "<ul class=\"contains-task-list\"><li class=\"task-list-item\">" +
                "<input type=\"checkbox\" disabled> <b>bold</b> and plain</li></ul>",
            state.toHtml(),
        )
    }

    @Test
    fun `export writes the GitHub form`() {
        val state = RichTextState()
        state.setMarkdown("- [ ] todo\n- [x] done")

        assertEquals(
            "<ul class=\"contains-task-list\">" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled> todo</li>" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> done</li>" +
                "</ul>",
            state.toHtml(),
        )
    }

    @Test
    fun `nested task items round trip`() {
        val html =
            "<ul class=\"contains-task-list\">" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled> a" +
                "<ul class=\"contains-task-list\">" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> b</li>" +
                "</ul></li>" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> c</li>" +
                "</ul>"

        val state = stateOf(html)

        assertEquals(listOf("o1:a", "x2:b", "x1:c"), state.outline())
        assertEquals(html, state.toHtml())
    }

    @Test
    fun `a list that mixes task and plain items round trips`() {
        val html =
            "<ul>" +
                "<li>plain</li>" +
                "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> task</li>" +
                "<li>plain again</li>" +
                "</ul>"

        val state = stateOf(html)

        assertEquals(listOf("-1:plain", "x1:task", "-1:plain again"), state.outline())
        assertEquals(html, state.toHtml())
    }

    @Test
    fun `an empty task item round trips`() {
        val state = RichTextState()
        state.setMarkdown("- [x] a")
        state.addTextAfterSelection("\n")
        assertEquals(listOf("x1:a", "o1:"), state.outline())

        val reloaded = stateOf(state.toHtml())

        assertEquals(listOf("x1:a", "o1:"), reloaded.outline())
    }

    @Test
    fun `a leading space of the item text survives the round trip`() {
        val state = RichTextState()
        state.setText(" a")
        state.addTaskList()

        val reloaded = stateOf(state.toHtml())

        assertEquals(listOf("o1: a"), reloaded.outline())
    }

    @Test
    fun `checking an item changes the exported checkbox`() {
        val state = stateOf("<ul><li><input type=\"checkbox\">a</li></ul>")
        assertFalse(state.toHtml().contains("checked"))

        state.toggleTaskListItemChecked()

        assertTrue(state.toHtml().contains("<input type=\"checkbox\" disabled checked> a"))
    }
}
