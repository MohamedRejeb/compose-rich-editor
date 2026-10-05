package com.mohamedrejeb.richeditor.parser.markdown

import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.TaskList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue 126, the Markdown form of a task list: the GitHub Flavored Markdown task list item,
 * `- [ ] todo` and `- [x] done`. The parser's GFM flavour reports the box as a token of the
 * list item, which becomes the item's checked state instead of text.
 */
class Issue126TaskListMarkdownTest {

    private fun stateOf(markdown: String): RichTextState =
        RichTextState().apply { setMarkdown(markdown) }

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
    fun `task list items load with their state`() {
        val state = stateOf("- [ ] todo\n- [x] done\n- [X] also done")

        assertEquals(listOf("o1:todo", "x1:done", "x1:also done"), state.outline())
        assertEquals("☐ todo ☑ done ☑ also done", state.annotatedString.text)
    }

    @Test
    fun `the other bullet characters load too`() {
        assertEquals(listOf("x1:a", "o1:b"), stateOf("* [x] a\n* [ ] b").outline())
        assertEquals(listOf("x1:a", "o1:b"), stateOf("+ [x] a\n+ [ ] b").outline())
    }

    @Test
    fun `a numbered task item loads as a task item`() {
        assertEquals(listOf("x1:a", "11:b"), stateOf("1. [x] a\n2. b").outline())
    }

    @Test
    fun `brackets that are not a box stay text`() {
        assertEquals(listOf("-1:[] a", "-1:[y] b", "-1:a [x] b"), stateOf("- [] a\n- [y] b\n- a [x] b").outline())
    }

    @Test
    fun `export writes the GFM form`() {
        val state = stateOf("- [ ] todo\n- [x] done")

        assertEquals("- [ ] todo\n- [x] done", state.toMarkdown())
    }

    // Export indents a nested item by four spaces per level, like it does for the other lists.
    @Test
    fun `nested task items round trip`() {
        val state = stateOf("- [ ] a\n  - [x] b\n    - [ ] c\n- [x] d")
        val outline = listOf("o1:a", "x2:b", "o3:c", "x1:d")
        assertEquals(outline, state.outline())

        val markdown = state.toMarkdown()

        assertEquals("- [ ] a\n    - [x] b\n        - [ ] c\n- [x] d", markdown)
        assertEquals(outline, stateOf(markdown).outline())
    }

    @Test
    fun `a list that mixes task and plain items round trips`() {
        val state = stateOf("- plain\n- [x] task\n  - nested plain\n  - [ ] nested task\n- plain again")
        val outline = listOf("-1:plain", "x1:task", "-2:nested plain", "o2:nested task", "-1:plain again")
        assertEquals(outline, state.outline())

        val markdown = state.toMarkdown()

        assertEquals("- plain\n- [x] task\n    - nested plain\n    - [ ] nested task\n- plain again", markdown)
        assertEquals(outline, stateOf(markdown).outline())
    }

    @Test
    fun `an empty task item keeps its state`() {
        val state = stateOf("- [ ] a\n- [x] \n- b")
        val outline = listOf("o1:a", "x1:", "-1:b")

        assertEquals(outline, state.outline())
        assertEquals(outline, stateOf(state.toMarkdown()).outline())
    }

    @Test
    fun `text styles in an item round trip`() {
        val markdown = "- [x] **bold** and plain"

        assertEquals(markdown, stateOf(markdown).toMarkdown())
    }

    @Test
    fun `a task list after a paragraph is separated by a blank line`() {
        val state = RichTextState()
        state.setHtml("<p>intro</p><ul><li><input type=\"checkbox\">a</li></ul>")

        assertEquals("intro\n\n- [ ] a", state.toMarkdown())
    }

    @Test
    fun `HTML and Markdown agree on the same content`() {
        val fromMarkdown = stateOf("- [ ] a\n  - [x] b")
        val fromHtml = RichTextState().apply { setHtml(fromMarkdown.toHtml()) }

        assertEquals(fromMarkdown.outline(), fromHtml.outline())
        assertEquals(fromMarkdown.toMarkdown(), fromHtml.toMarkdown())
    }
}
