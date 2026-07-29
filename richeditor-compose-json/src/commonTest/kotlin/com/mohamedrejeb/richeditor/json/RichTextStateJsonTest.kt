package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextStateJsonTest {

    @Test
    fun `state json round-trip preserves html output`() {
        val html = "<h1>Title</h1><p style=\"text-align:center\">Hello <b>bold</b> " +
            "<a href=\"https://example.com\">link</a></p><ol><li>One</li></ol>"
        val state = RichTextState().apply { setHtml(html) }
        val restored = RichTextState().setJson(state.toJson())
        assertEquals(state.toHtml(), restored.toHtml())
    }

    @Test
    fun `toJson matches the golden schema example`() {
        // The styled paragraph comes last: the v1 HTML parser's css style map leaks a closed
        // paragraph's text-align into following list items, which is an upstream parser quirk
        // this golden deliberately avoids.
        val state = RichTextState().apply {
            setHtml(
                "<h1>Title</h1><ol><li>One</li></ol><p style=\"text-align:center\">Hello <b>bold</b> " +
                    "<a href=\"https://example.com\">link</a></p>"
            )
        }
        assertEquals(
            """{"v":1,"blocks":[""" +
                """{"id":"b0","type":"heading","level":1,"text":"Title","spans":[]},""" +
                """{"id":"b1","type":"list-item","ordered":true,"indent":0,"text":"One","spans":[]},""" +
                """{"id":"b2","type":"paragraph","align":"center","text":"Hello bold link","spans":[""" +
                """{"k":"bold","r":[6,9]},{"k":"link","r":[11,14],"url":"https://example.com"}]}]}""",
            state.toJson(),
        )
    }
}
