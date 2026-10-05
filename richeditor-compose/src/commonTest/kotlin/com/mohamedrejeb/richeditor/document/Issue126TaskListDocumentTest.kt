package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextFeature
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.TaskList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Issue 126, the document form of a task list item: [RichTextBlockType.TaskItem], with the
 * checked state and the 0-based nesting depth.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue126TaskListDocumentTest {

    @Test
    fun `task items encode to TaskItem blocks`() {
        val state = RichTextState().apply { setMarkdown("- [ ] a\n  - [x] b\n- c") }

        val blocks = state.toRichTextDocument().blocks

        assertEquals(
            listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.TaskItem(checked = false)),
                RichTextBlock(text = "b", type = RichTextBlockType.TaskItem(checked = true, indent = 1)),
                RichTextBlock(text = "c", type = RichTextBlockType.ListItem(ordered = false)),
            ),
            blocks,
        )
    }

    @Test
    fun `TaskItem blocks decode to task items`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.TaskItem(checked = true)),
                RichTextBlock(text = "b", type = RichTextBlockType.TaskItem(checked = false, indent = 2)),
            ),
        )

        val state = RichTextState().setRichTextDocument(document)

        val first = assertIs<TaskList>(state.richParagraphList[0].type)
        val second = assertIs<TaskList>(state.richParagraphList[1].type)
        assertTrue(first.checked)
        assertEquals(1, first.level)
        assertFalse(second.checked)
        assertEquals(3, second.level)
        assertEquals("☑ a ☐ b", state.annotatedString.text)
    }

    @Test
    fun `the document round trip is the identity`() {
        val state = RichTextState().apply {
            setMarkdown("1. one\n2. two\n\n- [x] **done**\n  - [ ] todo\n- plain\n\n3. three")
        }

        val document = state.toRichTextDocument()
        val rebuilt = RichTextState().setRichTextDocument(document)

        assertEquals(document, rebuilt.toRichTextDocument())
        assertEquals(state.annotatedString.text, rebuilt.annotatedString.text)
    }

    @Test
    fun `a task item ends the numbering of the ordered list around it`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true)),
                RichTextBlock(text = "b", type = RichTextBlockType.TaskItem(checked = false)),
                RichTextBlock(text = "c", type = RichTextBlockType.ListItem(ordered = true)),
            ),
        )

        val state = RichTextState().setRichTextDocument(document)

        assertEquals("1. a ☐ b 1. c", state.annotatedString.text)
        assertEquals(document, state.toRichTextDocument())
    }

    @Test
    fun `a negative indent is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            RichTextBlockType.TaskItem(checked = false, indent = -1)
        }
    }

    @Test
    fun `without the feature a TaskItem block loads as a paragraph`() {
        val state = RichTextState().apply {
            config.features = RichTextFeature.All - RichTextFeature.TaskList
        }

        state.setRichTextDocument(
            RichTextDocument(
                blocks = listOf(
                    RichTextBlock(text = "a", type = RichTextBlockType.TaskItem(checked = true)),
                    RichTextBlock(text = "b", type = RichTextBlockType.ListItem(ordered = false)),
                ),
            )
        )

        assertEquals(
            listOf(RichTextBlockType.Paragraph, RichTextBlockType.ListItem(ordered = false)),
            state.toRichTextDocument().blocks.map { it.type },
        )
    }

    @Test
    fun `with only the task list feature other lists load as paragraphs`() {
        val state = RichTextState().apply { config.features = setOf(RichTextFeature.TaskList) }

        state.setMarkdown("- [x] a\n- b")

        assertEquals(
            listOf(RichTextBlockType.TaskItem(checked = true), RichTextBlockType.Paragraph),
            state.toRichTextDocument().blocks.map { it.type },
        )
    }
}
