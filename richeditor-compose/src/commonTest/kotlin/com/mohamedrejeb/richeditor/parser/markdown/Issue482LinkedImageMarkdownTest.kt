package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Issue 482: a Markdown image wrapped in a link, the usual README badge
 * (`[![alt](image)](url)`), was imported as a link showing the raw image markup.
 *
 * Cause: the link handler emitted the text between the brackets as is instead of parsing it,
 * so nothing inside a link label was interpreted: no image, and no bold or italic either.
 * Fix: the label is walked like any other inline content, a link keeps its own span when its
 * only child carries another rich span style, and the Markdown writer puts a link's children
 * inside the brackets.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue482LinkedImageMarkdownTest {

    private fun RichTextState.spans(): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        fun visit(span: RichSpan) {
            result += span
            span.children.forEach(::visit)
        }
        richParagraphList.forEach { it.children.forEach(::visit) }
        return result
    }

    private fun RichSpan.enclosingLink(): RichSpanStyle.Link? =
        generateSequence(this) { it.parent }.firstNotNullOfOrNull { it.richSpanStyle as? RichSpanStyle.Link }

    @Test
    fun `an image inside a link is an image that links`() {
        val state = RichTextState().apply {
            setMarkdown("[![Kotlin](https://img.example/kotlin.svg)](https://kotlinlang.org)")
        }

        val imageSpan = assertNotNull(state.spans().firstOrNull { it.richSpanStyle is RichSpanStyle.Image })
        val image = imageSpan.richSpanStyle as RichSpanStyle.Image
        assertEquals("https://img.example/kotlin.svg", image.model)
        assertEquals("Kotlin", image.contentDescription)
        assertEquals("https://kotlinlang.org", imageSpan.enclosingLink()?.url)
        assertFalse(state.toText().contains("!["), "the image markup must not show as text: ${state.toText()}")
    }

    @Test
    fun `two badges on consecutive lines are two linked images`() {
        val state = RichTextState().apply {
            setMarkdown(
                "[![Kotlin](https://img.example/kotlin.svg)](https://kotlinlang.org)\n" +
                    "[![Compose](https://img.example/compose.svg)](https://www.jetbrains.com/lp/compose-multiplatform)"
            )
        }

        val images = state.spans().filter { it.richSpanStyle is RichSpanStyle.Image }
        assertEquals(
            listOf("https://kotlinlang.org", "https://www.jetbrains.com/lp/compose-multiplatform"),
            images.map { it.enclosingLink()?.url },
        )
    }

    @Test
    fun `a linked image survives a markdown round trip`() {
        val markdown = "[![Kotlin](https://img.example/kotlin.svg)](https://kotlinlang.org)"
        val state = RichTextState().apply { setMarkdown(markdown) }

        assertEquals(markdown, state.toMarkdown())
    }

    @Test
    fun `a link with formatted parts in its label survives a markdown round trip`() {
        val markdown = "[**bold** text](https://example.com)"
        val state = RichTextState().apply { setMarkdown(markdown) }

        assertEquals(markdown, state.toMarkdown())
    }

    @Test
    fun `bold inside a link label is applied`() {
        val state = RichTextState().apply { setMarkdown("[**bold** text](https://example.com)") }

        assertEquals("bold text", state.toText())
        val bold = assertNotNull(state.spans().firstOrNull { it.text == "bold" })
        assertEquals(FontWeight.Bold, bold.fullSpanStyle.fontWeight)
        assertEquals("https://example.com", bold.enclosingLink()?.url)
        assertEquals("https://example.com", state.spans().first { it.text.contains("text") }.enclosingLink()?.url)
    }

    @Test
    fun `a link whose whole label is bold or code stays a link`() {
        val bold = RichTextState().apply { setMarkdown("[**bold**](https://example.com)") }
        val boldSpan = bold.spans().first { it.text == "bold" }
        assertEquals(FontWeight.Bold, boldSpan.fullSpanStyle.fontWeight)
        assertEquals("https://example.com", boldSpan.enclosingLink()?.url)

        val code = RichTextState().apply { setMarkdown("[`code`](https://example.com)") }
        val codeSpan = code.spans().first { it.text == "code" }
        assertEquals("https://example.com", codeSpan.enclosingLink()?.url)
        assertNotNull(generateSequence(codeSpan) { it.parent }.firstOrNull { it.richSpanStyle is RichSpanStyle.Code })
    }

    @Test
    fun `a plain link and a plain image are unchanged`() {
        val link = RichTextState().apply { setMarkdown("[label](https://example.com)") }
        assertEquals("label", link.toText())
        assertEquals("https://example.com", link.spans().first { it.text == "label" }.enclosingLink()?.url)

        val image = RichTextState().apply { setMarkdown("![alt](https://img.example/a.png)") }
        val imageStyle = image.spans().firstNotNullOf { it.richSpanStyle as? RichSpanStyle.Image }
        assertEquals("https://img.example/a.png", imageStyle.model)
        assertEquals("alt", imageStyle.contentDescription)
    }
}
