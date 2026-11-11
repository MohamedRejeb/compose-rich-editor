package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalRichTextApi::class)
class CodeBlockJsonTest {

    private val document = RichTextDocument(
        blocks = listOf(
            RichTextBlock(text = "val a", type = RichTextBlockType.CodeBlock("kotlin", isBlockStart = true)),
            RichTextBlock(text = "", type = RichTextBlockType.CodeBlock("kotlin", isBlockStart = false)),
            RichTextBlock(text = "c", type = RichTextBlockType.CodeBlock(null, isBlockStart = true)),
            RichTextBlock(text = "d", type = RichTextBlockType.CodeBlock(null, isBlockStart = false)),
        ),
    )

    @Test
    fun `language and start are written and left out when unset`() {
        assertEquals(
            """{"v":1,"blocks":[""" +
                """{"id":"b0","type":"code-block","language":"kotlin","start":true,"text":"val a","spans":[]},""" +
                """{"id":"b1","type":"code-block","language":"kotlin","text":"","spans":[]},""" +
                """{"id":"b2","type":"code-block","start":true,"text":"c","spans":[]},""" +
                """{"id":"b3","type":"code-block","text":"d","spans":[]}]}""",
            codecEncode(document),
        )
    }

    @Test
    fun `code blocks survive a json round trip`() {
        assertEquals(document.blocks, codecDecode(codecEncode(document)).blocks)
    }

    @Test
    fun `a language that is not a string is malformed`() {
        assertFailsWith<MalformedRichTextJsonException> {
            codecDecode("""{"v":1,"blocks":[{"id":"b0","type":"code-block","language":5,"text":"a","spans":[]}]}""")
        }
    }

    @Test
    fun `a code block survives a state json round trip`() {
        val html = "<p>Before</p><pre><code class=\"language-kotlin\">val a\n\n  val b</code></pre>" +
            "<pre><code>c</code></pre><p>After</p>"
        val state = RichTextState().apply { setHtml(html) }

        val json = state.toJson()
        val reloaded = RichTextState().setJson(json)

        assertEquals(html, reloaded.toHtml())
        assertEquals(json, reloaded.toJson())
    }
}
