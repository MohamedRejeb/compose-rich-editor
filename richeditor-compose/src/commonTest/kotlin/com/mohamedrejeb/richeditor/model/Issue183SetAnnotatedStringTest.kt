package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/**
 * Issue 183: an app holding styled text as a Compose [AnnotatedString] had no way to load it
 * into the editor; only plain text, HTML, Markdown and documents could be set.
 *
 * `RichTextState.setAnnotatedString` converts the string into paragraphs: a line feed starts a
 * new paragraph, `SpanStyle` ranges become span styles, `ParagraphStyle` ranges become
 * paragraph styles and `LinkAnnotation.Url` becomes a link. Everything else the string
 * carries is dropped and its text kept.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue183SetAnnotatedStringTest {

    private val bold = SpanStyle(fontWeight = FontWeight.Bold)
    private val italic = SpanStyle(fontStyle = FontStyle.Italic)

    private fun stateOf(annotatedString: AnnotatedString): RichTextState =
        RichTextState().setAnnotatedString(annotatedString)

    private fun documentOfHtml(html: String): RichTextDocument =
        RichTextState().setHtml(html).toRichTextDocument()

    private fun RichTextState.blocks(): List<RichTextBlock> = toRichTextDocument().blocks

    @Test
    fun `plain text loads as one paragraph`() {
        val state = stateOf(AnnotatedString("Hello world"))

        assertEquals(listOf(RichTextBlock("Hello world")), state.blocks())
        assertEquals("Hello world", state.annotatedString.text)
    }

    @Test
    fun `the setter returns the state and replaces the previous content`() {
        val state = RichTextState().setHtml("<ul><li><b>Old</b></li></ul>")

        assertSame(state, state.setAnnotatedString(AnnotatedString("New")))

        assertEquals(listOf(RichTextBlock("New")), state.blocks())
    }

    @Test
    fun `the selection moves to the end and the undo history is cleared`() {
        val state = RichTextState().setHtml("<p>Some old content</p>")
        state.selection = TextRange(2)

        state.setAnnotatedString(AnnotatedString("Hello\nworld"))

        assertEquals(TextRange(11), state.selection)
        assertFalse(state.history.canUndo)
    }

    @Test
    fun `a line feed starts a new paragraph like setText`() {
        val state = stateOf(AnnotatedString("One\nTwo\nThree"))

        assertEquals(
            listOf(RichTextBlock("One"), RichTextBlock("Two"), RichTextBlock("Three")),
            state.blocks(),
        )
        assertEquals(RichTextState().setText("One\nTwo\nThree").toHtml(), state.toHtml())
    }

    @Test
    fun `carriage returns are line breaks too`() {
        val text = buildAnnotatedString {
            append("One\r\n")
            withStyle(bold) { append("Two") }
            append("\rThree")
        }

        assertEquals(
            listOf(
                RichTextBlock("One"),
                RichTextBlock("Two", spans = listOf(RichTextSpanMark.Bold(0..2))),
                RichTextBlock("Three"),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `empty input gives one empty paragraph`() {
        assertEquals(RichTextDocument.empty(), stateOf(AnnotatedString("")).toRichTextDocument())
    }

    @Test
    fun `input of only line feeds gives empty paragraphs`() {
        assertEquals(
            listOf(RichTextBlock(""), RichTextBlock(""), RichTextBlock("")),
            stateOf(AnnotatedString("\n\n")).blocks(),
        )
    }

    @Test
    fun `a trailing line feed gives a trailing empty paragraph`() {
        assertEquals(
            listOf(RichTextBlock("One"), RichTextBlock("")),
            stateOf(AnnotatedString("One\n")).blocks(),
        )
    }

    @Test
    fun `a span style range becomes a span style`() {
        val text = buildAnnotatedString {
            append("Hello ")
            withStyle(bold) { append("world") }
        }

        val state = stateOf(text)

        assertEquals(documentOfHtml("<p>Hello <b>world</b></p>"), state.toRichTextDocument())
        assertEquals(RichTextState().setHtml("<p>Hello <b>world</b></p>").toHtml(), state.toHtml())
        assertEquals("Hello **world**", state.toMarkdown())
    }

    @Test
    fun `overlapping span style ranges merge on the overlap`() {
        val text = AnnotatedString(
            text = "abcdefgh",
            spanStyles = listOf(
                AnnotatedString.Range(bold, 0, 5),
                AnnotatedString.Range(italic, 3, 8),
            ),
        )

        assertEquals(
            setOf(RichTextSpanMark.Bold(0..4), RichTextSpanMark.Italic(3..7)),
            stateOf(text).blocks().single().spans.toSet(),
        )
    }

    @Test
    fun `a nested span style wins over the outer one for the same property`() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append("ab")
                withStyle(SpanStyle(color = Color.Blue)) { append("cd") }
                append("ef")
            }
        }

        val state = stateOf(text)

        assertEquals(Color.Red, state.getSpanStyle(TextRange(0, 2)).color)
        assertEquals(Color.Blue, state.getSpanStyle(TextRange(2, 4)).color)
        assertEquals(FontWeight.Bold, state.getSpanStyle(TextRange(2, 4)).fontWeight)
        assertEquals(Color.Red, state.getSpanStyle(TextRange(4, 6)).color)
    }

    @Test
    fun `overlapping text decorations combine`() {
        val text = AnnotatedString(
            text = "abcd",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 0, 4),
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.LineThrough), 2, 4),
            ),
        )

        assertEquals(
            setOf(RichTextSpanMark.Underline(0..3), RichTextSpanMark.Strikethrough(2..3)),
            stateOf(text).blocks().single().spans.toSet(),
        )
    }

    @Test
    fun `a span style range across a line feed styles both paragraphs`() {
        val text = AnnotatedString(
            text = "One\nTwo",
            spanStyles = listOf(AnnotatedString.Range(bold, 1, 6)),
        )

        assertEquals(
            listOf(
                RichTextBlock("One", spans = listOf(RichTextSpanMark.Bold(1..2))),
                RichTextBlock("Two", spans = listOf(RichTextSpanMark.Bold(0..1))),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `a span style range that ends at a line feed stays in its paragraph`() {
        val text = AnnotatedString(
            text = "One\nTwo",
            spanStyles = listOf(AnnotatedString.Range(bold, 0, 3)),
        )
        val withLineFeed = AnnotatedString(
            text = "One\nTwo",
            spanStyles = listOf(AnnotatedString.Range(bold, 0, 4)),
        )
        val expected = listOf(
            RichTextBlock("One", spans = listOf(RichTextSpanMark.Bold(0..2))),
            RichTextBlock("Two"),
        )

        assertEquals(expected, stateOf(text).blocks())
        assertEquals(expected, stateOf(withLineFeed).blocks())
    }

    @Test
    fun `a paragraph style range styles the paragraphs it covers`() {
        val text = buildAnnotatedString {
            append("One\n")
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) { append("Two") }
            append("\nThree")
        }

        assertEquals(
            listOf(
                RichTextBlock("One"),
                RichTextBlock("Two", textAlign = TextAlign.Center),
                RichTextBlock("Three"),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `a paragraph style range that includes its line feeds adds no empty paragraph`() {
        val text = AnnotatedString(
            text = "One\nTwo\nThree",
            paragraphStyles = listOf(
                AnnotatedString.Range(ParagraphStyle(textAlign = TextAlign.End), 3, 8),
            ),
        )

        assertEquals(
            listOf(
                RichTextBlock("One"),
                RichTextBlock("Two", textAlign = TextAlign.End),
                RichTextBlock("Three"),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `a paragraph style range inside a line splits the line like Compose does`() {
        val text = buildAnnotatedString {
            append("One")
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) { append("Two") }
            append("Three")
        }

        assertEquals(
            listOf(
                RichTextBlock("One"),
                RichTextBlock("Two", textAlign = TextAlign.Center),
                RichTextBlock("Three"),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `an empty line inside a paragraph style range takes the style`() {
        val text = AnnotatedString(
            text = "One\n\nTwo",
            paragraphStyles = listOf(
                AnnotatedString.Range(ParagraphStyle(textAlign = TextAlign.Center), 0, 8),
            ),
        )

        assertEquals(
            listOf(
                RichTextBlock("One", textAlign = TextAlign.Center),
                RichTextBlock("", textAlign = TextAlign.Center),
                RichTextBlock("Two", textAlign = TextAlign.Center),
            ),
            stateOf(text).blocks(),
        )
    }

    @Test
    fun `a url link annotation becomes a link and its styles are dropped`() {
        val text = buildAnnotatedString {
            append("Go to ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://example.com",
                    styles = TextLinkStyles(style = SpanStyle(color = Color.Green)),
                ),
            ) {
                append("the site")
            }
        }

        val state = stateOf(text)

        assertEquals(
            listOf(
                RichTextBlock(
                    "Go to the site",
                    spans = listOf(RichTextSpanMark.Link(6..13, "https://example.com")),
                ),
            ),
            state.blocks(),
        )
        assertEquals(
            RichTextState().setHtml("<p>Go to <a href=\"https://example.com\">the site</a></p>").toHtml(),
            state.toHtml(),
        )
    }

    @Test
    fun `a styled range inside a link keeps both`() {
        val text = buildAnnotatedString {
            withLink(LinkAnnotation.Url("https://example.com")) {
                append("ab")
                withStyle(bold) { append("cd") }
            }
        }

        assertEquals(
            setOf(RichTextSpanMark.Link(0..3, "https://example.com"), RichTextSpanMark.Bold(2..3)),
            stateOf(text).blocks().single().spans.toSet(),
        )
    }

    @Test
    fun `a clickable link annotation is dropped and its text kept`() {
        val text = buildAnnotatedString {
            append("Tap ")
            withLink(LinkAnnotation.Clickable(tag = "action", linkInteractionListener = {})) {
                append("here")
            }
        }

        assertEquals(listOf(RichTextBlock("Tap here")), stateOf(text).blocks())
    }

    @Test
    fun `string annotations are dropped and the styled text kept`() {
        val text = buildAnnotatedString {
            append("I'll be alright as long as there's light from a ")
            withAnnotation("squiggles", annotation = "ignored") {
                withStyle(SpanStyle(color = Color.Blue)) {
                    append("neon moon")
                }
            }
            append(". Very cool.")
        }

        val state = stateOf(text)

        assertEquals("I'll be alright as long as there's light from a neon moon. Very cool.", state.toText())
        assertEquals(Color.Blue, state.getSpanStyle(TextRange(48, 57)).color)
        assertEquals(Color.Unspecified, state.getSpanStyle(TextRange(0, 48)).color)
    }

    @Test
    fun `disallowed styles and links are removed and their text kept`() {
        val text = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
                withStyle(bold) { append("bold ") }
                withStyle(italic) { append("italic ") }
                withLink(LinkAnnotation.Url("https://example.com")) { append("link") }
            }
        }
        val state = RichTextState().apply { config.features = setOf(RichTextFeature.Bold) }

        state.setAnnotatedString(text)

        assertEquals(
            listOf(RichTextBlock("bold italic link", spans = listOf(RichTextSpanMark.Bold(0..4)))),
            state.blocks(),
        )
    }

    @Test
    fun `a single paragraph without lists links or code survives the rendered string round trip`() {
        val html = "<p style=\"text-align: center;\">Hello <b>bold <i>both</i></b> and " +
            "<span style=\"color: #ff0000;\">red</span> <u>under</u></p>"
        val source = RichTextState().setHtml(html)

        val reloaded = stateOf(source.annotatedString)

        // Spans load flat, as from a document, so the HTML matches the document load of the
        // same content; the parsed HTML nests `<i>` inside one `<b>` instead.
        assertEquals(source.toRichTextDocument(), reloaded.toRichTextDocument())
        assertEquals(
            RichTextState().setRichTextDocument(source.toRichTextDocument()).toHtml(),
            reloaded.toHtml(),
        )
    }

    @Test
    fun `paragraphs survive the rendered string round trip but keep their separator space`() {
        val source = RichTextState().setHtml("<p>One</p><p style=\"text-align: center;\">Two</p>")

        val reloaded = stateOf(source.annotatedString)

        assertEquals(
            listOf(RichTextBlock("One "), RichTextBlock("Two", textAlign = TextAlign.Center)),
            reloaded.blocks(),
        )
    }

    @Test
    fun `the rendered string of a list carries its markers as text`() {
        val source = RichTextState().setHtml("<ul><li>Item</li></ul>")

        val reloaded = stateOf(source.annotatedString)

        assertEquals("• Item", reloaded.toText())
        assertNotEquals(source.toRichTextDocument(), reloaded.toRichTextDocument())
    }

    @Test
    fun `the rendered string of a link carries its look but not the link`() {
        val source = RichTextState().setHtml("<p><a href=\"https://example.com\">site</a></p>")

        val reloaded = stateOf(source.annotatedString)

        assertEquals("site", reloaded.toText())
        assertFalse(reloaded.blocks().single().spans.any { it is RichTextSpanMark.Link })
        assertEquals(source.config.linkColor, reloaded.getSpanStyle(TextRange(0, 4)).color)
    }
}
