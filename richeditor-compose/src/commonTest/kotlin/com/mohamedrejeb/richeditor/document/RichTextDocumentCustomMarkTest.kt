package com.mohamedrejeb.richeditor.document

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * An app-defined rich span style carrying an identity payload (slug + axes) and a render
 * resource (fontFamily). Equality covers identity only, mirroring how a consumer carries
 * app-domain font runs that cannot round-trip through Compose types.
 */
@OptIn(ExperimentalRichTextApi::class)
internal class FontRunStyle(
    val slug: String,
    val axes: Map<String, Float> = emptyMap(),
    val fontFamily: FontFamily? = null,
    acceptsEdges: Boolean = true,
) : RichSpanStyle {
    override fun getSpanStyle(config: RichTextConfig): SpanStyle =
        SpanStyle(fontFamily = fontFamily)

    override val acceptsNewTextAtEdges: Boolean = acceptsEdges

    override fun equals(other: Any?): Boolean =
        other is FontRunStyle && slug == other.slug && axes == other.axes

    override fun hashCode(): Int = 31 * slug.hashCode() + axes.hashCode()
}

@OptIn(ExperimentalRichTextApi::class)
class RichTextDocumentCustomMarkTest {

    @Test
    fun `custom rich span style is encoded as a Custom mark carrying the same instance`() {
        val state = RichTextState()
        state.setText("Hello world")
        val style = FontRunStyle(slug = "amiri", axes = mapOf("wght" to 500f))
        state.addRichSpan(style, TextRange(0, 5))

        val block = state.toRichTextDocument().blocks.single()
        val custom = block.spans.filterIsInstance<RichTextSpanMark.Custom>().single()
        assertEquals(0..4, custom.range)
        assertSame(style, custom.style)
    }

    @Test
    fun `adjacent equal custom styles coalesce into one Custom mark`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 5))
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(5, 11))

        val block = state.toRichTextDocument().blocks.single()
        val custom = block.spans.filterIsInstance<RichTextSpanMark.Custom>().single()
        assertEquals(0..10, custom.range)
    }

    @Test
    fun `equal identity instances with different resources coalesce into one Custom mark`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri", fontFamily = null), TextRange(0, 5))
        state.addRichSpan(FontRunStyle(slug = "amiri", fontFamily = FontFamily.Serif), TextRange(5, 11))

        val block = state.toRichTextDocument().blocks.single()
        val custom = block.spans.filterIsInstance<RichTextSpanMark.Custom>().single()
        assertEquals(0..10, custom.range)
        assertEquals(FontRunStyle(slug = "amiri"), custom.style)
    }

    @Test
    fun `custom style round-trips through document encode and load`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "cairo"), TextRange(2, 7))

        val document = state.toRichTextDocument()
        val rebuilt = RichTextState().setRichTextDocument(document)

        assertEquals(document, rebuilt.toRichTextDocument())
        assertEquals(FontRunStyle(slug = "cairo"), rebuilt.getRichSpanStyle(TextRange(3, 4)))
    }

    @Test
    fun `hand-built document with a custom mark loads and reads back unchanged`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello world",
                    spans = listOf(
                        RichTextSpanMark.Custom(range = 2..6, style = FontRunStyle(slug = "cairo")),
                    ),
                ),
            ),
        )

        val state = RichTextState().setRichTextDocument(document)

        assertEquals(document, state.toRichTextDocument())
    }

    @Test
    fun `custom mark does not suppress span style marks on the same range`() {
        val state = RichTextState()
        state.setText("Hello world")
        state.addRichSpan(FontRunStyle(slug = "amiri"), TextRange(0, 5))
        state.selection = TextRange(0, 5)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        val block = state.toRichTextDocument().blocks.single()
        assertTrue(block.spans.any { it is RichTextSpanMark.Bold && it.range == 0..4 })
        assertTrue(block.spans.any { it is RichTextSpanMark.Custom && it.range == 0..4 })
    }

    @Test
    fun `overlapping link and custom marks decode with the link winning`() {
        val document = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello",
                    spans = listOf(
                        RichTextSpanMark.Link(range = 0..4, url = "https://example.com"),
                        RichTextSpanMark.Custom(range = 0..4, style = FontRunStyle(slug = "amiri")),
                    ),
                ),
            ),
        )

        val state = RichTextState().setRichTextDocument(document)

        assertIs<RichSpanStyle.Link>(state.getRichSpanStyle(TextRange(1, 2)))
    }
}
