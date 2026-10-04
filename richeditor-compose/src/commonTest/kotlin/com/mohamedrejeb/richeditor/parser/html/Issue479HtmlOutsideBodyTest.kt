package com.mohamedrejeb.richeditor.parser.html

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Issue 479: HTML content placed before the `<body>` tag was dropped on import.
 *
 * Cause: the parser cleared everything it had collected when it reached `<body>`. That reset
 * was meant to discard the document head, but `head`, `title`, `style` and `script` are
 * already skipped on their own. Fix: the reset is gone, so content before, inside and after
 * `<body>` is all kept.
 */
class Issue479HtmlOutsideBodyTest {

    private fun textOf(html: String): String = RichTextState().apply { setHtml(html) }.toText()

    @Test
    fun `text before the body tag is kept`() {
        assertEquals("before\ninside", textOf("<p>before</p><body><p>inside</p></body>"))
    }

    @Test
    fun `bare text before the body tag is kept`() {
        assertEquals("before\ninside", textOf("before<body><p>inside</p></body>"))
    }

    @Test
    fun `text after the body tag is kept`() {
        assertEquals("inside\nafter", textOf("<body><p>inside</p></body><p>after</p>"))
    }

    @Test
    fun `the document head is still skipped`() {
        val html = "<html><head><title>Title</title><style>p { color: red; }</style>" +
            "<script>var a = 1;</script></head><body><p>inside</p></body></html>"

        assertEquals("inside", textOf(html))
    }

    @Test
    fun `formatting before the body tag is kept`() {
        val state = RichTextState().apply { setHtml("<p><b>bold</b></p><body><p>inside</p></body>") }

        assertEquals("<p><b>bold</b></p><p>inside</p>", state.toHtml())
    }
}
