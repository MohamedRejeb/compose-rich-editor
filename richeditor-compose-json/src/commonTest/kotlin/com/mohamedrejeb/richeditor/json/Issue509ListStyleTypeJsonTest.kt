package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Regression pins for #509: a list item's `list-style-type` keyword is carried by the
 * optional "listStyle" field of a list-item block.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue509ListStyleTypeJsonTest {

    @Test
    fun `the keyword is written as listStyle and left out when unset`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "a", type = RichTextBlockType.ListItem(ordered = true, listStyleType = "lower-alpha")),
                RichTextBlock(text = "b", type = RichTextBlockType.ListItem(ordered = false)),
            ),
        )

        assertEquals(
            """{"v":1,"blocks":[""" +
                """{"id":"b0","type":"list-item","ordered":true,"indent":0,"listStyle":"lower-alpha","text":"a","spans":[]},""" +
                """{"id":"b1","type":"list-item","ordered":false,"indent":0,"text":"b","spans":[]}]}""",
            codecEncode(doc),
        )
    }

    @Test
    fun `listStyle is read on ordered and unordered items`() {
        val doc = codecDecode(
            """{"v":1,"blocks":[""" +
                """{"id":"b0","type":"list-item","ordered":true,"indent":0,"start":3,"listStyle":"upper-roman","text":"a","spans":[]},""" +
                """{"id":"b1","type":"list-item","ordered":false,"indent":1,"listStyle":"square","text":"b","spans":[]}]}"""
        )

        assertEquals(
            RichTextBlockType.ListItem(ordered = true, startNumber = 3, listStyleType = "upper-roman"),
            doc.blocks[0].type,
        )
        assertEquals(
            RichTextBlockType.ListItem(ordered = false, indent = 1, listStyleType = "square"),
            doc.blocks[1].type,
        )
    }

    @Test
    fun `a listStyle that is not a string is malformed`() {
        assertFailsWith<MalformedRichTextJsonException> {
            codecDecode(
                """{"v":1,"blocks":[{"id":"b0","type":"list-item","ordered":true,"indent":0,"listStyle":1,"text":"a","spans":[]}]}"""
            )
        }
    }

    @Test
    fun `the style survives a state json round trip`() {
        val state = RichTextState().apply {
            setHtml("""<ol style="list-style-type: lower-roman"><li>a<ul style="list-style-type: circle"><li>b</li></ul></li></ol>""")
        }

        val reloaded = RichTextState().apply { setJson(state.toJson()) }

        assertEquals(state.toHtml(), reloaded.toHtml())
        assertEquals("i. a ◦ b", reloaded.annotatedString.text)
    }
}
