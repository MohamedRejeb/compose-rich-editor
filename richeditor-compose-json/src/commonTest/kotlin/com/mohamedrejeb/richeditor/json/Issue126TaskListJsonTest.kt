package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue 126, the JSON form of a task list item: an unordered `list-item` block with a
 * `checked` field. A reader that does not know the field loads the block as a bullet item,
 * so the schema version does not change.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue126TaskListJsonTest {

    private val document = RichTextDocument(
        blocks = listOf(
            RichTextBlock(text = "todo", type = RichTextBlockType.TaskItem(checked = false)),
            RichTextBlock(text = "done", type = RichTextBlockType.TaskItem(checked = true, indent = 1)),
            RichTextBlock(text = "plain", type = RichTextBlockType.ListItem(ordered = false)),
        ),
    )

    private val json =
        """{"v":1,"blocks":[""" +
            """{"id":"b0","type":"list-item","ordered":false,"indent":0,"checked":false,"text":"todo","spans":[]},""" +
            """{"id":"b1","type":"list-item","ordered":false,"indent":1,"checked":true,"text":"done","spans":[]},""" +
            """{"id":"b2","type":"list-item","ordered":false,"indent":0,"text":"plain","spans":[]}""" +
            """]}"""

    @Test
    fun `a task item encodes as a list item with a checked field`() {
        assertEquals(json, codecEncode(document))
    }

    @Test
    fun `a list item with a checked field decodes as a task item`() {
        assertEquals(document, codecDecode(json))
    }

    @Test
    fun `a heading level on a task item round trips`() {
        val heading = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.TaskItem(checked = true), headingLevel = 2),
            ),
        )

        assertEquals(heading, codecDecode(codecEncode(heading)))
    }

    @Test
    fun `a checked field on an ordered item is ignored`() {
        val ordered =
            """{"v":1,"blocks":[{"id":"b0","type":"list-item","ordered":true,"indent":0,"checked":true,"text":"a","spans":[]}]}"""

        assertEquals(
            RichTextBlockType.ListItem(ordered = true),
            codecDecode(ordered).blocks.single().type,
        )
    }

    @Test
    fun `a state with task items round trips through JSON`() {
        val state = RichTextState().apply { setMarkdown("- [ ] a\n  - [x] b\n- c") }

        val reloaded = RichTextState().apply { setJson(state.toJson()) }

        assertEquals(state.toRichTextDocument(), reloaded.toRichTextDocument())
        assertEquals("☐ a ☑ b • c", reloaded.annotatedString.text)
    }
}
