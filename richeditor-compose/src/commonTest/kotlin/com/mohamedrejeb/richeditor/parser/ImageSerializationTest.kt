package com.mohamedrejeb.richeditor.parser

import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.utils.InlineContentPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for image serialization issues observed in the images
 * sample screen:
 *   - `state.toHtml()` emitted the raw inline-content placeholder char
 *     (`�`, shown HTML-encoded as `&#65533;`) inside the `<img>` tag.
 *   - `state.toMarkdown()` emitted the placeholder char instead of the
 *     proper `![alt](url)` image syntax.
 *
 * Root cause: the generic span-to-HTML/Markdown decoders naively append
 * `richSpan.text` as the tag's inner text. For an Image span, that text
 * is the placeholder char kept around so span textRanges line up with
 * the rendered annotated string (see #466).
 */
@OptIn(ExperimentalRichTextApi::class)
class ImageSerializationTest {

    private fun imageState(
        url: String = "https://example.com/img.png",
        widthSp: Int = 200,
        heightSp: Int = 100,
        contentDescription: String? = null,
    ): RichTextState = RichTextState(
        listOf(
            RichParagraph().also { paragraph ->
                paragraph.children.add(
                    RichSpan(
                        text = InlineContentPlaceholder,
                        paragraph = paragraph,
                        richSpanStyle = RichSpanStyle.Image(
                            model = url,
                            width = widthSp.sp,
                            height = heightSp.sp,
                            contentDescription = contentDescription,
                        ),
                    )
                )
            }
        )
    )

    @Test
    fun toHtml_imageDoesNotEmitPlaceholderCharacter() {
        val state = imageState()
        val html = state.toHtml()

        assertFalse(
            html.contains(InlineContentPlaceholder),
            "toHtml() output must not contain the raw placeholder char. Got: $html",
        )
        assertFalse(
            html.contains("&#65533;"),
            "toHtml() output must not contain the HTML-encoded placeholder char. Got: $html",
        )
        assertFalse(
            html.contains("�"),
            "toHtml() output must not contain U+FFFD. Got: $html",
        )
    }

    @Test
    fun toHtml_imageHasProperImgTag() {
        val state = imageState(url = "https://example.com/img.png", widthSp = 200, heightSp = 100)
        val html = state.toHtml()

        assertTrue(html.contains("<img "), "toHtml() should contain an <img tag. Got: $html")
        assertTrue(
            html.contains("src=\"https://example.com/img.png\""),
            "<img src> attribute should carry the url. Got: $html",
        )
    }

    @Test
    fun toHtml_imageRoundTripPreservesImage() {
        val original = """<p><img src="https://example.com/a.png" width="200" height="100"></p>"""
        val state = RichTextState()
        state.setHtml(original)
        val exported = state.toHtml()

        val reparsed = RichTextState()
        reparsed.setHtml(exported)

        val imageSpan = findImageSpan(reparsed)
        assertEquals("https://example.com/a.png", imageSpan.model)
    }

    @Test
    fun toMarkdown_imageEmitsMarkdownImageSyntax() {
        val state = imageState(
            url = "https://example.com/img.png",
            contentDescription = "My image",
        )
        val markdown = state.toMarkdown()

        assertFalse(
            markdown.contains(InlineContentPlaceholder),
            "toMarkdown() output must not contain the raw placeholder char. Got: $markdown",
        )
        assertTrue(
            markdown.contains("![My image](https://example.com/img.png)"),
            "toMarkdown() should emit `![alt](url)`. Got: $markdown",
        )
    }

    @Test
    fun toMarkdown_imageWithoutAltStillProducesImageSyntax() {
        val state = imageState(
            url = "https://example.com/img.png",
            contentDescription = null,
        )
        val markdown = state.toMarkdown()

        assertFalse(
            markdown.contains(InlineContentPlaceholder),
            "toMarkdown() output must not contain the raw placeholder char. Got: $markdown",
        )
        assertTrue(
            markdown.contains("](https://example.com/img.png)"),
            "toMarkdown() should emit an image-syntax url. Got: $markdown",
        )
        // The alt text part must be a valid (possibly empty) label.
        assertTrue(
            markdown.contains("!["),
            "toMarkdown() should emit `!` prefix for an image. Got: $markdown",
        )
    }

    @Test
    fun toMarkdown_imageRoundTripPreservesUrlAndAlt() {
        val original = "![Picture](https://example.com/img.png)"
        val state = RichTextState()
        state.setMarkdown(original)

        val markdown = state.toMarkdown()
        assertTrue(
            markdown.contains("![Picture](https://example.com/img.png)"),
            "round-trip markdown should preserve image. Got: $markdown",
        )
    }

    private fun findImageSpan(state: RichTextState): RichSpanStyle.Image {
        for (paragraph in state.richParagraphList) {
            for (child in paragraph.children) {
                val result = collectImage(child)
                if (result != null) return result
            }
        }
        error("no Image span found in state")
    }

    private fun collectImage(span: RichSpan): RichSpanStyle.Image? {
        if (span.richSpanStyle is RichSpanStyle.Image) return span.richSpanStyle as RichSpanStyle.Image
        for (c in span.children) {
            val r = collectImage(c)
            if (r != null) return r
        }
        return null
    }
}
